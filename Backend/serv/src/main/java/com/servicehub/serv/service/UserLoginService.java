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
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class UserLoginService {

    private final CredentialsRepository credentialsRepository;
    private final UsersRepository usersRepository;
    private final WorkerRepository workerRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public UserLoginService(
            CredentialsRepository credentialsRepository,
            UsersRepository usersRepository,
            WorkerRepository workerRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService
    ) {
        this.credentialsRepository = credentialsRepository;
        this.usersRepository = usersRepository;
        this.workerRepository = workerRepository;
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

        String fullName = null;

        /*
         * USER and WORKER accounts have a Users record.
         * ADMIN does not need one for authentication.
         */
        if (credentials.getRole() == UserRole.USER ||
                credentials.getRole() == UserRole.WORKER) {

            Users user = usersRepository
                    .findById(credentials.getUserId())
                    .orElseThrow(() ->
                            new IllegalStateException(
                                    "User account data not found."
                            )
                    );

            /*
             * CUSTOMER ACCOUNT CHECK
             */
            if (credentials.getRole() == UserRole.USER) {

                if (!user.isActive()) {
                    throw new AccountStatusException(
                            "Your account has been deactivated. Please contact support."
                    );
                }
            }

            fullName = user.getFullName();

            /*
             * WORKER VERIFICATION CHECK
             */
            if (credentials.getRole() == UserRole.WORKER) {

                Worker worker = workerRepository
                        .findById(credentials.getUserId())
                        .orElseThrow(() ->
                                new IllegalStateException(
                                        "Worker account data not found."
                                )
                        );

                VerificationStatus status =
                        worker.getVerificationStatus();

                if (status != VerificationStatus.VERIFIED) {

                    switch (status) {

                        case PENDING:
                            throw new AccountStatusException(
                                    "Your worker account is pending verification."
                            );

                        case REJECTED:
                            throw new AccountStatusException(
                                    "Your worker account has been rejected."
                            );

                        case SUSPENDED:
                            throw new AccountStatusException(
                                    "Your worker account has been suspended. Please contact support."
                            );

                        default:
                            throw new AccountStatusException(
                                    "Your worker account is not verified."
                            );
                    }
                }
            }
        }

        // Generate JWT only after all account checks pass
        String token = jwtService.generateToken(credentials);

        return new LoginResDto(
                credentials.getUserId(),
                fullName,
                credentials.getEmail(),
                credentials.getRole(),
                token
        );
    }
}