package com.servicehub.serv.service;

import com.servicehub.serv.dto.RegisterReqDto;
import com.servicehub.serv.entity.Credentials;
import com.servicehub.serv.entity.Customer;
import com.servicehub.serv.entity.Users;
import com.servicehub.serv.enums.UserRole;
import com.servicehub.serv.exception.EmailAlreadyExistsException;
import com.servicehub.serv.repository.CredentialsRepository;
import com.servicehub.serv.repository.CustomerRepository;
import com.servicehub.serv.repository.UsersRepository;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;

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

class UserRegisterServiceTest {

    @Test
    void registersCustomerWithEncodedPasswordAndLinkedProfiles() {
        CredentialsRepository credentialsRepository = mock(CredentialsRepository.class);
        UsersRepository usersRepository = mock(UsersRepository.class);
        CustomerRepository customerRepository = mock(CustomerRepository.class);
        PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
        RegisterReqDto request = validRequest();
        when(credentialsRepository.existsByEmail(request.getEmail())).thenReturn(false);
        when(passwordEncoder.encode(request.getPassword())).thenReturn("encoded-password");

        UserRegisterService service = new UserRegisterService(
                credentialsRepository,
                usersRepository,
                customerRepository,
                passwordEncoder);

        service.register(request);

        ArgumentCaptor<Credentials> credentialsCaptor =
                ArgumentCaptor.forClass(Credentials.class);
        ArgumentCaptor<Users> usersCaptor = ArgumentCaptor.forClass(Users.class);
        ArgumentCaptor<Customer> customerCaptor =
                ArgumentCaptor.forClass(Customer.class);
        InOrder saveOrder = inOrder(
                credentialsRepository,
                usersRepository,
                customerRepository);
        saveOrder.verify(credentialsRepository).save(credentialsCaptor.capture());
        saveOrder.verify(usersRepository).save(usersCaptor.capture());
        saveOrder.verify(customerRepository).save(customerCaptor.capture());

        Credentials credentials = credentialsCaptor.getValue();
        Users user = usersCaptor.getValue();
        Customer customer = customerCaptor.getValue();
        assertEquals(request.getEmail(), credentials.getEmail());
        assertEquals("encoded-password", credentials.getPasswordHash());
        assertEquals(UserRole.USER, credentials.getRole());
        assertEquals(request.getFullName(), user.getFullName());
        assertEquals(request.getState(), user.getState());
        assertEquals(request.getPhone(), user.getPhone());
        assertEquals(request.getAge(), user.getAge());
        assertEquals(true, user.isActive());
        assertEquals(true, user.isTermsAccepted());
        assertSame(credentials, user.getCredentials());
        assertSame(user, customer.getUser());
        verify(passwordEncoder).encode(request.getPassword());
    }

    @Test
    void rejectsDuplicateCustomerEmailBeforeEncodingOrSaving() {
        CredentialsRepository credentialsRepository = mock(CredentialsRepository.class);
        UsersRepository usersRepository = mock(UsersRepository.class);
        CustomerRepository customerRepository = mock(CustomerRepository.class);
        PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
        RegisterReqDto request = validRequest();
        when(credentialsRepository.existsByEmail(request.getEmail())).thenReturn(true);

        UserRegisterService service = new UserRegisterService(
                credentialsRepository,
                usersRepository,
                customerRepository,
                passwordEncoder);

        assertThrows(EmailAlreadyExistsException.class, () -> service.register(request));

        verify(credentialsRepository, never()).save(any(Credentials.class));
        verifyNoInteractions(usersRepository, customerRepository, passwordEncoder);
    }

    @Test
    void rejectsMismatchedCustomerPasswordsBeforeRepositoryAccess() {
        CredentialsRepository credentialsRepository = mock(CredentialsRepository.class);
        UsersRepository usersRepository = mock(UsersRepository.class);
        CustomerRepository customerRepository = mock(CustomerRepository.class);
        PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
        RegisterReqDto request = validRequest();
        request.setConfirmPassword("different-password");

        UserRegisterService service = new UserRegisterService(
                credentialsRepository,
                usersRepository,
                customerRepository,
                passwordEncoder);

        assertThrows(IllegalArgumentException.class, () -> service.register(request));

        verifyNoInteractions(
                credentialsRepository,
                usersRepository,
                customerRepository,
                passwordEncoder);
    }

    private RegisterReqDto validRequest() {
        RegisterReqDto request = new RegisterReqDto();
        request.setFullName("Test Customer");
        request.setAge(30);
        request.setGender("Other");
        request.setEmail("customer@example.com");
        request.setPhone("9876543210");
        request.setAddressLine1("1 Main Street");
        request.setCity("Bhubaneswar");
        request.setState("Odisha");
        request.setPinCode("751001");
        request.setPassword("customer-password");
        request.setConfirmPassword("customer-password");
        request.setTermsAccepted(true);
        return request;
    }
}
