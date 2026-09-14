package com.servicehub.serv.controller;

import com.servicehub.serv.service.WorkerAdminService;
import com.servicehub.serv.dto.AdminActionResponseDto;
import com.servicehub.serv.dto.WorkerAdminDetailDto;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.servicehub.serv.dto.WorkerAdminListDto;
import com.servicehub.serv.enums.VerificationStatus;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin/workers")
public class WorkerAdminController {

        private final WorkerAdminService workerAdminService;

        public WorkerAdminController(
                        WorkerAdminService workerAdminService) {
                this.workerAdminService = workerAdminService;
        }

        // Get Pending Workers

        @GetMapping("/pending")
        public ResponseEntity<List<WorkerAdminListDto>> getPendingWorkers() {

                return ResponseEntity.ok(
                                workerAdminService.getWorkersByStatus(
                                                VerificationStatus.PENDING));
        }

        // Get Verified Workers

        @GetMapping("/verified")
        public ResponseEntity<List<WorkerAdminListDto>> getVerifiedWorkers() {

                return ResponseEntity.ok(
                                workerAdminService.getWorkersByStatus(
                                                VerificationStatus.VERIFIED));
        }

        // Get Rejected Workers

        @GetMapping("/rejected")
        public ResponseEntity<List<WorkerAdminListDto>> getRejectedWorkers() {

                return ResponseEntity.ok(
                                workerAdminService.getWorkersByStatus(
                                                VerificationStatus.REJECTED));
        }

        // Get Suspended Workers

        @GetMapping("/suspended")
        public ResponseEntity<List<WorkerAdminListDto>> getSuspendedWorkers() {

                return ResponseEntity.ok(
                                workerAdminService.getWorkersByStatus(
                                                VerificationStatus.SUSPENDED));
        }

        // Get Worker

        @GetMapping("/{workerId}")
        public ResponseEntity<WorkerAdminDetailDto> getWorker(
                        @PathVariable UUID workerId) {

                return ResponseEntity.ok(
                                workerAdminService.getWorker(workerId));
        }

        // Approve Worker

        @PutMapping("/{workerId}/approve")
        public ResponseEntity<AdminActionResponseDto> approveWorker(
                        @PathVariable UUID workerId) {

                return ResponseEntity.ok(
                                workerAdminService.approveWorker(workerId));
        }

        // Reject Worker

        @PutMapping("/{workerId}/reject")
        public ResponseEntity<AdminActionResponseDto> rejectWorker(
                        @PathVariable UUID workerId) {

                return ResponseEntity.ok(
                                workerAdminService.rejectWorker(workerId));
        }

        // Suspend Worker

        @PutMapping("/{workerId}/suspend")
        public ResponseEntity<AdminActionResponseDto> suspendWorker(
                        @PathVariable UUID workerId) {

                return ResponseEntity.ok(
                                workerAdminService.suspendWorker(workerId));
        }

        // Restore Worker

        @PutMapping("/{workerId}/restore")
        public ResponseEntity<AdminActionResponseDto> restoreWorker(
                        @PathVariable UUID workerId) {

                return ResponseEntity.ok(
                                workerAdminService.restoreWorker(workerId));
        }

}