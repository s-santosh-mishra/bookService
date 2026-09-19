package com.servicehub.serv.controller;

import com.servicehub.serv.entity.UnlistedPartRequest;
import com.servicehub.serv.service.UnlistedPartRequestService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;
import java.util.List;

@RestController
@RequestMapping("/api/customer/bookings")
public class CustomerBookingPartController {

    private final UnlistedPartRequestService unlistedPartRequestService;

    public CustomerBookingPartController(
            UnlistedPartRequestService unlistedPartRequestService) {

        this.unlistedPartRequestService = unlistedPartRequestService;
    }

    @PostMapping("/parts/{requestId}/approve")
    public ResponseEntity<UnlistedPartRequest> approvePartRequest(
            @PathVariable UUID requestId,
            Authentication authentication) {

        UUID customerId = UUID.fromString(authentication.getName());

        return ResponseEntity.ok(
                unlistedPartRequestService.approveRequest(
                        customerId,
                        requestId));
    }

    @PostMapping("/parts/{requestId}/reject")
    public ResponseEntity<UnlistedPartRequest> rejectPartRequest(
            @PathVariable UUID requestId,
            Authentication authentication) {

        UUID customerId = UUID.fromString(authentication.getName());

        return ResponseEntity.ok(
                unlistedPartRequestService.rejectRequest(
                        customerId,
                        requestId));
    }

    @GetMapping("/parts/requests")
    public ResponseEntity<List<UnlistedPartRequest>> getPendingPartRequests(
            Authentication authentication) {

        UUID customerId = UUID.fromString(authentication.getName());

        return ResponseEntity.ok(
                unlistedPartRequestService
                        .getCustomerPendingRequests(customerId));
    }
}