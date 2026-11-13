package com.servicehub.serv.service;

import com.servicehub.serv.dto.WorkerDailySettlementSummaryDto;
import com.servicehub.serv.entity.Booking;
import com.servicehub.serv.entity.Payment;
import com.servicehub.serv.entity.Users;
import com.servicehub.serv.entity.Worker;
import com.servicehub.serv.entity.WorkerSettlement;
import com.servicehub.serv.entity.WorkerSettlementItem;
import com.servicehub.serv.entity.WorkerSettlementReversal;
import com.servicehub.serv.enums.BookingStatus;
import com.servicehub.serv.enums.PaymentMethod;
import com.servicehub.serv.enums.PaymentStatus;
import com.servicehub.serv.enums.SettlementStatus;
import com.servicehub.serv.repository.PaymentRepository;
import com.servicehub.serv.repository.UsersRepository;
import com.servicehub.serv.repository.WorkerRepository;
import com.servicehub.serv.repository.WorkerSettlementItemRepository;
import com.servicehub.serv.repository.WorkerSettlementRepository;
import com.servicehub.serv.repository.WorkerSettlementReversalRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

public class WorkerDailySettlementSummaryTest {

    private UUID workerId;
    private Worker worker;
    private List<Payment> paymentsList;
    private List<WorkerSettlementItem> settlementItemsList;

    private WorkerRepository workerRepositoryProxy;
    private PaymentRepository paymentRepositoryProxy;
    private WorkerSettlementItemRepository workerSettlementItemRepositoryProxy;
    private UsersRepository usersRepositoryProxy;
    private WorkerSettlementRepository workerSettlementRepositoryProxy;
    private WorkerSettlementReversalRepository workerSettlementReversalRepositoryProxy;

    @BeforeEach
    void setUp() {
        workerId = UUID.randomUUID();
        worker = new Worker();
        worker.setUserId(workerId);
        Users user = new Users();
        user.setUserId(workerId);
        user.setFullName("Ramesh Patel");
        worker.setUser(user);

        paymentsList = new ArrayList<>();
        settlementItemsList = new ArrayList<>();

        workerRepositoryProxy = createProxy(WorkerRepository.class, (proxy, method, args) -> {
            if ("findById".equals(method.getName()) && args.length > 0 && workerId.equals(args[0])) {
                return Optional.of(worker);
            }
            return Optional.empty();
        });

        paymentRepositoryProxy = createProxy(PaymentRepository.class, (proxy, method, args) -> {
            if ("findSuccessfulPaymentsForWorker".equals(method.getName())) {
                return paymentsList;
            }
            return Collections.emptyList();
        });

        workerSettlementItemRepositoryProxy = createProxy(WorkerSettlementItemRepository.class, (proxy, method, args) -> {
            if ("findByPayment_PaymentIdIn".equals(method.getName())) {
                if (args.length > 0 && args[0] instanceof Collection<?> requestedIds) {
                    return settlementItemsList.stream()
                            .filter(item -> requestedIds.contains(item.getPayment().getPaymentId()))
                            .toList();
                }
            }
            return Collections.emptyList();
        });

        usersRepositoryProxy = createProxy(UsersRepository.class, (proxy, method, args) -> Optional.empty());
        workerSettlementRepositoryProxy = createProxy(WorkerSettlementRepository.class, (proxy, method, args) -> Optional.empty());
        workerSettlementReversalRepositoryProxy = createProxy(WorkerSettlementReversalRepository.class, (proxy, method, args) -> Optional.empty());
    }

    private WorkerSettlementService createService(Clock clock) {
        SettlementBusinessDateCalculator calculator = new SettlementBusinessDateCalculator(clock);
        return new WorkerSettlementService(
                workerRepositoryProxy,
                usersRepositoryProxy,
                paymentRepositoryProxy,
                workerSettlementRepositoryProxy,
                workerSettlementItemRepositoryProxy,
                workerSettlementReversalRepositoryProxy,
                calculator
        );
    }

