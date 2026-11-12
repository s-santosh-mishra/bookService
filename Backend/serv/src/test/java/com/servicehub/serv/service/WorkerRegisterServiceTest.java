package com.servicehub.serv.service;

import com.servicehub.serv.dto.WorkerRegisterReqDto;
import com.servicehub.serv.entity.Category;
import com.servicehub.serv.entity.Credentials;
import com.servicehub.serv.entity.Service;
import com.servicehub.serv.entity.Users;
import com.servicehub.serv.entity.Worker;
import com.servicehub.serv.entity.WorkerService;
import com.servicehub.serv.enums.AvailabilityStatus;
import com.servicehub.serv.enums.UserRole;
import com.servicehub.serv.enums.VerificationStatus;
import com.servicehub.serv.repository.CredentialsRepository;
import com.servicehub.serv.repository.ServiceRepository;
import com.servicehub.serv.repository.UsersRepository;
import com.servicehub.serv.repository.WorkerRepository;
import com.servicehub.serv.repository.WorkerServiceRepository;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class WorkerRegisterServiceTest {

    @Test
    void registersWorkerWithEncodedPasswordAndSelectedService() {
        CredentialsRepository credentialsRepository = mock(CredentialsRepository.class);
        UsersRepository usersRepository = mock(UsersRepository.class);
        WorkerRepository workerRepository = mock(WorkerRepository.class);
        ServiceRepository serviceRepository = mock(ServiceRepository.class);
        WorkerServiceRepository workerServiceRepository =
                mock(WorkerServiceRepository.class);
        PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
        WorkerRegisterReqDto request = validRequest();
        UUID userId = UUID.randomUUID();
        Service selectedService = service(request.getServiceIds().get(0));
        when(credentialsRepository.existsByEmail(request.getEmail())).thenReturn(false);
        when(passwordEncoder.encode(request.getPassword())).thenReturn("encoded-worker-password");
        when(serviceRepository.findAllById(Set.copyOf(request.getServiceIds())))
                .thenReturn(List.of(selectedService));
        when(credentialsRepository.save(any(Credentials.class))).thenAnswer(invocation -> {
            Credentials credentials = invocation.getArgument(0);
            credentials.setUserId(userId);
            return credentials;
        });
        when(usersRepository.save(any(Users.class))).thenAnswer(invocation -> {
            Users user = invocation.getArgument(0);
            user.setUserId(userId);
            return user;
        });
        when(workerRepository.save(any(Worker.class))).thenAnswer(invocation -> {
            Worker worker = invocation.getArgument(0);
            worker.setUserId(userId);
            return worker;
        });

        WorkerRegisterService service = new WorkerRegisterService(
                credentialsRepository,
                usersRepository,
                workerRepository,
                serviceRepository,
                workerServiceRepository,
                passwordEncoder);

        service.register(request);

        ArgumentCaptor<Credentials> credentialsCaptor =
                ArgumentCaptor.forClass(Credentials.class);
        ArgumentCaptor<Users> usersCaptor = ArgumentCaptor.forClass(Users.class);
        ArgumentCaptor<Worker> workerCaptor = ArgumentCaptor.forClass(Worker.class);
        ArgumentCaptor<WorkerService> workerServiceCaptor =
                ArgumentCaptor.forClass(WorkerService.class);
        InOrder saveOrder = inOrder(
                credentialsRepository,
                usersRepository,
                workerRepository,
                workerServiceRepository);
        saveOrder.verify(credentialsRepository).save(credentialsCaptor.capture());
        saveOrder.verify(usersRepository).save(usersCaptor.capture());
        saveOrder.verify(workerRepository).save(workerCaptor.capture());
        saveOrder.verify(workerServiceRepository).save(workerServiceCaptor.capture());

        Credentials credentials = credentialsCaptor.getValue();
        Users user = usersCaptor.getValue();
        Worker worker = workerCaptor.getValue();
        WorkerService workerService = workerServiceCaptor.getValue();
        assertEquals(request.getEmail(), credentials.getEmail());
        assertEquals("encoded-worker-password", credentials.getPasswordHash());
        assertEquals(UserRole.WORKER, credentials.getRole());
        assertSame(credentials, user.getCredentials());
        assertEquals(VerificationStatus.PENDING, worker.getVerificationStatus());
        assertEquals(AvailabilityStatus.UNAVAILABLE, worker.getAvailabilityStatus());
        assertEquals("Account Holder", worker.getBankAccountHolderName());
        assertEquals("1234567890", worker.getBankAccountNumber());
        assertEquals("ABCD0123456", worker.getBankIfscCode());
        assertEquals("Test Bank", worker.getBankName());
        assertSame(user, worker.getUser());
        assertSame(worker, workerService.getWorker());
        assertSame(selectedService, workerService.getService());
        verify(passwordEncoder).encode(request.getPassword());
    }

    @Test
    void rejectsDuplicateWorkerEmailBeforeSaving() {
        CredentialsRepository credentialsRepository = mock(CredentialsRepository.class);
        UsersRepository usersRepository = mock(UsersRepository.class);
        WorkerRepository workerRepository = mock(WorkerRepository.class);
        ServiceRepository serviceRepository = mock(ServiceRepository.class);
        WorkerServiceRepository workerServiceRepository =
                mock(WorkerServiceRepository.class);
        PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
        WorkerRegisterReqDto request = validRequest();
        when(credentialsRepository.existsByEmail(request.getEmail())).thenReturn(true);

        WorkerRegisterService service = new WorkerRegisterService(
                credentialsRepository,
                usersRepository,
                workerRepository,
                serviceRepository,
                workerServiceRepository,
                passwordEncoder);

        assertThrows(IllegalArgumentException.class, () -> service.register(request));

        verify(credentialsRepository, never()).save(any(Credentials.class));
        verifyNoInteractions(
                usersRepository,
                workerRepository,
                serviceRepository,
                workerServiceRepository,
                passwordEncoder);
    }

    @Test
    void rejectsMismatchedWorkerPasswordsBeforeRepositoryAccess() {
        CredentialsRepository credentialsRepository = mock(CredentialsRepository.class);
        UsersRepository usersRepository = mock(UsersRepository.class);
        WorkerRepository workerRepository = mock(WorkerRepository.class);
        ServiceRepository serviceRepository = mock(ServiceRepository.class);
        WorkerServiceRepository workerServiceRepository =
                mock(WorkerServiceRepository.class);
        PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
        WorkerRegisterReqDto request = validRequest();
        request.setConfirmPassword("different-password");

        WorkerRegisterService service = new WorkerRegisterService(
                credentialsRepository,
                usersRepository,
                workerRepository,
                serviceRepository,
                workerServiceRepository,
                passwordEncoder);

        assertThrows(IllegalArgumentException.class, () -> service.register(request));

        verify(credentialsRepository, never()).save(any(Credentials.class));
        verifyNoInteractions(
                usersRepository,
                workerRepository,
                serviceRepository,
                workerServiceRepository,
                passwordEncoder);
    }

    @Test
    void rejectsDuplicateWorkerServiceIdsBeforeSaving() {
        CredentialsRepository credentialsRepository = mock(CredentialsRepository.class);
        UsersRepository usersRepository = mock(UsersRepository.class);
        WorkerRepository workerRepository = mock(WorkerRepository.class);
        ServiceRepository serviceRepository = mock(ServiceRepository.class);
        WorkerServiceRepository workerServiceRepository =
                mock(WorkerServiceRepository.class);
        PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
        WorkerRegisterReqDto request = validRequest();
        UUID serviceId = request.getServiceIds().get(0);
        request.setServiceIds(List.of(serviceId, serviceId));

        WorkerRegisterService service = new WorkerRegisterService(
                credentialsRepository,
                usersRepository,
                workerRepository,
                serviceRepository,
                workerServiceRepository,
                passwordEncoder);

        assertThrows(IllegalArgumentException.class, () -> service.register(request));

        verify(credentialsRepository, never()).save(any(Credentials.class));
        verifyNoInteractions(
                usersRepository,
                workerRepository,
                serviceRepository,
                workerServiceRepository,
                passwordEncoder);
    }

    private WorkerRegisterReqDto validRequest() {
        WorkerRegisterReqDto request = new WorkerRegisterReqDto();
        request.setFullName("Test Worker");
        request.setAge(32);
        request.setGender("Other");
        request.setEmail("worker@example.com");
        request.setPhone("9876543210");
        request.setAddressLine1("2 Main Street");
        request.setCity("Bhubaneswar");
        request.setState("Odisha");
        request.setPinCode("751001");
        request.setBankAccountHolderName("  Account Holder  ");
        request.setBankAccountNumber("  1234567890  ");
        request.setBankIfscCode("abcd0123456");
        request.setBankName("  Test Bank  ");
        request.setPassword("worker-password");
        request.setConfirmPassword("worker-password");
        request.setExperienceYears(5);
        request.setQualification("Trade Certificate");
        request.setServiceIds(List.of(UUID.randomUUID()));
        request.setTermsAccepted(true);
        return request;
    }

    private Service service(UUID serviceId) {
        Category category = new Category();
        category.setCategoryId(UUID.randomUUID());
        Service service = new Service();
        service.setServiceId(serviceId);
        service.setCategory(category);
        return service;
    }
}
