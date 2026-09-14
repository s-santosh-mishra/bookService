package com.servicehub.serv.controller;

import com.servicehub.serv.dto.AdminActionResponseDto;
import com.servicehub.serv.dto.CustomerAdminDetailDto;
import com.servicehub.serv.dto.CustomerAdminListDto;
import com.servicehub.serv.service.CustomerAdminService;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin/customers")
public class CustomerAdminController {

    private final CustomerAdminService customerAdminService;

    public CustomerAdminController(
            CustomerAdminService customerAdminService) {
        this.customerAdminService = customerAdminService;
    }

    // Get All Customers

    @GetMapping
    public ResponseEntity<List<CustomerAdminListDto>> getAllCustomers() {

        return ResponseEntity.ok(
                customerAdminService.getAllCustomers());
    }

    // Get Customer

    @GetMapping("/{customerId}")
    public ResponseEntity<CustomerAdminDetailDto> getCustomer(
            @PathVariable UUID customerId) {

        return ResponseEntity.ok(
                customerAdminService.getCustomer(customerId));
    }

    // Deactivate Customer

    @PutMapping("/{customerId}/deactivate")
    public ResponseEntity<AdminActionResponseDto> deactivateCustomer(
            @PathVariable UUID customerId) {

        return ResponseEntity.ok(
                customerAdminService.deactivateCustomer(customerId));
    }

    // Activate Customer

    @PutMapping("/{customerId}/activate")
    public ResponseEntity<AdminActionResponseDto> activateCustomer(
            @PathVariable UUID customerId) {

        return ResponseEntity.ok(
                customerAdminService.activateCustomer(customerId));
    }
}