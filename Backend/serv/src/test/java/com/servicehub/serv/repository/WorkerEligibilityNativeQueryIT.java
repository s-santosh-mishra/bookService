package com.servicehub.serv.repository;

import com.servicehub.serv.entity.Booking;
import com.servicehub.serv.entity.BookingOffer;
import com.servicehub.serv.entity.Category;
import com.servicehub.serv.entity.Credentials;
import com.servicehub.serv.entity.Customer;
import com.servicehub.serv.entity.Service;
import com.servicehub.serv.entity.Users;
import com.servicehub.serv.entity.Worker;
import com.servicehub.serv.entity.WorkerBookingRejection;
import com.servicehub.serv.entity.WorkerService;
import com.servicehub.serv.entity.WorkerServiceId;
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
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * PostgreSQL Integration Test for WorkerServiceRepository.findEligibleWorkersForOffer native query.
 *
 * Runs against an isolated, dynamically allocated Embedded PostgreSQL 15.6 instance.
 * Validates actual native PostgreSQL query execution, subqueries, row locking, and constraints.
 */
@DataJpaTest
class WorkerEligibilityNativeQueryIT {

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
    private WorkerServiceRepository workerServiceRepository;

    @Autowired
    private WorkerRepository workerRepository;

    @Autowired
    private UsersRepository usersRepository;

    @Autowired
    private CredentialsRepository credentialsRepository;

    @Autowired
    private ServiceRepository serviceRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private BookingOfferRepository bookingOfferRepository;

    @Autowired
    private WorkerBookingRejectionRepository workerBookingRejectionRepository;

