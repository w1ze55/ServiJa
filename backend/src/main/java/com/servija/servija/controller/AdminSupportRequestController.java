package com.servija.servija.controller;

import com.servija.servija.dto.SupportRequestActionRequest;
import com.servija.servija.dto.SupportRequestResponse;
import com.servija.servija.service.SupportRequestService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin/support-requests")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminSupportRequestController {

    private final SupportRequestService supportRequestService;

    @GetMapping
    public ResponseEntity<List<SupportRequestResponse>> listAll() {
        return ResponseEntity.ok(supportRequestService.listAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<SupportRequestResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(supportRequestService.getByIdForAdmin(id));
    }

    @PatchMapping("/{id}/approve")
    public ResponseEntity<SupportRequestResponse> approve(
            @PathVariable Long id,
            @Valid @RequestBody SupportRequestActionRequest request,
            Authentication authentication) {
        return ResponseEntity.ok(
                supportRequestService.approve(id, authentication.getName(), request.getResponseMessage()));
    }

    @PatchMapping("/{id}/reject")
    public ResponseEntity<SupportRequestResponse> reject(
            @PathVariable Long id,
            @Valid @RequestBody SupportRequestActionRequest request,
            Authentication authentication) {
        return ResponseEntity.ok(
                supportRequestService.reject(id, authentication.getName(), request.getResponseMessage()));
    }
}
