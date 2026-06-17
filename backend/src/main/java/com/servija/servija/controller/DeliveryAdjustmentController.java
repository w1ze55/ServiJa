package com.servija.servija.controller;

import com.servija.servija.dto.AdjustmentStatusUpdate;
import com.servija.servija.dto.DeliveryAdjustmentRequest;
import com.servija.servija.dto.DeliveryAdjustmentResponse;
import com.servija.servija.service.DeliveryAdjustmentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/delivery-adjustments")
@RequiredArgsConstructor
@Tag(name = "Ajustes de Entrega", description = "Endpoints para gerenciamento de solicitações de ajustes em entregas")
public class DeliveryAdjustmentController {

    private final DeliveryAdjustmentService adjustmentService;

    @PostMapping("/service-request/{serviceRequestId}")
    @Operation(summary = "Solicitar ajustes", description = "Cliente solicita ajustes na entrega do freelancer")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<DeliveryAdjustmentResponse> requestAdjustment(
            @PathVariable Long serviceRequestId,
            @RequestBody DeliveryAdjustmentRequest request,
            Authentication authentication) {
        DeliveryAdjustmentResponse response = adjustmentService.requestAdjustment(
                serviceRequestId, request, authentication.getName());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/service-request/{serviceRequestId}")
    @Operation(summary = "Listar ajustes da solicitação", description = "Lista todos os ajustes de uma solicitação específica")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<List<DeliveryAdjustmentResponse>> getByServiceRequest(
            @PathVariable Long serviceRequestId,
            Authentication authentication) {
        return ResponseEntity.ok(
                adjustmentService.getAdjustmentsByServiceRequest(serviceRequestId, authentication.getName()));
    }

    @GetMapping("/my-requests")
    @Operation(summary = "Listar meus pedidos de ajuste", description = "Lista todos os ajustes solicitados pelo cliente autenticado")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<List<DeliveryAdjustmentResponse>> getMyRequests(Authentication authentication) {
        return ResponseEntity.ok(adjustmentService.getAdjustmentsByContractor(authentication.getName()));
    }

    @GetMapping("/received")
    @Operation(summary = "Listar ajustes recebidos", description = "Lista todos os ajustes recebidos pelo freelancer autenticado")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<List<DeliveryAdjustmentResponse>> getReceived(Authentication authentication) {
        return ResponseEntity.ok(adjustmentService.getAdjustmentsByFreelancer(authentication.getName()));
    }

    @GetMapping("/{adjustmentId}")
    @Operation(summary = "Obter detalhes do ajuste", description = "Retorna os detalhes de um ajuste específico")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<DeliveryAdjustmentResponse> getById(@PathVariable Long adjustmentId,
            Authentication authentication) {
        return ResponseEntity.ok(adjustmentService.getById(adjustmentId, authentication.getName()));
    }

    @PatchMapping("/{adjustmentId}/status")
    @Operation(summary = "Atualizar status do ajuste", description = "Freelancer atualiza o status do ajuste (reconhecer, completar, rejeitar)")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<DeliveryAdjustmentResponse> updateStatus(
            @PathVariable Long adjustmentId,
            @RequestBody AdjustmentStatusUpdate update,
            Authentication authentication) {
        return ResponseEntity.ok(adjustmentService.updateStatus(adjustmentId, update, authentication.getName()));
    }

    @DeleteMapping("/{adjustmentId}")
    @Operation(summary = "Deletar ajuste", description = "Cliente deleta um pedido de ajuste pendente")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<Void> delete(
            @PathVariable Long adjustmentId,
            Authentication authentication) {
        adjustmentService.deleteAdjustment(adjustmentId, authentication.getName());
        return ResponseEntity.noContent().build();
    }
}