    @Test
    @DisplayName("Comprehensive 12-worker scenario: only the genuinely eligible worker is returned by native PostgreSQL SQL")
    void testWorkerEligibilityAllTwelveScenarios() {
        LocalDateTime now = LocalDateTime.of(2026, 6, 1, 12, 0, 0);
        LocalDateTime freshAfter = now.minusMinutes(3);

        // Bounding box: center (20.0, 85.0), radius approx 5km
        double minLat = 19.955;
        double maxLat = 20.045;
        double minLon = 84.952;
        double maxLon = 85.048;

        // Base domain setup: Category, Services, Customer, and Target Booking
        Category category = createCategory("Home Cleaning");
        Service targetService = createService(category, "Deep Cleaning");
        Service otherService = createService(category, "Plumbing");
        Customer customer = createCustomer("testcust@example.com");
        Booking targetBooking = createBooking(customer, targetService, BookingStatus.PENDING, 20.0, 85.0);

        // 1. Worker 1: FULLY ELIGIBLE
        Worker eligibleWorker = createWorker(
                "eligible@test.com", "Eligible Worker", true,
                VerificationStatus.VERIFIED, AvailabilityStatus.AVAILABLE,
                20.01, 85.01, now.minusMinutes(1), targetService);

        // 2. Worker 2: Outside bounding box (lat 20.10 > maxLat 20.045)
        Worker outsideBoxWorker = createWorker(
                "outside@test.com", "Outside Box", true,
                VerificationStatus.VERIFIED, AvailabilityStatus.AVAILABLE,
                20.10, 85.01, now.minusMinutes(1), targetService);

        // 3. Worker 3: Stale GPS location (updated 5 min ago < freshAfter 3 min)
        Worker staleGpsWorker = createWorker(
                "stale@test.com", "Stale GPS", true,
                VerificationStatus.VERIFIED, AvailabilityStatus.AVAILABLE,
                20.01, 85.01, now.minusMinutes(5), targetService);

        // 4. Worker 4: Without GPS coordinates (NULL lat/lon)
        Worker noGpsWorker = createWorker(
                "nogps@test.com", "No GPS", true,
                VerificationStatus.VERIFIED, AvailabilityStatus.AVAILABLE,
                null, null, now.minusMinutes(1), targetService);

        // 5. Worker 5: AvailabilityStatus BUSY
        Worker busyWorker = createWorker(
                "busy@test.com", "Busy Worker", true,
                VerificationStatus.VERIFIED, AvailabilityStatus.BUSY,
                20.01, 85.01, now.minusMinutes(1), targetService);

        // 6. Worker 6: AvailabilityStatus UNAVAILABLE
        Worker unavailableWorker = createWorker(
                "unavail@test.com", "Unavailable Worker", true,
                VerificationStatus.VERIFIED, AvailabilityStatus.UNAVAILABLE,
                20.01, 85.01, now.minusMinutes(1), targetService);

        // 7. Worker 7: Unverified worker (VerificationStatus PENDING)
        Worker unverifiedWorker = createWorker(
                "unverified@test.com", "Unverified Worker", true,
                VerificationStatus.PENDING, AvailabilityStatus.AVAILABLE,
                20.01, 85.01, now.minusMinutes(1), targetService);

        // 8. Worker 8: Inactive user (Users.is_active = FALSE)
        Worker inactiveUserWorker = createWorker(
                "inactive@test.com", "Inactive User", false,
                VerificationStatus.VERIFIED, AvailabilityStatus.AVAILABLE,
                20.01, 85.01, now.minusMinutes(1), targetService);

        // 9. Worker 9: Worker who does NOT support the requested service (registered for otherService only)
        Worker otherServiceWorker = createWorker(
                "othersvc@test.com", "Other Service Worker", true,
                VerificationStatus.VERIFIED, AvailabilityStatus.AVAILABLE,
                20.01, 85.01, now.minusMinutes(1), otherService);

        // 10. Worker 10: Worker with an active conflicting booking (ACCEPTED)
        Worker conflictingBookingWorker = createWorker(
                "conflicting@test.com", "Conflicting Booking Worker", true,
                VerificationStatus.VERIFIED, AvailabilityStatus.AVAILABLE,
                20.01, 85.01, now.minusMinutes(1), targetService);
        createBookingWithWorker(customer, targetService, conflictingBookingWorker, BookingStatus.ACCEPTED);

        // 11. Worker 11: Worker who already received an offer for this booking
        Worker alreadyOfferedWorker = createWorker(
                "offered@test.com", "Already Offered Worker", true,
                VerificationStatus.VERIFIED, AvailabilityStatus.AVAILABLE,
                20.01, 85.01, now.minusMinutes(1), targetService);
        bookingOfferRepository.save(new BookingOffer(targetBooking, alreadyOfferedWorker));

        // 12. Worker 12: Worker who previously rejected this booking
        Worker rejectedWorker = createWorker(
                "rejected@test.com", "Rejected Worker", true,
                VerificationStatus.VERIFIED, AvailabilityStatus.AVAILABLE,
                20.01, 85.01, now.minusMinutes(1), targetService);
        createRejection(targetBooking, rejectedWorker);

        // Execute actual native SQL query in PostgreSQL 15.6
        List<Worker> result = workerServiceRepository.findEligibleWorkersForOffer(
                targetBooking.getBookingId(),
                targetService.getServiceId(),
                freshAfter,
                now,
                minLat,
                maxLat,
                minLon,
                maxLon
        );

        // Assertions: EXACT worker IDs
        assertEquals(1, result.size(), "Only the single eligible worker must be returned");
        assertEquals(eligibleWorker.getUserId(), result.get(0).getUserId(),
                "The returned worker must be the genuinely eligible worker");

        Set<UUID> returnedIds = result.stream().map(Worker::getUserId).collect(Collectors.toSet());
        assertTrue(returnedIds.contains(eligibleWorker.getUserId()));
        assertFalse(returnedIds.contains(outsideBoxWorker.getUserId()), "Outside box worker must be excluded");
        assertFalse(returnedIds.contains(staleGpsWorker.getUserId()), "Stale GPS worker must be excluded");
        assertFalse(returnedIds.contains(noGpsWorker.getUserId()), "Worker without coordinates must be excluded");
        assertFalse(returnedIds.contains(busyWorker.getUserId()), "BUSY worker must be excluded");
        assertFalse(returnedIds.contains(unavailableWorker.getUserId()), "UNAVAILABLE worker must be excluded");
        assertFalse(returnedIds.contains(unverifiedWorker.getUserId()), "Unverified worker must be excluded");
        assertFalse(returnedIds.contains(inactiveUserWorker.getUserId()), "Inactive user must be excluded");
        assertFalse(returnedIds.contains(otherServiceWorker.getUserId()), "Worker without service must be excluded");
        assertFalse(returnedIds.contains(conflictingBookingWorker.getUserId()), "Worker with active booking must be excluded");
        assertFalse(returnedIds.contains(alreadyOfferedWorker.getUserId()), "Already offered worker must be excluded");
        assertFalse(returnedIds.contains(rejectedWorker.getUserId()), "Previously rejected worker must be excluded");
    }

