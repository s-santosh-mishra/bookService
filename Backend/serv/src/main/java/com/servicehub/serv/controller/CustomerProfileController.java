package com.servicehub.serv.controller;

import com.servicehub.serv.dto.CustomerProfileDto;
import com.servicehub.serv.dto.UpdateCustomerProfileDto;
import com.servicehub.serv.service.CustomerProfileService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/customer/profile")
public class CustomerProfileController {

    private final CustomerProfileService customerProfileService;

    public CustomerProfileController(
            CustomerProfileService customerProfileService) {

        this.customerProfileService = customerProfileService;
    }

    // Get Profile

    @GetMapping
    public ResponseEntity<CustomerProfileDto> getProfile(
            Authentication authentication) {

        UUID userId =
                UUID.fromString(authentication.getName());

        return ResponseEntity.ok(
                customerProfileService.getProfile(userId)
        );
    }

    // Update Profile

    @PutMapping
    public ResponseEntity<CustomerProfileDto> updateProfile(
            Authentication authentication,
            @RequestBody UpdateCustomerProfileDto dto) {

        UUID userId =
                UUID.fromString(authentication.getName());

        return ResponseEntity.ok(
                customerProfileService.updateProfile(
                        userId,
                        dto
                )
        );
    }
}