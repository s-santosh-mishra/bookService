package com.servicehub.serv.service;

import com.servicehub.serv.entity.Payment;
import com.servicehub.serv.entity.Users;
import com.servicehub.serv.entity.Worker;
import com.servicehub.serv.entity.WorkerSettlement;
import com.servicehub.serv.entity.WorkerSettlementItem;
import com.servicehub.serv.entity.WorkerSettlementReversal;
import com.servicehub.serv.dto.EligiblePaymentDto;
import com.servicehub.serv.dto.AdminWorkerSettlementSummaryDto;
import com.servicehub.serv.dto.WorkerEligiblePaymentsDto;
import com.servicehub.serv.dto.WorkerSettlementBatchDto;
import com.servicehub.serv.dto.WorkerSettlementBatchPaymentsDto;
import com.servicehub.serv.dto.WorkerSettlementBatchesDto;
import com.servicehub.serv.dto.WorkerSettlementDetailsDto;
import com.servicehub.serv.dto.WorkerSettlementHistoryDto;
import com.servicehub.serv.dto.WorkerSettlementItemDto;
import com.servicehub.serv.dto.WorkerSettlementResponseDto;
import com.servicehub.serv.dto.WorkerSettlementReversalDto;
import com.servicehub.serv.dto.WorkerSettlementSummaryDto;
import com.servicehub.serv.enums.SettlementStatus;
import com.servicehub.serv.enums.SettlementBatchStatus;
import com.servicehub.serv.enums.UserRole;
import com.servicehub.serv.exception.ResourceNotFoundException;
import com.servicehub.serv.exception.SettlementConflictException;
import com.servicehub.serv.repository.PaymentRepository;
import com.servicehub.serv.repository.UsersRepository;
import com.servicehub.serv.repository.WorkerRepository;
import com.servicehub.serv.repository.WorkerSettlementItemRepository;
import com.servicehub.serv.repository.WorkerSettlementRepository;
import com.servicehub.serv.repository.WorkerSettlementReversalRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.UUID;

@Service
public class WorkerSettlementService {

    private final WorkerRepository workerRepository;
    private final UsersRepository usersRepository;
    private final PaymentRepository paymentRepository;
    private final WorkerSettlementRepository workerSettlementRepository;
    private final WorkerSettlementItemRepository workerSettlementItemRepository;
    private final WorkerSettlementReversalRepository workerSettlementReversalRepository;
    private final SettlementBusinessDateCalculator businessDateCalculator;

    public WorkerSettlementService(
            WorkerRepository workerRepository,
            UsersRepository usersRepository,
            PaymentRepository paymentRepository,
            WorkerSettlementRepository workerSettlementRepository,
            WorkerSettlementItemRepository workerSettlementItemRepository,
            WorkerSettlementReversalRepository workerSettlementReversalRepository,
            SettlementBusinessDateCalculator businessDateCalculator) {
        this.workerRepository = workerRepository;
        this.usersRepository = usersRepository;
        this.paymentRepository = paymentRepository;
        this.workerSettlementRepository = workerSettlementRepository;
        this.workerSettlementItemRepository = workerSettlementItemRepository;
        this.workerSettlementReversalRepository = workerSettlementReversalRepository;
        this.businessDateCalculator = businessDateCalculator;
    }

    @Transactional
    public WorkerSettlement createSettlement(
            UUID workerId,
            List<UUID> paymentIds,
            UUID adminId) {
        return createSettlement(workerId, paymentIds, null, adminId);
    }

