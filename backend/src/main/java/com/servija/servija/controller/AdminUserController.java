package com.servija.servija.controller;

import com.servija.servija.dto.AdminUserDetailDto;
import com.servija.servija.dto.AdminUserEditRequest;
import com.servija.servija.dto.AdminUserKycRequest;
import com.servija.servija.dto.AdminUserPageResponse;
import com.servija.servija.dto.AdminUserStatusRequest;
import com.servija.servija.dto.AdminUserSummaryDto;
import com.servija.servija.model.User;
import com.servija.servija.service.AdminUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminUserController {

    private final AdminUserService adminUserService;

    @GetMapping("/users")
    public ResponseEntity<AdminUserPageResponse> listUsers(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) User.UserType type,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String kycStatus,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "25") int size,
            @RequestParam(defaultValue = "false") boolean includeDeleted) {

        return ResponseEntity.ok(adminUserService.listUsers(
                search, type, status, kycStatus, dateFrom, dateTo, page, size, includeDeleted));
    }

    @GetMapping("/users/{id}")
    public ResponseEntity<AdminUserDetailDto> getUserById(@PathVariable Long id) {
        return ResponseEntity.ok(adminUserService.getUserById(id));
    }

    @PatchMapping("/users/{id}/status")
    public ResponseEntity<AdminUserSummaryDto> updateStatus(
            @PathVariable Long id,
            @RequestBody AdminUserStatusRequest request,
            Authentication authentication) {

        String adminEmail = authentication.getName();
        return ResponseEntity.ok(adminUserService.updateStatus(id, request, adminEmail));
    }

    @PutMapping("/users/{id}")
    public ResponseEntity<AdminUserDetailDto> updateUser(
            @PathVariable Long id,
            @RequestBody AdminUserEditRequest request) {

        return ResponseEntity.ok(adminUserService.updateUser(id, request));
    }

    @DeleteMapping("/users/{id}")
    public ResponseEntity<Void> deleteUser(
            @PathVariable Long id,
            @RequestParam(defaultValue = "false") boolean hard) {

        adminUserService.deleteUser(id, hard);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/users/{id}/reset-password")
    public ResponseEntity<Map<String, String>> resetPassword(@PathVariable Long id) {
        adminUserService.resetPasswordByAdmin(id);
        return ResponseEntity.ok(Map.of("message", "Email de redefinição enviado"));
    }

    @PatchMapping("/users/{id}/kyc")
    public ResponseEntity<AdminUserDetailDto> updateKycStatus(
            @PathVariable Long id,
            @RequestBody AdminUserKycRequest request) {

        return ResponseEntity.ok(adminUserService.updateKycStatus(id, request));
    }

}
