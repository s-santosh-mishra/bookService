package com.servicehub.serv.service;

import com.servicehub.serv.dto.WorkerRegisterReqDto;
import com.servicehub.serv.entity.Credentials;
import com.servicehub.serv.entity.Service;
import com.servicehub.serv.entity.Users;
import com.servicehub.serv.entity.Worker;
import com.servicehub.serv.entity.WorkerService;
import com.servicehub.serv.entity.WorkerServiceId;
import com.servicehub.serv.enums.AvailabilityStatus;
import com.servicehub.serv.enums.UserRole;
import com.servicehub.serv.enums.VerificationStatus;
import com.servicehub.serv.repository.CredentialsRepository;
import com.servicehub.serv.repository.ServiceRepository;
import com.servicehub.serv.repository.UsersRepository;
import com.servicehub.serv.repository.WorkerRepository;
import com.servicehub.serv.repository.WorkerServiceRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@org.springframework.stereotype.Service
public class WorkerRegisterService {

    private final CredentialsRepository credentialsRepository;
    private final UsersRepository usersRepository;
    private final WorkerRepository workerRepository;
    private final ServiceRepository serviceRepository;
    private final WorkerServiceRepository workerServiceRepository;
    private final PasswordEncoder passwordEncoder;

    public WorkerRegisterService(
            CredentialsRepository credentialsRepository,
            UsersRepository usersRepository,
            WorkerRepository workerRepository,
            ServiceRepository serviceRepository,
            WorkerServiceRepository workerServiceRepository,
            PasswordEncoder passwordEncoder) {

        this.credentialsRepository = credentialsRepository;
        this.usersRepository = usersRepository;
        this.workerRepository = workerRepository;
        this.serviceRepository = serviceRepository;
        this.workerServiceRepository = workerServiceRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public void register(WorkerRegisterReqDto request) {

        // 1. Check passwords
        if (!request.getPassword().equals(request.getConfirmPassword())) {
            throw new IllegalArgumentException("Passwords do not match");
        }

        // 2. Check duplicate email
        if (credentialsRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException(
                    "An account with this email already exists"
            );
        }

        // 3. Validate selected services
        List<UUID> serviceIds = request.getServiceIds();

        if (serviceIds == null || serviceIds.isEmpty()) {
            throw new IllegalArgumentException(
                    "At least one service must be selected"
            );
        }

        // Remove duplicate service IDs
        Set<UUID> uniqueServiceIds = new HashSet<>(serviceIds);

        if (uniqueServiceIds.size() != serviceIds.size()) {
            throw new IllegalArgumentException(
                    "Duplicate services are not allowed"
            );
        }

        // Find all selected services
        List<Service> services =
                serviceRepository.findAllById(uniqueServiceIds);

        if (services.size() != uniqueServiceIds.size()) {
            throw new IllegalArgumentException(
                    "One or more selected services do not exist"
            );
        }

        // 4. Check maximum 3 categories
        Set<UUID> categoryIds = new HashSet<>();

        for (Service service : services) {
            categoryIds.add(
                    service.getCategory().getCategoryId()
            );
        }

        if (categoryIds.size() > 3) {
            throw new IllegalArgumentException(
                    "A worker can select services from a maximum of 3 categories"
            );
        }

        // 5. Create credentials
        Credentials credentials = new Credentials();

        credentials.setEmail(request.getEmail());
        credentials.setPasswordHash(
                passwordEncoder.encode(request.getPassword())
        );
        credentials.setRole(UserRole.WORKER);

        credentialsRepository.save(credentials);

        // 6. Create user
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

        usersRepository.save(user);

        // 7. Create worker
        Worker worker = new Worker();

        worker.setUser(user);
        worker.setExperienceYears(request.getExperienceYears());
        worker.setQualification(request.getQualification());
        worker.setBio(request.getBio());

        worker.setVerificationStatus(VerificationStatus.PENDING);
        worker.setAvailabilityStatus(AvailabilityStatus.UNAVAILABLE);

        workerRepository.save(worker);

        // 8. Create WorkerService mappings
        for (Service service : services) {

            WorkerService workerService = new WorkerService();

            workerService.setId(
                    new WorkerServiceId(
                            worker.getUserId(),
                            service.getServiceId()
                    )
            );

            workerService.setWorker(worker);
            workerService.setService(service);

            workerServiceRepository.save(workerService);
        }
    }
}