    @Transactional
    public WorkerSettlement createSettlement(
            UUID workerId,
            List<UUID> paymentIds,
            LocalDate expectedBusinessDate,
            UUID adminId) {
        if (paymentIds == null || paymentIds.isEmpty()) {
            throw new IllegalArgumentException(
                    "At least one payment must be selected.");
        }
        Set<UUID> selectedPaymentIds = new HashSet<>(paymentIds);
        if (selectedPaymentIds.contains(null)) {
            throw new IllegalArgumentException(
                    "Selected payment IDs cannot be null.");
        }
        if (selectedPaymentIds.size() != paymentIds.size()) {
            throw new IllegalArgumentException(
                    "Duplicate payment IDs are not allowed.");
        }

        Worker worker = workerRepository.findByUserIdForUpdate(workerId)
                .orElseThrow(() -> new ResourceNotFoundException("Worker not found."));

        validateBankDetails(worker);

        Users admin = usersRepository.findById(adminId)
                .orElseThrow(() -> new ResourceNotFoundException("Admin not found."));
        if (admin.getCredentials() == null
                || admin.getCredentials().getRole() != UserRole.ADMIN) {
            throw new IllegalArgumentException("Settlement creator must be an admin.");
        }

        List<Payment> selectedPayments =
                paymentRepository.findEligiblePaymentsForWorkerSettlementForUpdate(
                        workerId,
                        selectedPaymentIds);
        Set<UUID> lockedPaymentIds = selectedPayments.stream()
                .map(payment -> payment.getPaymentId())
                .collect(Collectors.toSet());
        if (!lockedPaymentIds.containsAll(selectedPaymentIds)) {
            throw new SettlementConflictException(
                    "One or more selected payments are no longer eligible.");
        }

        Set<LocalDate> businessDates = selectedPayments.stream()
                .map(payment -> businessDateCalculator.businessDate(payment.getPaidAt()))
                .collect(Collectors.toSet());
        if (businessDates.size() != 1) {
            throw new SettlementConflictException(
                    "A settlement can contain payments from only one business date.");
        }
        LocalDate businessDate = businessDates.iterator().next();
        if (expectedBusinessDate != null
                && !expectedBusinessDate.equals(businessDate)) {
            throw new SettlementConflictException(
                    "The selected payments do not belong to the requested business date.");
        }
        ZonedDateTime decisionTime = businessDateCalculator.now();
        if (!businessDateCalculator.isClosed(businessDate, decisionTime)) {
            throw new SettlementConflictException(
                    "This business-date batch is still open and cannot be settled before 8:00 PM IST.");
        }

        BigDecimal totalAmount = selectedPayments.stream()
                .map(payment -> payment.getAmount())
                .reduce(BigDecimal.ZERO, (total, amount) -> total.add(amount));

        WorkerSettlement settlement = new WorkerSettlement();
        settlement.setWorker(worker);
        settlement.setBusinessDate(businessDate);
        settlement.setAmount(totalAmount);
        settlement.setStatus(SettlementStatus.PENDING);
        settlement.setBankAccountHolderName(worker.getBankAccountHolderName());
        settlement.setBankAccountNumber(worker.getBankAccountNumber());
        settlement.setBankIfscCode(worker.getBankIfscCode());
        settlement.setBankName(worker.getBankName());
        settlement.setSettledBy(admin);

        WorkerSettlement savedSettlement =
                workerSettlementRepository.save(settlement);

        List<WorkerSettlementItem> items = new ArrayList<>(selectedPayments.size());
        for (Payment payment : selectedPayments) {
            WorkerSettlementItem item = new WorkerSettlementItem();
            item.setSettlement(savedSettlement);
            item.setPayment(payment);
            item.setAmount(payment.getAmount());
            savedSettlement.getItems().add(item);
            items.add(item);
        }
        workerSettlementItemRepository.saveAll(items);

        return savedSettlement;
    }

    @Transactional
    public WorkerSettlement markSettlementProcessing(UUID settlementId, UUID adminId) {
        WorkerSettlement settlement = getSettlementForUpdate(settlementId);
        validateAdmin(adminId);
        requireStatus(settlement, SettlementStatus.PENDING,
                "Only pending settlements can be marked as processing.");

        settlement.setStatus(SettlementStatus.PROCESSING);
        return workerSettlementRepository.save(settlement);
    }