    private Payment createPayment(UUID bookingId, BigDecimal amount, PaymentMethod method, LocalDateTime paidAt) {
        Payment payment = new Payment();
        payment.setPaymentId(UUID.randomUUID());
        Booking booking = new Booking();
        booking.setBookingId(bookingId);
        booking.setWorker(worker);
        booking.setStatus(BookingStatus.COMPLETED);

        payment.setBooking(booking);
        payment.setAmount(amount);
        payment.setPaymentMethod(method);
        payment.setStatus(PaymentStatus.SUCCESS);
        payment.setPaidAt(paidAt);
        return payment;
    }

    private WorkerSettlementItem attachSettlementItem(Payment payment, SettlementStatus status, boolean reversed) {
        WorkerSettlement settlement = new WorkerSettlement();
        settlement.setSettlementId(UUID.randomUUID());
        settlement.setWorker(worker);
        settlement.setStatus(status);
        settlement.setAmount(payment.getAmount());

        if (reversed) {
            WorkerSettlementReversal reversal = new WorkerSettlementReversal(
                    settlement,
                    "REV-REF-100",
                    "Bank account closed",
                    LocalDateTime.now(),
                    new Users()
            );
            settlement.setReversal(reversal);
        }

        WorkerSettlementItem item = new WorkerSettlementItem();
        item.setSettlementItemId(UUID.randomUUID());
        item.setSettlement(settlement);
        item.setPayment(payment);
        item.setAmount(payment.getAmount());

        settlementItemsList.add(item);
        return item;
    }

    @SuppressWarnings("unchecked")
    private static <T> T createProxy(Class<T> iface, java.lang.reflect.InvocationHandler handler) {
        return (T) Proxy.newProxyInstance(iface.getClassLoader(), new Class<?>[]{iface}, handler);
    }

    @Test
    void returnsZeroSummaryWhenNoCompletedJobs() {
        // Clock at 2026-11-13 14:00 IST (UTC: 2026-11-13 08:30)
        Clock clock = Clock.fixed(Instant.parse("2026-11-13T08:30:00Z"), ZoneId.of("Asia/Kolkata"));
        WorkerSettlementService service = createService(clock);

        WorkerDailySettlementSummaryDto summary = service.getWorkerDailySummary(workerId);

        assertNotNull(summary);
        assertEquals(LocalDate.of(2026, 11, 13), summary.getBusinessDate());
        assertEquals(0, summary.getCompletedJobsCount());
        assertEquals(BigDecimal.ZERO, summary.getTotalEarnings());
        assertEquals(BigDecimal.ZERO, summary.getCashEarnings());
        assertEquals(BigDecimal.ZERO, summary.getOnlineEarnings());
        assertEquals(BigDecimal.ZERO, summary.getSettledAmount());
        assertEquals(BigDecimal.ZERO, summary.getPendingSettlementAmount());
        assertEquals(BigDecimal.ZERO, summary.getUnsettledAmount());
        assertFalse(summary.isBatchClosed());
        assertEquals("NO_JOBS", summary.getSettlementStatus());
    }

    @Test
    void handlesCashOnlyEarningsCorrectly() {
        // Clock at 2026-11-13 15:00 IST
        Clock clock = Clock.fixed(Instant.parse("2026-11-13T09:30:00Z"), ZoneId.of("Asia/Kolkata"));
        WorkerSettlementService service = createService(clock);

        Payment cashJob = createPayment(
                UUID.randomUUID(),
                new BigDecimal("450.00"),
                PaymentMethod.CASH,
                LocalDateTime.of(2026, 11, 13, 11, 30)
        );
        paymentsList.add(cashJob);

        WorkerDailySettlementSummaryDto summary = service.getWorkerDailySummary(workerId);

        assertEquals(1, summary.getCompletedJobsCount());
        assertEquals(new BigDecimal("450.00"), summary.getTotalEarnings());
        assertEquals(new BigDecimal("450.00"), summary.getCashEarnings());
        assertEquals(BigDecimal.ZERO, summary.getOnlineEarnings());
        assertEquals(BigDecimal.ZERO, summary.getSettledAmount());
        assertEquals(BigDecimal.ZERO, summary.getPendingSettlementAmount());
        assertEquals(BigDecimal.ZERO, summary.getUnsettledAmount());
        assertEquals("CASH_COLLECTED", summary.getSettlementStatus());
    }

