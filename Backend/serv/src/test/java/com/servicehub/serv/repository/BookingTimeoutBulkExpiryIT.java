package com.servicehub.serv.repository;

import com.servicehub.serv.entity.Booking;
import com.servicehub.serv.entity.Category;
import com.servicehub.serv.entity.Credentials;
import com.servicehub.serv.entity.Customer;
import com.servicehub.serv.entity.Service;
import com.servicehub.serv.entity.Users;
import com.servicehub.serv.entity.Worker;
import com.servicehub.serv.enums.AvailabilityStatus;
import com.servicehub.serv.enums.BookingStatus;
import com.servicehub.serv.enums.UserRole;
import com.servicehub.serv.enums.VerificationStatus;
import io.zonky.test.db.postgres.embedded.EmbeddedPostgres;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * PostgreSQL Integration Test for production bulk booking timeout/expiry query:
 * BookingRepository.expireUnassignedPendingBookings(...)
 *
 * Runs against an isolated Embedded PostgreSQL 15.6 instance.
 */
@DataJpaTest
class BookingTimeoutBulkExpiryIT {

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
    private CustomerRepository customerRepository;

    @Autowired
    private ServiceRepository serviceRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private WorkerRepository workerRepository;

    @Autowired
    private UsersRepository usersRepository;

    @Autowired
    private CredentialsRepository credentialsRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private TestEntityManager entityManager;

    @Test
    @DisplayName("Scenario 1: Pending booking older than 30 minutes becomes NO_WORKER")
    void testPendingBookingOlderThan30MinutesBecomesNoWorker() {
        LocalDateTime now = LocalDateTime.of(2026, 6, 1, 12, 0, 0);
        LocalDateTime deadline = now.minusMinutes(30);

        Customer customer = createCustomer("timeout_cust1@example.com");
        Service service = createService("Cleaning");
        // Created 35 minutes ago (older than 30 min deadline)
        Booking booking = createBookingWithCreatedAt(customer, service, BookingStatus.PENDING, null, deadline.minusMinutes(5));

        int updatedCount = bookingRepository.expireUnassignedPendingBookings(
                BookingStatus.PENDING,
                BookingStatus.NO_WORKER,
                deadline,
                now
        );

        assertEquals(1, updatedCount, "Expected exactly 1 booking to be expired");
        Booking updated = bookingRepository.findById(booking.getBookingId()).orElseThrow();
        assertEquals(BookingStatus.NO_WORKER, updated.getStatus());
        assertEquals(now, updated.getUpdatedAt());
    }

    @Test
    @DisplayName("Scenario 2: Pending booking just before the 30-minute deadline remains PENDING")
    void testPendingBookingJustBeforeDeadlineRemainsPending() {
        LocalDateTime now = LocalDateTime.of(2026, 6, 1, 12, 0, 0);
        LocalDateTime deadline = now.minusMinutes(30);

        Customer customer = createCustomer("timeout_cust2@example.com");
        Service service = createService("Plumbing");
        // Created 1 second AFTER deadline (29 min 59 sec old, strictly before deadline)
        Booking booking = createBookingWithCreatedAt(customer, service, BookingStatus.PENDING, null, deadline.plusSeconds(1));

        int updatedCount = bookingRepository.expireUnassignedPendingBookings(
                BookingStatus.PENDING,
                BookingStatus.NO_WORKER,
                deadline,
                now
        );

        assertEquals(0, updatedCount, "Unexpired booking must not be updated");
        Booking reloaded = bookingRepository.findById(booking.getBookingId()).orElseThrow();
        assertEquals(BookingStatus.PENDING, reloaded.getStatus());
    }

    @Test
    @DisplayName("Scenario 3: Pending booking exactly at deadline follows <= boundary and becomes NO_WORKER")
    void testPendingBookingExactlyAtDeadlineBecomesNoWorker() {
        LocalDateTime now = LocalDateTime.of(2026, 6, 1, 12, 0, 0);
        LocalDateTime deadline = now.minusMinutes(30);

        Customer customer = createCustomer("timeout_cust3@example.com");
        Service service = createService("Electrician");
        // Created at exact deadline timestamp
        Booking booking = createBookingWithCreatedAt(customer, service, BookingStatus.PENDING, null, deadline);

        int updatedCount = bookingRepository.expireUnassignedPendingBookings(
                BookingStatus.PENDING,
                BookingStatus.NO_WORKER,
                deadline,
                now
        );

        assertEquals(1, updatedCount, "Boundary <= condition must include booking created exactly at deadline");
        Booking updated = bookingRepository.findById(booking.getBookingId()).orElseThrow();
        assertEquals(BookingStatus.NO_WORKER, updated.getStatus());
    }

