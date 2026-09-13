package com.servicehub.serv.repository;

import com.servicehub.serv.entity.WorkerService;
import com.servicehub.serv.entity.WorkerServiceId;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WorkerServiceRepository
        extends JpaRepository<WorkerService, WorkerServiceId> {
}