    @Test
    void handlesOpenAccumulatingOnlineBatchBefore8pmCutoff() {
        // Clock at 2026-11-13 16:00 IST (before 20:00 cutoff)
        Clock clock = Clock.fixed(Instant.parse("2026-11-13T10:30:00Z"), ZoneId.of("Asia/Kolkata"));
        WorkerSettlementService service = createService(clock);

        Payment onlineJob = createPayment(
                UUID.randomUUID(),
                new BigDecimal("600.00"),
                PaymentMethod.RAZORPAY,
                LocalDateTime.of(2026, 11, 13, 14, 0)
        );
        paymentsList.add(onlineJob);

        WorkerDailySettlementSummaryDto summary = service.getWorkerDailySummary(workerId);

        assertEquals(LocalDate.of(2026, 11, 13), summary.getBusinessDate());
        assertEquals(1, summary.getCompletedJobsCount());
        assertEquals(new BigDecimal("600.00"), summary.getTotalEarnings());
        assertEquals(BigDecimal.ZERO, summary.getCashEarnings());
        assertEquals(new BigDecimal("600.00"), summary.getOnlineEarnings());
        assertEquals(BigDecimal.ZERO, summary.getSettledAmount());
        assertEquals(BigDecimal.ZERO, summary.getPendingSettlementAmount());
        assertEquals(new BigDecimal("600.00"), summary.getUnsettledAmount());
        assertFalse(summary.isBatchClosed());
        assertEquals("OPEN_ACCUMULATING", summary.getSettlementStatus());
    }

    @Test
    void handlesReadyForSettlementWhenBatchClosesPast8pm() {
        // Suppose current business date is closed.
        // Let's test a payment on 2026-11-13 at 20:30 IST. Its business date is 2026-11-14.
        // Let's evaluate at 2026-11-14 21:00 IST (after 20:00 cutoff for 2026-11-14).
        Clock clock = Clock.fixed(Instant.parse("2026-11-14T15:30:00Z"), ZoneId.of("Asia/Kolkata"));
        // Wait, at 2026-11-14 21:00 IST, business date for current time is 2026-11-15 (since it's past 20:00 cutoff).
        // If we want 2026-11-14 business batch closed:
        // At 2026-11-14 19:59 IST, business date is 2026-11-14, isClosed is false.
        // What if business date is set and isClosed is true?
        // SettlementBusinessDateCalculator: isClosed(businessDate, now)
        // If businessDate is 2026-11-14, and now is 2026-11-14 20:05 IST:
        // now.toLocalDate() is 2026-11-14, and time >= 20:00 -> isClosed(2026-11-14) is TRUE!
        // But businessDate(now) gives 2026-11-15 (next accumulating batch).
        // What if a batch date is closed?
        // Let's test targetBusinessDate closed state directly:
        SettlementBusinessDateCalculator customCalculator = new SettlementBusinessDateCalculator(clock) {
            @Override
            public boolean isClosed(LocalDate businessDate, ZonedDateTime decisionTime) {
                return true;
            }
        };
        WorkerSettlementService service = new WorkerSettlementService(
                workerRepositoryProxy,
                usersRepositoryProxy,
                paymentRepositoryProxy,
                workerSettlementRepositoryProxy,
                workerSettlementItemRepositoryProxy,
                workerSettlementReversalRepositoryProxy,
                customCalculator
        );

        LocalDate businessDate = customCalculator.businessDate(customCalculator.now().toLocalDateTime());
        Payment onlineJob = createPayment(
                UUID.randomUUID(),
                new BigDecimal("750.00"),
                PaymentMethod.RAZORPAY,
                LocalDateTime.of(businessDate, LocalTime.of(10, 0))
        );
        paymentsList.add(onlineJob);

        WorkerDailySettlementSummaryDto summary = service.getWorkerDailySummary(workerId);

        assertTrue(summary.isBatchClosed());
        assertEquals("READY_FOR_SETTLEMENT", summary.getSettlementStatus());
        assertEquals(new BigDecimal("750.00"), summary.getUnsettledAmount());
    }

