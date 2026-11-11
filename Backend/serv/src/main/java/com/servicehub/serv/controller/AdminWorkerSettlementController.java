package com.servicehub.serv.controller;

import com.servicehub.serv.dto.CreateSettlementRequestDto;
import com.servicehub.serv.dto.AdminWorkerSettlementSummaryDto;
import com.servicehub.serv.dto.UpdateSettlementFailedDto;
import com.servicehub.serv.dto.UpdateSettlementSettledDto;
import com.servicehub.serv.dto.WorkerEligiblePaymentsDto;
import com.servicehub.serv.dto.WorkerSettlementDetailsDto;
import com.servicehub.serv.dto.WorkerSettlementBatchPaymentsDto;
import com.servicehub.serv.dto.WorkerSettlementBatchesDto;
import com.servicehub.serv.dto.WorkerSettlementHistoryDto;
import com.servicehub.serv.dto.WorkerSettlementResponseDto;
import com.servicehub.serv.dto.WorkerSettlementReversalDto;
import com.servicehub.serv.dto.WorkerSettlementSummaryDto;
import com.servicehub.serv.dto.RecordSettlementReversalRequestDto;
import com.servicehub.serv.enums.SettlementStatus;
import com.servicehub.serv.service.WorkerSettlementService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.List;
import java.time.LocalDate;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin/settlements")
public class AdminWorkerSettlementController {

    private final WorkerSettlementService workerSettlementService;

    public AdminWorkerSettlementController(
            WorkerSettlementService workerSettlementService) {
        this.workerSettlementService = workerSettlementService;
    }

    @GetMapping("/workers")
    public ResponseEntity<List<WorkerSettlementSummaryDto>> getWorkersAwaitingSettlement() {
        return ResponseEntity.ok(
                workerSettlementService.getWorkersAwaitingSettlement());
    }

    @GetMapping("/summary")
    public ResponseEntity<AdminWorkerSettlementSummaryDto> getSettlementSummary() {
        return ResponseEntity.ok(workerSettlementService.getAdminSummary());
    }

    @GetMapping("/workers/{workerId}/payments")
    public ResponseEntity<WorkerEligiblePaymentsDto> getEligiblePaymentsForWorker(
            @PathVariable UUID workerId) {
        return ResponseEntity.ok(
                workerSettlementService.getEligiblePaymentsForWorker(workerId));
    }

    @PostMapping
    public ResponseEntity<WorkerSettlementResponseDto> createSettlement(
            @Valid @RequestBody CreateSettlementRequestDto request,
            Authentication authentication) {
        WorkerSettlementResponseDto response =
                workerSettlementService.createSettlementResponse(
                        request.getWorkerId(),
                        request.getPaymentIds(),
                        request.getBusinessDate(),
                        authenticatedAdminId(authentication));
        return ResponseEntity
                .created(URI.create("/api/admin/settlements/" + response.getSettlementId()))
                .body(response);
    }

    @GetMapping("/batches")
    public ResponseEntity<WorkerSettlementBatchesDto> getSettlementBatches() {
        return ResponseEntity.ok(workerSettlementService.getSettlementBatches());
    }

    @GetMapping("/batches/{businessDate}/payments")
    public ResponseEntity<WorkerSettlementBatchPaymentsDto> getSettlementBatchPayments(
            @PathVariable LocalDate businessDate) {
        return ResponseEntity.ok(
                workerSettlementService.getSettlementBatchPayments(businessDate));
    }

    @GetMapping
    public ResponseEntity<List<WorkerSettlementHistoryDto>> getSettlementHistory(
            @RequestParam(required = false) UUID workerId,
            @RequestParam(required = false) SettlementStatus status) {
        return ResponseEntity.ok(
                workerSettlementService.getSettlementHistory(workerId, status));
    }

    @GetMapping("/{settlementId}")
    public ResponseEntity<WorkerSettlementDetailsDto> getSettlementDetails(
            @PathVariable UUID settlementId) {
        return ResponseEntity.ok(
                workerSettlementService.getSettlementDetails(settlementId));
    }

    @PatchMapping("/{settlementId}/processing")
    public ResponseEntity<WorkerSettlementResponseDto> markSettlementProcessing(
            @PathVariable UUID settlementId,
            Authentication authentication) {
        return ResponseEntity.ok(
                workerSettlementService.markSettlementProcessingResponse(
                        settlementId,
                        authenticatedAdminId(authentication)));
    }

    @PatchMapping("/{settlementId}/settled")
    public ResponseEntity<WorkerSettlementResponseDto> markSettlementSettled(
            @PathVariable UUID settlementId,
            @Valid @RequestBody UpdateSettlementSettledDto request,
            Authentication authentication) {
        return ResponseEntity.ok(
                workerSettlementService.markSettlementSettledResponse(
                        settlementId,
                        request.getPaymentReference(),
                        authenticatedAdminId(authentication)));
    }

    @PatchMapping("/{settlementId}/failed")
    public ResponseEntity<WorkerSettlementResponseDto> markSettlementFailed(
            @PathVariable UUID settlementId,
            @Valid @RequestBody UpdateSettlementFailedDto request,
            Authentication authentication) {
        return ResponseEntity.ok(
                workerSettlementService.markSettlementFailedResponse(
                        settlementId,
                        request.getFailureReason(),
                        authenticatedAdminId(authentication)));
    }

    @PostMapping("/{settlementId}/reversal")
    public ResponseEntity<WorkerSettlementReversalDto> recordConfirmedReversal(
            @PathVariable UUID settlementId,
            @Valid @RequestBody RecordSettlementReversalRequestDto request,
            Authentication authentication) {
        return ResponseEntity.ok(
                workerSettlementService.recordConfirmedReversal(
                        settlementId,
                        request.getBankReversalReference(),
                        request.getReversalReason(),
                        request.getVerifiedWithBankRecords(),
                        authenticatedAdminId(authentication)));
    }

    private UUID authenticatedAdminId(Authentication authentication) {
        return UUID.fromString(authentication.getName());
    }
}
