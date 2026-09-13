package com.servicehub.serv.controller;

import com.servicehub.serv.dto.CategoryResponseDto;
import com.servicehub.serv.service.CategoryService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/categories")
public class CategoryController {

    private final CategoryService categoryService;

    public CategoryController(CategoryService categoryService) {
        this.categoryService = categoryService;
    }

    @GetMapping
    public ResponseEntity<List<CategoryResponseDto>> getCategories() {

        List<CategoryResponseDto> categories =
                categoryService.getActiveCategories();

        return ResponseEntity.ok(categories);
    }
}