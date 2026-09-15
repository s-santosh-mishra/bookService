package com.servicehub.serv.service;

import com.servicehub.serv.dto.WorkerServiceDto;
import com.servicehub.serv.entity.WorkerService;
import com.servicehub.serv.repository.WorkerServiceRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class WorkerServicesService {

    private final WorkerServiceRepository workerServiceRepository;

    public WorkerServicesService(
            WorkerServiceRepository workerServiceRepository) {

        this.workerServiceRepository =
                workerServiceRepository;
    }


    public List<WorkerServiceDto> getWorkerServices(UUID workerId) {

        List<WorkerService> workerServices =
                workerServiceRepository.findByWorker_UserId(workerId);

        return workerServices.stream()
                .map(workerService ->
                        new WorkerServiceDto(
                                workerService
                                        .getService()
                                        .getServiceName()
                        )
                )
                .toList();
    }
}