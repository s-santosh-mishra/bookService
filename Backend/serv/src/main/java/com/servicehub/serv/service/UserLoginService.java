package com.servicehub.serv.service;

import com.servicehub.serv.dto.LoginReqDto;
import com.servicehub.serv.dto.LoginResDto;
import com.servicehub.serv.entity.Credentials;
import com.servicehub.serv.exception.InvalidCredentialsException;
import com.servicehub.serv.repository.CredentialsRepository;
import com.servicehub.serv.security.JwtService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class UserLoginService {

    private final CredentialsRepository credentialsRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public UserLoginService(
            CredentialsRepository credentialsRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService
    ) {
        this.credentialsRepository = credentialsRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    public LoginResDto login(LoginReqDto request) {

        Credentials credentials = credentialsRepository
                .findByEmail(request.getEmail())
                .orElseThrow(() ->
                        new InvalidCredentialsException(
                                "Invalid email or password"
                        )
                );

        if (!passwordEncoder.matches(
                request.getPassword(),
                credentials.getPasswordHash()
        )) {
            throw new InvalidCredentialsException(
                    "Invalid email or password"
            );
        }

        // Generate JWT after successful authentication
        String token = jwtService.generateToken(credentials);

        return new LoginResDto(
                credentials.getUserId(),
                credentials.getEmail(),
                credentials.getRole(),
                token
        );
    }
}