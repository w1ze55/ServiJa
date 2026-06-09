package com.servija.servija.controller;

import com.servija.servija.dto.CounterProposalDTO;
import com.servija.servija.dto.FreelancerAttachmentsRequest;
import com.servija.servija.dto.FreelancerResponseDTO;
import com.servija.servija.dto.ProjectAttachmentsRequest;
import com.servija.servija.dto.ServiceRequestDTO;
import com.servija.servija.dto.ServiceRequestHistoryResponse;
import com.servija.servija.dto.ServiceRequestResponse;
import com.servija.servija.service.ServiceRequestHistoryService;
import com.servija.servija.service.ServiceRequestService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/service-requests")
@RequiredArgsConstructor
@Tag(name = "Solicitações de Serviço", description = "Endpoints para gerenciamento de solicitações de orçamento")
public class ServiceRequestController {

    private final ServiceRequestService serviceRequestService;
    private final ServiceRequestHistoryService historyService;

    @PostMapping
    @Operation(summary = "Criar solicitação de orçamento", description = "Cria uma nova solicitação de orçamento para um freelancer")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<ServiceRequestResponse> create(@Valid @RequestBody ServiceRequestDTO dto) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String email = authentication.getName();

        ServiceRequestResponse response = serviceRequestService.create(email, dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/my")
    @Operation(summary = "Listar minhas solicitações", description = "Lista todas as solicitações feitas pelo contratante autenticado")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<List<ServiceRequestResponse>> getMyRequests() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String email = authentication.getName();

        return ResponseEntity.ok(serviceRequestService.getByContractorEmail(email));
    }

    @GetMapping("/received")
    @Operation(summary = "Listar solicitações recebidas", description = "Lista todas as solicitações recebidas pelo freelancer autenticado")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<List<ServiceRequestResponse>> getReceivedRequests() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String email = authentication.getName();

        return ResponseEntity.ok(serviceRequestService.getByFreelancerEmail(email));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obter detalhes da solicitação", description = "Retorna os detalhes de uma solicitação específica")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<ServiceRequestResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(serviceRequestService.getById(id));
    }

    @PatchMapping("/{id}/status")
    @Operation(summary = "Atualizar status da solicitação", description = "Atualiza o status de uma solicitação (aceitar, rejeitar, cancelar)")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<ServiceRequestResponse> updateStatus(
            @PathVariable Long id,
            @RequestBody Map<String, String> body) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String email = authentication.getName();
        String status = body.get("status");
        String reason = body.get("reason");
        return ResponseEntity.ok(serviceRequestService.updateStatus(id, status, reason, email));
    }

    @PatchMapping("/{id}/respond")
    @Operation(summary = "Responder à solicitação", description = "Freelancer responde com proposta de valor e prazo")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<ServiceRequestResponse> respond(
            @PathVariable Long id,
            @Valid @RequestBody FreelancerResponseDTO dto) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String email = authentication.getName();
        return ResponseEntity.ok(serviceRequestService.respondToRequest(id, dto, email));
    }

    @PatchMapping("/{id}/milestones/{milestoneId}")
    @Operation(summary = "Atualizar status de marco", description = "Atualiza o status de um marco cadastrado na proposta do freelancer")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<ServiceRequestResponse> updateMilestoneStatus(
            @PathVariable Long id,
            @PathVariable String milestoneId,
            @RequestBody Map<String, String> body,
            Authentication authentication) {
        String status = body != null ? body.get("status") : null;
        return ResponseEntity.ok(serviceRequestService.updateMilestoneStatus(id, milestoneId, status, authentication.getName()));
    }

    @PatchMapping("/{id}/counter-proposal")
    @Operation(summary = "Enviar contra proposta", description = "Cliente envia uma contra proposta com novo valor e prazo")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<ServiceRequestResponse> counterProposal(
            @PathVariable Long id,
            @Valid @RequestBody CounterProposalDTO dto,
            Authentication authentication) {
        return ResponseEntity.ok(serviceRequestService.counterProposal(id, dto, authentication.getName()));
    }

    @PatchMapping("/{id}/accept-counter")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<ServiceRequestResponse> acceptCounter(
            @PathVariable Long id,
            Authentication authentication) {
        return ResponseEntity.ok(serviceRequestService.acceptCounterProposal(id, authentication.getName()));
    }

    @PatchMapping("/{id}/reject-counter")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<ServiceRequestResponse> rejectCounter(
            @PathVariable Long id,
            @RequestBody(required = false) Map<String, String> body,
            Authentication authentication) {
        String reason = (body != null) ? body.get("reason") : null;
        return ResponseEntity.ok(serviceRequestService.rejectCounterProposal(id, reason, authentication.getName()));
    }

    @PostMapping("/{id}/freelancer-attachments")
    @Operation(summary = "Adicionar arquivos do freelancer", description = "Freelancer envia URLs de arquivos para vincular à solicitação")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<ServiceRequestResponse> addFreelancerAttachments(
            @PathVariable Long id,
            @RequestBody FreelancerAttachmentsRequest body,
            Authentication authentication) {
        boolean hasDetailedAttachments = body != null && body.getAttachments() != null && !body.getAttachments().isEmpty();
        boolean hasLegacyUrls = body != null && body.getUrls() != null && !body.getUrls().isEmpty();
        if (!hasDetailedAttachments && !hasLegacyUrls) {
            return ResponseEntity.badRequest().build();
        }
        return ResponseEntity.ok(serviceRequestService.addFreelancerAttachments(id, body, authentication.getName()));
    }

    @PostMapping("/{id}/contractor-attachments")
    @Operation(summary = "Adicionar arquivos do cliente", description = "Contratante envia URLs de arquivos para vincular ao projeto")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<ServiceRequestResponse> addContractorAttachments(
            @PathVariable Long id,
            @RequestBody ProjectAttachmentsRequest body,
            Authentication authentication) {
        boolean hasUrls = body != null && body.getUrls() != null && !body.getUrls().isEmpty();
        if (!hasUrls) {
            return ResponseEntity.badRequest().build();
        }
        return ResponseEntity.ok(serviceRequestService.addContractorAttachments(id, body, authentication.getName()));
    }

    @GetMapping("/{id}/history")
    @Operation(summary = "Obter histórico de ações", description = "Retorna o histórico de ações realizadas em uma solicitação")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<List<ServiceRequestHistoryResponse>> getHistory(@PathVariable Long id) {
        return ResponseEntity.ok(historyService.getHistory(id));
    }

    @PatchMapping("/{id}/complete")
    @Operation(summary = "Marcar serviço como concluído", description = "Cliente marca o serviço como concluído após execução pelo freelancer")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<ServiceRequestResponse> completeService(@PathVariable Long id) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String email = authentication.getName();
        return ResponseEntity.ok(serviceRequestService.completeService(id, email));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Excluir solicitação", description = "Exclui uma solicitação de orçamento")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        serviceRequestService.delete(id, authentication.getName());
        return ResponseEntity.noContent().build();
    }
}
