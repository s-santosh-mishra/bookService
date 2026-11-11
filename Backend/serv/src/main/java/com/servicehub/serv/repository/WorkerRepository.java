package com.servicehub.serv.repository;

import com.servicehub.serv.entity.Worker;
import com.servicehub.serv.enums.AvailabilityStatus;
import com.servicehub.serv.enums.VerificationStatus;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface WorkerRepository extends JpaRepository<Worker, UUID> {

    List<Worker> findByVerificationStatus(
            VerificationStatus verificationStatus
    );

    long countByVerificationStatus(
            VerificationStatus verificationStatus
    );

    List<Worker> findByVerificationStatusAndAvailabilityStatus(
        VerificationStatus verificationStatus,
        AvailabilityStatus availabilityStatus
);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT worker FROM Worker worker WHERE worker.userId = :workerId")
    Optional<Worker> findByUserIdForUpdate(@Param("workerId") UUID workerId);
}