package com.servicehub.serv.controller;

import com.servicehub.serv.dto.*;
import com.servicehub.serv.enums.SettlementStatus;
import com.servicehub.serv.service.WorkerSettlementService;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

public class AdminWorkerSettlementControllerTest {

    private Authentication createAdminAuth(UUID adminId) {
        return new Authentication() {
            @Override
            public String getName() {
                return adminId.toString();
            }

            @Override
            public Collection<? extends org.springframework.security.core.GrantedAuthority> getAuthorities() {
                return List.of(new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_ADMIN"));
            }

            @Override
            public Object getCredentials() {
                return null;
            }

            @Override
            public Object getDetails() {
                return null;
            }

            @Override
            public Object getPrincipal() {
                return adminId.toString();
            }

            @Override
            public boolean isAuthenticated() {
                return true;
            }

            @Override
            public void setAuthenticated(boolean isAuthenticated) throws IllegalArgumentException {
            }
        };
    }

    @Test
    void getSettlementHistoryReturnsHistoryList() {
        UUID workerId = UUID.randomUUID();
        WorkerSettlementHistoryDto historyItem = new WorkerSettlementHistoryDto(
                UUID.randomUUID(),
                LocalDate.of(2026, 11, 13),
                workerId,
                "Test Worker",
                new BigDecimal("1500.00"),
                SettlementStatus.PENDING,
                2L,
                null,
                LocalDateTime.now(),
                null
        );

        WorkerSettlementService serviceStub = new WorkerSettlementService(
                null, null, null, null, null, null, null, null
        ) {
            @Override
            public List<WorkerSettlementHistoryDto> getSettlementHistory(UUID wId, SettlementStatus status) {
                assertEquals(workerId, wId);
                assertEquals(SettlementStatus.PENDING, status);
                return List.of(historyItem);
            }
        };

        AdminWorkerSettlementController controller = new AdminWorkerSettlementController(serviceStub);
        ResponseEntity<List<WorkerSettlementHistoryDto>> response =
                controller.getSettlementHistory(workerId, SettlementStatus.PENDING);

        assertNotNull(response);
        assertEquals(200, response.getStatusCode().value());
        assertEquals(1, response.getBody().size());
        assertSame(historyItem, response.getBody().get(0));
    }

    @Test
    void getSettlementDetailsReturnsDetailsDto() {
        UUID settlementId = UUID.randomUUID();
        WorkerSettlementDetailsDto detailsDto = new WorkerSettlementDetailsDto(
                settlementId,
                LocalDate.of(2026, 11, 13),
                UUID.randomUUID(),
                "Test Worker",
                new BigDecimal("1500.00"),
                new BigDecimal("1500.00"),
                true,
                SettlementStatus.PROCESSING,
                "REF-123",
                LocalDateTime.now(),
                null,
                null,
                "Test Worker",
                "001234567890",
                "BKID0001234",
                "Bank of India",
                UUID.randomUUID(),
                "Administrator",
                List.of(),
                null
        );

        WorkerSettlementService serviceStub = new WorkerSettlementService(
                null, null, null, null, null, null, null, null
        ) {
            @Override
            public WorkerSettlementDetailsDto getSettlementDetails(UUID id) {
                assertEquals(settlementId, id);
                return detailsDto;
            }
        };

        AdminWorkerSettlementController controller = new AdminWorkerSettlementController(serviceStub);
        ResponseEntity<WorkerSettlementDetailsDto> response = controller.getSettlementDetails(settlementId);

        assertNotNull(response);
        assertEquals(200, response.getStatusCode().value());
        assertSame(detailsDto, response.getBody());
    }

    @Test
    void createSettlementDerivesAdminIdFromAuthentication() {
        UUID adminId = UUID.randomUUID();
        UUID workerId = UUID.randomUUID();
        UUID paymentId = UUID.randomUUID();
        LocalDate businessDate = LocalDate.of(2026, 11, 13);
        UUID createdSettlementId = UUID.randomUUID();

        CreateSettlementRequestDto request = new CreateSettlementRequestDto();
        request.setWorkerId(workerId);
        request.setBusinessDate(businessDate);
        request.setPaymentIds(List.of(paymentId));

        WorkerSettlementResponseDto expectedResponse = new WorkerSettlementResponseDto(
                createdSettlementId,
                businessDate,
                workerId,
                "Test Worker",
                new BigDecimal("500.00"),
                SettlementStatus.PENDING,
                1,
                "Test Worker",
                "******7890",
                "BKID0001234",
                "Bank of India",
                null,
                null,
                LocalDateTime.now(),
                null
        );

        final UUID[] capturedAdminId = new UUID[1];
        WorkerSettlementService serviceStub = new WorkerSettlementService(
                null, null, null, null, null, null, null, null
        ) {
            @Override
            public WorkerSettlementResponseDto createSettlementResponse(
                    UUID wId, List<UUID> pIds, LocalDate bDate, UUID aId) {
                capturedAdminId[0] = aId;
                assertEquals(workerId, wId);
                assertEquals(List.of(paymentId), pIds);
                assertEquals(businessDate, bDate);
                return expectedResponse;
            }
        };

        AdminWorkerSettlementController controller = new AdminWorkerSettlementController(serviceStub);
        Authentication auth = createAdminAuth(adminId);

        ResponseEntity<WorkerSettlementResponseDto> response = controller.createSettlement(request, auth);

        assertNotNull(response);
        assertEquals(201, response.getStatusCode().value());
        assertEquals("/api/admin/settlements/" + createdSettlementId, response.getHeaders().getLocation().getPath());
        assertEquals(adminId, capturedAdminId[0], "Admin ID must be derived from authentication principal");
        assertSame(expectedResponse, response.getBody());
    }

    @Test
    void markSettlementProcessingCallsServiceWithAdminId() {
        UUID adminId = UUID.randomUUID();
        UUID settlementId = UUID.randomUUID();

        WorkerSettlementResponseDto expectedResponse = new WorkerSettlementResponseDto(
                settlementId,
                LocalDate.of(2026, 11, 13),
                UUID.randomUUID(),
                "Test Worker",
                new BigDecimal("500.00"),
                SettlementStatus.PROCESSING,
                1,
                "Test Worker",
                "******7890",
                "BKID0001234",
                "Bank of India",
                null,
                null,
                LocalDateTime.now(),
                null
        );

        final UUID[] capturedAdminId = new UUID[1];
        WorkerSettlementService serviceStub = new WorkerSettlementService(
                null, null, null, null, null, null, null, null
        ) {
            @Override
            public WorkerSettlementResponseDto markSettlementProcessingResponse(UUID sId, UUID aId) {
                assertEquals(settlementId, sId);
                capturedAdminId[0] = aId;
                return expectedResponse;
            }
        };

        AdminWorkerSettlementController controller = new AdminWorkerSettlementController(serviceStub);
        Authentication auth = createAdminAuth(adminId);

        ResponseEntity<WorkerSettlementResponseDto> response = controller.markSettlementProcessing(settlementId, auth);

        assertNotNull(response);
        assertEquals(200, response.getStatusCode().value());
        assertEquals(adminId, capturedAdminId[0]);
        assertEquals(SettlementStatus.PROCESSING, response.getBody().getStatus());
    }

    @Test
    void markSettlementSettledCallsServiceWithAdminIdAndReference() {
        UUID adminId = UUID.randomUUID();
        UUID settlementId = UUID.randomUUID();
        UpdateSettlementSettledDto request = new UpdateSettlementSettledDto();
        request.setPaymentReference("REF-12345");

        WorkerSettlementResponseDto expectedResponse = new WorkerSettlementResponseDto(
                settlementId,
                LocalDate.of(2026, 11, 13),
                UUID.randomUUID(),
                "Test Worker",
                new BigDecimal("500.00"),
                SettlementStatus.SETTLED,
                1,
                "Test Worker",
                "******7890",
                "BKID0001234",
                "Bank of India",
                "REF-12345",
                null,
                LocalDateTime.now(),
                LocalDateTime.now()
        );

        final UUID[] capturedAdminId = new UUID[1];
        final String[] capturedRef = new String[1];
        WorkerSettlementService serviceStub = new WorkerSettlementService(
                null, null, null, null, null, null, null, null
        ) {
            @Override
            public WorkerSettlementResponseDto markSettlementSettledResponse(UUID sId, String ref, UUID aId) {
                assertEquals(settlementId, sId);
                capturedRef[0] = ref;
                capturedAdminId[0] = aId;
                return expectedResponse;
            }
        };

        AdminWorkerSettlementController controller = new AdminWorkerSettlementController(serviceStub);
        Authentication auth = createAdminAuth(adminId);

        ResponseEntity<WorkerSettlementResponseDto> response =
                controller.markSettlementSettled(settlementId, request, auth);

        assertNotNull(response);
        assertEquals(200, response.getStatusCode().value());
        assertEquals(adminId, capturedAdminId[0]);
        assertEquals("REF-12345", capturedRef[0]);
        assertEquals(SettlementStatus.SETTLED, response.getBody().getStatus());
    }

    @Test
    void markSettlementFailedCallsServiceWithAdminIdAndReason() {
        UUID adminId = UUID.randomUUID();
        UUID settlementId = UUID.randomUUID();
        UpdateSettlementFailedDto request = new UpdateSettlementFailedDto();
        request.setFailureReason("Account blocked");

        WorkerSettlementResponseDto expectedResponse = new WorkerSettlementResponseDto(
                settlementId,
                LocalDate.of(2026, 11, 13),
                UUID.randomUUID(),
                "Test Worker",
                new BigDecimal("500.00"),
                SettlementStatus.FAILED,
                1,
                "Test Worker",
                "******7890",
                "BKID0001234",
                "Bank of India",
                null,
                "Account blocked",
                LocalDateTime.now(),
                null
        );

        final UUID[] capturedAdminId = new UUID[1];
        final String[] capturedReason = new String[1];
        WorkerSettlementService serviceStub = new WorkerSettlementService(
                null, null, null, null, null, null, null, null
        ) {
            @Override
            public WorkerSettlementResponseDto markSettlementFailedResponse(UUID sId, String reason, UUID aId) {
                assertEquals(settlementId, sId);
                capturedReason[0] = reason;
                capturedAdminId[0] = aId;
                return expectedResponse;
            }
        };

        AdminWorkerSettlementController controller = new AdminWorkerSettlementController(serviceStub);
        Authentication auth = createAdminAuth(adminId);

        ResponseEntity<WorkerSettlementResponseDto> response =
                controller.markSettlementFailed(settlementId, request, auth);

        assertNotNull(response);
        assertEquals(200, response.getStatusCode().value());
        assertEquals(adminId, capturedAdminId[0]);
        assertEquals("Account blocked", capturedReason[0]);
        assertEquals(SettlementStatus.FAILED, response.getBody().getStatus());
    }

    @Test
    void recordConfirmedReversalCallsService() {
        UUID adminId = UUID.randomUUID();
        UUID settlementId = UUID.randomUUID();
        RecordSettlementReversalRequestDto request = new RecordSettlementReversalRequestDto();
        request.setBankReversalReference("REV-9999");
        request.setReversalReason("Bank confirmed chargeback");
        request.setVerifiedWithBankRecords(true);

        WorkerSettlementReversalDto expectedResponse = new WorkerSettlementReversalDto(
                "REV-9999",
                "Bank confirmed chargeback",
                LocalDateTime.now(),
                adminId,
                "Administrator"
        );

        WorkerSettlementService serviceStub = new WorkerSettlementService(
                null, null, null, null, null, null, null, null
        ) {
            @Override
            public WorkerSettlementReversalDto recordConfirmedReversal(
                    UUID sId, String ref, String reason, Boolean verified, UUID aId) {
                assertEquals(settlementId, sId);
                assertEquals("REV-9999", ref);
                assertEquals("Bank confirmed chargeback", reason);
                assertTrue(verified);
                assertEquals(adminId, aId);
                return expectedResponse;
            }
        };

        AdminWorkerSettlementController controller = new AdminWorkerSettlementController(serviceStub);
        Authentication auth = createAdminAuth(adminId);

        ResponseEntity<WorkerSettlementReversalDto> response =
                controller.recordConfirmedReversal(settlementId, request, auth);

        assertNotNull(response);
        assertEquals(200, response.getStatusCode().value());
        assertSame(expectedResponse, response.getBody());
    }

    @Test
    void getSettlementBatchesReturnsBatchesDto() {
        WorkerSettlementBatchesDto expectedDto = new WorkerSettlementBatchesDto(List.of(), 0, BigDecimal.ZERO, 0);

        WorkerSettlementService serviceStub = new WorkerSettlementService(
                null, null, null, null, null, null, null, null
        ) {
            @Override
            public WorkerSettlementBatchesDto getSettlementBatches() {
                return expectedDto;
            }
        };

        AdminWorkerSettlementController controller = new AdminWorkerSettlementController(serviceStub);
        ResponseEntity<WorkerSettlementBatchesDto> response = controller.getSettlementBatches();

        assertNotNull(response);
        assertEquals(200, response.getStatusCode().value());
        assertSame(expectedDto, response.getBody());
    }

    @Test
    void getSettlementSummaryReturnsSummaryDto() {
        AdminWorkerSettlementSummaryDto expectedDto = new AdminWorkerSettlementSummaryDto(
                5, new BigDecimal("10000.00"), 2, 1, new BigDecimal("5000.00"), 0
        );

        WorkerSettlementService serviceStub = new WorkerSettlementService(
                null, null, null, null, null, null, null, null
        ) {
            @Override
            public AdminWorkerSettlementSummaryDto getAdminSummary() {
                return expectedDto;
            }
        };

        AdminWorkerSettlementController controller = new AdminWorkerSettlementController(serviceStub);
        ResponseEntity<AdminWorkerSettlementSummaryDto> response = controller.getSettlementSummary();

        assertNotNull(response);
        assertEquals(200, response.getStatusCode().value());
        assertSame(expectedDto, response.getBody());
    }
}