    @Test
    void handlesSettledAndPendingSettlementWithoutDoubleCounting() {
        Clock clock = Clock.fixed(Instant.parse("2026-11-13T09:30:00Z"), ZoneId.of("Asia/Kolkata"));
        WorkerSettlementService service = createService(clock);

        // Job 1: Settled (₹500)
        Payment job1 = createPayment(
                UUID.randomUUID(),
                new BigDecimal("500.00"),
                PaymentMethod.RAZORPAY,
                LocalDateTime.of(2026, 11, 13, 10, 0)
        );
        attachSettlementItem(job1, SettlementStatus.SETTLED, false);

        // Job 2: Pending/Processing (₹300)
        Payment job2 = createPayment(
                UUID.randomUUID(),
                new BigDecimal("300.00"),
                PaymentMethod.RAZORPAY,
                LocalDateTime.of(2026, 11, 13, 11, 0)
        );
        attachSettlementItem(job2, SettlementStatus.PROCESSING, false);

        // Job 3: Unsettled (₹200)
        Payment job3 = createPayment(
                UUID.randomUUID(),
                new BigDecimal("200.00"),
                PaymentMethod.RAZORPAY,
                LocalDateTime.of(2026, 11, 13, 12, 0)
        );

        paymentsList.add(job1);
        paymentsList.add(job2);
        paymentsList.add(job3);

        WorkerDailySettlementSummaryDto summary = service.getWorkerDailySummary(workerId);

        assertEquals(3, summary.getCompletedJobsCount());
        assertEquals(new BigDecimal("1000.00"), summary.getTotalEarnings());
        assertEquals(new BigDecimal("1000.00"), summary.getOnlineEarnings());
        assertEquals(new BigDecimal("500.00"), summary.getSettledAmount());
        assertEquals(new BigDecimal("300.00"), summary.getPendingSettlementAmount());
        assertEquals(new BigDecimal("200.00"), summary.getUnsettledAmount());
        // Since some settled and some processing:
        assertEquals("PARTIALLY_SETTLED", summary.getSettlementStatus());

        // Check mutual exclusivity: settled + pending + unsettled == onlineEarnings
        assertEquals(
                summary.getOnlineEarnings(),
                summary.getSettledAmount()
                        .add(summary.getPendingSettlementAmount())
                        .add(summary.getUnsettledAmount())
        );
    }

    @Test
    void handlesReversedSettlementCorrectly() {
        Clock clock = Clock.fixed(Instant.parse("2026-11-13T09:30:00Z"), ZoneId.of("Asia/Kolkata"));
        WorkerSettlementService service = createService(clock);

        Payment job = createPayment(
                UUID.randomUUID(),
                new BigDecimal("900.00"),
                PaymentMethod.RAZORPAY,
                LocalDateTime.of(2026, 11, 13, 10, 0)
        );
        // Settled but reversed!
        attachSettlementItem(job, SettlementStatus.SETTLED, true);
        paymentsList.add(job);

        WorkerDailySettlementSummaryDto summary = service.getWorkerDailySummary(workerId);

        assertEquals(BigDecimal.ZERO, summary.getSettledAmount());
        assertEquals(BigDecimal.ZERO, summary.getPendingSettlementAmount());
        assertEquals(new BigDecimal("900.00"), summary.getUnsettledAmount());
        assertEquals("REVERSED_RETRY_AVAILABLE", summary.getSettlementStatus());
    }

