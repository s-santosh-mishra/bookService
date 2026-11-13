package com.servicehub.serv.service;

import com.servicehub.serv.dto.BookingDto;
import com.servicehub.serv.entity.Billing;
import com.servicehub.serv.entity.Booking;
import com.servicehub.serv.entity.Customer;
import com.servicehub.serv.entity.Payment;
import com.servicehub.serv.entity.Service;
import com.servicehub.serv.entity.Users;
import com.servicehub.serv.entity.Worker;
import com.servicehub.serv.enums.BookingStatus;
import com.servicehub.serv.enums.PaymentMethod;
import com.servicehub.serv.enums.PaymentStatus;
import com.servicehub.serv.repository.BillingRepository;
import com.servicehub.serv.repository.BookingOfferRepository;
import com.servicehub.serv.repository.BookingPartRepository;
import com.servicehub.serv.repository.BookingRepository;
import com.servicehub.serv.repository.CustomerRepository;
import com.servicehub.serv.repository.PaymentRepository;
import com.servicehub.serv.repository.ServiceRepository;
import com.servicehub.serv.repository.WorkerBookingRejectionRepository;
import com.servicehub.serv.repository.WorkerRepository;
import com.servicehub.serv.repository.WorkerServiceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

public class CustomerBookingPaymentDetailsTest {

    private UUID customerId;
    private Customer customer;
    private Service testService;
    private Worker testWorker;
    private Map<UUID, List<Payment>> paymentsByBooking;
    private Map<UUID, Billing> billingByBooking;
    private List<Booking> customerBookings;

    private BookingRepository bookingRepositoryProxy;
    private BillingRepository billingRepositoryProxy;
    private PaymentRepository paymentRepositoryProxy;

    @BeforeEach
    void setUp() {
        customerId = UUID.randomUUID();
        customer = new Customer();
        customer.setUserId(customerId);
        Users custUser = new Users();
        custUser.setUserId(customerId);
        custUser.setFullName("Aarav Sharma");
        custUser.setPhone("9876543210");
        custUser.setCity("Bhubaneswar");
        custUser.setState("Odisha");
        custUser.setPinCode("751001");
        customer.setUser(custUser);

        testService = new Service();
        testService.setServiceId(UUID.randomUUID());
        testService.setServiceName("AC Deep Cleaning");
        testService.setBasePricePerHour(new BigDecimal("999.00")); // Estimated/base price

        testWorker = new Worker();
        UUID workerId = UUID.randomUUID();
        testWorker.setUserId(workerId);
        Users workerUser = new Users();
        workerUser.setUserId(workerId);
        workerUser.setFullName("Suresh Kumar");
        testWorker.setUser(workerUser);

        paymentsByBooking = new HashMap<>();
        billingByBooking = new HashMap<>();
        customerBookings = new ArrayList<>();

        bookingRepositoryProxy = createProxy(BookingRepository.class, (proxy, method, args) -> {
            if ("findByCustomerUserId".equals(method.getName()) && args.length > 0 && customerId.equals(args[0])) {
                return customerBookings;
            }
            return Collections.emptyList();
        });

        billingRepositoryProxy = createProxy(BillingRepository.class, (proxy, method, args) -> {
            if ("findByBookingBookingId".equals(method.getName()) && args.length > 0 && args[0] instanceof UUID bId) {
                return Optional.ofNullable(billingByBooking.get(bId));
            }
            return Optional.empty();
        });

        paymentRepositoryProxy = createProxy(PaymentRepository.class, (proxy, method, args) -> {
            if ("findByBookingBookingIdOrderByCreatedAtDesc".equals(method.getName()) && args.length > 0 && args[0] instanceof UUID bId) {
                return paymentsByBooking.getOrDefault(bId, Collections.emptyList());
            }
            return Collections.emptyList();
        });
    }

    private BookingService createService() {
        return new BookingService(
                bookingRepositoryProxy,
                createProxy(BookingPartRepository.class, (p, m, a) -> null),
                createProxy(CustomerRepository.class, (p, m, a) -> null),
                createProxy(ServiceRepository.class, (p, m, a) -> null),
                createProxy(WorkerRepository.class, (p, m, a) -> null),
                createProxy(WorkerServiceRepository.class, (p, m, a) -> null),
                createProxy(WorkerBookingRejectionRepository.class, (p, m, a) -> null),
                createProxy(BookingOfferRepository.class, (p, m, a) -> null),
                null,
                Clock.systemUTC(),
                null,
                null,
                billingRepositoryProxy,
                paymentRepositoryProxy
        );
    }

