package com.servicehub.serv.service;

import com.servicehub.serv.dto.AdminActionResponseDto;
import com.servicehub.serv.dto.ServiceAdminListDto;
import com.servicehub.serv.dto.ServiceWorkersAdminDto;
import com.servicehub.serv.entity.Category;
import com.servicehub.serv.entity.Service;
import com.servicehub.serv.entity.Worker;
import com.servicehub.serv.repository.CategoryRepository;
import com.servicehub.serv.repository.ServiceRepository;
import com.servicehub.serv.repository.WorkerServiceRepository;

import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@org.springframework.stereotype.Service
public class ServiceAdminService {

        private final ServiceRepository serviceRepository;
        private final CategoryRepository categoryRepository;
        private final WorkerServiceRepository workerServiceRepository;

        public ServiceAdminService(
                        ServiceRepository serviceRepository,
                        CategoryRepository categoryRepository,
                        WorkerServiceRepository workerServiceRepository) {
                this.serviceRepository = serviceRepository;
                this.categoryRepository = categoryRepository;
                this.workerServiceRepository = workerServiceRepository;
        }

        public List<ServiceAdminListDto> getAllServices() {
                return serviceRepository.findAll()
                                .stream()
                                .map(this::toListDto)
                                .toList();
        }

        @Transactional
        public ServiceAdminListDto addService(
                        String serviceName,
                        UUID categoryId,
                        BigDecimal basePricePerHour,
                        BigDecimal minimumServiceFee) {
                String name = serviceName.trim();

                if (name.isEmpty()) {
                        throw new IllegalArgumentException(
                                        "Service name cannot be empty.");
                }

                if (basePricePerHour == null
                                || basePricePerHour.compareTo(BigDecimal.ZERO) <= 0) {
                        throw new IllegalArgumentException(
                                        "Price per hour must be greater than zero.");
                }

                if (minimumServiceFee == null
                                || minimumServiceFee.compareTo(BigDecimal.ZERO) <= 0) {
                        throw new IllegalArgumentException(
                                        "Minimum service fee must be greater than zero.");
                }

                Category category = categoryRepository.findById(categoryId)
                                .orElseThrow(() -> new IllegalArgumentException(
                                                "Category not found."));

                if (!category.isActive()) {
                        throw new IllegalStateException(
                                        "Cannot add a service to an inactive category.");
                }

                Service service = new Service();

                service.setServiceName(name);
                service.setCategory(category);
                service.setActive(true);
                service.setBasePricePerHour(basePricePerHour);
                service.setMinimumServiceFee(minimumServiceFee);

                Service savedService = serviceRepository.save(service);

                return toListDto(savedService);
        }

        @Transactional
        public AdminActionResponseDto activateService(
                        UUID serviceId) {
                Service service = getServiceEntity(serviceId);

                if (service.isActive()) {
                        throw new IllegalStateException(
                                        "Service is already active.");
                }

                if (!service.getCategory().isActive()) {
                        throw new IllegalStateException(
                                        "Cannot activate a service under an inactive category.");
                }

                service.setActive(true);

                return new AdminActionResponseDto(
                                "Service activated successfully.");
        }

        @Transactional
        public AdminActionResponseDto deactivateService(
                        UUID serviceId) {
                Service service = getServiceEntity(serviceId);

                if (!service.isActive()) {
                        throw new IllegalStateException(
                                        "Service is already inactive.");
                }

                service.setActive(false);

                return new AdminActionResponseDto(
                                "Service deactivated successfully.");
        }

        public List<ServiceWorkersAdminDto> getWorkersForService(UUID serviceId) {
                getServiceEntity(serviceId);

                return workerServiceRepository.findByService_ServiceId(serviceId)
                                .stream()
                                .map(workerService -> {
                                        Worker worker = workerService.getWorker();

                                        return new ServiceWorkersAdminDto(
                                                        worker.getUserId(),
                                                        worker.getUser().getFullName(),
                                                        worker.getUser().getCredentials().getEmail(),
                                                        worker.getUser().getPhone(),
                                                        worker.getExperienceYears(),
                                                        worker.getVerificationStatus().name(),
                                                        worker.getAvailabilityStatus().name());
                                })
                                .toList();
        }

        private Service getServiceEntity(UUID serviceId) {
                return serviceRepository.findById(serviceId)
                                .orElseThrow(() -> new IllegalArgumentException(
                                                "Service not found."));
        }

        private ServiceAdminListDto toListDto(Service service) {
                return new ServiceAdminListDto(
                                service.getServiceId(),
                                service.getServiceName(),
                                service.getCategory().getCategoryId(),
                                service.getCategory().getCategoryName(),
                                service.isActive());
        }
}