    @Transactional
    public WorkerSettlementReversalDto recordConfirmedReversal(
            UUID settlementId,
            String bankReversalReference,
            String reversalReason,
            Boolean verifiedWithBankRecords,
            UUID adminId) {
        if (!Boolean.TRUE.equals(verifiedWithBankRecords)) {
            throw new IllegalArgumentException(
                    "Confirm the bank reversal was verified against external bank records.");
        }
        if (bankReversalReference == null || bankReversalReference.isBlank()) {
            throw new IllegalArgumentException(
                    "Bank reversal reference cannot be blank.");
        }
        String normalizedReference = bankReversalReference.strip();
        if (normalizedReference.length() > 100) {
            throw new IllegalArgumentException(
                    "Bank reversal reference cannot exceed 100 characters.");
        }
        if (reversalReason == null || reversalReason.isBlank()) {
            throw new IllegalArgumentException("Reversal reason cannot be blank.");
        }
        String normalizedReason = reversalReason.strip();
        if (normalizedReason.length() > 1000) {
            throw new IllegalArgumentException(
                    "Reversal reason cannot exceed 1000 characters.");
        }

        WorkerSettlement settlementSnapshot = workerSettlementRepository
                .findById(settlementId)
                .orElseThrow(() -> new ResourceNotFoundException("Settlement not found."));
        UUID workerId = settlementSnapshot.getWorker().getUserId();
        workerRepository.findByUserIdForUpdate(workerId)
                .orElseThrow(() -> new ResourceNotFoundException("Worker not found."));

        WorkerSettlement settlement = getSettlementForUpdate(settlementId);
        Users admin = getAdmin(adminId);
        requireStatus(settlement, SettlementStatus.SETTLED,
                "Only settled settlements can have a verified bank reversal recorded.");
        if (workerSettlementReversalRepository
                .existsBySettlement_SettlementId(settlementId)) {
            throw new SettlementConflictException(
                    "A verified reversal is already recorded for this settlement.");
        }

        WorkerSettlementReversal reversal =
                new WorkerSettlementReversal(
                        settlement,
                        normalizedReference,
                        normalizedReason,
                        LocalDateTime.now(),
                        admin);
        return toReversalDto(
                workerSettlementReversalRepository.save(reversal));
    }

    @Transactional
    public WorkerSettlement markSettlementSettled(
            UUID settlementId,
            String paymentReference,
            UUID adminId) {
        WorkerSettlement settlement = getSettlementForUpdate(settlementId);
        validateAdmin(adminId);
        requireStatus(settlement, SettlementStatus.PROCESSING,
                "Only processing settlements can be marked as settled.");

        if (paymentReference == null || paymentReference.isBlank()) {
            throw new IllegalArgumentException("Payment reference cannot be blank.");
        }
        String normalizedPaymentReference = paymentReference.strip();
        if (normalizedPaymentReference.length() > 100) {
            throw new IllegalArgumentException(
                    "Payment reference cannot exceed 100 characters.");
        }

        settlement.setStatus(SettlementStatus.SETTLED);
        settlement.setPaymentReference(normalizedPaymentReference);
        settlement.setSettledAt(LocalDateTime.now());
        return workerSettlementRepository.save(settlement);
    }

    @Transactional
    public WorkerSettlement markSettlementFailed(
            UUID settlementId,
            String failureReason,
            UUID adminId) {
        WorkerSettlement settlement = getSettlementForUpdate(settlementId);
        validateAdmin(adminId);
        requireStatus(settlement, SettlementStatus.PROCESSING,
                "Only processing settlements can be marked as failed.");

        if (failureReason == null || failureReason.isBlank()) {
            throw new IllegalArgumentException("Failure reason cannot be blank.");
        }
        String normalizedFailureReason = failureReason.strip();
        if (normalizedFailureReason.length() > 1000) {
            throw new IllegalArgumentException(
                    "Failure reason cannot exceed 1000 characters.");
        }

        settlement.setStatus(SettlementStatus.FAILED);
        settlement.setFailureReason(normalizedFailureReason);
        return workerSettlementRepository.save(settlement);
    }

    @Transactional(readOnly = true)
    public List<WorkerSettlementSummaryDto> getWorkersAwaitingSettlement() {
        return paymentRepository.findWorkersWithEligibleSettlementPayments();
    }

