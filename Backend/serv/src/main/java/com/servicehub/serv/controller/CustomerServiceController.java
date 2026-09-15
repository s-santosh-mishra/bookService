package com.servicehub.serv.controller;

import com.servicehub.serv.dto.CustomerCategoryDto;
import com.servicehub.serv.dto.CustomerServiceDto;
import com.servicehub.serv.service.CustomerServiceService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/customer")
public class CustomerServiceController {

    private final CustomerServiceService customerServiceService;

    public CustomerServiceController(
            CustomerServiceService customerServiceService
    ) {
        this.customerServiceService = customerServiceService;
    }


    @GetMapping("/categories")
    public ResponseEntity<List<CustomerCategoryDto>> getActiveCategories() {

        return ResponseEntity.ok(
                customerServiceService.getActiveCategories()
        );
    }


    @GetMapping("/services")
    public ResponseEntity<List<CustomerServiceDto>> getActiveServices() {

        return ResponseEntity.ok(
                customerServiceService.getActiveServices()
        );
    }
}