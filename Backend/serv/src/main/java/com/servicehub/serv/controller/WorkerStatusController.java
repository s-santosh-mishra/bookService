package com.servicehub.serv.controller;

import com.servicehub.serv.dto.WorkerStatusDto;
import com.servicehub.serv.service.WorkerStatusService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/worker/status")
public class WorkerStatusController {

    private final WorkerStatusService workerStatusService;

    public WorkerStatusController(WorkerStatusService workerStatusService) {
        this.workerStatusService = workerStatusService;
    }

    @GetMapping
    public ResponseEntity<WorkerStatusDto> getWorkerStatus(
            Authentication authentication) {

        UUID workerId = UUID.fromString(authentication.getName());

        WorkerStatusDto status = workerStatusService.getWorkerStatus(workerId);

        return ResponseEntity.ok(status);
    }
}