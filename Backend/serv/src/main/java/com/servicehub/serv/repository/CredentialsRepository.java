package com.servicehub.serv.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.servicehub.serv.entity.Credentials;

import java.util.Optional;
import java.util.UUID;

public interface CredentialsRepository extends JpaRepository<Credentials, UUID> {

    Optional<Credentials> findByEmail(String email);

    boolean existsByEmail(String email);
}
