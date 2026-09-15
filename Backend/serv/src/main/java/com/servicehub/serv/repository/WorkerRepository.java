package com.servicehub.serv.repository;

import com.servicehub.serv.entity.Worker;
import com.servicehub.serv.enums.VerificationStatus;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface WorkerRepository extends JpaRepository<Worker, UUID> {

    List<Worker> findByVerificationStatus(
            VerificationStatus verificationStatus
    );

    long countByVerificationStatus(
            VerificationStatus verificationStatus
    );
}