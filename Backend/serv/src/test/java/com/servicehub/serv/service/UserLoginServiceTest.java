package com.servicehub.serv.service;

import com.servicehub.serv.dto.LoginReqDto;
import com.servicehub.serv.dto.LoginResDto;
import com.servicehub.serv.entity.Credentials;
import com.servicehub.serv.entity.Users;
import com.servicehub.serv.entity.Worker;
import com.servicehub.serv.enums.UserRole;
import com.servicehub.serv.enums.VerificationStatus;
import com.servicehub.serv.exception.AccountStatusException;
import com.servicehub.serv.exception.InvalidCredentialsException;
import com.servicehub.serv.repository.CredentialsRepository;
import com.servicehub.serv.repository.UsersRepository;
import com.servicehub.serv.repository.WorkerRepository;
import com.servicehub.serv.security.JwtService;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class UserLoginServiceTest {

    @Test
    void logsInActiveCustomerAndReturnsCustomerIdentityAndToken() {
        Fixture fixture = fixture();
        UUID userId = UUID.randomUUID();
        Credentials credentials = credentials(userId, UserRole.USER);
        Users user = user(userId, "Test Customer");
        when(fixture.credentialsRepository().findByEmail("person@example.com"))
                .thenReturn(Optional.of(credentials));
        when(fixture.passwordEncoder().matches("correct-password", "stored-hash"))
                .thenReturn(true);
        when(fixture.usersRepository().findById(userId)).thenReturn(Optional.of(user));
        when(fixture.jwtService().generateToken(credentials)).thenReturn("customer-token");

        LoginResDto response = fixture.service().login(validLoginRequest());

        assertEquals(userId, response.getUserId());
        assertEquals("Test Customer", response.getFullName());
        assertEquals("person@example.com", response.getEmail());
        assertEquals(UserRole.USER, response.getRole());
        assertEquals("customer-token", response.getToken());
        verify(fixture.jwtService()).generateToken(credentials);
        verify(fixture.workerRepository(), never()).findById(userId);
    }

    @Test
    void logsInVerifiedWorkerAndReturnsWorkerIdentityAndToken() {
        Fixture fixture = fixture();
        UUID userId = UUID.randomUUID();
        Credentials credentials = credentials(userId, UserRole.WORKER);
        Users user = user(userId, "Test Worker");
        Worker worker = new Worker();
        worker.setVerificationStatus(VerificationStatus.VERIFIED);
        when(fixture.credentialsRepository().findByEmail("person@example.com"))
                .thenReturn(Optional.of(credentials));
        when(fixture.passwordEncoder().matches("correct-password", "stored-hash"))
                .thenReturn(true);
        when(fixture.usersRepository().findById(userId)).thenReturn(Optional.of(user));
        when(fixture.workerRepository().findById(userId)).thenReturn(Optional.of(worker));
        when(fixture.jwtService().generateToken(credentials)).thenReturn("worker-token");

        LoginResDto response = fixture.service().login(validLoginRequest());

        assertEquals(userId, response.getUserId());
        assertEquals("Test Worker", response.getFullName());
        assertEquals(UserRole.WORKER, response.getRole());
        assertEquals("worker-token", response.getToken());
        verify(fixture.jwtService()).generateToken(credentials);
    }

    @Test
    void rejectsIncorrectPasswordWithoutLoadingAccountOrCreatingToken() {
        Fixture fixture = fixture();
        Credentials credentials = credentials(UUID.randomUUID(), UserRole.USER);
        when(fixture.credentialsRepository().findByEmail("person@example.com"))
                .thenReturn(Optional.of(credentials));
        when(fixture.passwordEncoder().matches("correct-password", "stored-hash"))
                .thenReturn(false);

        assertThrows(
                InvalidCredentialsException.class,
                () -> fixture.service().login(validLoginRequest()));

        verify(fixture.usersRepository(), never()).findById(credentials.getUserId());
        verify(fixture.jwtService(), never()).generateToken(credentials);
    }

    @Test
    void rejectsUnknownEmailWithoutCheckingPasswordOrCreatingToken() {
        Fixture fixture = fixture();
        when(fixture.credentialsRepository().findByEmail("person@example.com"))
                .thenReturn(Optional.empty());

        assertThrows(
                InvalidCredentialsException.class,
                () -> fixture.service().login(validLoginRequest()));

        verify(fixture.passwordEncoder(), never()).matches(
                org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.anyString());
        verify(fixture.jwtService(), never()).generateToken(
                org.mockito.ArgumentMatchers.any(Credentials.class));
    }

    @Test
    void rejectsCustomerLoginWhenCredentialsHaveNoUserProfile() {
        Fixture fixture = fixture();
        Credentials credentials = credentials(UUID.randomUUID(), UserRole.USER);
        when(fixture.credentialsRepository().findByEmail("person@example.com"))
                .thenReturn(Optional.of(credentials));
        when(fixture.passwordEncoder().matches("correct-password", "stored-hash"))
                .thenReturn(true);
        when(fixture.usersRepository().findById(credentials.getUserId()))
                .thenReturn(Optional.empty());

        assertThrows(
                IllegalStateException.class,
                () -> fixture.service().login(validLoginRequest()));

        verify(fixture.jwtService(), never()).generateToken(credentials);
    }

    @Test
    void rejectsWorkerLoginWhenCredentialsHaveNoWorkerProfile() {
        Fixture fixture = fixture();
        UUID userId = UUID.randomUUID();
        Credentials credentials = credentials(userId, UserRole.WORKER);
        when(fixture.credentialsRepository().findByEmail("person@example.com"))
                .thenReturn(Optional.of(credentials));
        when(fixture.passwordEncoder().matches("correct-password", "stored-hash"))
                .thenReturn(true);
        when(fixture.usersRepository().findById(userId))
                .thenReturn(Optional.of(user(userId, "Test Worker")));
        when(fixture.workerRepository().findById(userId)).thenReturn(Optional.empty());

        assertThrows(
                IllegalStateException.class,
                () -> fixture.service().login(validLoginRequest()));

        verify(fixture.jwtService(), never()).generateToken(credentials);
    }

    @Test
    void rejectsWorkerLoginUntilVerificationIsComplete() {
        Fixture fixture = fixture();
        UUID userId = UUID.randomUUID();
        Credentials credentials = credentials(userId, UserRole.WORKER);
        Worker worker = new Worker();
        worker.setVerificationStatus(VerificationStatus.PENDING);
        when(fixture.credentialsRepository().findByEmail("person@example.com"))
                .thenReturn(Optional.of(credentials));
        when(fixture.passwordEncoder().matches("correct-password", "stored-hash"))
                .thenReturn(true);
        when(fixture.usersRepository().findById(userId))
                .thenReturn(Optional.of(user(userId, "Test Worker")));
        when(fixture.workerRepository().findById(userId)).thenReturn(Optional.of(worker));

        assertThrows(
                AccountStatusException.class,
                () -> fixture.service().login(validLoginRequest()));

        verify(fixture.jwtService(), never()).generateToken(credentials);
    }

    @Test
    void rejectsDeactivatedCustomerWithoutCreatingToken() {
        Fixture fixture = fixture();
        UUID userId = UUID.randomUUID();
        Credentials credentials = credentials(userId, UserRole.USER);
        Users user = user(userId, "Inactive Customer");
        user.setActive(false);
        when(fixture.credentialsRepository().findByEmail("person@example.com"))
                .thenReturn(Optional.of(credentials));
        when(fixture.passwordEncoder().matches("correct-password", "stored-hash"))
                .thenReturn(true);
        when(fixture.usersRepository().findById(userId)).thenReturn(Optional.of(user));

        assertThrows(
                AccountStatusException.class,
                () -> fixture.service().login(validLoginRequest()));

        verify(fixture.jwtService(), never()).generateToken(credentials);
    }

    private Fixture fixture() {
        CredentialsRepository credentialsRepository = mock(CredentialsRepository.class);
        UsersRepository usersRepository = mock(UsersRepository.class);
        WorkerRepository workerRepository = mock(WorkerRepository.class);
        PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
        JwtService jwtService = mock(JwtService.class);
        return new Fixture(
                new UserLoginService(
                        credentialsRepository,
                        usersRepository,
                        workerRepository,
                        passwordEncoder,
                        jwtService),
                credentialsRepository,
                usersRepository,
                workerRepository,
                passwordEncoder,
                jwtService);
    }

    private Credentials credentials(UUID userId, UserRole role) {
        Credentials credentials = new Credentials();
        credentials.setUserId(userId);
        credentials.setEmail("person@example.com");
        credentials.setPasswordHash("stored-hash");
        credentials.setRole(role);
        return credentials;
    }

    private Users user(UUID userId, String fullName) {
        Users user = new Users();
        user.setUserId(userId);
        user.setFullName(fullName);
        user.setActive(true);
        return user;
    }

    private LoginReqDto validLoginRequest() {
        LoginReqDto request = new LoginReqDto();
        request.setEmail("person@example.com");
        request.setPassword("correct-password");
        return request;
    }

    private record Fixture(
            UserLoginService service,
            CredentialsRepository credentialsRepository,
            UsersRepository usersRepository,
            WorkerRepository workerRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService) {
    }
}
