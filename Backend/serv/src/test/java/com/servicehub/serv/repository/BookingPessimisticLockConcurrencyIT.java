package com.servicehub.serv.repository;

import com.servicehub.serv.dto.WorkerBookingLocationDto;
import com.servicehub.serv.entity.Booking;
import com.servicehub.serv.entity.BookingOffer;
import com.servicehub.serv.entity.Category;
import com.servicehub.serv.entity.Credentials;
import com.servicehub.serv.entity.Customer;
import com.servicehub.serv.entity.Service;
import com.servicehub.serv.entity.Users;
import com.servicehub.serv.entity.Worker;
import com.servicehub.serv.entity.WorkerService;
import com.servicehub.serv.entity.WorkerServiceId;
import com.servicehub.serv.enums.AvailabilityStatus;
import com.servicehub.serv.enums.BookingStatus;
import com.servicehub.serv.enums.UserRole;
import com.servicehub.serv.enums.VerificationStatus;
import com.servicehub.serv.service.BillingService;
import com.servicehub.serv.service.BookingDiscoveryService;
import com.servicehub.serv.service.BookingService;
import com.servicehub.serv.service.OdishaServiceArea;
import io.zonky.test.db.postgres.embedded.EmbeddedPostgres;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * PostgreSQL Integration Test for Pessimistic Locking and Concurrency:
 * Verifies that concurrent worker acceptance and booking timeout cannot corrupt
 * or overwrite each other's state in PostgreSQL 15.6.
 */
