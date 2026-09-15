package com.servicehub.serv.service;

import com.servicehub.serv.dto.CustomerCategoryDto;
import com.servicehub.serv.dto.CustomerServiceDto;
import com.servicehub.serv.repository.CategoryRepository;
import com.servicehub.serv.repository.ServiceRepository;

import java.util.List;

@org.springframework.stereotype.Service
public class CustomerServiceService {

    private final CategoryRepository categoryRepository;
    private final ServiceRepository serviceRepository;

    public CustomerServiceService(
            CategoryRepository categoryRepository,
            ServiceRepository serviceRepository
    ) {
        this.categoryRepository = categoryRepository;
        this.serviceRepository = serviceRepository;
    }

    public List<CustomerCategoryDto> getActiveCategories() {

        return categoryRepository.findAll()
                .stream()
                .filter(category -> category.isActive())
                .map(category -> new CustomerCategoryDto(
                        category.getCategoryId(),
                        category.getCategoryName()
                ))
                .toList();
    }


    public List<CustomerServiceDto> getActiveServices() {

        return serviceRepository.findAll()
                .stream()
                .filter(service -> service.isActive())
                .filter(service -> service.getCategory() != null)
                .filter(service -> service.getCategory().isActive())
                .map(service -> new CustomerServiceDto(
                        service.getServiceId(),
                        service.getServiceName(),
                        service.getCategory().getCategoryId(),
                        service.getCategory().getCategoryName()
                ))
                .toList();
    }
}