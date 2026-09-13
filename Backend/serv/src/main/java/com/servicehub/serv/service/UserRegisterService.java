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
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserRegisterService {

    private final CredentialsRepository credentialsRepository;
    private final UsersRepository usersRepository;
    private final CustomerRepository customerRepository;
    private final PasswordEncoder passwordEncoder;


    public UserRegisterService(
            CredentialsRepository credentialsRepository,
            UsersRepository usersRepository,
            CustomerRepository customerRepository,
            PasswordEncoder passwordEncoder) {

        this.credentialsRepository = credentialsRepository;
        this.usersRepository = usersRepository;
        this.customerRepository = customerRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public void register(RegisterReqDto request) {

        // 1. Check password confirmation
        if (!request.getPassword().equals(request.getConfirmPassword())) {
            throw new IllegalArgumentException("Passwords do not match");
        }

        // 2. Check whether email already exists
        if (credentialsRepository.existsByEmail(request.getEmail())) {
            throw new EmailAlreadyExistsException(
                    "An account with this email already exists");
        }

        // 3. Create credentials
        Credentials credentials = new Credentials();

        credentials.setEmail(request.getEmail());

        credentials.setPasswordHash(
                passwordEncoder.encode(request.getPassword()));

        credentials.setRole(UserRole.USER);

        // 4. Save credentials first
        credentialsRepository.save(credentials);

        // 5. Create user profile
        Users user = new Users();

        user.setCredentials(credentials);

        user.setFullName(request.getFullName());
        user.setAge(request.getAge());
        user.setGender(request.getGender());
        user.setPhone(request.getPhone());

        user.setAddressLine1(request.getAddressLine1());
        user.setAddressLine2(request.getAddressLine2());
        user.setLandmark(request.getLandmark());
        user.setCity(request.getCity());
        user.setState(request.getState());
        user.setPinCode(request.getPinCode());

        user.setActive(true);
        user.setTermsAccepted(request.isTermsAccepted());

        // 6. Save user
        usersRepository.save(user);

        // 7. Create customer profile
        Customer customer = new Customer();

        customer.setUser(user);

        // 8. Save customer
        customerRepository.save(customer);
    }
}