    private Booking createBooking(BookingStatus status) {
        Booking booking = new Booking();
        booking.setBookingId(UUID.randomUUID());
        booking.setCustomer(customer);
        booking.setWorker(testWorker);
        booking.setService(testService);
        booking.setStatus(status);
        if (status == BookingStatus.COMPLETED) {
            booking.setCompletedAt(LocalDateTime.now().minusMinutes(30));
        }
        customerBookings.add(booking);
        return booking;
    }

    private Payment createPayment(Booking booking, BigDecimal amount, PaymentMethod method, PaymentStatus status, LocalDateTime paidAt) {
        Payment payment = new Payment();
        payment.setPaymentId(UUID.randomUUID());
        payment.setBooking(booking);
        payment.setAmount(amount);
        payment.setPaymentMethod(method);
        payment.setStatus(status);
        payment.setPaidAt(paidAt);

        paymentsByBooking.computeIfAbsent(booking.getBookingId(), k -> new ArrayList<>()).add(payment);
        return payment;
    }

    @SuppressWarnings("unchecked")
    private static <T> T createProxy(Class<T> iface, java.lang.reflect.InvocationHandler handler) {
        return (T) Proxy.newProxyInstance(iface.getClassLoader(), new Class<?>[]{iface}, handler);
    }

    @Test
    void returnsConfirmedOnlinePaymentDetails() {
        Booking booking = createBooking(BookingStatus.COMPLETED);
        LocalDateTime paidTime = LocalDateTime.of(2026, 11, 13, 14, 30);
        // Actual billed price was 1250.00 (service price 999 + parts)
        createPayment(booking, new BigDecimal("1250.00"), PaymentMethod.RAZORPAY, PaymentStatus.SUCCESS, paidTime);

        BookingService service = createService();
        List<BookingDto> dtos = service.getCustomerBookings(customerId);

        assertEquals(1, dtos.size());
        BookingDto dto = dtos.get(0);
        assertEquals(PaymentStatus.SUCCESS, dto.getPaymentStatus());
        assertEquals(PaymentMethod.RAZORPAY, dto.getPaymentMethod());
        assertEquals(new BigDecimal("1250.00"), dto.getPaidAmount());
        assertEquals(new BigDecimal("1250.00"), dto.getPaymentAmount());
        assertEquals(paidTime, dto.getPaidAt());
        assertNotEquals(testService.getBasePricePerHour(), dto.getPaidAmount(), "Must use actual payment amount, not service price");
    }

    @Test
    void returnsConfirmedCashPaymentDetails() {
        Booking booking = createBooking(BookingStatus.COMPLETED);
        LocalDateTime paidTime = LocalDateTime.of(2026, 11, 13, 15, 0);
        createPayment(booking, new BigDecimal("850.00"), PaymentMethod.CASH, PaymentStatus.SUCCESS, paidTime);

        BookingService service = createService();
        List<BookingDto> dtos = service.getCustomerBookings(customerId);

        assertEquals(1, dtos.size());
        BookingDto dto = dtos.get(0);
        assertEquals(PaymentStatus.SUCCESS, dto.getPaymentStatus());
        assertEquals(PaymentMethod.CASH, dto.getPaymentMethod());
        assertEquals(new BigDecimal("850.00"), dto.getPaidAmount());
        assertEquals(paidTime, dto.getPaidAt());
    }

    @Test
    void distinguishesPendingCashSelectionFromConfirmedReceipt() {
        Booking booking = createBooking(BookingStatus.WAITING_FOR_PAYMENT);
        // Customer selected cash payment; worker has not confirmed yet
        createPayment(booking, new BigDecimal("850.00"), PaymentMethod.CASH, PaymentStatus.PENDING, null);

        BookingService service = createService();
        List<BookingDto> dtos = service.getCustomerBookings(customerId);

        assertEquals(1, dtos.size());
        BookingDto dto = dtos.get(0);
        assertEquals(PaymentStatus.PENDING, dto.getPaymentStatus());
        assertEquals(PaymentMethod.CASH, dto.getPaymentMethod());
        assertNull(dto.getPaidAmount(), "Amount must NOT be marked as paid while pending cash receipt");
        assertEquals(new BigDecimal("850.00"), dto.getPaymentAmount(), "Payment attempt amount must be recorded");
        assertNull(dto.getPaidAt());
    }

