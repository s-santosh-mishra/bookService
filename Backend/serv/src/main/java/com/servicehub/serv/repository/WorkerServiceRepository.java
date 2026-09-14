package com.servicehub.serv.repository;

import com.servicehub.serv.entity.WorkerService;
import com.servicehub.serv.entity.WorkerServiceId;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface WorkerServiceRepository
        extends JpaRepository<WorkerService, WorkerServiceId> {
                List<WorkerService> findByWorker_UserId(UUID workerId);
                List<WorkerService> findByService_ServiceId(UUID serviceId);
}