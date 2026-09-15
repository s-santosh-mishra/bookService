package com.servicehub.serv.repository;

import com.servicehub.serv.entity.Users;
import com.servicehub.serv.enums.UserRole;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface UsersRepository extends JpaRepository<Users, UUID> {
    long countByCredentials_Role(UserRole role);
}