    @Test
    void handlesPendingOnlinePaymentCorrectly() {
        Booking booking = createBooking(BookingStatus.WAITING_FOR_PAYMENT);
        createPayment(booking, new BigDecimal("1100.00"), PaymentMethod.RAZORPAY, PaymentStatus.PENDING, null);

        BookingService service = createService();
        List<BookingDto> dtos = service.getCustomerBookings(customerId);

        assertEquals(1, dtos.size());
        BookingDto dto = dtos.get(0);
        assertEquals(PaymentStatus.PENDING, dto.getPaymentStatus());
        assertEquals(PaymentMethod.RAZORPAY, dto.getPaymentMethod());
        assertNull(dto.getPaidAmount(), "Pending online payment must not show paidAmount");
        assertEquals(new BigDecimal("1100.00"), dto.getPaymentAmount());
    }

    @Test
    void handlesFailedPaymentScenario() {
        Booking booking = createBooking(BookingStatus.WAITING_FOR_PAYMENT);
        createPayment(booking, new BigDecimal("700.00"), PaymentMethod.RAZORPAY, PaymentStatus.FAILED, null);

        BookingService service = createService();
        List<BookingDto> dtos = service.getCustomerBookings(customerId);

        assertEquals(1, dtos.size());
        BookingDto dto = dtos.get(0);
        assertEquals(PaymentStatus.FAILED, dto.getPaymentStatus());
        assertEquals(PaymentMethod.RAZORPAY, dto.getPaymentMethod());
        assertNull(dto.getPaidAmount(), "Failed payment must not have paidAmount");
        assertEquals(new BigDecimal("700.00"), dto.getPaymentAmount());
    }

    @Test
    void handlesRefundedPaymentScenario() {
        Booking booking = createBooking(BookingStatus.CANCELLED);
        createPayment(booking, new BigDecimal("600.00"), PaymentMethod.RAZORPAY, PaymentStatus.REFUNDED, null);

        BookingService service = createService();
        List<BookingDto> dtos = service.getCustomerBookings(customerId);

        assertEquals(1, dtos.size());
        BookingDto dto = dtos.get(0);
        assertEquals(PaymentStatus.REFUNDED, dto.getPaymentStatus());
        assertEquals(PaymentMethod.RAZORPAY, dto.getPaymentMethod());
        assertNull(dto.getPaidAmount());
        assertEquals(new BigDecimal("600.00"), dto.getPaymentAmount());
    }

    @Test
    void handlesBookingWithNoPaymentRecordYet() {
        Booking booking = createBooking(BookingStatus.PENDING);

        BookingService service = createService();
        List<BookingDto> dtos = service.getCustomerBookings(customerId);

        assertEquals(1, dtos.size());
        BookingDto dto = dtos.get(0);
        assertNull(dto.getPaymentStatus());
        assertNull(dto.getPaymentMethod());
        assertNull(dto.getPaidAmount());
        assertNull(dto.getPaymentAmount());
        assertNull(dto.getPaidAt());
    }

    @Test
    void prioritizesSuccessfulPaymentWhenMultipleAttemptsExist() {
        Booking booking = createBooking(BookingStatus.COMPLETED);
        // Attempt 1: Failed
        Payment failed = createPayment(booking, new BigDecimal("950.00"), PaymentMethod.RAZORPAY, PaymentStatus.FAILED, null);

        // Attempt 2: Success
        LocalDateTime successTime = LocalDateTime.of(2026, 11, 13, 16, 15);
        Payment success = createPayment(booking, new BigDecimal("950.00"), PaymentMethod.RAZORPAY, PaymentStatus.SUCCESS, successTime);

        BookingService service = createService();
        List<BookingDto> dtos = service.getCustomerBookings(customerId);

        assertEquals(1, dtos.size());
        BookingDto dto = dtos.get(0);
        assertEquals(PaymentStatus.SUCCESS, dto.getPaymentStatus(), "Should resolve to the successful payment");
        assertEquals(new BigDecimal("950.00"), dto.getPaidAmount());
        assertEquals(successTime, dto.getPaidAt());
    }
}
