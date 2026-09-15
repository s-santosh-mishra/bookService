package com.servicehub.serv.controller;

import com.servicehub.serv.dto.WorkerServiceDto;
import com.servicehub.serv.service.WorkerServicesService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/worker")
public class WorkerServicesController {

    private final WorkerServicesService workerServicesService;

    public WorkerServicesController(
            WorkerServicesService workerServicesService) {

        this.workerServicesService =
                workerServicesService;
    }


    @GetMapping("/services/{workerId}")
    public ResponseEntity<List<WorkerServiceDto>> getWorkerServices(
            @PathVariable UUID workerId) {

        return ResponseEntity.ok(
                workerServicesService.getWorkerServices(workerId)
        );
    }
}