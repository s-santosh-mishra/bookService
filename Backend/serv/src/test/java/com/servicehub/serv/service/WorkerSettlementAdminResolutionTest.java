package com.servicehub.serv.service;

import com.servicehub.serv.entity.*;
import com.servicehub.serv.enums.PaymentMethod;
import com.servicehub.serv.enums.PaymentStatus;
import com.servicehub.serv.enums.SettlementStatus;
import com.servicehub.serv.enums.UserRole;
import com.servicehub.serv.exception.ResourceNotFoundException;
import com.servicehub.serv.repository.*;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Example;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.repository.query.FluentQuery;

import java.lang.reflect.Proxy;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.*;

public class WorkerSettlementAdminResolutionTest {

    @Test
    void createsAdminUserWhenOnlyPresentInCredentialsRepository() {
        UUID adminId = UUID.randomUUID();
        UUID workerId = UUID.randomUUID();
        UUID paymentId = UUID.randomUUID();
        LocalDate businessDate = LocalDate.of(2026, 11, 13);

        Credentials adminCredentials = new Credentials();
        adminCredentials.setUserId(adminId);
        adminCredentials.setEmail("admin@servicehub.com");
        adminCredentials.setRole(UserRole.ADMIN);

        Map<UUID, Users> usersDb = new HashMap<>();
        Map<UUID, Credentials> credentialsDb = new HashMap<>();
        credentialsDb.put(adminId, adminCredentials);

        Worker worker = new Worker();
        worker.setUserId(workerId);
        Users workerUser = new Users();
        workerUser.setUserId(workerId);
        workerUser.setFullName("Test Worker");
        worker.setUser(workerUser);
        worker.setBankAccountHolderName("Test Worker");
        worker.setBankAccountNumber("001234567890");
        worker.setBankIfscCode("BKID0001234");
        worker.setBankName("Bank of India");

        Payment payment = new Payment();
        payment.setPaymentId(paymentId);
        payment.setAmount(new BigDecimal("1000.00"));
        payment.setPaymentMethod(PaymentMethod.RAZORPAY);
        payment.setStatus(PaymentStatus.SUCCESS);
        payment.setPaidAt(LocalDateTime.of(2026, 11, 13, 15, 0));

        WorkerRepository workerRepo = createProxy(WorkerRepository.class, (method, args) -> {
            if ("findByUserIdForUpdate".equals(method.getName())) {
                return Optional.of(worker);
            }
            return null;
        });

        UsersRepository usersRepo = createProxy(UsersRepository.class, (method, args) -> {
            if ("findById".equals(method.getName())) {
                return Optional.ofNullable(usersDb.get(args[0]));
            }
            if ("save".equals(method.getName())) {
                Users u = (Users) args[0];
                usersDb.put(u.getUserId(), u);
                return u;
            }
            return null;
        });

        CredentialsRepository credentialsRepo = createProxy(CredentialsRepository.class, (method, args) -> {
            if ("findById".equals(method.getName())) {
                return Optional.ofNullable(credentialsDb.get(args[0]));
            }
            return null;
        });

        PaymentRepository paymentRepo = createProxy(PaymentRepository.class, (method, args) -> {
            if ("findEligiblePaymentsForWorkerSettlementForUpdate".equals(method.getName())) {
                return List.of(payment);
            }
            return null;
        });

        WorkerSettlementRepository settlementRepo = createProxy(WorkerSettlementRepository.class, (method, args) -> {
            if ("save".equals(method.getName())) {
                WorkerSettlement s = (WorkerSettlement) args[0];
                s.setSettlementId(UUID.randomUUID());
                return s;
            }
            return null;
        });

        WorkerSettlementItemRepository itemRepo = createProxy(WorkerSettlementItemRepository.class, (method, args) -> {
            if ("saveAll".equals(method.getName())) {
                return args[0];
            }
            return null;
        });

        SettlementBusinessDateCalculator dateCalc = new SettlementBusinessDateCalculator(
                java.time.Clock.system(SettlementBusinessDateCalculator.SETTLEMENT_ZONE)
        );

        WorkerSettlementService service = new WorkerSettlementService(
                workerRepo,
                usersRepo,
                credentialsRepo,
                paymentRepo,
                settlementRepo,
                itemRepo,
                null,
                dateCalc
        );

        // Before creation, users table has no record for admin
        assertFalse(usersDb.containsKey(adminId));

        WorkerSettlement settlement = service.createSettlement(
                workerId,
                List.of(paymentId),
                businessDate,
                adminId
        );

        assertNotNull(settlement);
        assertEquals(SettlementStatus.PENDING, settlement.getStatus());
        assertEquals(new BigDecimal("1000.00"), settlement.getAmount());

        // Admin was created in users table so foreign key constraint is satisfied
        assertTrue(usersDb.containsKey(adminId), "Admin Users entity must be persisted to database");
        Users savedAdmin = usersDb.get(adminId);
        assertEquals(adminId, savedAdmin.getUserId());
        assertEquals("Administrator", savedAdmin.getFullName());
        assertSame(savedAdmin, settlement.getSettledBy());
    }

