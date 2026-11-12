package com.servicehub.serv.repository;

import com.servicehub.serv.entity.Booking;
import com.servicehub.serv.entity.BookingOffer;
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
import org.hibernate.exception.ConstraintViolationException;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.postgresql.util.PSQLException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * PostgreSQL Integration Test for the unique constraint on booking_offers (uk_booking_offer_booking_worker).
 *
 * Runs against an isolated Embedded PostgreSQL 15.6 instance to exercise actual database-level constraints.
 */
@DataJpaTest
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class BookingOfferUniqueConstraintIT {

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

        // Fulfill application.properties placeholders for isolated test execution
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
    private BookingOfferRepository bookingOfferRepository;

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private WorkerRepository workerRepository;

    @Autowired
    private UsersRepository usersRepository;

    @Autowired
    private CredentialsRepository credentialsRepository;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private ServiceRepository serviceRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private PlatformTransactionManager transactionManager;

    private TransactionTemplate transactionTemplate;

    @BeforeEach
    void setUp() {
        transactionTemplate = new TransactionTemplate(transactionManager);
        bookingOfferRepository.deleteAll();
    }

    @Test
    @DisplayName("Duplicate offer for same (booking_id, worker_id) violates PostgreSQL unique constraint uk_booking_offer_booking_worker")
    void testDuplicateOfferThrowsPostgresUniqueConstraintViolation() {
        Category category = createCategory("Cleaning");
        Service service = createService(category, "Standard Cleaning");
        Customer customer = createCustomer("cust_offer1@example.com");
        Booking booking = createBooking(customer, service);
        Worker worker = createWorker("worker_offer1@example.com");

        // 1. First offer insertion succeeds
        BookingOffer firstOffer = transactionTemplate.execute(status ->
                bookingOfferRepository.saveAndFlush(new BookingOffer(booking, worker))
        );
        assertNotNull(firstOffer);
        assertNotNull(firstOffer.getOfferId());
        assertEquals(1L, bookingOfferRepository.count(), "Exactly 1 offer must be persisted initially");

        // 2. Second offer for same (booking, worker) MUST trigger PostgreSQL unique constraint violation
        DataIntegrityViolationException ex = assertThrows(DataIntegrityViolationException.class, () ->
                transactionTemplate.execute(status ->
                        bookingOfferRepository.saveAndFlush(new BookingOffer(booking, worker))
                )
        );

        // 3. Assert actual PostgreSQL constraint violation details
        Throwable rootCause = ex.getRootCause();
        assertNotNull(rootCause, "Root cause must be present");
        assertTrue(rootCause instanceof PSQLException, "Root cause must be PSQLException");

        PSQLException psqlException = (PSQLException) rootCause;
        assertEquals("23505", psqlException.getSQLState(), "PostgreSQL SQLState 23505 represents unique_violation");
        assertTrue(psqlException.getMessage().contains("uk_booking_offer_booking_worker"),
                "Error message must reference constraint 'uk_booking_offer_booking_worker'");

        if (ex.getCause() instanceof ConstraintViolationException cve) {
            assertEquals("uk_booking_offer_booking_worker", cve.getConstraintName());
        }

        // 4. Verify persisted record count after failed insert
        assertEquals(1L, bookingOfferRepository.count(),
                "Persisted record count must remain exactly 1 after duplicate offer rejection");
    }

    @Test
    @DisplayName("Two offers for DIFFERENT workers on the SAME booking are allowed")
    void testTwoOffersForDifferentWorkersOnSameBookingAllowed() {
        Category category = createCategory("Electrician");
        Service service = createService(category, "Wiring Repair");
        Customer customer = createCustomer("cust_diff_workers@example.com");
        Booking booking = createBooking(customer, service);
        Worker workerA = createWorker("worker_a@example.com");
        Worker workerB = createWorker("worker_b@example.com");

        transactionTemplate.execute(status -> {
            bookingOfferRepository.saveAndFlush(new BookingOffer(booking, workerA));
            bookingOfferRepository.saveAndFlush(new BookingOffer(booking, workerB));
            return null;
        });

        assertEquals(2L, bookingOfferRepository.count(), "Both offers for different workers must be persisted");

        List<BookingOffer> offers = bookingOfferRepository.findAll();
        assertEquals(2, offers.size());
        assertTrue(offers.stream().anyMatch(o -> o.getWorker().getUserId().equals(workerA.getUserId())));
        assertTrue(offers.stream().anyMatch(o -> o.getWorker().getUserId().equals(workerB.getUserId())));
    }

    @Test
    @DisplayName("The SAME worker can receive offers for DIFFERENT bookings")
    void testSameWorkerCanReceiveOffersForDifferentBookings() {
        Category category = createCategory("Plumbing");
        Service service = createService(category, "Pipe Leakage");
        Customer customerA = createCustomer("cust_booking_a@example.com");
        Customer customerB = createCustomer("cust_booking_b@example.com");
        Booking bookingA = createBooking(customerA, service);
        Booking bookingB = createBooking(customerB, service);
        Worker worker = createWorker("multi_booking_worker@example.com");

        transactionTemplate.execute(status -> {
            bookingOfferRepository.saveAndFlush(new BookingOffer(bookingA, worker));
            bookingOfferRepository.saveAndFlush(new BookingOffer(bookingB, worker));
            return null;
        });

        assertEquals(2L, bookingOfferRepository.count(), "Both offers for different bookings must be persisted");

        List<BookingOffer> offers = bookingOfferRepository.findAll();
        assertEquals(2, offers.size());
        assertTrue(offers.stream().anyMatch(o -> o.getBooking().getBookingId().equals(bookingA.getBookingId())));
        assertTrue(offers.stream().anyMatch(o -> o.getBooking().getBookingId().equals(bookingB.getBookingId())));
    }

    // ────────────────────────────────────────────────────────────
    //  Helper Methods for Test Data Creation
    // ────────────────────────────────────────────────────────────

    private Category createCategory(String name) {
        Category category = new Category();
        category.setCategoryName(name);
        category.setActive(true);
        return categoryRepository.save(category);
    }

    private Service createService(Category category, String name) {
        Service service = new Service();
        service.setCategory(category);
        service.setServiceName(name);
        service.setActive(true);
        service.setBasePricePerHour(BigDecimal.valueOf(150.00));
        service.setMinimumServiceFee(BigDecimal.valueOf(80.00));
        return serviceRepository.save(service);
    }

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

