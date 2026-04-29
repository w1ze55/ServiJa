package com.servija.servija.controller;

import com.servija.servija.dto.ServiceCategoryRequest;
import com.servija.servija.dto.ServiceCategoryResponse;
import com.servija.servija.dto.ServiceSubcategoryRequest;
import com.servija.servija.dto.ServiceSubcategoryResponse;
import com.servija.servija.service.CategoryCatalogService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminCategoryController {

    private final CategoryCatalogService categoryCatalogService;

    @GetMapping("/categories")
    public ResponseEntity<List<ServiceCategoryResponse>> getCategories() {
        return ResponseEntity.ok(categoryCatalogService.getAdminCategories());
    }

    @PostMapping("/categories")
    public ResponseEntity<ServiceCategoryResponse> createCategory(@Valid @RequestBody ServiceCategoryRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(categoryCatalogService.createCategory(request));
    }

    @PutMapping("/categories/{categoryId}")
    public ResponseEntity<ServiceCategoryResponse> updateCategory(
            @PathVariable Long categoryId,
            @Valid @RequestBody ServiceCategoryRequest request) {
        return ResponseEntity.ok(categoryCatalogService.updateCategory(categoryId, request));
    }

    @PatchMapping("/categories/{categoryId}/archive")
    public ResponseEntity<ServiceCategoryResponse> archiveCategory(@PathVariable Long categoryId) {
        return ResponseEntity.ok(categoryCatalogService.archiveCategory(categoryId));
    }

    @PatchMapping("/categories/{categoryId}/restore")
    public ResponseEntity<ServiceCategoryResponse> restoreCategory(@PathVariable Long categoryId) {
        return ResponseEntity.ok(categoryCatalogService.restoreCategory(categoryId));
    }

    @GetMapping("/subcategories")
    public ResponseEntity<List<ServiceSubcategoryResponse>> getSubcategories(
            @RequestParam(required = false) Long categoryId) {
        return ResponseEntity.ok(categoryCatalogService.getAdminSubcategories(categoryId));
    }

    @PostMapping("/subcategories")
    public ResponseEntity<ServiceSubcategoryResponse> createSubcategory(
            @Valid @RequestBody ServiceSubcategoryRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(categoryCatalogService.createSubcategory(request));
    }

    @PutMapping("/subcategories/{subcategoryId}")
    public ResponseEntity<ServiceSubcategoryResponse> updateSubcategory(
            @PathVariable Long subcategoryId,
            @Valid @RequestBody ServiceSubcategoryRequest request) {
        return ResponseEntity.ok(categoryCatalogService.updateSubcategory(subcategoryId, request));
    }

    @PatchMapping("/subcategories/{subcategoryId}/archive")
    public ResponseEntity<ServiceSubcategoryResponse> archiveSubcategory(@PathVariable Long subcategoryId) {
        return ResponseEntity.ok(categoryCatalogService.archiveSubcategory(subcategoryId));
    }

    @PatchMapping("/subcategories/{subcategoryId}/restore")
    public ResponseEntity<ServiceSubcategoryResponse> restoreSubcategory(@PathVariable Long subcategoryId) {
        return ResponseEntity.ok(categoryCatalogService.restoreSubcategory(subcategoryId));
    }
}