    @Transactional(readOnly = true)
    public WorkerSettlementBatchesDto getSettlementBatches() {
        List<Payment> batchPayments = paymentRepository.findSettlementBatchPayments();
        Set<UUID> eligiblePaymentIds = paymentRepository
                .findEligiblePaymentsForSettlement()
                .stream()
                .map(payment -> payment.getPaymentId())
                .collect(Collectors.toSet());
        List<WorkerSettlementItem> items =
                workerSettlementItemRepository.findAllForBatchHistory();
        List<WorkerSettlement> settlements =
                workerSettlementRepository.findAllForBatchDashboard();
        Map<UUID, List<WorkerSettlementItem>> itemsByPayment =
                groupItemsByPayment(items);
        Map<UUID, List<WorkerSettlementItem>> itemsBySettlement =
                groupItemsBySettlement(items);
        Map<LocalDate, BatchAccumulator> accumulators = new HashMap<>();

        for (Payment payment : batchPayments) {
            LocalDate businessDate =
                    businessDateCalculator.businessDate(payment.getPaidAt());
            accumulators.computeIfAbsent(businessDate, BatchAccumulator::new)
                    .addPayment(payment);
        }

        long unclassifiedSettlementCount = 0;
        for (WorkerSettlement settlement : settlements) {
            LocalDate businessDate = resolveBusinessDate(
                    settlement,
                    itemsBySettlement.getOrDefault(
                            settlement.getSettlementId(),
                            List.of()));
            if (businessDate == null) {
                unclassifiedSettlementCount++;
                continue;
            }
            accumulators.computeIfAbsent(businessDate, BatchAccumulator::new)
                    .addSettlement(settlement);
        }

        ZonedDateTime decisionTime = businessDateCalculator.now();
        List<WorkerSettlementBatchDto> batches = accumulators.values().stream()
                .map(accumulator -> toBatchDto(
                        accumulator,
                        eligiblePaymentIds,
                        itemsByPayment,
                        decisionTime))
                .sorted(Comparator.comparing(
                        (WorkerSettlementBatchDto batch) -> batch.getBusinessDate())
                        .reversed())
                .toList();

        BigDecimal unclassifiedAmount =
                paymentRepository.sumUndatedSuccessfulOnlinePaymentAmount();
        if (unclassifiedAmount == null) {
            unclassifiedAmount = BigDecimal.ZERO;
        }
        return new WorkerSettlementBatchesDto(
                batches,
                paymentRepository.countUndatedSuccessfulOnlinePayments(),
                unclassifiedAmount,
                unclassifiedSettlementCount);
    }

    @Transactional(readOnly = true)
    public WorkerSettlementBatchPaymentsDto getSettlementBatchPayments(
            LocalDate businessDate) {
        WorkerSettlementBatchDto batch = getSettlementBatches().getBatches().stream()
                .filter(candidate -> businessDate.equals(candidate.getBusinessDate()))
                .findFirst()
                .orElse(null);
        boolean closed = batch != null
                ? batch.isClosedForSettlement()
                : businessDateCalculator.isClosed(
                        businessDate,
                        businessDateCalculator.now());
        SettlementBatchStatus batchStatus = batch == null
                ? (closed
                    ? SettlementBatchStatus.CLOSED_READY_FOR_SETTLEMENT
                    : SettlementBatchStatus.OPEN_ACCUMULATING)
                : batch.getStatus();

        Set<UUID> eligiblePaymentIds = paymentRepository
                .findEligiblePaymentsForSettlement()
                .stream()
                .map(payment -> payment.getPaymentId())
                .collect(Collectors.toSet());
        Map<UUID, List<WorkerSettlementItem>> itemsByPayment =
                groupItemsByPayment(
                        workerSettlementItemRepository.findAllForBatchHistory());
        List<WorkerSettlementBatchPaymentsDto.PaymentRow> paymentRows =
                paymentRepository.findSettlementBatchPayments().stream()
                        .filter(payment -> businessDate.equals(
                                businessDateCalculator.businessDate(payment.getPaidAt())))
                        .map(payment -> toBatchPaymentRow(
                                payment,
                                eligiblePaymentIds.contains(payment.getPaymentId()),
                                itemsByPayment.getOrDefault(
                                        payment.getPaymentId(),
                                        List.of())))
                        .toList();

        return new WorkerSettlementBatchPaymentsDto(
                businessDate,
                batchStatus,
                closed,
                paymentRows);
    }

    @Transactional(readOnly = true)
    public WorkerEligiblePaymentsDto getEligiblePaymentsForWorker(UUID workerId) {
        Worker worker = workerRepository.findById(workerId)
                .orElseThrow(() -> new ResourceNotFoundException("Worker not found."));
        List<Payment> eligiblePayments =
                paymentRepository.findEligiblePaymentsForWorker(workerId);

        List<EligiblePaymentDto> paymentDtos = eligiblePayments.stream()
                .map(payment -> new EligiblePaymentDto(
                        payment.getPaymentId(),
                        payment.getBooking().getBookingId(),
                        payment.getAmount(),
                        payment.getPaidAt(),
                        payment.getCreatedAt(),
                        payment.getPaymentMethod(),
                        payment.getStatus(),
                        payment.getBooking().getStatus()))
                .toList();
        BigDecimal eligibleAmount = eligiblePayments.stream()
                .map(payment -> payment.getAmount())
                .reduce(BigDecimal.ZERO, (total, amount) -> total.add(amount));

        return new WorkerEligiblePaymentsDto(
                workerId,
                worker.getUser().getFullName(),
                (long) paymentDtos.size(),
                eligibleAmount,
                paymentDtos);
    }

