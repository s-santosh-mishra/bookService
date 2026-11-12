package com.servicehub.serv.controller;

import com.servicehub.serv.dto.WorkerLocationUpdateDto;
import com.servicehub.serv.dto.WorkerStatusDto;
import com.servicehub.serv.enums.AvailabilityStatus;
import com.servicehub.serv.enums.VerificationStatus;
import com.servicehub.serv.service.WorkerStatusService;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class WorkerStatusControllerTest {

    @Test
    void availabilityChangeUsesAuthenticatedWorkerIdentity() {
        UUID authenticatedWorkerId = UUID.randomUUID();
        Authentication authentication = mock(Authentication.class);
        WorkerStatusDto status = new WorkerStatusDto(
                VerificationStatus.VERIFIED,
                AvailabilityStatus.UNAVAILABLE);
        WorkerStatusService workerStatusService =
                mock(WorkerStatusService.class);
        WorkerStatusController controller =
                new WorkerStatusController(workerStatusService);

        when(authentication.getName())
                .thenReturn(authenticatedWorkerId.toString());
        when(workerStatusService.goOffline(authenticatedWorkerId))
                .thenReturn(status);

        ResponseEntity<WorkerStatusDto> response =
                controller.goOffline(authentication);

        assertSame(status, response.getBody());
        verify(workerStatusService).goOffline(authenticatedWorkerId);
    }

    @Test
    void locationUpdateUsesAuthenticatedWorkerIdentity() {
        UUID authenticatedWorkerId = UUID.randomUUID();
        Authentication authentication = mock(Authentication.class);
        WorkerLocationUpdateDto request = new WorkerLocationUpdateDto();
        request.setLatitude(20.2961);
        request.setLongitude(85.8245);
        WorkerStatusDto status = new WorkerStatusDto(
                VerificationStatus.VERIFIED,
                AvailabilityStatus.AVAILABLE);
        WorkerStatusService workerStatusService =
                mock(WorkerStatusService.class);
        WorkerStatusController controller =
                new WorkerStatusController(workerStatusService);

        when(authentication.getName())
                .thenReturn(authenticatedWorkerId.toString());
        when(workerStatusService.updateLocation(authenticatedWorkerId, request))
                .thenReturn(status);

        ResponseEntity<WorkerStatusDto> response =
                controller.updateLocation(authentication, request);

        assertSame(status, response.getBody());
        verify(workerStatusService).updateLocation(authenticatedWorkerId, request);
    }
}
