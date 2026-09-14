package com.servicehub.serv.controller;

import com.servicehub.serv.dto.AdminActionResponseDto;
import com.servicehub.serv.dto.ServiceAdminListDto;
import com.servicehub.serv.dto.ServiceWorkersAdminDto;
import com.servicehub.serv.service.ServiceAdminService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin/services")
public class ServiceAdminController {

        private final ServiceAdminService serviceAdminService;

        public ServiceAdminController(
                        ServiceAdminService serviceAdminService) {
                this.serviceAdminService = serviceAdminService;
        }

        @GetMapping
        public ResponseEntity<List<ServiceAdminListDto>> getAllServices() {
                return ResponseEntity.ok(
                                serviceAdminService.getAllServices());
        }

        @PostMapping
        public ResponseEntity<ServiceAdminListDto> addService(
                        @RequestParam String serviceName,
                        @RequestParam UUID categoryId) {
                return ResponseEntity.ok(
                                serviceAdminService.addService(
                                                serviceName,
                                                categoryId));
        }

        @PutMapping("/{serviceId}/activate")
        public ResponseEntity<AdminActionResponseDto> activateService(
                        @PathVariable UUID serviceId) {
                return ResponseEntity.ok(
                                serviceAdminService.activateService(serviceId));
        }

        @PutMapping("/{serviceId}/deactivate")
        public ResponseEntity<AdminActionResponseDto> deactivateService(
                        @PathVariable UUID serviceId) {
                return ResponseEntity.ok(
                                serviceAdminService.deactivateService(serviceId));
        }

        @GetMapping("/{serviceId}/workers")
        public ResponseEntity<List<ServiceWorkersAdminDto>> getWorkersForService(
                        @PathVariable UUID serviceId) {
                return ResponseEntity.ok(
                                serviceAdminService.getWorkersForService(serviceId));
        }
}