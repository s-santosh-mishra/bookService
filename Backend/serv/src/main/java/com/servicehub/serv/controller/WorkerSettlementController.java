package com.servicehub.serv.controller;

import com.servicehub.serv.dto.WorkerDailySettlementSummaryDto;
import com.servicehub.serv.service.WorkerSettlementService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/worker/settlements")
public class WorkerSettlementController {

    private final WorkerSettlementService workerSettlementService;

    public WorkerSettlementController(WorkerSettlementService workerSettlementService) {
        this.workerSettlementService = workerSettlementService;
    }

    @GetMapping("/today-summary")
    public ResponseEntity<WorkerDailySettlementSummaryDto> getTodaySummary(
            Authentication authentication) {
        UUID workerId = UUID.fromString(authentication.getName());
        return ResponseEntity.ok(
                workerSettlementService.getWorkerDailySummary(workerId));
    }
}