    @Test
    @DisplayName("Scenario 4: Recently created pending booking remains PENDING")
    void testRecentlyCreatedPendingBookingRemainsPending() {
        LocalDateTime now = LocalDateTime.of(2026, 6, 1, 12, 0, 0);
        LocalDateTime deadline = now.minusMinutes(30);

        Customer customer = createCustomer("timeout_cust4@example.com");
        Service service = createService("Carpentry");
        // Created 2 minutes ago
        Booking booking = createBookingWithCreatedAt(customer, service, BookingStatus.PENDING, null, now.minusMinutes(2));

        int updatedCount = bookingRepository.expireUnassignedPendingBookings(
                BookingStatus.PENDING,
                BookingStatus.NO_WORKER,
                deadline,
                now
        );

        assertEquals(0, updatedCount, "Recently created booking must not be updated");
        Booking reloaded = bookingRepository.findById(booking.getBookingId()).orElseThrow();
        assertEquals(BookingStatus.PENDING, reloaded.getStatus());
    }

    @Test
    @DisplayName("Scenarios 5, 6, 7, 8: Non-pending bookings (ACCEPTED, IN_PROGRESS, COMPLETED, CANCELLED) are NEVER changed to NO_WORKER")
    void testNonPendingBookingsNeverChangedToNoWorker() {
        LocalDateTime now = LocalDateTime.of(2026, 6, 1, 12, 0, 0);
        LocalDateTime deadline = now.minusMinutes(30);

        Customer customer = createCustomer("timeout_cust5@example.com");
        Service service = createService("Painting");
        Worker worker = createWorker("worker_assigned@example.com");
        LocalDateTime createdWayBack = deadline.minusMinutes(60);

        // Scenario 5: ACCEPTED booking
        Booking accepted = createBookingWithCreatedAt(customer, service, BookingStatus.ACCEPTED, worker, createdWayBack);
        // Scenario 6: IN_PROGRESS booking
        Booking inProgress = createBookingWithCreatedAt(customer, service, BookingStatus.IN_PROGRESS, worker, createdWayBack);
        // Scenario 7: COMPLETED booking
        Booking completed = createBookingWithCreatedAt(customer, service, BookingStatus.COMPLETED, worker, createdWayBack);
        // Scenario 8: CANCELLED booking
        Booking cancelled = createBookingWithCreatedAt(customer, service, BookingStatus.CANCELLED, null, createdWayBack);

        int updatedCount = bookingRepository.expireUnassignedPendingBookings(
                BookingStatus.PENDING,
                BookingStatus.NO_WORKER,
                deadline,
                now
        );

        assertEquals(0, updatedCount, "Non-pending bookings must not be updated");
        assertEquals(BookingStatus.ACCEPTED, bookingRepository.findById(accepted.getBookingId()).orElseThrow().getStatus());
        assertEquals(BookingStatus.IN_PROGRESS, bookingRepository.findById(inProgress.getBookingId()).orElseThrow().getStatus());
        assertEquals(BookingStatus.COMPLETED, bookingRepository.findById(completed.getBookingId()).orElseThrow().getStatus());
        assertEquals(BookingStatus.CANCELLED, bookingRepository.findById(cancelled.getBookingId()).orElseThrow().getStatus());
    }