    @Transactional(readOnly = true)
    public List<WorkerSettlementHistoryDto> getSettlementHistory(
            UUID workerId,
            SettlementStatus status) {
        return workerSettlementRepository.findSettlementHistory(workerId, status);
    }

    @Transactional(readOnly = true)
    public AdminWorkerSettlementSummaryDto getAdminSummary() {
        List<WorkerSettlementBatchDto> closedBatches =
                getSettlementBatches().getBatches().stream()
                        .filter(batch -> batch.isClosedForSettlement())
                        .toList();
        Set<UUID> eligibleWorkerIds = closedBatches.stream()
                .flatMap(batch -> batch.getWorkers().stream())
                .map(worker -> worker.getWorkerId())
                .collect(Collectors.toSet());
        BigDecimal totalEligibleAmount = closedBatches.stream()
                .map(batch -> batch.getRemainingEligibleAmount())
                .reduce(BigDecimal.ZERO, (total, amount) -> total.add(amount));
        BigDecimal successfullySettledAmount = closedBatches.stream()
                .map(batch -> batch.getSuccessfullySettledAmount())
                .reduce(BigDecimal.ZERO, (total, amount) -> total.add(amount));

        return new AdminWorkerSettlementSummaryDto(
                eligibleWorkerIds.size(),
                totalEligibleAmount,
                workerSettlementRepository.countByStatus(SettlementStatus.PENDING),
                workerSettlementRepository.countByStatus(SettlementStatus.PROCESSING),
                successfullySettledAmount,
                workerSettlementRepository.countByStatus(SettlementStatus.FAILED));
    }

    @Transactional(readOnly = true)
    public WorkerSettlementDetailsDto getSettlementDetails(UUID settlementId) {
        WorkerSettlement settlement = workerSettlementRepository
                .findDetailsBySettlementId(settlementId)
                .orElseThrow(() -> new ResourceNotFoundException("Settlement not found."));

        List<WorkerSettlementItemDto> paymentItems = settlement.getItems().stream()
                .map(item -> {
                    Payment payment = item.getPayment();
                    return new WorkerSettlementItemDto(
                            payment.getPaymentId(),
                            payment.getBooking().getBookingId(),
                            item.getAmount(),
                            payment.getPaidAt(),
                            payment.getCreatedAt(),
                            payment.getPaymentMethod(),
                            payment.getStatus());
                })
                .toList();

        BigDecimal paymentItemsTotal = paymentItems.stream()
                .map(item -> item.getAmount())
                .reduce(BigDecimal.ZERO, (total, amount) -> total.add(amount));

        return new WorkerSettlementDetailsDto(
                settlement.getSettlementId(),
                settlement.getBusinessDate(),
                settlement.getWorker().getUserId(),
                settlement.getWorker().getUser().getFullName(),
                settlement.getAmount(),
                paymentItemsTotal,
                settlement.getAmount().compareTo(paymentItemsTotal) == 0,
                settlement.getStatus(),
                settlement.getPaymentReference(),
                settlement.getCreatedAt(),
                settlement.getSettledAt(),
                settlement.getFailureReason(),
                settlement.getBankAccountHolderName(),
                settlement.getBankAccountNumber(),
                settlement.getBankIfscCode(),
                settlement.getBankName(),
                settlement.getSettledBy().getUserId(),
                settlement.getSettledBy().getFullName(),
                paymentItems,
                toReversalDto(settlement.getReversal()));
    }

    @Transactional
    public WorkerSettlementResponseDto createSettlementResponse(
            UUID workerId,
            List<UUID> paymentIds,
            UUID adminId) {
        return toResponseDto(createSettlement(workerId, paymentIds, adminId));
    }

