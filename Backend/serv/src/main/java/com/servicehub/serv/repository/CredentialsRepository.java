package com.servicehub.serv.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.servicehub.serv.entity.Credentials;
import jakarta.persistence.LockModeType;

import java.util.Optional;
import java.util.UUID;

public interface CredentialsRepository extends JpaRepository<Credentials, UUID> {

    Optional<Credentials> findByEmail(String email);

    boolean existsByEmail(String email);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT c FROM Credentials c WHERE c.userId = :userId")
    Optional<Credentials> findByUserIdForUpdate(@Param("userId") UUID userId);
}

