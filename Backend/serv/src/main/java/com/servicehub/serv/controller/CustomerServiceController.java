package com.servicehub.serv.controller;

import com.servicehub.serv.dto.CustomerCategoryDto;
import com.servicehub.serv.dto.CustomerServiceDto;
import com.servicehub.serv.service.CustomerServiceService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.servicehub.serv.dto.PriceEstimateDto;
import com.servicehub.serv.dto.PriceEstimateRequestDto;
import com.servicehub.serv.service.BillingService;

import java.util.List;

@RestController
@RequestMapping("/api/customer")
public class CustomerServiceController {

    private final CustomerServiceService customerServiceService;
    private final BillingService billingService;

    public CustomerServiceController(
            CustomerServiceService customerServiceService,
            BillingService billingService) {

        this.customerServiceService = customerServiceService;
        this.billingService = billingService;
    }

    @GetMapping("/categories")
    public ResponseEntity<List<CustomerCategoryDto>> getActiveCategories() {

        return ResponseEntity.ok(
                customerServiceService.getActiveCategories());
    }

    @PostMapping("/price-estimate")
    public ResponseEntity<PriceEstimateDto> estimatePrice(
            @RequestBody PriceEstimateRequestDto request) {

        PriceEstimateDto estimate = billingService.estimatePrice(
                request.getServiceId(),
                request.getCustomerLatitude(),
                request.getCustomerLongitude(),
                request.getEstimatedDurationHours());

        return ResponseEntity.ok(estimate);
    }

    @GetMapping("/services")
    public ResponseEntity<List<CustomerServiceDto>> getActiveServices() {

        return ResponseEntity.ok(
                customerServiceService.getActiveServices());
    }
}