    @Test
    @DisplayName("Non-conflicting bookings (COMPLETED, CANCELLED) do NOT exclude the worker")
    void testNonConflictingBookingsDoNotExcludeWorker() {
        LocalDateTime now = LocalDateTime.of(2026, 6, 1, 12, 0, 0);
        LocalDateTime freshAfter = now.minusMinutes(3);

        Category category = createCategory("Repairs");
        Service service = createService(category, "AC Repair");
        Customer customer = createCustomer("customer2@example.com");
        Booking targetBooking = createBooking(customer, service, BookingStatus.PENDING, 20.0, 85.0);

        Worker workerWithCompletedBooking = createWorker(
                "completed@test.com", "Completed Worker", true,
                VerificationStatus.VERIFIED, AvailabilityStatus.AVAILABLE,
                20.01, 85.01, now.minusMinutes(1), service);
        createBookingWithWorker(customer, service, workerWithCompletedBooking, BookingStatus.COMPLETED);

        Worker workerWithCancelledBooking = createWorker(
                "cancelled@test.com", "Cancelled Worker", true,
                VerificationStatus.VERIFIED, AvailabilityStatus.AVAILABLE,
                20.02, 85.02, now.minusMinutes(1), service);
        createBookingWithWorker(customer, service, workerWithCancelledBooking, BookingStatus.CANCELLED);

        List<Worker> result = workerServiceRepository.findEligibleWorkersForOffer(
                targetBooking.getBookingId(),
                service.getServiceId(),
                freshAfter, now,
                19.9, 20.1, 84.9, 85.1
        );

        Set<UUID> resultIds = result.stream().map(Worker::getUserId).collect(Collectors.toSet());
        assertEquals(2, result.size());
        assertTrue(resultIds.contains(workerWithCompletedBooking.getUserId()));
        assertTrue(resultIds.contains(workerWithCancelledBooking.getUserId()));
    }

    @Test
    @DisplayName("Offers and rejections for DIFFERENT bookings do NOT exclude the worker")
    void testOffersAndRejectionsForDifferentBookingsDoNotExcludeWorker() {
        LocalDateTime now = LocalDateTime.of(2026, 6, 1, 12, 0, 0);
        LocalDateTime freshAfter = now.minusMinutes(3);

        Category category = createCategory("Electrician");
        Service service = createService(category, "Wiring");
        Customer customer = createCustomer("customer3@example.com");
        Booking targetBooking = createBooking(customer, service, BookingStatus.PENDING, 20.0, 85.0);
        Booking otherBooking = createBooking(customer, service, BookingStatus.PENDING, 20.0, 85.0);

        Worker workerOfferedOther = createWorker(
                "offeredother@test.com", "Offered Other", true,
                VerificationStatus.VERIFIED, AvailabilityStatus.AVAILABLE,
                20.01, 85.01, now.minusMinutes(1), service);
        bookingOfferRepository.save(new BookingOffer(otherBooking, workerOfferedOther));

        Worker workerRejectedOther = createWorker(
                "rejectedother@test.com", "Rejected Other", true,
                VerificationStatus.VERIFIED, AvailabilityStatus.AVAILABLE,
                20.02, 85.02, now.minusMinutes(1), service);
        createRejection(otherBooking, workerRejectedOther);

        List<Worker> result = workerServiceRepository.findEligibleWorkersForOffer(
                targetBooking.getBookingId(),
                service.getServiceId(),
                freshAfter, now,
                19.9, 20.1, 84.9, 85.1
        );

        Set<UUID> resultIds = result.stream().map(Worker::getUserId).collect(Collectors.toSet());
        assertEquals(2, result.size());
        assertTrue(resultIds.contains(workerOfferedOther.getUserId()));
        assertTrue(resultIds.contains(workerRejectedOther.getUserId()));
    }

    @Test
    @DisplayName("Exact freshness boundary timestamps (freshAfter and locationNow) are included")
    void testFreshnessBoundaryTimestamps() {
        LocalDateTime now = LocalDateTime.of(2026, 6, 1, 12, 0, 0);
        LocalDateTime freshAfter = now.minusMinutes(3);

        Category category = createCategory("Carpentry");
        Service service = createService(category, "Furniture Assembly");
        Customer customer = createCustomer("customer4@example.com");
        Booking booking = createBooking(customer, service, BookingStatus.PENDING, 20.0, 85.0);

        // At exact freshAfter boundary
        Worker atFreshAfter = createWorker(
                "atfreshafter@test.com", "At Fresh After", true,
                VerificationStatus.VERIFIED, AvailabilityStatus.AVAILABLE,
                20.01, 85.01, freshAfter, service);

        // At exact locationNow boundary
        Worker atLocationNow = createWorker(
                "atlocationnow@test.com", "At Location Now", true,
                VerificationStatus.VERIFIED, AvailabilityStatus.AVAILABLE,
                20.02, 85.02, now, service);

        List<Worker> result = workerServiceRepository.findEligibleWorkersForOffer(
                booking.getBookingId(),
                service.getServiceId(),
                freshAfter, now,
                19.9, 20.1, 84.9, 85.1
        );

        Set<UUID> resultIds = result.stream().map(Worker::getUserId).collect(Collectors.toSet());
        assertEquals(2, result.size());
        assertTrue(resultIds.contains(atFreshAfter.getUserId()));
        assertTrue(resultIds.contains(atLocationNow.getUserId()));
    }

