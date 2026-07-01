package com.servija.servija.controller;

import com.servija.servija.dto.SupportRequestCreateRequest;
import com.servija.servija.dto.SupportRequestResponse;
import com.servija.servija.service.SupportRequestService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/support-requests")
@RequiredArgsConstructor
@Tag(name = "Support Requests", description = "Endpoints de solicitações de suporte")
public class SupportRequestController {

    private final SupportRequestService supportRequestService;

    @PostMapping
    @Operation(summary = "Criar solicitação de suporte")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<SupportRequestResponse> create(
            @Valid @RequestBody SupportRequestCreateRequest request,
            Authentication authentication) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(supportRequestService.create(authentication.getName(), request));
    }

    @GetMapping("/my")
    @Operation(summary = "Listar solicitações do usuário autenticado")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<List<SupportRequestResponse>> getMyRequests(Authentication authentication) {
        return ResponseEntity.ok(supportRequestService.getMyRequests(authentication.getName()));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obter detalhes de uma solicitação do usuário autenticado")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<SupportRequestResponse> getById(
            @PathVariable Long id,
            Authentication authentication) {
        return ResponseEntity.ok(supportRequestService.getByIdForUser(id, authentication.getName()));
    }
}
