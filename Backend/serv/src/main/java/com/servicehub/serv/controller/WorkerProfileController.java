package com.servicehub.serv.controller;

import com.servicehub.serv.dto.UpdateWorkerProfileDto;
import com.servicehub.serv.dto.WorkerProfileDto;
import com.servicehub.serv.service.WorkerProfileService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/worker/profile")
public class WorkerProfileController {

    private final WorkerProfileService workerProfileService;

    public WorkerProfileController(
            WorkerProfileService workerProfileService) {

        this.workerProfileService = workerProfileService;
    }

    // ================================
    // Get Profile
    // ================================

    @GetMapping
    public ResponseEntity<WorkerProfileDto> getProfile(
            Authentication authentication) {

        UUID workerId =
                UUID.fromString(authentication.getName());

        return ResponseEntity.ok(
                workerProfileService.getProfile(workerId)
        );
    }

    // ================================
    // Update Profile
    // ================================

    @PutMapping
    public ResponseEntity<WorkerProfileDto> updateProfile(
            Authentication authentication,
            @RequestBody UpdateWorkerProfileDto dto) {

        UUID workerId =
                UUID.fromString(authentication.getName());

        return ResponseEntity.ok(
                workerProfileService.updateProfile(
                        workerId,
                        dto
                )
        );
    }
}