    @Transactional
    public WorkerSettlementResponseDto createSettlementResponse(
            UUID workerId,
            List<UUID> paymentIds,
            LocalDate expectedBusinessDate,
            UUID adminId) {
        return toResponseDto(
                createSettlement(workerId, paymentIds, expectedBusinessDate, adminId));
    }

    @Transactional
    public WorkerSettlementResponseDto markSettlementProcessingResponse(
            UUID settlementId,
            UUID adminId) {
        return toResponseDto(markSettlementProcessing(settlementId, adminId));
    }

    @Transactional
    public WorkerSettlementResponseDto markSettlementSettledResponse(
            UUID settlementId,
            String paymentReference,
            UUID adminId) {
        return toResponseDto(
                markSettlementSettled(settlementId, paymentReference, adminId));
    }

    @Transactional
    public WorkerSettlementResponseDto markSettlementFailedResponse(
            UUID settlementId,
            String failureReason,
            UUID adminId) {
        return toResponseDto(
                markSettlementFailed(settlementId, failureReason, adminId));
    }

    private WorkerSettlementBatchDto toBatchDto(
            BatchAccumulator batch,
            Set<UUID> eligiblePaymentIds,
            Map<UUID, List<WorkerSettlementItem>> itemsByPayment,
            ZonedDateTime decisionTime) {
        BigDecimal batchAmount = BigDecimal.ZERO;
        BigDecimal eligibleAmount = BigDecimal.ZERO;
        BigDecimal settledAmount = BigDecimal.ZERO;
        BigDecimal failedAmount = BigDecimal.ZERO;
        BigDecimal processingAmount = BigDecimal.ZERO;
        BigDecimal pendingAmount = BigDecimal.ZERO;
        long eligibleCount = 0;
        long settledCount = 0;
        long failedCount = 0;
        Map<UUID, WorkerAccumulator> workers = new HashMap<>();

        for (Payment payment : batch.payments.values()) {
            BigDecimal amount = payment.getAmount();
            batchAmount = batchAmount.add(amount);
            List<WorkerSettlementItem> attempts = itemsByPayment.getOrDefault(
                    payment.getPaymentId(),
                    List.of());
            WorkerSettlementItem blockingAttempt = currentBlockingAttempt(attempts);
            boolean eligible = eligiblePaymentIds.contains(payment.getPaymentId());
            if (eligible) {
                eligibleCount++;
                eligibleAmount = eligibleAmount.add(amount);
                Worker worker = payment.getBooking().getWorker();
                workers.computeIfAbsent(
                        worker.getUserId(),
                        ignored -> new WorkerAccumulator(worker))
                        .add(amount);
            }

            if (blockingAttempt != null) {
                SettlementStatus status =
                        blockingAttempt.getSettlement().getStatus();
                if (status == SettlementStatus.SETTLED) {
                    settledCount++;
                    settledAmount = settledAmount.add(amount);
                } else if (status == SettlementStatus.PROCESSING) {
                    processingAmount = processingAmount.add(amount);
                } else if (status == SettlementStatus.PENDING) {
                    pendingAmount = pendingAmount.add(amount);
                }
            } else if (eligible && latestAttempt(attempts)
                    .map(item -> item.getSettlement().getStatus()
                            == SettlementStatus.FAILED)
                    .orElse(false)) {
                failedCount++;
                failedAmount = failedAmount.add(amount);
            }
        }

        boolean closed = businessDateCalculator.isClosed(
                batch.businessDate,
                decisionTime);
        SettlementBatchStatus status;
        if (!closed) {
            status = SettlementBatchStatus.OPEN_ACCUMULATING;
        } else if (!batch.payments.isEmpty()
                && settledCount == batch.payments.size()) {
            status = SettlementBatchStatus.FULLY_SETTLED;
        } else if (eligibleCount > 0 && failedCount == eligibleCount) {
            status = SettlementBatchStatus.FAILED_RETRY_AVAILABLE;
        } else if (!batch.settlementIds.isEmpty()) {
            status = SettlementBatchStatus.PARTIALLY_PROCESSED;
        } else {
            status = SettlementBatchStatus.CLOSED_READY_FOR_SETTLEMENT;
        }

        List<WorkerSettlementBatchDto.WorkerSummary> workerSummaries =
                workers.values().stream()
                        .map(accumulator -> accumulator.toDto())
                        .sorted(Comparator.comparing(
                                worker -> worker.getWorkerName(),
                                Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)))
                        .toList();
        return new WorkerSettlementBatchDto(
                batch.businessDate,
                status,
                closed,
                batch.payments.size(),
                batchAmount,
                eligibleCount,
                eligibleAmount,
                workerSummaries.size(),
                batch.settlementIds.size(),
                settledAmount,
                failedAmount,
                processingAmount,
                pendingAmount,
                eligibleCount,
                eligibleAmount,
                workerSummaries);
    }

    private WorkerSettlementBatchPaymentsDto.PaymentRow toBatchPaymentRow(
            Payment payment,
            boolean eligible,
            List<WorkerSettlementItem> attempts) {
        List<WorkerSettlementBatchPaymentsDto.SettlementAttempt> attemptDtos =
                attempts.stream()
                        .sorted(settlementAttemptOrder())
                        .map(item -> {
                            WorkerSettlement settlement = item.getSettlement();
                            WorkerSettlementReversal reversal =
                                    settlement.getReversal();
                            return new WorkerSettlementBatchPaymentsDto.SettlementAttempt(
                                    settlement.getSettlementId(),
                                    settlement.getBusinessDate(),
                                    settlement.getStatus(),
                                    settlement.getAmount(),
                                    settlement.getFailureReason(),
                                    settlement.getPaymentReference(),
                                    settlement.getCreatedAt(),
                                    settlement.getSettledAt(),
                                    reversal == null
                                            ? null
                                            : reversal.getBankReversalReference(),
                                    reversal == null ? null : reversal.getReversalReason(),
                                    reversal == null ? null : reversal.getVerifiedAt(),
                                    reversal == null
                                            ? null
                                            : reversal.getVerifiedBy().getFullName());
                        })
                        .toList();
        WorkerSettlementItem blockingAttempt = currentBlockingAttempt(attempts);
        Worker worker = payment.getBooking().getWorker();
        return new WorkerSettlementBatchPaymentsDto.PaymentRow(
                payment.getPaymentId(),
                worker.getUserId(),
                worker.getUser().getFullName(),
                payment.getBooking().getBookingId(),
                payment.getBooking().getStatus(),
                payment.getPaidAt(),
                payment.getCreatedAt(),
                payment.getAmount(),
                payment.getPaymentMethod(),
                payment.getStatus(),
                eligible,
                blockingAttempt == null
                        ? null
                        : blockingAttempt.getSettlement().getSettlementId(),
                attemptDtos);
    }

    private Map<UUID, List<WorkerSettlementItem>> groupItemsByPayment(
            List<WorkerSettlementItem> items) {
        return items.stream().collect(Collectors.groupingBy(
                item -> item.getPayment().getPaymentId()));
    }

    private Map<UUID, List<WorkerSettlementItem>> groupItemsBySettlement(
            List<WorkerSettlementItem> items) {
        return items.stream().collect(Collectors.groupingBy(
                item -> item.getSettlement().getSettlementId()));
    }

    private LocalDate resolveBusinessDate(
            WorkerSettlement settlement,
            List<WorkerSettlementItem> items) {
        if (settlement.getBusinessDate() != null) {
            return settlement.getBusinessDate();
        }
        if (items.isEmpty()
                || items.stream().anyMatch(item -> item.getPayment().getPaidAt() == null)) {
            return null;
        }
        Set<LocalDate> businessDates = items.stream()
                .map(item -> businessDateCalculator.businessDate(
                        item.getPayment().getPaidAt()))
                .collect(Collectors.toSet());
        return businessDates.size() == 1
                ? businessDates.iterator().next()
                : null;
    }

    private WorkerSettlementItem currentBlockingAttempt(
            List<WorkerSettlementItem> attempts) {
        return attempts.stream()
                .filter(item -> {
                    WorkerSettlement settlement = item.getSettlement();
                    return settlement.getStatus() == SettlementStatus.PENDING
                            || settlement.getStatus() == SettlementStatus.PROCESSING
                            || (settlement.getStatus() == SettlementStatus.SETTLED
                                && settlement.getReversal() == null);
                })
                .max(settlementAttemptOrder())
                .orElse(null);
    }

    private java.util.Optional<WorkerSettlementItem> latestAttempt(
            List<WorkerSettlementItem> attempts) {
        return attempts.stream().max(settlementAttemptOrder());
    }

    private Comparator<WorkerSettlementItem> settlementAttemptOrder() {
        return Comparator
                .comparing(
                        (WorkerSettlementItem item) ->
                                item.getSettlement().getCreatedAt(),
                        Comparator.nullsFirst(Comparator.naturalOrder()))
                .thenComparing(
                        item -> item.getSettlement().getSettlementId());
    }

    private WorkerSettlementResponseDto toResponseDto(WorkerSettlement settlement) {
        return new WorkerSettlementResponseDto(
                settlement.getSettlementId(),
                settlement.getBusinessDate(),
                settlement.getWorker().getUserId(),
                settlement.getWorker().getUser().getFullName(),
                settlement.getAmount(),
                settlement.getStatus(),
                settlement.getItems().size(),
                settlement.getBankAccountHolderName(),
                settlement.getBankAccountNumber(),
                settlement.getBankIfscCode(),
                settlement.getBankName(),
                settlement.getPaymentReference(),
                settlement.getFailureReason(),
                settlement.getCreatedAt(),
                settlement.getSettledAt());
    }

    private static class BatchAccumulator {

        private final LocalDate businessDate;
        private final Map<UUID, Payment> payments = new HashMap<>();
        private final Set<UUID> settlementIds = new HashSet<>();

        private BatchAccumulator(LocalDate businessDate) {
            this.businessDate = businessDate;
        }

        private void addPayment(Payment payment) {
            payments.put(payment.getPaymentId(), payment);
        }

        private void addSettlement(WorkerSettlement settlement) {
            settlementIds.add(settlement.getSettlementId());
        }
    }

    private static class WorkerAccumulator {

        private final Worker worker;
        private long eligiblePaymentCount;
        private BigDecimal eligibleAmount = BigDecimal.ZERO;

        private WorkerAccumulator(Worker worker) {
            this.worker = worker;
        }

        private void add(BigDecimal amount) {
            eligiblePaymentCount++;
            eligibleAmount = eligibleAmount.add(amount);
        }

        private WorkerSettlementBatchDto.WorkerSummary toDto() {
            return new WorkerSettlementBatchDto.WorkerSummary(
                    worker.getUserId(),
                    worker.getUser().getFullName(),
                    worker.getBankAccountHolderName(),
                    com.servicehub.serv.dto.BankAccountMasker.mask(
                            worker.getBankAccountNumber()),
                    worker.getBankIfscCode(),
                    worker.getBankName(),
                    eligiblePaymentCount,
                    eligibleAmount);
        }
    }

    private WorkerSettlement getSettlementForUpdate(UUID settlementId) {
        return workerSettlementRepository.findBySettlementIdForUpdate(settlementId)
                .orElseThrow(() -> new ResourceNotFoundException("Settlement not found."));
    }

    private void validateAdmin(UUID adminId) {
        getAdmin(adminId);
    }

    private Users getAdmin(UUID adminId) {
        Users admin = usersRepository.findById(adminId)
                .orElseThrow(() -> new ResourceNotFoundException("Admin not found."));
        if (admin.getCredentials() == null
                || admin.getCredentials().getRole() != UserRole.ADMIN) {
            throw new IllegalArgumentException("Settlement updates are restricted to admins.");
        }
        return admin;
    }

    private WorkerSettlementReversalDto toReversalDto(
            WorkerSettlementReversal reversal) {
        if (reversal == null) {
            return null;
        }
        Users verifier = reversal.getVerifiedBy();
        return new WorkerSettlementReversalDto(
                reversal.getBankReversalReference(),
                reversal.getReversalReason(),
                reversal.getVerifiedAt(),
                verifier.getUserId(),
                verifier.getFullName());
    }

    private void requireStatus(
            WorkerSettlement settlement,
            SettlementStatus expectedStatus,
            String errorMessage) {
        if (settlement.getStatus() != expectedStatus) {
                throw new SettlementConflictException(errorMessage);
        }
    }

    private void validateBankDetails(Worker worker) {
        if (isBlank(worker.getBankAccountHolderName())
                || isBlank(worker.getBankAccountNumber())
                || isBlank(worker.getBankIfscCode())
                || isBlank(worker.getBankName())) {
            throw new IllegalStateException(
                    "Worker must have complete bank details before settlement.");
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
