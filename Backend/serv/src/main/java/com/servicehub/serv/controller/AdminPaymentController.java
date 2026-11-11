package com.servicehub.serv.controller;

import com.servicehub.serv.dto.AdminPaymentDashboardDto;
import com.servicehub.serv.dto.AdminPaymentDetailsDto;
import com.servicehub.serv.service.AdminPaymentService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/admin/payments")
public class AdminPaymentController {

    private final AdminPaymentService adminPaymentService;

    public AdminPaymentController(AdminPaymentService adminPaymentService) {
        this.adminPaymentService = adminPaymentService;
    }

    @GetMapping
    public ResponseEntity<AdminPaymentDashboardDto> getPayments() {
        return ResponseEntity.ok(adminPaymentService.getPayments());
    }

    @GetMapping("/{paymentId}")
    public ResponseEntity<AdminPaymentDetailsDto> getPayment(
            @PathVariable UUID paymentId) {
        return ResponseEntity.ok(adminPaymentService.getPayment(paymentId));
    }
}
