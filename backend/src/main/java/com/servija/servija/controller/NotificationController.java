package com.servija.servija.controller;

import com.servija.servija.dto.NotificationListResponse;
import com.servija.servija.dto.NotificationResponse;
import com.servija.servija.service.NotificationService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<NotificationListResponse> getNotifications(
            @RequestParam(required = false) Integer limit,
            @RequestParam(required = false) Boolean read,
            @RequestParam(required = false) String category,
            Authentication authentication) {
        return ResponseEntity.ok(
                notificationService.getNotifications(authentication.getName(), limit, read, category)
        );
    }

    @PatchMapping("/{id}/read")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<NotificationResponse> markAsRead(
            @PathVariable Long id,
            Authentication authentication) {
        return ResponseEntity.ok(notificationService.markAsRead(id, authentication.getName()));
    }

    @PatchMapping("/read-all")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<Void> markAllAsRead(Authentication authentication) {
        notificationService.markAllAsRead(authentication.getName());
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<Void> deleteNotification(
            @PathVariable Long id,
            Authentication authentication) {
        notificationService.deleteNotification(id, authentication.getName());
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<Void> clearAll(Authentication authentication) {
        notificationService.clearAll(authentication.getName());
        return ResponseEntity.noContent().build();
    }
}