@DataJpaTest
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class BookingPessimisticLockConcurrencyIT {

    static EmbeddedPostgres embeddedPg;

    @BeforeAll
    static void startDb() throws Exception {
        embeddedPg = EmbeddedPostgres.builder().start();
    }

    @AfterAll
    static void stopDb() throws Exception {
        if (embeddedPg != null) {
            embeddedPg.close();
        }
    }

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", () -> embeddedPg.getJdbcUrl("postgres", "postgres"));
        registry.add("spring.datasource.username", () -> "postgres");
        registry.add("spring.datasource.password", () -> "postgres");
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
        registry.add("spring.jpa.show-sql", () -> "true");

        // Fulfill application.properties placeholders
        registry.add("DB_URL", () -> embeddedPg.getJdbcUrl("postgres", "postgres"));
        registry.add("DB_USERNAME", () -> "postgres");
        registry.add("DB_PASSWORD", () -> "postgres");
        registry.add("JWT_SECRET", () -> "test-jwt-secret-key-at-least-256-bits-long-for-testing-purpose-only");
        registry.add("RAZORPAY_KEY_ID", () -> "test_razorpay_key");
        registry.add("RAZORPAY_KEY_SECRET", () -> "test_razorpay_secret");
        registry.add("CLOUDINARY_CLOUD_NAME", () -> "test_cloud");
        registry.add("CLOUDINARY_API_KEY", () -> "test_api_key");
        registry.add("CLOUDINARY_API_SECRET", () -> "test_api_secret");
    }

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private BookingPartRepository bookingPartRepository;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private ServiceRepository serviceRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private WorkerRepository workerRepository;

    @Autowired
    private WorkerServiceRepository workerServiceRepository;

    @Autowired
    private WorkerBookingRejectionRepository workerBookingRejectionRepository;

    @Autowired
    private BookingOfferRepository bookingOfferRepository;

    @Autowired
    private BillingRepository billingRepository;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private UsersRepository usersRepository;

    @Autowired
    private CredentialsRepository credentialsRepository;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private BookingService bookingService;
    private ExecutorService executor;

    @BeforeEach
    void setUp() {
        executor = Executors.newFixedThreadPool(4);
        bookingService = new BookingService(
                bookingRepository,
                bookingPartRepository,
                customerRepository,
                serviceRepository,
                workerRepository,
                workerServiceRepository,
                workerBookingRejectionRepository,
                bookingOfferRepository,
                Mockito.mock(BookingDiscoveryService.class),
                Clock.systemDefaultZone(),
                new OdishaServiceArea(),
                Mockito.mock(BillingService.class),
                billingRepository,
                paymentRepository
        );
    }

    @AfterEach
    void tearDown() {
        if (executor != null) {
            executor.shutdownNow();
        }
    }

    private TransactionTemplate newTransactionTemplate() {
        return new TransactionTemplate(transactionManager);
    }

    @Test
    @DisplayName("Scenario A: Worker acceptance obtains booking lock first; timeout runs concurrently and cannot overwrite")
    void testAcceptanceLocksFirst_TimeoutCannotOverwrite() throws Exception {
        Customer customer = createCustomer("concurrency_a_cust@example.com");
        Service service = createService("Cleaning A");
        Worker worker = createWorker("concurrency_a_worker@example.com", service);
        Booking booking = createBooking(customer, service);
        bookingOfferRepository.save(new BookingOffer(booking, worker));

        UUID bookingId = booking.getBookingId();
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime deadline = now.plusMinutes(5); // would match booking createdAt

        CountDownLatch lockAcquiredLatch = new CountDownLatch(1);
        CountDownLatch timeoutAttemptedLatch = new CountDownLatch(1);
        AtomicInteger timeoutAffectedRows = new AtomicInteger(-1);

        TransactionTemplate tt1 = newTransactionTemplate();
        TransactionTemplate tt2 = newTransactionTemplate();

        // Thread 1: Obtains pessimistic write lock on the booking row
        Future<?> acceptFuture = executor.submit(() -> {
            tt1.execute(status -> {
                Booking lockedBooking = bookingRepository.findByBookingIdForUpdate(bookingId).orElseThrow();
                lockAcquiredLatch.countDown();

                // Wait until Thread 2 attempts timeout and is blocked in PostgreSQL
                try {
                    timeoutAttemptedLatch.await(5, TimeUnit.SECONDS);
                    Thread.sleep(100); // ensure PostgreSQL has blocked thread 2
                } catch (InterruptedException ignored) {}

                // Perform acceptance update while holding the lock
                lockedBooking.setWorker(worker);
                lockedBooking.setStatus(BookingStatus.ACCEPTED);
                bookingRepository.saveAndFlush(lockedBooking);
                worker.setAvailabilityStatus(AvailabilityStatus.BUSY);
                workerRepository.saveAndFlush(worker);
                return null;
            });
        });

        // Thread 2: Timeout runs concurrently
        Future<?> timeoutFuture = executor.submit(() -> {
            try {
                lockAcquiredLatch.await(5, TimeUnit.SECONDS);
            } catch (InterruptedException ignored) {}

            timeoutAttemptedLatch.countDown();
            tt2.execute(status -> {
                int affected = bookingRepository.expireUnassignedPendingBookings(
                        BookingStatus.PENDING,
                        BookingStatus.NO_WORKER,
                        deadline,
                        now
                );
                timeoutAffectedRows.set(affected);
                return affected;
            });
        });

        acceptFuture.get(10, TimeUnit.SECONDS);
        timeoutFuture.get(10, TimeUnit.SECONDS);

        // Verification: Timeout did not overwrite the accepted booking
        assertEquals(0, timeoutAffectedRows.get(), "Timeout must affect 0 rows because row was updated to ACCEPTED");
        Booking finalBooking = bookingRepository.findById(bookingId).orElseThrow();
        assertEquals(BookingStatus.ACCEPTED, finalBooking.getStatus());
        assertNotNull(finalBooking.getWorker());
        assertEquals(worker.getUserId(), finalBooking.getWorker().getUserId());
    }

    @Test
    @DisplayName("Scenario B: Timeout obtains booking lock first; worker acceptance runs concurrently and is rejected")
    void testTimeoutLocksFirst_AcceptanceIsRejected() throws Exception {
        Customer customer = createCustomer("concurrency_b_cust@example.com");
        Service service = createService("Cleaning B");
        Worker worker = createWorker("concurrency_b_worker@example.com", service);
        Booking booking = createBooking(customer, service);
        bookingOfferRepository.save(new BookingOffer(booking, worker));

        UUID bookingId = booking.getBookingId();
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime deadline = now.plusMinutes(5);

        CountDownLatch timeoutExecutedLatch = new CountDownLatch(1);
        CountDownLatch acceptAttemptedLatch = new CountDownLatch(1);
        AtomicInteger timeoutRows = new AtomicInteger(-1);
        AtomicReference<Exception> acceptException = new AtomicReference<>();

        TransactionTemplate tt1 = newTransactionTemplate();
        TransactionTemplate tt2 = newTransactionTemplate();

        WorkerBookingLocationDto loc = new WorkerBookingLocationDto();
        loc.setLatitude(BigDecimal.valueOf(20.01));
        loc.setLongitude(BigDecimal.valueOf(85.01));

        // Thread 1: Timeout runs update, acquiring exclusive lock on booking row
        Future<?> timeoutFuture = executor.submit(() -> {
            tt1.execute(status -> {
                int affected = bookingRepository.expireUnassignedPendingBookings(
                        BookingStatus.PENDING,
                        BookingStatus.NO_WORKER,
                        deadline,
                        now
                );
                timeoutRows.set(affected);
                timeoutExecutedLatch.countDown();

                // Wait for Thread 2 to attempt accept and block in PostgreSQL
                try {
                    acceptAttemptedLatch.await(5, TimeUnit.SECONDS);
                    Thread.sleep(100);
                } catch (InterruptedException ignored) {}

                return affected;
            });
        });

        // Thread 2: Worker acceptance calls production bookingService.acceptBooking
        Future<?> acceptFuture = executor.submit(() -> {
            try {
                timeoutExecutedLatch.await(5, TimeUnit.SECONDS);
            } catch (InterruptedException ignored) {}

            acceptAttemptedLatch.countDown();
            try {
                tt2.execute(status -> {
                    bookingService.acceptBooking(worker.getUserId(), bookingId, loc);
                    return null;
                });
            } catch (Exception ex) {
                acceptException.set(ex);
            }
        });

        timeoutFuture.get(10, TimeUnit.SECONDS);
        acceptFuture.get(10, TimeUnit.SECONDS);

        // Verification: Timeout succeeded, acceptBooking threw IllegalStateException
        assertEquals(1, timeoutRows.get(), "Timeout must expire 1 row");
        assertNotNull(acceptException.get(), "Acceptance must throw an exception");
        assertTrue(acceptException.get() instanceof IllegalStateException, "Expected IllegalStateException");
        assertTrue(acceptException.get().getMessage().contains("no longer available"),
                "Message must indicate booking is no longer available");

        Booking finalBooking = bookingRepository.findById(bookingId).orElseThrow();
        assertEquals(BookingStatus.NO_WORKER, finalBooking.getStatus());
        assertNull(finalBooking.getWorker(), "Worker must remain null");
    }

    @Test
    @DisplayName("Scenario C: Both operations begin nearly simultaneously; exactly one valid terminal transition wins")
    void testBothOperationsSimultaneous_ExactlyOneWins() throws Exception {
        Customer customer = createCustomer("concurrency_c_cust@example.com");
        Service service = createService("Cleaning C");
        Worker worker = createWorker("concurrency_c_worker@example.com", service);
        Booking booking = createBooking(customer, service);
        bookingOfferRepository.save(new BookingOffer(booking, worker));

        UUID bookingId = booking.getBookingId();
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime deadline = now.plusMinutes(5);

        CyclicBarrier barrier = new CyclicBarrier(2);
        AtomicInteger timeoutUpdated = new AtomicInteger(-1);
        AtomicBoolean acceptSucceeded = new AtomicBoolean(false);

        TransactionTemplate tt1 = newTransactionTemplate();
        TransactionTemplate tt2 = newTransactionTemplate();

        WorkerBookingLocationDto loc = new WorkerBookingLocationDto();
        loc.setLatitude(BigDecimal.valueOf(20.01));
        loc.setLongitude(BigDecimal.valueOf(85.01));

        Future<?> f1 = executor.submit(() -> {
            try {
                barrier.await(5, TimeUnit.SECONDS);
                tt1.execute(status -> {
                    bookingService.acceptBooking(worker.getUserId(), bookingId, loc);
                    acceptSucceeded.set(true);
                    return null;
                });
            } catch (Exception ignored) {}
        });

        Future<?> f2 = executor.submit(() -> {
            try {
                barrier.await(5, TimeUnit.SECONDS);
                tt2.execute(status -> {
                    int affected = bookingRepository.expireUnassignedPendingBookings(
                            BookingStatus.PENDING,
                            BookingStatus.NO_WORKER,
                            deadline,
                            now
                    );
                    timeoutUpdated.set(affected);
                    return affected;
                });
            } catch (Exception ignored) {}
        });

        f1.get(10, TimeUnit.SECONDS);
        f2.get(10, TimeUnit.SECONDS);

        Booking finalBooking = bookingRepository.findById(bookingId).orElseThrow();
        if (acceptSucceeded.get()) {
            assertEquals(BookingStatus.ACCEPTED, finalBooking.getStatus());
            assertEquals(worker.getUserId(), finalBooking.getWorker().getUserId());
            assertEquals(0, timeoutUpdated.get(), "Timeout must affect 0 rows if accept won");
        } else {
            assertEquals(BookingStatus.NO_WORKER, finalBooking.getStatus());
            assertNull(finalBooking.getWorker());
            assertEquals(1, timeoutUpdated.get(), "Timeout must affect 1 row if timeout won");
        }
    }

    @Test
    @DisplayName("Scenario D: Multiple timeout attempts run concurrently; booking expired once with total count 1")
    void testMultipleTimeoutsConcurrently_ExpiresExactlyOnce() throws Exception {
        Customer customer = createCustomer("concurrency_d_cust@example.com");
        Service service = createService("Cleaning D");
        Booking booking = createBooking(customer, service);

        UUID bookingId = booking.getBookingId();
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime deadline = now.plusMinutes(5);

        CyclicBarrier barrier = new CyclicBarrier(2);
        AtomicInteger count1 = new AtomicInteger(-1);
        AtomicInteger count2 = new AtomicInteger(-1);

        TransactionTemplate tt1 = newTransactionTemplate();
        TransactionTemplate tt2 = newTransactionTemplate();

        Future<?> f1 = executor.submit(() -> {
            try {
                barrier.await(5, TimeUnit.SECONDS);
                tt1.execute(status -> {
                    int rows = bookingRepository.expireUnassignedPendingBookings(
                            BookingStatus.PENDING,
                            BookingStatus.NO_WORKER,
                            deadline,
                            now
                    );
                    count1.set(rows);
                    return rows;
                });
            } catch (Exception ignored) {}
        });

        Future<?> f2 = executor.submit(() -> {
            try {
                barrier.await(5, TimeUnit.SECONDS);
                tt2.execute(status -> {
                    int rows = bookingRepository.expireUnassignedPendingBookings(
                            BookingStatus.PENDING,
                            BookingStatus.NO_WORKER,
                            deadline,
                            now
                    );
                    count2.set(rows);
                    return rows;
                });
            } catch (Exception ignored) {}
        });

        f1.get(10, TimeUnit.SECONDS);
        f2.get(10, TimeUnit.SECONDS);

        assertEquals(1, count1.get() + count2.get(), "Exactly one timeout attempt must update the booking row");
        Booking finalBooking = bookingRepository.findById(bookingId).orElseThrow();
        assertEquals(BookingStatus.NO_WORKER, finalBooking.getStatus());
    }

    @Test
    @DisplayName("Scenario E: Multiple workers accept concurrently; exactly one worker is assigned")
    void testMultipleWorkersAcceptConcurrently_AtMostOneAssigned() throws Exception {
        Customer customer = createCustomer("concurrency_e_cust@example.com");
        Service service = createService("Cleaning E");
        Worker workerA = createWorker("worker_ea@example.com", service);
        Worker workerB = createWorker("worker_eb@example.com", service);
        Booking booking = createBooking(customer, service);
        bookingOfferRepository.save(new BookingOffer(booking, workerA));
        bookingOfferRepository.save(new BookingOffer(booking, workerB));

        UUID bookingId = booking.getBookingId();
        CyclicBarrier barrier = new CyclicBarrier(2);

        AtomicBoolean workerASucceeded = new AtomicBoolean(false);
        AtomicBoolean workerBSucceeded = new AtomicBoolean(false);

        TransactionTemplate tt1 = newTransactionTemplate();
        TransactionTemplate tt2 = newTransactionTemplate();

        WorkerBookingLocationDto loc = new WorkerBookingLocationDto();
        loc.setLatitude(BigDecimal.valueOf(20.01));
        loc.setLongitude(BigDecimal.valueOf(85.01));

        Future<?> f1 = executor.submit(() -> {
            try {
                barrier.await(5, TimeUnit.SECONDS);
                tt1.execute(status -> {
                    bookingService.acceptBooking(workerA.getUserId(), bookingId, loc);
                    workerASucceeded.set(true);
                    return null;
                });
            } catch (Exception ignored) {}
        });

        Future<?> f2 = executor.submit(() -> {
            try {
                barrier.await(5, TimeUnit.SECONDS);
                tt2.execute(status -> {
                    bookingService.acceptBooking(workerB.getUserId(), bookingId, loc);
                    workerBSucceeded.set(true);
                    return null;
                });
            } catch (Exception ignored) {}
        });

        f1.get(10, TimeUnit.SECONDS);
        f2.get(10, TimeUnit.SECONDS);

        // Verification: Exactly one worker acceptance succeeded
        assertTrue(workerASucceeded.get() ^ workerBSucceeded.get(),
                "Exactly one worker acceptance must succeed (XOR)");

        Booking finalBooking = bookingRepository.findById(bookingId).orElseThrow();
        assertEquals(BookingStatus.ACCEPTED, finalBooking.getStatus());
        UUID assignedWorkerId = finalBooking.getWorker().getUserId();
        assertTrue(assignedWorkerId.equals(workerA.getUserId()) || assignedWorkerId.equals(workerB.getUserId()),
                "Assigned worker must be either Worker A or Worker B");
    }

    @Test
    @DisplayName("Requirement 8: Transaction rollback releases pessimistic write lock; waiting transaction proceeds")
    void testTransactionRollbackReleasesPessimisticLock() throws Exception {
        Customer customer = createCustomer("concurrency_rb_cust@example.com");
        Service service = createService("Cleaning RB");
        Worker worker = createWorker("concurrency_rb_worker@example.com", service);
        Booking booking = createBooking(customer, service);
        bookingOfferRepository.save(new BookingOffer(booking, worker));

        UUID bookingId = booking.getBookingId();
        CountDownLatch lockAcquiredLatch = new CountDownLatch(1);
        CountDownLatch thread2WaitingLatch = new CountDownLatch(1);

        TransactionTemplate tt1 = newTransactionTemplate();
        TransactionTemplate tt2 = newTransactionTemplate();

        WorkerBookingLocationDto loc = new WorkerBookingLocationDto();
        loc.setLatitude(BigDecimal.valueOf(20.01));
        loc.setLongitude(BigDecimal.valueOf(85.01));

        // Thread 1: Acquires lock, then simulates failure to trigger rollback
        Future<?> f1 = executor.submit(() -> {
            try {
                tt1.execute(status -> {
                    bookingRepository.findByBookingIdForUpdate(bookingId).orElseThrow();
                    lockAcquiredLatch.countDown();

                    try {
                        thread2WaitingLatch.await(5, TimeUnit.SECONDS);
                        Thread.sleep(100);
                    } catch (InterruptedException ignored) {}

                    throw new RuntimeException("Simulated transaction failure for rollback");
                });
            } catch (RuntimeException ignored) {}
        });

        // Thread 2: Waits for lock, then successfully accepts booking upon lock release
        Future<?> f2 = executor.submit(() -> {
            try {
                lockAcquiredLatch.await(5, TimeUnit.SECONDS);
            } catch (InterruptedException ignored) {}

            thread2WaitingLatch.countDown();
            return tt2.execute(status -> {
                bookingService.acceptBooking(worker.getUserId(), bookingId, loc);
                return true;
            });
        });

        f1.get(10, TimeUnit.SECONDS);
        Object result2 = f2.get(10, TimeUnit.SECONDS);

        assertEquals(Boolean.TRUE, result2, "Thread 2 must succeed after lock release");
        Booking finalBooking = bookingRepository.findById(bookingId).orElseThrow();
        assertEquals(BookingStatus.ACCEPTED, finalBooking.getStatus());
        assertEquals(worker.getUserId(), finalBooking.getWorker().getUserId());
    }

    // ────────────────────────────────────────────────────────────
    //  Helper Methods for Test Data Creation
    // ────────────────────────────────────────────────────────────

    private Customer createCustomer(String email) {
        Credentials creds = new Credentials();
        creds.setEmail(email);
        creds.setPasswordHash("hashed_pass");
        creds.setRole(UserRole.USER);
        creds = credentialsRepository.save(creds);

        Users user = new Users();
        user.setCredentials(creds);
        user.setFullName("Customer Test");
        user.setAge(28);
        user.setGender("FEMALE");
        user.setPhone("99" + UUID.randomUUID().toString().substring(0, 8));
        user.setAddressLine1("Customer Road");
        user.setCity("Bhubaneswar");
        user.setState("Odisha");
        user.setPinCode("751001");
        user.setActive(true);
        user.setTermsAccepted(true);
        user = usersRepository.save(user);

        Customer customer = new Customer();
        customer.setUser(user);
        return customerRepository.save(customer);
    }

    private Service createService(String name) {
        Category category = new Category();
        category.setCategoryName("Category " + name);
        category.setActive(true);
        category = categoryRepository.save(category);

        Service service = new Service();
        service.setCategory(category);
        service.setServiceName(name);
        service.setActive(true);
        service.setBasePricePerHour(BigDecimal.valueOf(150.00));
        service.setMinimumServiceFee(BigDecimal.valueOf(80.00));
        return serviceRepository.save(service);
    }

    private Worker createWorker(String email, Service service) {
        Credentials creds = new Credentials();
        creds.setEmail(email);
        creds.setPasswordHash("hashed_pass");
        creds.setRole(UserRole.WORKER);
        creds = credentialsRepository.save(creds);

        Users user = new Users();
        user.setCredentials(creds);
        user.setFullName("Worker Test");
        user.setAge(32);
        user.setGender("MALE");
        user.setPhone("97" + UUID.randomUUID().toString().substring(0, 8));
        user.setAddressLine1("Worker Colony");
        user.setCity("Bhubaneswar");
        user.setState("Odisha");
        user.setPinCode("751001");
        user.setActive(true);
        user.setTermsAccepted(true);
        user = usersRepository.save(user);

        Worker worker = new Worker();
        worker.setUser(user);
        worker.setExperienceYears(4);
        worker.setQualification("Certified Expert");
        worker.setVerificationStatus(VerificationStatus.VERIFIED);
        worker.setAvailabilityStatus(AvailabilityStatus.AVAILABLE);
        worker.setCurrentLatitude(BigDecimal.valueOf(20.005));
        worker.setCurrentLongitude(BigDecimal.valueOf(85.005));
        worker.setLocationLastUpdatedAt(LocalDateTime.now());
        worker.setRating(BigDecimal.valueOf(4.8));
        worker = workerRepository.save(worker);

        WorkerService ws = new WorkerService(worker, service);
        ws.setId(new WorkerServiceId(worker.getUserId(), service.getServiceId()));
        workerServiceRepository.save(ws);

        return worker;
    }

    private Booking createBooking(Customer customer, Service service) {
        Booking booking = new Booking();
        booking.setCustomer(customer);
        booking.setService(service);
        booking.setStatus(BookingStatus.PENDING);
        booking.setServiceLatitude(BigDecimal.valueOf(20.0));
        booking.setServiceLongitude(BigDecimal.valueOf(85.0));
        booking.setWorkerAcceptanceLatitude(BigDecimal.ZERO);
        booking.setWorkerAcceptanceLongitude(BigDecimal.ZERO);
        return bookingRepository.save(booking);
    }
}