    @Test
    void throwsAdminNotFoundWhenNeitherInUsersNorCredentials() {
        UUID unknownAdminId = UUID.randomUUID();
        UUID workerId = UUID.randomUUID();

        Worker worker = new Worker();
        worker.setUserId(workerId);
        worker.setBankAccountHolderName("Test Worker");
        worker.setBankAccountNumber("001234567890");
        worker.setBankIfscCode("BKID0001234");
        worker.setBankName("Bank of India");

        WorkerRepository workerRepo = createProxy(WorkerRepository.class, (method, args) -> {
            if ("findByUserIdForUpdate".equals(method.getName())) {
                return Optional.of(worker);
            }
            return null;
        });

        UsersRepository usersRepo = createProxy(UsersRepository.class, (method, args) -> {
            if ("findById".equals(method.getName())) {
                return Optional.empty();
            }
            return null;
        });

        CredentialsRepository credentialsRepo = createProxy(CredentialsRepository.class, (method, args) -> {
            if ("findById".equals(method.getName())) {
                return Optional.empty();
            }
            return null;
        });

        WorkerSettlementService service = new WorkerSettlementService(
                workerRepo,
                usersRepo,
                credentialsRepo,
                null,
                null,
                null,
                null,
                null
        );

        ResourceNotFoundException ex = assertThrows(
                ResourceNotFoundException.class,
                () -> service.createSettlement(workerId, List.of(UUID.randomUUID()), unknownAdminId)
        );

        assertEquals("Admin not found.", ex.getMessage());
    }

    @Test
    void throwsExceptionWhenCredentialsRoleIsNotAdmin() {
        UUID nonAdminId = UUID.randomUUID();
        UUID workerId = UUID.randomUUID();

        Credentials userCredentials = new Credentials();
        userCredentials.setUserId(nonAdminId);
        userCredentials.setRole(UserRole.USER);

        Worker worker = new Worker();
        worker.setUserId(workerId);
        worker.setBankAccountHolderName("Test Worker");
        worker.setBankAccountNumber("001234567890");
        worker.setBankIfscCode("BKID0001234");
        worker.setBankName("Bank of India");

        WorkerRepository workerRepo = createProxy(WorkerRepository.class, (method, args) -> {
            if ("findByUserIdForUpdate".equals(method.getName())) {
                return Optional.of(worker);
            }
            return null;
        });

        UsersRepository usersRepo = createProxy(UsersRepository.class, (method, args) -> {
            if ("findById".equals(method.getName())) {
                return Optional.empty();
            }
            return null;
        });

        CredentialsRepository credentialsRepo = createProxy(CredentialsRepository.class, (method, args) -> {
            if ("findById".equals(method.getName())) {
                return Optional.of(userCredentials);
            }
            return null;
        });

        WorkerSettlementService service = new WorkerSettlementService(
                workerRepo,
                usersRepo,
                credentialsRepo,
                null,
                null,
                null,
                null,
                null
        );

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> service.createSettlement(workerId, List.of(UUID.randomUUID()), nonAdminId)
        );

