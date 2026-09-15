package com.servicehub.serv.service;

import com.servicehub.serv.dto.AdminDashboardDto;
import com.servicehub.serv.enums.UserRole;
import com.servicehub.serv.enums.VerificationStatus;
import com.servicehub.serv.repository.CategoryRepository;
import com.servicehub.serv.repository.ServiceRepository;
import com.servicehub.serv.repository.UsersRepository;
import com.servicehub.serv.repository.WorkerRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceException;
import org.springframework.stereotype.Service;

@Service
public class AdminDashboardService {

    private final UsersRepository usersRepository;
    private final WorkerRepository workerRepository;
    private final CategoryRepository categoryRepository;
    private final ServiceRepository serviceRepository;
    private final EntityManager entityManager;

    public AdminDashboardService(
            UsersRepository usersRepository,
            WorkerRepository workerRepository,
            CategoryRepository categoryRepository,
            ServiceRepository serviceRepository,
            EntityManager entityManager
    ) {
        this.usersRepository = usersRepository;
        this.workerRepository = workerRepository;
        this.categoryRepository = categoryRepository;
        this.serviceRepository = serviceRepository;
        this.entityManager = entityManager;
    }

    public AdminDashboardDto getDashboardData() {

        long totalCustomers =
                usersRepository.countByCredentials_Role(UserRole.USER);

        long totalWorkers =
                workerRepository.count();

        long pendingWorkers =
                workerRepository.countByVerificationStatus(
                        VerificationStatus.PENDING
                );

        long verifiedWorkers =
                workerRepository.countByVerificationStatus(
                        VerificationStatus.VERIFIED
                );

        long totalCategories =
                categoryRepository.count();

        long totalServices =
                serviceRepository.count();

        String databaseStatus = checkDatabaseStatus();

        return new AdminDashboardDto(
                totalCustomers,
                totalWorkers,
                pendingWorkers,
                verifiedWorkers,
                totalCategories,
                totalServices,
                "Online",
                databaseStatus,
                "Active"
        );
    }

    private String checkDatabaseStatus() {

        try {

            entityManager
                    .createNativeQuery("SELECT 1")
                    .getSingleResult();

            return "Connected";

        } catch (PersistenceException | IllegalStateException exception) {

            return "Disconnected";
        }
    }
}