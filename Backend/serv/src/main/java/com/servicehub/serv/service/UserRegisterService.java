package com.servicehub.serv.service;

import com.servicehub.serv.dto.RegisterReqDto;
import com.servicehub.serv.entity.Credentials;
import com.servicehub.serv.entity.Users;
import com.servicehub.serv.enums.UserRole;
import com.servicehub.serv.exception.EmailAlreadyExistsException;
import com.servicehub.serv.repository.CredentialsRepository;
import com.servicehub.serv.repository.UsersRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserRegisterService {

    private final CredentialsRepository credentialsRepository;
    private final UsersRepository usersRepository;
    private final PasswordEncoder passwordEncoder;

    public UserRegisterService(
            CredentialsRepository credentialsRepository,
            UsersRepository usersRepository,
            PasswordEncoder passwordEncoder) {
        this.credentialsRepository = credentialsRepository;
        this.usersRepository = usersRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public void register(RegisterReqDto request) {
        //if the given password and the retyped password doesnt match
        if (!request.getPassword().equals(request.getConfirmPassword())) {
            throw new IllegalArgumentException("Passwords do not match");
        }
        // 1. Check whether email already exists
        if (credentialsRepository.existsByEmail(request.getEmail())) {
            throw new EmailAlreadyExistsException(
                    "An account with this email already exists");
        }

        // 2. Create credentials
        Credentials credentials = new Credentials();

        credentials.setEmail(request.getEmail());

        credentials.setPasswordHash(
                passwordEncoder.encode(request.getPassword()));

        credentials.setRole(UserRole.USER);

        // 3. Save credentials first
        credentialsRepository.save(credentials);

        // 4. Create user profile
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

        // 5. Save user
        usersRepository.save(user);
    }
}