        assertEquals("Settlement updates are restricted to admins.", ex.getMessage());
    }

    @Test
    void existingAdminProfileIsNeverOverwritten() {
        UUID adminId = UUID.randomUUID();
        UUID workerId = UUID.randomUUID();
        UUID paymentId = UUID.randomUUID();
        LocalDate businessDate = LocalDate.of(2026, 11, 13);

        Credentials adminCredentials = new Credentials();
        adminCredentials.setUserId(adminId);
        adminCredentials.setEmail("admin@servicehub.com");
        adminCredentials.setRole(UserRole.ADMIN);

        Users existingAdmin = new Users();
        existingAdmin.setUserId(adminId);
        existingAdmin.setCredentials(adminCredentials);
        existingAdmin.setFullName("Existing Admin Custom Name");
        existingAdmin.setAge(45);
        existingAdmin.setGender("FEMALE");
        existingAdmin.setPhone("9876543210");
        existingAdmin.setAddressLine1("123 Custom Road");
        existingAdmin.setCity("Cuttack");
        existingAdmin.setState("Odisha");
        existingAdmin.setPinCode("753001");
        existingAdmin.setActive(true);
        existingAdmin.setTermsAccepted(true);

        java.util.concurrent.atomic.AtomicInteger usersSaveCount = new java.util.concurrent.atomic.AtomicInteger(0);

        Worker worker = new Worker();
        worker.setUserId(workerId);
        Users workerUser = new Users();
        workerUser.setUserId(workerId);
        worker.setUser(workerUser);
        worker.setBankAccountHolderName("Test Worker");
        worker.setBankAccountNumber("001234567890");
        worker.setBankIfscCode("BKID0001234");
        worker.setBankName("Bank of India");

        Payment payment = new Payment();
        payment.setPaymentId(paymentId);
        payment.setAmount(new BigDecimal("500.00"));
        payment.setPaymentMethod(PaymentMethod.RAZORPAY);
        payment.setStatus(PaymentStatus.SUCCESS);
        payment.setPaidAt(LocalDateTime.of(2026, 11, 13, 14, 0));

        WorkerRepository workerRepo = createProxy(WorkerRepository.class, (method, args) -> {
            if ("findByUserIdForUpdate".equals(method.getName())) {
                return Optional.of(worker);
            }
            return null;
        });

        UsersRepository usersRepo = createProxy(UsersRepository.class, (method, args) -> {
            if ("findById".equals(method.getName())) {
                return Optional.of(existingAdmin);
            }
            if ("save".equals(method.getName())) {
                usersSaveCount.incrementAndGet();
                return args[0];
            }
            return null;
        });

        CredentialsRepository credentialsRepo = createProxy(CredentialsRepository.class, (method, args) -> {
            if ("findById".equals(method.getName()) || "findByUserIdForUpdate".equals(method.getName())) {
                return Optional.of(adminCredentials);
            }
            return null;
        });

        PaymentRepository paymentRepo = createProxy(PaymentRepository.class, (method, args) -> {
            if ("findEligiblePaymentsForWorkerSettlementForUpdate".equals(method.getName())) {
                return List.of(payment);
            }
            return null;
        });

        WorkerSettlementRepository settlementRepo = createProxy(WorkerSettlementRepository.class, (method, args) -> {
            if ("save".equals(method.getName())) {
                WorkerSettlement s = (WorkerSettlement) args[0];
                s.setSettlementId(UUID.randomUUID());
                return s;
            }
            return null;
        });

        WorkerSettlementItemRepository itemRepo = createProxy(WorkerSettlementItemRepository.class, (method, args) -> {
            if ("saveAll".equals(method.getName())) {
                return args[0];
            }
            return null;
        });

        SettlementBusinessDateCalculator dateCalc = new SettlementBusinessDateCalculator(
                java.time.Clock.system(SettlementBusinessDateCalculator.SETTLEMENT_ZONE)
        );

        WorkerSettlementService service = new WorkerSettlementService(
                workerRepo,
                usersRepo,
                credentialsRepo,
                paymentRepo,
                settlementRepo,
                itemRepo,
                null,
                dateCalc
        );

        WorkerSettlement settlement = service.createSettlement(
                workerId,
                List.of(paymentId),
                businessDate,
                adminId
        );

        assertNotNull(settlement);
        assertSame(existingAdmin, settlement.getSettledBy());
        assertEquals("Existing Admin Custom Name", existingAdmin.getFullName());
        assertEquals("9876543210", existingAdmin.getPhone());
        assertEquals("123 Custom Road", existingAdmin.getAddressLine1());
        assertEquals(0, usersSaveCount.get(), "usersRepository.save() must NOT be called when admin profile exists");
    }

    @Test
    void adminEntityPopulatesEveryNotNullFieldWithValidPostgreSqlValues() {
        UUID adminId = UUID.randomUUID();
        UUID workerId = UUID.randomUUID();
        UUID paymentId = UUID.randomUUID();
        LocalDate businessDate = LocalDate.of(2026, 11, 13);

        Credentials adminCredentials = new Credentials();
        adminCredentials.setUserId(adminId);
        adminCredentials.setEmail("admin@servicehub.com");
        adminCredentials.setRole(UserRole.ADMIN);

        java.util.concurrent.atomic.AtomicReference<Users> savedUserRef = new java.util.concurrent.atomic.AtomicReference<>();

        Worker worker = new Worker();
        worker.setUserId(workerId);
        Users workerUser = new Users();
        workerUser.setUserId(workerId);
        worker.setUser(workerUser);
        worker.setBankAccountHolderName("Test Worker");
        worker.setBankAccountNumber("001234567890");
        worker.setBankIfscCode("BKID0001234");
        worker.setBankName("Bank of India");

        Payment payment = new Payment();
        payment.setPaymentId(paymentId);
        payment.setAmount(new BigDecimal("1000.00"));
        payment.setPaymentMethod(PaymentMethod.RAZORPAY);
        payment.setStatus(PaymentStatus.SUCCESS);
        payment.setPaidAt(LocalDateTime.of(2026, 11, 13, 15, 0));

        WorkerRepository workerRepo = createProxy(WorkerRepository.class, (method, args) -> {
            if ("findByUserIdForUpdate".equals(method.getName())) {
                return Optional.of(worker);
            }
            return null;
        });

        UsersRepository usersRepo = createProxy(UsersRepository.class, (method, args) -> {
            if ("findById".equals(method.getName())) {
                return Optional.ofNullable(savedUserRef.get());
            }
            if ("save".equals(method.getName())) {
                Users u = (Users) args[0];
                savedUserRef.set(u);
                return u;
            }
            return null;
        });

        CredentialsRepository credentialsRepo = createProxy(CredentialsRepository.class, (method, args) -> {
            if ("findById".equals(method.getName()) || "findByUserIdForUpdate".equals(method.getName())) {
                return Optional.of(adminCredentials);
            }
            return null;
        });

        PaymentRepository paymentRepo = createProxy(PaymentRepository.class, (method, args) -> {
            if ("findEligiblePaymentsForWorkerSettlementForUpdate".equals(method.getName())) {
                return List.of(payment);
            }
            return null;
        });

        WorkerSettlementRepository settlementRepo = createProxy(WorkerSettlementRepository.class, (method, args) -> {
            if ("save".equals(method.getName())) {
                WorkerSettlement s = (WorkerSettlement) args[0];
                s.setSettlementId(UUID.randomUUID());
                return s;
            }
            return null;
        });

        WorkerSettlementItemRepository itemRepo = createProxy(WorkerSettlementItemRepository.class, (method, args) -> {
            if ("saveAll".equals(method.getName())) {
                return args[0];
            }
            return null;
        });

        SettlementBusinessDateCalculator dateCalc = new SettlementBusinessDateCalculator(
                java.time.Clock.system(SettlementBusinessDateCalculator.SETTLEMENT_ZONE)
        );

        WorkerSettlementService service = new WorkerSettlementService(
                workerRepo,
                usersRepo,
                credentialsRepo,
                paymentRepo,
                settlementRepo,
                itemRepo,
                null,
                dateCalc
        );

        service.createSettlement(workerId, List.of(paymentId), businessDate, adminId);

        Users createdAdmin = savedUserRef.get();
        assertNotNull(createdAdmin, "Users entity must be created and saved");

        // Validate each PostgreSQL NOT NULL column constraint
        assertNotNull(createdAdmin.getUserId(), "user_id NOT NULL");
        assertEquals(adminId, createdAdmin.getUserId());

        assertNotNull(createdAdmin.getCredentials(), "credentials FK NOT NULL");
        assertEquals(adminId, createdAdmin.getCredentials().getUserId());

        assertNotNull(createdAdmin.getFullName(), "full_name NOT NULL");
        assertTrue(createdAdmin.getFullName().length() > 0 && createdAdmin.getFullName().length() <= 150,
                "full_name varchar(150)");

        assertNotNull(createdAdmin.getAge(), "age NOT NULL");
        assertTrue(createdAdmin.getAge() >= 18, "age must be positive integer");

        assertNotNull(createdAdmin.getGender(), "gender NOT NULL");
        assertTrue(createdAdmin.getGender().length() <= 20, "gender varchar(20)");

        assertNotNull(createdAdmin.getPhone(), "phone NOT NULL");
        assertTrue(createdAdmin.getPhone().length() <= 20, "phone varchar(20)");

        assertNotNull(createdAdmin.getAddressLine1(), "address_line1 NOT NULL");
        assertTrue(createdAdmin.getAddressLine1().length() <= 255, "address_line1 varchar(255)");

        assertNotNull(createdAdmin.getCity(), "city NOT NULL");
        assertTrue(createdAdmin.getCity().length() <= 100, "city varchar(100)");

        assertNotNull(createdAdmin.getState(), "state NOT NULL");
        assertTrue(createdAdmin.getState().length() <= 100, "state varchar(100)");

        assertNotNull(createdAdmin.getPinCode(), "pin_code NOT NULL");
        assertTrue(createdAdmin.getPinCode().length() <= 10, "pin_code varchar(10)");

        assertTrue(createdAdmin.isActive(), "is_active boolean NOT NULL");
        assertTrue(createdAdmin.isTermsAccepted(), "terms_accepted boolean NOT NULL");
    }

    @Test
    void concurrentSettlementRequestsDoNotDuplicateOrFail() throws Exception {
        UUID adminId = UUID.randomUUID();
        UUID workerId = UUID.randomUUID();
        LocalDate businessDate = LocalDate.of(2026, 11, 13);

        Credentials adminCredentials = new Credentials();
        adminCredentials.setUserId(adminId);
        adminCredentials.setEmail("admin@servicehub.com");
        adminCredentials.setRole(UserRole.ADMIN);

        java.util.concurrent.ConcurrentHashMap<UUID, Users> usersDb = new java.util.concurrent.ConcurrentHashMap<>();
        java.util.concurrent.atomic.AtomicInteger saveCallCount = new java.util.concurrent.atomic.AtomicInteger(0);

        Worker worker = new Worker();
        worker.setUserId(workerId);
        Users workerUser = new Users();
        workerUser.setUserId(workerId);
        worker.setUser(workerUser);
        worker.setBankAccountHolderName("Test Worker");
        worker.setBankAccountNumber("001234567890");
        worker.setBankIfscCode("BKID0001234");
        worker.setBankName("Bank of India");

        Payment payment1 = new Payment();
        payment1.setPaymentId(UUID.randomUUID());
        payment1.setAmount(new BigDecimal("500.00"));
        payment1.setPaymentMethod(PaymentMethod.RAZORPAY);
        payment1.setStatus(PaymentStatus.SUCCESS);
        payment1.setPaidAt(LocalDateTime.of(2026, 11, 13, 14, 0));

        Payment payment2 = new Payment();
        payment2.setPaymentId(UUID.randomUUID());
        payment2.setAmount(new BigDecimal("700.00"));
        payment2.setPaymentMethod(PaymentMethod.RAZORPAY);
        payment2.setStatus(PaymentStatus.SUCCESS);
        payment2.setPaidAt(LocalDateTime.of(2026, 11, 13, 14, 30));

        WorkerRepository workerRepo = createProxy(WorkerRepository.class, (method, args) -> {
            if ("findByUserIdForUpdate".equals(method.getName())) {
                return Optional.of(worker);
            }
            return null;
        });

        // Simulate pessimistic locking on credentials
        Object credLock = new Object();
        CredentialsRepository credentialsRepo = createProxy(CredentialsRepository.class, (method, args) -> {
            if ("findByUserIdForUpdate".equals(method.getName()) || "findById".equals(method.getName())) {
                return Optional.of(adminCredentials);
            }
            return null;
        });

        UsersRepository usersRepo = createProxy(UsersRepository.class, (method, args) -> {
            if ("findById".equals(method.getName())) {
                return Optional.ofNullable(usersDb.get(args[0]));
            }
            if ("save".equals(method.getName())) {
                saveCallCount.incrementAndGet();
                Users u = (Users) args[0];
                Users existing = usersDb.putIfAbsent(u.getUserId(), u);
                if (existing != null) {
                    throw new org.springframework.dao.DataIntegrityViolationException(
                            "duplicate key value violates unique constraint users_pkey");
                }
                return u;
            }
            return null;
        });

        PaymentRepository paymentRepo = createProxy(PaymentRepository.class, (method, args) -> {
            if ("findEligiblePaymentsForWorkerSettlementForUpdate".equals(method.getName())) {
                Collection<?> ids = (Collection<?>) args[1];
                if (ids.contains(payment1.getPaymentId())) return List.of(payment1);
                if (ids.contains(payment2.getPaymentId())) return List.of(payment2);
            }
            return null;
        });

        WorkerSettlementRepository settlementRepo = createProxy(WorkerSettlementRepository.class, (method, args) -> {
            if ("save".equals(method.getName())) {
                WorkerSettlement s = (WorkerSettlement) args[0];
                s.setSettlementId(UUID.randomUUID());
                return s;
            }
            return null;
        });

        WorkerSettlementItemRepository itemRepo = createProxy(WorkerSettlementItemRepository.class, (method, args) -> {
            if ("saveAll".equals(method.getName())) {
                return args[0];
            }
            return null;
        });

        SettlementBusinessDateCalculator dateCalc = new SettlementBusinessDateCalculator(
                java.time.Clock.system(SettlementBusinessDateCalculator.SETTLEMENT_ZONE)
        );

        WorkerSettlementService service = new WorkerSettlementService(
                workerRepo,
                usersRepo,
                credentialsRepo,
                paymentRepo,
                settlementRepo,
                itemRepo,
                null,
                dateCalc
        );

        java.util.concurrent.ExecutorService executor = java.util.concurrent.Executors.newFixedThreadPool(2);
        java.util.concurrent.CyclicBarrier barrier = new java.util.concurrent.CyclicBarrier(2);

        java.util.concurrent.Future<WorkerSettlement> future1 = executor.submit(() -> {
            barrier.await();
            return service.createSettlement(workerId, List.of(payment1.getPaymentId()), businessDate, adminId);
        });

        java.util.concurrent.Future<WorkerSettlement> future2 = executor.submit(() -> {
            barrier.await();
            return service.createSettlement(workerId, List.of(payment2.getPaymentId()), businessDate, adminId);
        });

        WorkerSettlement result1 = future1.get(5, java.util.concurrent.TimeUnit.SECONDS);
        WorkerSettlement result2 = future2.get(5, java.util.concurrent.TimeUnit.SECONDS);
        executor.shutdown();

        assertNotNull(result1);
        assertNotNull(result2);
        assertEquals(adminId, result1.getSettledBy().getUserId());
        assertEquals(adminId, result2.getSettledBy().getUserId());
        assertEquals("Administrator", result1.getSettledBy().getFullName());
        assertEquals(1, usersDb.size(), "Only one users entity should exist for the admin");
    }

    @Test
    void handlesDataIntegrityViolationExceptionGracefully() {
        UUID adminId = UUID.randomUUID();
        UUID workerId = UUID.randomUUID();
        UUID paymentId = UUID.randomUUID();
        LocalDate businessDate = LocalDate.of(2026, 11, 13);

        Credentials adminCredentials = new Credentials();
        adminCredentials.setUserId(adminId);
        adminCredentials.setEmail("admin@servicehub.com");
        adminCredentials.setRole(UserRole.ADMIN);

        Users raceWinner = new Users();
        raceWinner.setUserId(adminId);
        raceWinner.setCredentials(adminCredentials);
        raceWinner.setFullName("Administrator");

        Worker worker = new Worker();
        worker.setUserId(workerId);
        Users workerUser = new Users();
        workerUser.setUserId(workerId);
        worker.setUser(workerUser);
        worker.setBankAccountHolderName("Test Worker");
        worker.setBankAccountNumber("001234567890");
        worker.setBankIfscCode("BKID0001234");
        worker.setBankName("Bank of India");

        Payment payment = new Payment();
        payment.setPaymentId(paymentId);
        payment.setAmount(new BigDecimal("1000.00"));
        payment.setPaymentMethod(PaymentMethod.RAZORPAY);
        payment.setStatus(PaymentStatus.SUCCESS);
        payment.setPaidAt(LocalDateTime.of(2026, 11, 13, 15, 0));

        WorkerRepository workerRepo = createProxy(WorkerRepository.class, (method, args) -> {
            if ("findByUserIdForUpdate".equals(method.getName())) {
                return Optional.of(worker);
            }
            return null;
        });

        java.util.concurrent.atomic.AtomicInteger findByIdCount = new java.util.concurrent.atomic.AtomicInteger(0);

        UsersRepository usersRepo = createProxy(UsersRepository.class, (method, args) -> {
            if ("findById".equals(method.getName())) {
                int count = findByIdCount.incrementAndGet();
                // First call before save returns empty; call after exception returns the winner
                if (count > 1) {
                    return Optional.of(raceWinner);
                }
                return Optional.empty();
            }
            if ("save".equals(method.getName())) {
                throw new org.springframework.dao.DataIntegrityViolationException(
                        "duplicate key value violates unique constraint users_pkey");
            }
            return null;
        });

        CredentialsRepository credentialsRepo = createProxy(CredentialsRepository.class, (method, args) -> {
            if ("findById".equals(method.getName()) || "findByUserIdForUpdate".equals(method.getName())) {
                return Optional.of(adminCredentials);
            }
            return null;
        });

        PaymentRepository paymentRepo = createProxy(PaymentRepository.class, (method, args) -> {
            if ("findEligiblePaymentsForWorkerSettlementForUpdate".equals(method.getName())) {
                return List.of(payment);
            }
            return null;
        });

        WorkerSettlementRepository settlementRepo = createProxy(WorkerSettlementRepository.class, (method, args) -> {
            if ("save".equals(method.getName())) {
                WorkerSettlement s = (WorkerSettlement) args[0];
                s.setSettlementId(UUID.randomUUID());
                return s;
            }
            return null;
        });

        WorkerSettlementItemRepository itemRepo = createProxy(WorkerSettlementItemRepository.class, (method, args) -> {
            if ("saveAll".equals(method.getName())) {
                return args[0];
            }
            return null;
        });

        SettlementBusinessDateCalculator dateCalc = new SettlementBusinessDateCalculator(
                java.time.Clock.system(SettlementBusinessDateCalculator.SETTLEMENT_ZONE)
        );

        WorkerSettlementService service = new WorkerSettlementService(
                workerRepo,
                usersRepo,
                credentialsRepo,
                paymentRepo,
                settlementRepo,
                itemRepo,
                null,
                dateCalc
        );

        WorkerSettlement settlement = service.createSettlement(workerId, List.of(paymentId), businessDate, adminId);
        assertNotNull(settlement);
        assertSame(raceWinner, settlement.getSettledBy());
    }

    @SuppressWarnings("unchecked")
    private static <T> T createProxy(Class<T> intf, InvocationHandler handler) {
        return (T) Proxy.newProxyInstance(
                intf.getClassLoader(),
                new Class<?>[]{intf},
                (proxy, method, args) -> {
                    Object res = handler.handle(method, args);
                    if (res != null) return res;
                    Class<?> returnType = method.getReturnType();
                    if (returnType.equals(Optional.class)) return Optional.empty();
                    if (returnType.equals(List.class)) return List.of();
                    if (returnType.equals(boolean.class)) return false;
                    return null;
                }
        );
    }

    @FunctionalInterface
    private interface InvocationHandler {
        Object handle(java.lang.reflect.Method method, Object[] args) throws Throwable;
    }
}
