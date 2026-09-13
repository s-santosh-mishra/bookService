package com.servicehub.serv.controller;

import com.servicehub.serv.dto.WorkerRegisterReqDto;
import com.servicehub.serv.service.WorkerRegisterService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/worker")
public class WorkerRegisterController {

    private final WorkerRegisterService workerRegisterService;

    public WorkerRegisterController(
            WorkerRegisterService workerRegisterService
    ) {
        this.workerRegisterService = workerRegisterService;
    }

    @PostMapping("/register")
    public ResponseEntity<String> register(
            @Valid @RequestBody WorkerRegisterReqDto request
    ) {

        workerRegisterService.register(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body("Worker registration submitted successfully");
    }
}