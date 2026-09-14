package com.servicehub.serv.controller;

import com.servicehub.serv.dto.AdminActionResponseDto;
import com.servicehub.serv.dto.CategoryAdminListDto;
import com.servicehub.serv.service.CategoryAdminService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin/categories")
public class CategoryAdminController {

    private final CategoryAdminService categoryAdminService;

    public CategoryAdminController(
            CategoryAdminService categoryAdminService) {
        this.categoryAdminService = categoryAdminService;
    }

    @GetMapping
    public ResponseEntity<List<CategoryAdminListDto>> getAllCategories() {

        return ResponseEntity.ok(
                categoryAdminService.getAllCategories()
        );
    }

    @PostMapping
    public ResponseEntity<CategoryAdminListDto> addCategory(
            @RequestParam String categoryName) {

        return ResponseEntity.ok(
                categoryAdminService.addCategory(categoryName)
        );
    }

    @PutMapping("/{categoryId}/activate")
    public ResponseEntity<AdminActionResponseDto> activateCategory(
            @PathVariable UUID categoryId) {

        return ResponseEntity.ok(
                categoryAdminService.activateCategory(categoryId)
        );
    }

    @PutMapping("/{categoryId}/deactivate")
    public ResponseEntity<AdminActionResponseDto> deactivateCategory(
            @PathVariable UUID categoryId) {

        return ResponseEntity.ok(
                categoryAdminService.deactivateCategory(categoryId)
        );
    }
}