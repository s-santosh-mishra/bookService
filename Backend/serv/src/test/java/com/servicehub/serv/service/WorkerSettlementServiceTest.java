package com.servicehub.serv.service;

import com.servicehub.serv.dto.WorkerSettlementBatchDto;
import com.servicehub.serv.dto.WorkerSettlementReversalDto;
import com.servicehub.serv.entity.Credentials;
import com.servicehub.serv.entity.Payment;
import com.servicehub.serv.entity.Users;
import com.servicehub.serv.entity.Worker;
import com.servicehub.serv.entity.WorkerSettlement;
import com.servicehub.serv.entity.WorkerSettlementItem;
import com.servicehub.serv.entity.WorkerSettlementReversal;
import com.servicehub.serv.entity.Booking;
import com.servicehub.serv.enums.BookingStatus;
import com.servicehub.serv.enums.PaymentMethod;
import com.servicehub.serv.enums.PaymentStatus;
import com.servicehub.serv.enums.SettlementStatus;
import com.servicehub.serv.enums.SettlementBatchStatus;
import com.servicehub.serv.enums.UserRole;
import com.servicehub.serv.exception.SettlementConflictException;
import com.servicehub.serv.repository.PaymentRepository;
import com.servicehub.serv.repository.UsersRepository;
import com.servicehub.serv.repository.WorkerRepository;
import com.servicehub.serv.repository.WorkerSettlementItemRepository;
import com.servicehub.serv.repository.WorkerSettlementRepository;
import com.servicehub.serv.repository.WorkerSettlementReversalRepository;
import jakarta.persistence.LockModeType;
import org.junit.jupiter.api.Test;
import org.springframework.data.jpa.repository.Lock;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class WorkerSettlementServiceTest {

    @Test
    void createsSettlementForOnlySelectedEligiblePayments() {
        UUID workerId = UUID.randomUUID();
        UUID adminId = UUID.randomUUID();
        Worker worker = createWorker(workerId);
        Users admin = createAdmin(adminId);
        Payment selectedPayment = createPayment("25.40");

        WorkerRepository workerRepository = mock(WorkerRepository.class);
        UsersRepository usersRepository = mock(UsersRepository.class);
        PaymentRepository paymentRepository = mock(PaymentRepository.class);
        WorkerSettlementRepository settlementRepository =
                mock(WorkerSettlementRepository.class);
        WorkerSettlementItemRepository itemRepository =
                mock(WorkerSettlementItemRepository.class);

        when(workerRepository.findByUserIdForUpdate(workerId))
                .thenReturn(Optional.of(worker));
        when(usersRepository.findById(adminId))
                .thenReturn(Optional.of(admin));
        when(paymentRepository
                .findEligiblePaymentsForWorkerSettlementForUpdate(
                        eq(workerId),
                        anyCollection()))
                .thenReturn(List.of(selectedPayment));
        when(settlementRepository.save(any(WorkerSettlement.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        WorkerSettlementService service = new WorkerSettlementService(
                workerRepository,
                usersRepository,
                paymentRepository,
                settlementRepository,
                itemRepository,
                mock(WorkerSettlementReversalRepository.class),
                testBusinessDateCalculator());

        WorkerSettlement settlement = service.createSettlement(
                workerId,
                List.of(selectedPayment.getPaymentId()),
                adminId);

        assertEquals(new BigDecimal("25.40"), settlement.getAmount());
        assertEquals(LocalDate.of(2026, 11, 11), settlement.getBusinessDate());
        assertEquals(1, settlement.getItems().size());
        assertEquals(selectedPayment, settlement.getItems().get(0).getPayment());
        verify(itemRepository).saveAll(any());
        var lockOrder = inOrder(workerRepository, paymentRepository);
        lockOrder.verify(workerRepository).findByUserIdForUpdate(workerId);
        lockOrder.verify(paymentRepository)
                .findEligiblePaymentsForWorkerSettlementForUpdate(
                        eq(workerId),
                        anyCollection());
    }

    @Test
    void rejectsPaymentsThatAreNoLongerEligible() {
        UUID workerId = UUID.randomUUID();
        UUID adminId = UUID.randomUUID();
        UUID unavailablePaymentId = UUID.randomUUID();

        WorkerRepository workerRepository = mock(WorkerRepository.class);
        UsersRepository usersRepository = mock(UsersRepository.class);
        PaymentRepository paymentRepository = mock(PaymentRepository.class);
        WorkerSettlementRepository settlementRepository =
                mock(WorkerSettlementRepository.class);

        when(workerRepository.findByUserIdForUpdate(workerId))
                .thenReturn(Optional.of(createWorker(workerId)));
        when(usersRepository.findById(adminId))
                .thenReturn(Optional.of(createAdmin(adminId)));
        when(paymentRepository
                .findEligiblePaymentsForWorkerSettlementForUpdate(
                        eq(workerId),
                        anyCollection()))
                .thenReturn(List.of());

        WorkerSettlementService service = new WorkerSettlementService(
                workerRepository,
                usersRepository,
                paymentRepository,
                settlementRepository,
                mock(WorkerSettlementItemRepository.class),
                mock(WorkerSettlementReversalRepository.class),
                testBusinessDateCalculator());

        assertThrows(
                SettlementConflictException.class,
                () -> service.createSettlement(
                        workerId,
                        List.of(unavailablePaymentId),
                        adminId));
        verify(settlementRepository, never()).save(any(WorkerSettlement.class));
    }

    @Test
    void creatingAnotherSettlementDoesNotModifyFailedSettlementHistory() {
        UUID workerId = UUID.randomUUID();
        UUID adminId = UUID.randomUUID();
        Worker worker = createWorker(workerId);
        Users admin = createAdmin(adminId);
        Payment retryablePayment = createPayment("55.50");
        WorkerSettlement failedSettlement = new WorkerSettlement();
        failedSettlement.setSettlementId(UUID.randomUUID());
        failedSettlement.setWorker(worker);
        failedSettlement.setAmount(new BigDecimal("55.50"));
        failedSettlement.setStatus(SettlementStatus.FAILED);
        failedSettlement.setPaymentReference(null);
        failedSettlement.setFailureReason("Bank confirmed the original attempt failed.");
        failedSettlement.setCreatedAt(LocalDateTime.now().minusDays(1));
        failedSettlement.setSettledBy(admin);
        WorkerSettlementItem historicalItem = new WorkerSettlementItem();
        historicalItem.setSettlement(failedSettlement);
        historicalItem.setPayment(retryablePayment);
        historicalItem.setAmount(new BigDecimal("55.50"));
        failedSettlement.setItems(new ArrayList<>(List.of(historicalItem)));

        WorkerRepository workerRepository = mock(WorkerRepository.class);
        UsersRepository usersRepository = mock(UsersRepository.class);
        PaymentRepository paymentRepository = mock(PaymentRepository.class);
        WorkerSettlementRepository settlementRepository =
                mock(WorkerSettlementRepository.class);
        when(workerRepository.findByUserIdForUpdate(workerId))
                .thenReturn(Optional.of(worker));
        when(usersRepository.findById(adminId)).thenReturn(Optional.of(admin));
        when(paymentRepository
                .findEligiblePaymentsForWorkerSettlementForUpdate(
                        eq(workerId),
                        anyCollection()))
                .thenReturn(List.of(retryablePayment));
        when(settlementRepository.save(any(WorkerSettlement.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        WorkerSettlementService service = new WorkerSettlementService(
                workerRepository,
                usersRepository,
                paymentRepository,
                settlementRepository,
                mock(WorkerSettlementItemRepository.class),
                mock(WorkerSettlementReversalRepository.class),
                testBusinessDateCalculator());

        WorkerSettlement newSettlement = service.createSettlement(
                workerId,
                List.of(retryablePayment.getPaymentId()),
                adminId);

        assertEquals(SettlementStatus.PENDING, newSettlement.getStatus());
        assertEquals(
                failedSettlement.getSettlementId(),
                historicalItem.getSettlement().getSettlementId());
        assertEquals(SettlementStatus.FAILED, failedSettlement.getStatus());
        assertEquals(
                "Bank confirmed the original attempt failed.",
                failedSettlement.getFailureReason());
        assertSame(historicalItem, failedSettlement.getItems().get(0));
    }

    @Test
    void enforcesClosedBusinessDateAndKeepsOlderBatchesSettleable() {
        LocalDate today = LocalDate.of(2026, 11, 11);

        CreationFixture beforeCutoff = createCreationFixture(
                List.of(createPaymentAt("10.00", "2026-11-11T19:00:00")),
                "2026-11-11T14:29:59Z");
        assertThrows(
                SettlementConflictException.class,
                () -> beforeCutoff.service().createSettlement(
                        beforeCutoff.workerId(),
                        List.of(beforeCutoff.paymentIds().get(0)),
                        today,
                        beforeCutoff.adminId()));
        verify(beforeCutoff.settlementRepository(), never())
                .save(any(WorkerSettlement.class));

        CreationFixture atCutoff = createCreationFixture(
                List.of(createPaymentAt("10.00", "2026-11-11T19:00:00")),
                "2026-11-11T14:30:00Z");
        WorkerSettlement atCutoffSettlement = assertDoesNotThrow(
                () -> atCutoff.service().createSettlement(
                        atCutoff.workerId(),
                        atCutoff.paymentIds(),
                        today,
                        atCutoff.adminId()));
        assertEquals(today, atCutoffSettlement.getBusinessDate());

        CreationFixture afterCutoff = createCreationFixture(
                List.of(createPaymentAt("10.00", "2026-11-11T19:00:00")),
                "2026-11-11T14:30:01Z");
        assertDoesNotThrow(
                () -> afterCutoff.service().createSettlement(
                        afterCutoff.workerId(),
                        afterCutoff.paymentIds(),
                        today,
                        afterCutoff.adminId()));

        CreationFixture futureBatch = createCreationFixture(
                List.of(createPaymentAt("10.00", "2026-11-11T20:00:00")),
                "2026-11-11T14:30:00Z");
        assertThrows(
                SettlementConflictException.class,
                () -> futureBatch.service().createSettlement(
                        futureBatch.workerId(),
                        futureBatch.paymentIds(),
                        today.plusDays(1),
                        futureBatch.adminId()));

        CreationFixture olderBatch = createCreationFixture(
                List.of(createPaymentAt("10.00", "2026-11-10T19:00:00")),
                "2026-11-11T14:29:59Z");
        assertEquals(
                LocalDate.of(2026, 11, 10),
                assertDoesNotThrow(
                        () -> olderBatch.service().createSettlement(
                                olderBatch.workerId(),
                                olderBatch.paymentIds(),
                                LocalDate.of(2026, 11, 10),
                                olderBatch.adminId()))
                        .getBusinessDate());
    }

    @Test
    void rejectsPaymentsFromDifferentBusinessDatesInOneSettlement() {
        CreationFixture fixture = createCreationFixture(
                List.of(
                        createPaymentAt("10.00", "2026-11-11T19:59:59"),
                        createPaymentAt("20.00", "2026-11-11T20:00:00")),
                "2026-11-11T15:00:00Z");

        assertThrows(
                SettlementConflictException.class,
                () -> fixture.service().createSettlement(
                        fixture.workerId(),
                        fixture.paymentIds(),
                        fixture.adminId()));
        verify(fixture.settlementRepository(), never())
                .save(any(WorkerSettlement.class));
    }

    @Test
    void batchMetricsDoNotDoubleCountFailedRetriesAndKeepProcessingBlocked() {
        Worker worker = createWorker(UUID.randomUUID());
        Payment retryablePayment = createDashboardPayment("50.25", worker);
        Payment processingPayment = createDashboardPayment("75.00", worker);
        LocalDate businessDate = LocalDate.of(2026, 11, 11);
        WorkerSettlement failedAttempt1 = createBatchSettlement(
                worker, businessDate, SettlementStatus.FAILED, "50.25", 1);
        WorkerSettlement failedAttempt2 = createBatchSettlement(
                worker, businessDate, SettlementStatus.FAILED, "50.25", 2);
        WorkerSettlement processingAttempt = createBatchSettlement(
                worker, businessDate, SettlementStatus.PROCESSING, "75.00", 3);

        List<WorkerSettlementItem> items = List.of(
                createSettlementItem(failedAttempt1, retryablePayment),
                createSettlementItem(failedAttempt2, retryablePayment),
                createSettlementItem(processingAttempt, processingPayment));
        PaymentRepository paymentRepository = mock(PaymentRepository.class);
        WorkerSettlementRepository settlementRepository =
                mock(WorkerSettlementRepository.class);
        WorkerSettlementItemRepository itemRepository =
                mock(WorkerSettlementItemRepository.class);
        when(paymentRepository.findSettlementBatchPayments())
                .thenReturn(List.of(retryablePayment, processingPayment));
        when(paymentRepository.findEligiblePaymentsForSettlement())
                .thenReturn(List.of(retryablePayment));
        when(paymentRepository.sumUndatedSuccessfulOnlinePaymentAmount())
                .thenReturn(BigDecimal.ZERO);
        when(paymentRepository.countUndatedSuccessfulOnlinePayments()).thenReturn(0L);
        when(itemRepository.findAllForBatchHistory()).thenReturn(items);
        when(settlementRepository.findAllForBatchDashboard())
                .thenReturn(List.of(
                        failedAttempt1,
                        failedAttempt2,
                        processingAttempt));

        WorkerSettlementService service = new WorkerSettlementService(
                mock(WorkerRepository.class),
                mock(UsersRepository.class),
                paymentRepository,
                settlementRepository,
                itemRepository,
                mock(WorkerSettlementReversalRepository.class),
                testBusinessDateCalculator());

        WorkerSettlementBatchDto batch = service.getSettlementBatches()
                .getBatches()
                .get(0);

        assertEquals(SettlementBatchStatus.FAILED_RETRY_AVAILABLE, batch.getStatus());
        assertEquals(3, batch.getSettlementsCreatedCount());
        assertEquals(new BigDecimal("50.25"), batch.getFailedSettlementAmount());
        assertEquals(new BigDecimal("75.00"), batch.getProcessingAmount());
        assertEquals(new BigDecimal("50.25"), batch.getRemainingEligibleAmount());
    }

    @Test
    void rejectsDuplicateSelectedPaymentIds() {
        WorkerRepository workerRepository = mock(WorkerRepository.class);
        WorkerSettlementService service = new WorkerSettlementService(
                workerRepository,
                mock(UsersRepository.class),
                mock(PaymentRepository.class),
                mock(WorkerSettlementRepository.class),
                mock(WorkerSettlementItemRepository.class),
                mock(WorkerSettlementReversalRepository.class),
                testBusinessDateCalculator());
        UUID paymentId = UUID.randomUUID();

        assertThrows(
                IllegalArgumentException.class,
                () -> service.createSettlement(
                        UUID.randomUUID(),
                        List.of(paymentId, paymentId),
                        UUID.randomUUID()));
        verify(workerRepository, never()).findByUserIdForUpdate(any());
    }

    @Test
    void permitsPendingToProcessingWithoutChangingCreatorOrSettlementData() {
        StatusFixture fixture = createStatusFixture(SettlementStatus.PENDING);
        LocalDateTime createdAt = fixture.settlement().getCreatedAt();
        BigDecimal amount = fixture.settlement().getAmount();

        WorkerSettlement updated = fixture.service().markSettlementProcessing(
                fixture.settlementId(),
                fixture.adminId());

        assertEquals(SettlementStatus.PROCESSING, updated.getStatus());
        assertNull(updated.getSettledAt());
        assertEquals(createdAt, updated.getCreatedAt());
        assertEquals(amount, updated.getAmount());
        assertSame(fixture.creator(), updated.getSettledBy());
        assertSame(fixture.item(), updated.getItems().get(0));
        verify(fixture.settlementRepository())
                .findBySettlementIdForUpdate(fixture.settlementId());
    }

    @Test
    void permitsProcessingToSettledWithTrimmedReferenceAndTimestamp() {
        StatusFixture fixture = createStatusFixture(SettlementStatus.PROCESSING);

        WorkerSettlement updated = fixture.service().markSettlementSettled(
                fixture.settlementId(),
                "  bank-ref-123  ",
                fixture.adminId());

        assertEquals(SettlementStatus.SETTLED, updated.getStatus());
        assertEquals("bank-ref-123", updated.getPaymentReference());
        assertNotNull(updated.getSettledAt());
        assertSame(fixture.creator(), updated.getSettledBy());
        assertEquals(new BigDecimal("55.50"), updated.getAmount());
        assertSame(fixture.item(), updated.getItems().get(0));
    }

    @Test
    void permitsProcessingToFailedWithTrimmedReasonAndNoSettledTimestamp() {
        StatusFixture fixture = createStatusFixture(SettlementStatus.PROCESSING);

        WorkerSettlement updated = fixture.service().markSettlementFailed(
                fixture.settlementId(),
                "  Bank confirmed the transfer failed.  ",
                fixture.adminId());

        assertEquals(SettlementStatus.FAILED, updated.getStatus());
        assertEquals(
                "Bank confirmed the transfer failed.",
                updated.getFailureReason());
        assertNull(updated.getSettledAt());
        assertSame(fixture.creator(), updated.getSettledBy());
        assertEquals(new BigDecimal("55.50"), updated.getAmount());
        assertSame(fixture.item(), updated.getItems().get(0));
    }

    @Test
    void rejectsSettledAndFailedStatusReversalsWithoutChangingHistory() {
        StatusFixture settled = createStatusFixture(SettlementStatus.SETTLED);
        settled.settlement().setPaymentReference("original-bank-reference");
        LocalDateTime settledAt = LocalDateTime.now().minusDays(1);
        settled.settlement().setSettledAt(settledAt);

        assertThrows(
                SettlementConflictException.class,
                () -> settled.service().markSettlementFailed(
                        settled.settlementId(),
                        "A later report is not proof of failure.",
                        settled.adminId()));
        assertThrows(
                SettlementConflictException.class,
                () -> settled.service().markSettlementProcessing(
                        settled.settlementId(),
                        settled.adminId()));
        assertEquals(SettlementStatus.SETTLED, settled.settlement().getStatus());
        assertEquals(
                "original-bank-reference",
                settled.settlement().getPaymentReference());
        assertEquals(settledAt, settled.settlement().getSettledAt());

        StatusFixture failed = createStatusFixture(SettlementStatus.FAILED);
        failed.settlement().setFailureReason("Bank confirmed failure.");
        assertThrows(
                SettlementConflictException.class,
                () -> failed.service().markSettlementProcessing(
                        failed.settlementId(),
                        failed.adminId()));
        assertThrows(
                SettlementConflictException.class,
                () -> failed.service().markSettlementSettled(
                        failed.settlementId(),
                        "retry-reference",
                        failed.adminId()));
        assertEquals(SettlementStatus.FAILED, failed.settlement().getStatus());
        assertEquals("Bank confirmed failure.", failed.settlement().getFailureReason());
    }

    @Test
    void rejectsDirectPendingToSettledOrFailedTransitions() {
        StatusFixture fixture = createStatusFixture(SettlementStatus.PENDING);

        assertThrows(
                SettlementConflictException.class,
                () -> fixture.service().markSettlementSettled(
                        fixture.settlementId(),
                        "bank-reference",
                        fixture.adminId()));
        assertThrows(
                SettlementConflictException.class,
                () -> fixture.service().markSettlementFailed(
                        fixture.settlementId(),
                        "confirmed failure",
                        fixture.adminId()));
        assertEquals(SettlementStatus.PENDING, fixture.settlement().getStatus());
    }

    @Test
    void rejectsBlankSettlementReferenceAndFailureReasonWithoutChangingStatus() {
        StatusFixture referenceFixture =
                createStatusFixture(SettlementStatus.PROCESSING);
        assertThrows(
                IllegalArgumentException.class,
                () -> referenceFixture.service().markSettlementSettled(
                        referenceFixture.settlementId(),
                        " \t ",
                        referenceFixture.adminId()));
        assertEquals(
                SettlementStatus.PROCESSING,
                referenceFixture.settlement().getStatus());
        assertNull(referenceFixture.settlement().getSettledAt());

        StatusFixture reasonFixture =
                createStatusFixture(SettlementStatus.PROCESSING);
        assertThrows(
                IllegalArgumentException.class,
                () -> reasonFixture.service().markSettlementFailed(
                        reasonFixture.settlementId(),
                        " \t ",
                        reasonFixture.adminId()));
        assertEquals(
                SettlementStatus.PROCESSING,
                reasonFixture.settlement().getStatus());
        assertNull(reasonFixture.settlement().getSettledAt());
    }

    @Test
    void recordsVerifiedReversalWithoutChangingOriginalSettledSettlement() {
        StatusFixture fixture = createStatusFixture(SettlementStatus.SETTLED);
        fixture.settlement().setPaymentReference("original-transfer-reference");
        LocalDateTime originalSettledAt = LocalDateTime.now().minusDays(2);
        fixture.settlement().setSettledAt(originalSettledAt);
        when(fixture.reversalRepository()
                .save(any(WorkerSettlementReversal.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        WorkerSettlementReversalDto reversal =
                fixture.service().recordConfirmedReversal(
                        fixture.settlementId(),
                        "  bank-reversal-789  ",
                        "  Bank confirmed the original transfer was reversed.  ",
                        true,
                        fixture.adminId());

        assertEquals("bank-reversal-789", reversal.getBankReversalReference());
        assertEquals(
                "Bank confirmed the original transfer was reversed.",
                reversal.getReversalReason());
        assertEquals(fixture.adminId(), reversal.getVerifiedByAdminId());
        assertNotNull(reversal.getVerifiedAt());
        assertEquals(SettlementStatus.SETTLED, fixture.settlement().getStatus());
        assertEquals(
                "original-transfer-reference",
                fixture.settlement().getPaymentReference());
        assertEquals(originalSettledAt, fixture.settlement().getSettledAt());
        assertEquals(new BigDecimal("55.50"), fixture.settlement().getAmount());
        assertSame(fixture.creator(), fixture.settlement().getSettledBy());
        assertSame(fixture.item(), fixture.settlement().getItems().get(0));
        var lockOrder = inOrder(
                fixture.workerRepository(),
                fixture.settlementRepository());
        lockOrder.verify(fixture.workerRepository())
                .findByUserIdForUpdate(fixture.settlement().getWorker().getUserId());
        lockOrder.verify(fixture.settlementRepository())
                .findBySettlementIdForUpdate(fixture.settlementId());
        verify(fixture.reversalRepository()).save(any(WorkerSettlementReversal.class));
    }

    @Test
    void requiresBankVerificationAndRejectsDuplicateReversals() {
        StatusFixture unverified = createStatusFixture(SettlementStatus.SETTLED);
        assertThrows(
                IllegalArgumentException.class,
                () -> unverified.service().recordConfirmedReversal(
                        unverified.settlementId(),
                        "bank-reversal-123",
                        "Bank confirmed the transfer reversal.",
                        false,
                        unverified.adminId()));
        verify(unverified.reversalRepository(), never())
                .save(any(WorkerSettlementReversal.class));
        verify(unverified.settlementRepository(), never())
                .findBySettlementIdForUpdate(unverified.settlementId());

        StatusFixture duplicate = createStatusFixture(SettlementStatus.SETTLED);
        when(duplicate.reversalRepository()
                .existsBySettlement_SettlementId(duplicate.settlementId()))
                .thenReturn(false, true);
        when(duplicate.reversalRepository()
                .save(any(WorkerSettlementReversal.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        duplicate.service().recordConfirmedReversal(
                duplicate.settlementId(),
                "bank-reversal-456",
                "Bank confirmed the transfer reversal.",
                true,
                duplicate.adminId());

        assertThrows(
                SettlementConflictException.class,
                () -> duplicate.service().recordConfirmedReversal(
                        duplicate.settlementId(),
                        "bank-reversal-456",
                        "Bank confirmed the transfer reversal.",
                        true,
                        duplicate.adminId()));
        verify(duplicate.reversalRepository())
                .save(any(WorkerSettlementReversal.class));
    }

    @Test
    void onlySettledRecordsCanReceiveAReversal() {
        StatusFixture processing =
                createStatusFixture(SettlementStatus.PROCESSING);

        assertThrows(
                SettlementConflictException.class,
                () -> processing.service().recordConfirmedReversal(
                        processing.settlementId(),
                        "bank-reversal-000",
                        "Bank confirmed the transfer reversal.",
                        true,
                        processing.adminId()));
        verify(processing.reversalRepository(), never())
                .save(any(WorkerSettlementReversal.class));
    }

    @Test
    void repositoryMethodsUsePessimisticWriteLocks() throws NoSuchMethodException {
        assertEquals(
                LockModeType.PESSIMISTIC_WRITE,
                WorkerRepository.class
                        .getMethod("findByUserIdForUpdate", UUID.class)
                        .getAnnotation(Lock.class)
                        .value());
        assertEquals(
                LockModeType.PESSIMISTIC_WRITE,
                PaymentRepository.class
                        .getMethod(
                                "findEligiblePaymentsForWorkerSettlementForUpdate",
                                UUID.class,
                                java.util.Collection.class)
                        .getAnnotation(Lock.class)
                        .value());
        assertEquals(
                LockModeType.PESSIMISTIC_WRITE,
                WorkerSettlementRepository.class
                        .getMethod("findBySettlementIdForUpdate", UUID.class)
                        .getAnnotation(Lock.class)
                        .value());
    }

    @Test
    void computesAdminSummaryUsingSettlementRecordsAndEligibleWorkerTotals() {
        PaymentRepository paymentRepository = mock(PaymentRepository.class);
        WorkerSettlementRepository settlementRepository =
                mock(WorkerSettlementRepository.class);
        WorkerSettlementItemRepository itemRepository =
                mock(WorkerSettlementItemRepository.class);

        Worker workerA = createWorker(UUID.randomUUID());
        Worker workerB = createWorker(UUID.randomUUID());
        Payment paymentA1 = createDashboardPayment("120.25", workerA);
        Payment paymentA2 = createDashboardPayment("79.75", workerB);
        Payment paymentB = createDashboardPayment("0.00", workerA);
        when(paymentRepository.findSettlementBatchPayments())
                .thenReturn(List.of(paymentA1, paymentA2, paymentB));
        when(paymentRepository.findEligiblePaymentsForSettlement())
                .thenReturn(List.of(paymentA1, paymentA2, paymentB));
        when(paymentRepository.sumUndatedSuccessfulOnlinePaymentAmount())
                .thenReturn(BigDecimal.ZERO);
        when(paymentRepository.countUndatedSuccessfulOnlinePayments()).thenReturn(0L);
        when(settlementRepository.findAllForBatchDashboard()).thenReturn(List.of());
        when(itemRepository.findAllForBatchHistory()).thenReturn(List.of());
        when(settlementRepository.countByStatus(SettlementStatus.PENDING))
                .thenReturn(1L);
        when(settlementRepository.countByStatus(SettlementStatus.PROCESSING))
                .thenReturn(1L);
        when(settlementRepository.sumAmountByStatus(SettlementStatus.SETTLED))
                .thenReturn(new BigDecimal("51.00"));
        when(settlementRepository.countByStatus(SettlementStatus.FAILED))
                .thenReturn(1L);

        WorkerSettlementService service = new WorkerSettlementService(
                mock(WorkerRepository.class),
                mock(UsersRepository.class),
                paymentRepository,
                settlementRepository,
                itemRepository,
                mock(WorkerSettlementReversalRepository.class),
                testBusinessDateCalculator());

        var summary = service.getAdminSummary();

        assertEquals(2, summary.workersWithEligiblePayments());
        assertEquals(new BigDecimal("200.00"), summary.totalEligibleAmount());
        assertEquals(1, summary.pendingSettlements());
        assertEquals(1, summary.processingSettlements());
        assertEquals(BigDecimal.ZERO, summary.successfullySettledAmount());
        assertEquals(1, summary.failedSettlements());
    }

    private Worker createWorker(UUID workerId) {
        Users user = new Users();
        user.setUserId(workerId);
        user.setFullName("Test Worker");

        Worker worker = new Worker();
        worker.setUserId(workerId);
        worker.setUser(user);
        worker.setBankAccountHolderName("Test Worker");
        worker.setBankAccountNumber("001234567890");
        worker.setBankIfscCode("ABCD0123456");
        worker.setBankName("Test Bank");
        return worker;
    }

    private Users createAdmin(UUID adminId) {
        Credentials credentials = new Credentials();
        credentials.setRole(UserRole.ADMIN);

        Users admin = new Users();
        admin.setUserId(adminId);
        admin.setCredentials(credentials);
        return admin;
    }

    private Payment createPayment(String amount) {
        Payment payment = new Payment();
        payment.setPaymentId(UUID.randomUUID());
        payment.setAmount(new BigDecimal(amount));
        payment.setPaymentMethod(PaymentMethod.RAZORPAY);
        payment.setStatus(PaymentStatus.SUCCESS);
        payment.setPaidAt(LocalDateTime.of(2026, 11, 11, 19, 0));
        return payment;
    }

    private Payment createPaymentAt(String amount, String paidAt) {
        Payment payment = createPayment(amount);
        payment.setPaidAt(LocalDateTime.parse(paidAt));
        return payment;
    }

    private CreationFixture createCreationFixture(
            List<Payment> payments,
            String decisionInstant) {
        UUID workerId = UUID.randomUUID();
        UUID adminId = UUID.randomUUID();
        Worker worker = createWorker(workerId);
        Users admin = createAdmin(adminId);
        WorkerRepository workerRepository = mock(WorkerRepository.class);
        UsersRepository usersRepository = mock(UsersRepository.class);
        PaymentRepository paymentRepository = mock(PaymentRepository.class);
        WorkerSettlementRepository settlementRepository =
                mock(WorkerSettlementRepository.class);
        WorkerSettlementItemRepository itemRepository =
                mock(WorkerSettlementItemRepository.class);

        when(workerRepository.findByUserIdForUpdate(workerId))
                .thenReturn(Optional.of(worker));
        when(usersRepository.findById(adminId)).thenReturn(Optional.of(admin));
        when(paymentRepository.findEligiblePaymentsForWorkerSettlementForUpdate(
                eq(workerId),
                anyCollection()))
                .thenReturn(payments);
        when(settlementRepository.save(any(WorkerSettlement.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        WorkerSettlementService service = new WorkerSettlementService(
                workerRepository,
                usersRepository,
                paymentRepository,
                settlementRepository,
                itemRepository,
                mock(WorkerSettlementReversalRepository.class),
                new SettlementBusinessDateCalculator(
                        Clock.fixed(
                                Instant.parse(decisionInstant),
                                ZoneOffset.UTC)));
        return new CreationFixture(
                service,
                settlementRepository,
                workerId,
                adminId,
                payments.stream().map(payment -> payment.getPaymentId()).toList());
    }

    private Payment createDashboardPayment(String amount, Worker worker) {
        Booking booking = new Booking();
        booking.setBookingId(UUID.randomUUID());
        booking.setWorker(worker);
        booking.setStatus(BookingStatus.COMPLETED);
        Payment payment = createPayment(amount);
        payment.setBooking(booking);
        return payment;
    }

    private WorkerSettlement createBatchSettlement(
            Worker worker,
            LocalDate businessDate,
            SettlementStatus status,
            String amount,
            int createdMinute) {
        WorkerSettlement settlement = new WorkerSettlement();
        settlement.setSettlementId(UUID.randomUUID());
        settlement.setWorker(worker);
        settlement.setBusinessDate(businessDate);
        settlement.setAmount(new BigDecimal(amount));
        settlement.setStatus(status);
        settlement.setCreatedAt(
                LocalDateTime.of(2026, 11, 11, 20, createdMinute));
        return settlement;
    }

    private WorkerSettlementItem createSettlementItem(
            WorkerSettlement settlement,
            Payment payment) {
        WorkerSettlementItem item = new WorkerSettlementItem();
        item.setSettlement(settlement);
        item.setPayment(payment);
        item.setAmount(payment.getAmount());
        return item;
    }

    private SettlementBusinessDateCalculator testBusinessDateCalculator() {
        return new SettlementBusinessDateCalculator(
                Clock.fixed(
                        Instant.parse("2026-11-11T15:00:00Z"),
                        ZoneOffset.UTC));
    }

    private StatusFixture createStatusFixture(SettlementStatus status) {
        UUID adminId = UUID.randomUUID();
        UUID creatorId = UUID.randomUUID();
        UUID settlementId = UUID.randomUUID();
        Users creator = createAdmin(creatorId);
        WorkerSettlement settlement = new WorkerSettlement();
        settlement.setSettlementId(settlementId);
        settlement.setWorker(createWorker(UUID.randomUUID()));
        settlement.setAmount(new BigDecimal("55.50"));
        settlement.setStatus(status);
        settlement.setBankAccountHolderName("Test Worker");
        settlement.setBankAccountNumber("001234567890");
        settlement.setBankIfscCode("ABCD0123456");
        settlement.setBankName("Test Bank");
        settlement.setSettledBy(creator);
        settlement.setCreatedAt(LocalDateTime.now().minusDays(2));

        WorkerSettlementItem item = new WorkerSettlementItem();
        item.setSettlement(settlement);
        item.setAmount(new BigDecimal("55.50"));
        settlement.setItems(new ArrayList<>(List.of(item)));

        WorkerSettlementRepository settlementRepository =
                mock(WorkerSettlementRepository.class);
        WorkerSettlementReversalRepository reversalRepository =
                mock(WorkerSettlementReversalRepository.class);
        WorkerRepository workerRepository = mock(WorkerRepository.class);
        Worker worker = settlement.getWorker();
        when(workerRepository.findByUserIdForUpdate(worker.getUserId()))
                .thenReturn(Optional.of(worker));
        UsersRepository usersRepository = mock(UsersRepository.class);
        when(settlementRepository.findById(settlementId))
                .thenReturn(Optional.of(settlement));
        when(settlementRepository.findBySettlementIdForUpdate(settlementId))
                .thenReturn(Optional.of(settlement));
        when(settlementRepository.save(any(WorkerSettlement.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(reversalRepository.existsBySettlement_SettlementId(settlementId))
                .thenReturn(false);
        when(usersRepository.findById(adminId))
                .thenReturn(Optional.of(createAdmin(adminId)));

        WorkerSettlementService service = new WorkerSettlementService(
                workerRepository,
                usersRepository,
                mock(PaymentRepository.class),
                settlementRepository,
                mock(WorkerSettlementItemRepository.class),
                reversalRepository,
                testBusinessDateCalculator());

        return new StatusFixture(
                service,
                workerRepository,
                settlementRepository,
                reversalRepository,
                settlement,
                settlementId,
                adminId,
                creator,
                item);
    }

    private record StatusFixture(
            WorkerSettlementService service,
            WorkerRepository workerRepository,
            WorkerSettlementRepository settlementRepository,
            WorkerSettlementReversalRepository reversalRepository,
            WorkerSettlement settlement,
            UUID settlementId,
            UUID adminId,
            Users creator,
            WorkerSettlementItem item) {
    }

    private record CreationFixture(
            WorkerSettlementService service,
            WorkerSettlementRepository settlementRepository,
            UUID workerId,
            UUID adminId,
            List<UUID> paymentIds) {
    }
}
