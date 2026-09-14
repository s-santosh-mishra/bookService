package com.servicehub.serv.service;

import com.servicehub.serv.dto.AdminActionResponseDto;
import com.servicehub.serv.dto.CategoryAdminListDto;
import com.servicehub.serv.entity.Category;
import com.servicehub.serv.repository.CategoryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class CategoryAdminService {

    private final CategoryRepository categoryRepository;

    public CategoryAdminService(CategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    public List<CategoryAdminListDto> getAllCategories() {
        return categoryRepository.findAll()
                .stream()
                .map(this::toListDto)
                .toList();
    }

    @Transactional
    public CategoryAdminListDto addCategory(String categoryName) {

        String name = categoryName.trim();

        if (name.isEmpty()) {
            throw new IllegalArgumentException("Category name cannot be empty.");
        }

        if (categoryRepository.existsByCategoryNameIgnoreCase(name)) {
            throw new IllegalArgumentException(
                    "A category with this name already exists."
            );
        }

        Category category = new Category();
        category.setCategoryName(name);
        category.setActive(true);

        Category savedCategory = categoryRepository.save(category);

        return toListDto(savedCategory);
    }

    @Transactional
    public AdminActionResponseDto activateCategory(UUID categoryId) {

        Category category = getCategoryEntity(categoryId);

        if (category.isActive()) {
            throw new IllegalStateException(
                    "Category is already active."
            );
        }

        category.setActive(true);

        return new AdminActionResponseDto(
                "Category activated successfully."
        );
    }

    @Transactional
    public AdminActionResponseDto deactivateCategory(UUID categoryId) {

        Category category = getCategoryEntity(categoryId);

        if (!category.isActive()) {
            throw new IllegalStateException(
                    "Category is already inactive."
            );
        }

        category.setActive(false);

        return new AdminActionResponseDto(
                "Category deactivated successfully."
        );
    }

    private Category getCategoryEntity(UUID categoryId) {

        return categoryRepository.findById(categoryId)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Category not found."
                        )
                );
    }

    private CategoryAdminListDto toListDto(Category category) {

        return new CategoryAdminListDto(
                category.getCategoryId(),
                category.getCategoryName(),
                category.isActive(),
                category.getServices().size()
        );
    }
}