    @Test
    @DisplayName("Scenario 9: Multiple eligible expired bookings are updated correctly in one bulk operation")
    void testMultipleEligibleExpiredBookingsUpdatedInSingleBulkOperation() {
        LocalDateTime now = LocalDateTime.of(2026, 6, 1, 12, 0, 0);
        LocalDateTime deadline = now.minusMinutes(30);

        Customer customer = createCustomer("bulk_cust@example.com");
        Service service = createService("Deep Cleaning");
        Worker worker = createWorker("bulk_worker@example.com");

        // 3 eligible expired bookings
        Booking expired1 = createBookingWithCreatedAt(customer, service, BookingStatus.PENDING, null, deadline.minusMinutes(10));
        Booking expired2 = createBookingWithCreatedAt(customer, service, BookingStatus.PENDING, null, deadline.minusMinutes(5));
        Booking expired3 = createBookingWithCreatedAt(customer, service, BookingStatus.PENDING, null, deadline);

        // 2 ineligible bookings (one recent pending, one accepted)
        Booking recent = createBookingWithCreatedAt(customer, service, BookingStatus.PENDING, null, deadline.plusMinutes(10));
        Booking accepted = createBookingWithCreatedAt(customer, service, BookingStatus.ACCEPTED, worker, deadline.minusMinutes(20));

        int updatedCount = bookingRepository.expireUnassignedPendingBookings(
                BookingStatus.PENDING,
                BookingStatus.NO_WORKER,
                deadline,
                now
        );

        assertEquals(3, updatedCount, "All 3 eligible bookings must be updated in a single bulk statement");
        assertEquals(BookingStatus.NO_WORKER, bookingRepository.findById(expired1.getBookingId()).orElseThrow().getStatus());
        assertEquals(BookingStatus.NO_WORKER, bookingRepository.findById(expired2.getBookingId()).orElseThrow().getStatus());
        assertEquals(BookingStatus.NO_WORKER, bookingRepository.findById(expired3.getBookingId()).orElseThrow().getStatus());
        assertEquals(BookingStatus.PENDING, bookingRepository.findById(recent.getBookingId()).orElseThrow().getStatus());
        assertEquals(BookingStatus.ACCEPTED, bookingRepository.findById(accepted.getBookingId()).orElseThrow().getStatus());
    }

    @Test
    @DisplayName("Scenario 10: Repeated expiry execution does not modify already expired bookings again")
    void testRepeatedExpiryExecutionDoesNotModifyAlreadyExpiredBookings() {
        LocalDateTime now = LocalDateTime.of(2026, 6, 1, 12, 0, 0);
        LocalDateTime deadline = now.minusMinutes(30);

        Customer customer = createCustomer("repeat_cust@example.com");
        Service service = createService("Gardening");
        Booking expired = createBookingWithCreatedAt(customer, service, BookingStatus.PENDING, null, deadline.minusMinutes(15));

        // First run
        int firstRunCount = bookingRepository.expireUnassignedPendingBookings(
                BookingStatus.PENDING,
                BookingStatus.NO_WORKER,
                deadline,
                now
        );
        assertEquals(1, firstRunCount);
        assertEquals(BookingStatus.NO_WORKER, bookingRepository.findById(expired.getBookingId()).orElseThrow().getStatus());

        // Second run with updated timestamp
        LocalDateTime nowLater = now.plusSeconds(10);
        int secondRunCount = bookingRepository.expireUnassignedPendingBookings(
                BookingStatus.PENDING,
                BookingStatus.NO_WORKER,
                nowLater.minusMinutes(30),
                nowLater
        );

        assertEquals(0, secondRunCount, "Repeated execution must not modify already expired bookings");
        assertEquals(BookingStatus.NO_WORKER, bookingRepository.findById(expired.getBookingId()).orElseThrow().getStatus());
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

    private Worker createWorker(String email) {
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
        return workerRepository.save(worker);
    }

    private Booking createBookingWithCreatedAt(
            Customer customer,
            Service service,
            BookingStatus status,
            Worker worker,
            LocalDateTime createdAt) {

        Booking booking = new Booking();
        booking.setCustomer(customer);
        booking.setService(service);
        booking.setWorker(worker);
        booking.setStatus(status);
        booking.setServiceLatitude(BigDecimal.valueOf(20.0));
        booking.setServiceLongitude(BigDecimal.valueOf(85.0));
        booking.setWorkerAcceptanceLatitude(BigDecimal.ZERO);
        booking.setWorkerAcceptanceLongitude(BigDecimal.ZERO);
        Booking saved = bookingRepository.saveAndFlush(booking);

        if (createdAt != null) {
            jdbcTemplate.update(
                    "UPDATE bookings SET created_at = ? WHERE booking_id = ?",
                    createdAt,
                    saved.getBookingId()
            );
            entityManager.clear();
        }

        return saved;
    }
}