    // ────────────────────────────────────────────────────────────
    //  Helper Methods for Test Data Setup
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
        service.setBasePricePerHour(BigDecimal.valueOf(200.00));
        service.setMinimumServiceFee(BigDecimal.valueOf(100.00));
        return serviceRepository.save(service);
    }

    private Customer createCustomer(String email) {
        Credentials creds = new Credentials();
        creds.setEmail(email);
        creds.setPasswordHash("hashed_password");
        creds.setRole(UserRole.USER);
        creds = credentialsRepository.save(creds);

        Users user = new Users();
        user.setCredentials(creds);
        user.setFullName("Customer User");
        user.setAge(25);
        user.setGender("OTHER");
        user.setPhone("9123456789");
        user.setAddressLine1("Customer Lane");
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

    private Worker createWorker(
            String email, String fullName, boolean isActive,
            VerificationStatus verificationStatus, AvailabilityStatus availabilityStatus,
            Double lat, Double lon, LocalDateTime locationUpdatedAt,
            Service service) {

        Credentials creds = new Credentials();
        creds.setEmail(email);
        creds.setPasswordHash("hashed_password");
        creds.setRole(UserRole.WORKER);
        creds = credentialsRepository.save(creds);

        Users user = new Users();
        user.setCredentials(creds);
        user.setFullName(fullName);
        user.setAge(30);
        user.setGender("MALE");
        user.setPhone("98" + UUID.randomUUID().toString().substring(0, 8));
        user.setAddressLine1("Worker Street");
        user.setCity("Bhubaneswar");
        user.setState("Odisha");
        user.setPinCode("751001");
        user.setActive(isActive);
        user.setTermsAccepted(true);
        user = usersRepository.save(user);

        Worker worker = new Worker();
        worker.setUser(user);
        worker.setExperienceYears(3);
        worker.setQualification("Experienced");
        worker.setVerificationStatus(verificationStatus);
        worker.setAvailabilityStatus(availabilityStatus);
        worker.setCurrentLatitude(lat != null ? BigDecimal.valueOf(lat) : null);
        worker.setCurrentLongitude(lon != null ? BigDecimal.valueOf(lon) : null);
        worker.setLocationLastUpdatedAt(locationUpdatedAt);
        worker.setRating(BigDecimal.valueOf(5.0));
        worker = workerRepository.save(worker);

        if (service != null) {
            WorkerService ws = new WorkerService(worker, service);
            ws.setId(new WorkerServiceId(worker.getUserId(), service.getServiceId()));
            workerServiceRepository.save(ws);
        }

        return worker;
    }

    private Booking createBooking(Customer customer, Service service, BookingStatus status, double lat, double lon) {
        Booking booking = new Booking();
        booking.setCustomer(customer);
        booking.setService(service);
        booking.setStatus(status);
        booking.setServiceLatitude(BigDecimal.valueOf(lat));
        booking.setServiceLongitude(BigDecimal.valueOf(lon));
        booking.setWorkerAcceptanceLatitude(BigDecimal.ZERO);
        booking.setWorkerAcceptanceLongitude(BigDecimal.ZERO);
        return bookingRepository.save(booking);
    }

    private Booking createBookingWithWorker(Customer customer, Service service, Worker worker, BookingStatus status) {
        Booking booking = new Booking();
        booking.setCustomer(customer);
        booking.setService(service);
        booking.setWorker(worker);
        booking.setStatus(status);
        booking.setServiceLatitude(BigDecimal.valueOf(20.0));
        booking.setServiceLongitude(BigDecimal.valueOf(85.0));
        booking.setWorkerAcceptanceLatitude(BigDecimal.valueOf(20.0));
        booking.setWorkerAcceptanceLongitude(BigDecimal.valueOf(85.0));
        return bookingRepository.save(booking);
    }

    private void createRejection(Booking booking, Worker worker) {
        WorkerBookingRejection rejection = new WorkerBookingRejection();
        rejection.setBooking(booking);
        rejection.setWorker(worker);
        workerBookingRejectionRepository.save(rejection);
    }
}
