package com.servicehub.serv.controller;

import com.servicehub.serv.dto.WorkerDailySettlementSummaryDto;
import com.servicehub.serv.service.WorkerSettlementService;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

public class WorkerSettlementControllerTest {

    @Test
    void derivesWorkerIdFromAuthenticationAndReturnsSummary() {
        UUID authenticatedWorkerId = UUID.randomUUID();
        WorkerDailySettlementSummaryDto expectedDto = new WorkerDailySettlementSummaryDto(
                LocalDate.of(2026, 11, 13),
                3,
                new BigDecimal("1500.00"),
                new BigDecimal("500.00"),
                new BigDecimal("1000.00"),
                new BigDecimal("1000.00"),
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                false,
                "SETTLED",
                "Bank transfer completed successfully."
        );

        final UUID[] queriedWorkerId = new UUID[1];
        WorkerSettlementService serviceStub = new WorkerSettlementService(
                null, null, null, null, null, null, null
        ) {
            @Override
            public WorkerDailySettlementSummaryDto getWorkerDailySummary(UUID workerId) {
                queriedWorkerId[0] = workerId;
                return expectedDto;
            }
        };

        WorkerSettlementController controller = new WorkerSettlementController(serviceStub);

        // Custom authentication object returning the UUID string
        Authentication authentication = new Authentication() {
            @Override
            public String getName() {
                return authenticatedWorkerId.toString();
            }

            @Override
            public java.util.Collection<? extends org.springframework.security.core.GrantedAuthority> getAuthorities() {
                return java.util.List.of();
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
                return authenticatedWorkerId.toString();
            }

            @Override
            public boolean isAuthenticated() {
                return true;
            }

            @Override
            public void setAuthenticated(boolean isAuthenticated) throws IllegalArgumentException {
            }
        };

        ResponseEntity<WorkerDailySettlementSummaryDto> response = controller.getTodaySummary(authentication);

        assertNotNull(response);
        assertEquals(200, response.getStatusCode().value());
        assertSame(expectedDto, response.getBody());
        assertEquals(authenticatedWorkerId, queriedWorkerId[0], "Worker ID must be derived from authenticated credentials");
    }
}
