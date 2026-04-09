package com.servija.servija.controller;

import com.servija.servija.dto.CategoryTreeResponse;
import com.servija.servija.service.CategoryCatalogService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/categories")
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryCatalogService categoryCatalogService;

    @GetMapping("/public/tree")
    public ResponseEntity<CategoryTreeResponse> getPublicTree() {
        return ResponseEntity.ok(categoryCatalogService.getPublicTree());
    }
}
