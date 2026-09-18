package com.servicehub.serv.service;

import com.servicehub.serv.dto.WorkerStatusDto;
import com.servicehub.serv.entity.Worker;
import com.servicehub.serv.repository.WorkerRepository;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class WorkerStatusService {

    private final WorkerRepository workerRepository;

    public WorkerStatusService(WorkerRepository workerRepository) {
        this.workerRepository = workerRepository;
    }

    public WorkerStatusDto getWorkerStatus(UUID workerId) {

        Worker worker = workerRepository.findById(workerId)
                .orElseThrow(() -> new IllegalArgumentException("Worker not found."));

        return new WorkerStatusDto(
                worker.getVerificationStatus(),
                worker.getAvailabilityStatus()
        );
    }
}