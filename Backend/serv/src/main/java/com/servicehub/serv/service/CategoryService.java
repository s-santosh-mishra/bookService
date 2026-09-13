package com.servicehub.serv.service;

import com.servicehub.serv.dto.CategoryResponseDto;
import com.servicehub.serv.dto.ServiceResponseDto;
import com.servicehub.serv.entity.Category;
import com.servicehub.serv.repository.CategoryRepository;

import java.util.List;

@org.springframework.stereotype.Service
public class CategoryService {

    private final CategoryRepository categoryRepository;

    public CategoryService(CategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    public List<CategoryResponseDto> getActiveCategories() {

        List<Category> categories = categoryRepository.findAll()
                .stream()
                .filter(category -> category.isActive())
                .toList();

        return categories.stream()
                .map(category -> {

                    List<ServiceResponseDto> services =
                            category.getServices()
                                    .stream()
                                    .filter(service -> service.isActive())
                                    .map(service -> new ServiceResponseDto(
                                            service.getServiceId(),
                                            service.getServiceName()
                                    ))
                                    .toList();

                    return new CategoryResponseDto(
                            category.getCategoryId(),
                            category.getCategoryName(),
                            services
                    );
                })
                .toList();
    }
}