    @Test
    void handlesSeparationOfMixedCashAndOnlineEarnings() {
        Clock clock = Clock.fixed(Instant.parse("2026-11-13T09:30:00Z"), ZoneId.of("Asia/Kolkata"));
        WorkerSettlementService service = createService(clock);

        Payment cash = createPayment(
                UUID.randomUUID(),
                new BigDecimal("250.00"),
                PaymentMethod.CASH,
                LocalDateTime.of(2026, 11, 13, 10, 0)
        );
        Payment online = createPayment(
                UUID.randomUUID(),
                new BigDecimal("750.00"),
                PaymentMethod.RAZORPAY,
                LocalDateTime.of(2026, 11, 13, 11, 0)
        );
        attachSettlementItem(online, SettlementStatus.SETTLED, false);

        paymentsList.add(cash);
        paymentsList.add(online);

        WorkerDailySettlementSummaryDto summary = service.getWorkerDailySummary(workerId);

        assertEquals(2, summary.getCompletedJobsCount());
        assertEquals(new BigDecimal("1000.00"), summary.getTotalEarnings());
        assertEquals(new BigDecimal("250.00"), summary.getCashEarnings());
        assertEquals(new BigDecimal("750.00"), summary.getOnlineEarnings());
        assertEquals(new BigDecimal("750.00"), summary.getSettledAmount());
        assertEquals("SETTLED", summary.getSettlementStatus());
    }

    @Test
    void respects8pmCutoffBoundary() {
        // Cutoff is 20:00 IST.
        // Payment at 19:59:59 IST belongs to 2026-11-13
        // Payment at 20:00:00 IST belongs to 2026-11-14
        Clock clockToday = Clock.fixed(Instant.parse("2026-11-13T14:00:00Z"), ZoneId.of("Asia/Kolkata")); // 19:30 IST
        WorkerSettlementService serviceToday = createService(clockToday);

        Payment beforeCutoff = createPayment(
                UUID.randomUUID(),
                new BigDecimal("400.00"),
                PaymentMethod.RAZORPAY,
                LocalDateTime.of(2026, 11, 13, 19, 59, 59)
        );
        Payment atCutoff = createPayment(
                UUID.randomUUID(),
                new BigDecimal("600.00"),
                PaymentMethod.RAZORPAY,
                LocalDateTime.of(2026, 11, 13, 20, 0, 0)
        );

        paymentsList.add(beforeCutoff);
        paymentsList.add(atCutoff);

        WorkerDailySettlementSummaryDto summaryToday = serviceToday.getWorkerDailySummary(workerId);

        // Only beforeCutoff belongs to 2026-11-13 business date
        assertEquals(LocalDate.of(2026, 11, 13), summaryToday.getBusinessDate());
        assertEquals(1, summaryToday.getCompletedJobsCount());
        assertEquals(new BigDecimal("400.00"), summaryToday.getTotalEarnings());
        assertEquals(new BigDecimal("400.00"), summaryToday.getOnlineEarnings());

        // Now evaluate at 20:05 IST on 2026-11-13 (current business date is 2026-11-14)
        Clock clockTomorrow = Clock.fixed(Instant.parse("2026-11-13T14:35:00Z"), ZoneId.of("Asia/Kolkata")); // 20:05 IST
        WorkerSettlementService serviceTomorrow = createService(clockTomorrow);

        WorkerDailySettlementSummaryDto summaryTomorrow = serviceTomorrow.getWorkerDailySummary(workerId);
        assertEquals(LocalDate.of(2026, 11, 14), summaryTomorrow.getBusinessDate());
        assertEquals(1, summaryTomorrow.getCompletedJobsCount());
        assertEquals(new BigDecimal("600.00"), summaryTomorrow.getTotalEarnings());
        assertEquals(new BigDecimal("600.00"), summaryTomorrow.getOnlineEarnings());
    }
}
