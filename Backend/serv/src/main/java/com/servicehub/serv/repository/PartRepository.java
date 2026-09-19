package com.servicehub.serv.repository;

import com.servicehub.serv.entity.Part;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface PartRepository extends JpaRepository<Part, UUID> {

    List<Part> findByActiveTrueOrderByNameAsc();
}