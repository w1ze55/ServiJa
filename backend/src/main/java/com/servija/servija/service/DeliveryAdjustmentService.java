package com.servija.servija.service;

import com.servija.servija.dto.AdjustmentStatusUpdate;
import com.servija.servija.dto.DeliveryAdjustmentRequest;
import com.servija.servija.dto.DeliveryAdjustmentResponse;
import com.servija.servija.exception.ResourceNotFoundException;
import com.servija.servija.model.DeliveryAdjustment;
import com.servija.servija.model.ServiceRequest;
import com.servija.servija.model.ServiceRequestHistory;
import com.servija.servija.model.User;
import com.servija.servija.repository.DeliveryAdjustmentRepository;
import com.servija.servija.repository.ServiceRequestRepository;
import com.servija.servija.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class DeliveryAdjustmentService {

    private final DeliveryAdjustmentRepository adjustmentRepository;
    private final ServiceRequestRepository serviceRequestRepository;
    private final UserRepository userRepository;
    private final ServiceRequestHistoryService historyService;
    private final WebSocketService notificationService;
    private final NotificationService persistedNotificationService;
    private final AuditLogger auditLogger;

    @Transactional
    public DeliveryAdjustmentResponse requestAdjustment(Long serviceRequestId, DeliveryAdjustmentRequest request, String contractorEmail) {
        log.info("Cliente {} solicitando ajustes para solicitação {}", contractorEmail, serviceRequestId);

        ServiceRequest serviceRequest = serviceRequestRepository.findById(serviceRequestId)
                .orElseThrow(() -> new RuntimeException("Solicitação de serviço não encontrada"));

        User contractor = userRepository.findByEmail(contractorEmail)
                .orElseThrow(() -> new RuntimeException("Cliente não encontrado"));

        // Verificar se o cliente é o dono da solicitação
        if (!serviceRequest.getContractor().getEmail().equals(contractorEmail)) {
            throw new RuntimeException("Você não tem permissão para solicitar ajustes nesta solicitação");
        }

        // Verificar se o status é DELIVERED
        if (serviceRequest.getStatus() != ServiceRequest.Status.DELIVERED) {
            throw new RuntimeException("Só é possível solicitar ajustes quando o serviço foi entregue");
        }

        // Verificar se já existe um ajuste para esta solicitação
        if (adjustmentRepository.existsByServiceRequestId(serviceRequestId)) {
            throw new RuntimeException("Já existe uma solicitação de ajuste para este serviço. Apenas uma é permitida.");
        }

        // Criar ajuste
        DeliveryAdjustment adjustment = DeliveryAdjustment.builder()
                .serviceRequest(serviceRequest)
                .requestedBy(contractor)
                .reason(request.getReason())
                .status(DeliveryAdjustment.Status.PENDING)
                .build();

        adjustment = adjustmentRepository.save(adjustment);

        // Registrar no histórico
        historyService.record(
                serviceRequest,
                contractor,
                ServiceRequestHistory.OperationType.ADJUSTMENTS_REQUESTED,
                "Ajustes solicitados"
        );

        // Notificar freelancer via WebSocket
        if (serviceRequest.getFreelancer() != null) {
            notificationService.notifyServiceRequestUpdate(
                    serviceRequest.getFreelancer().getEmail(),
                    serviceRequestId,
                    "ADJUSTMENTS_REQUESTED",
                    contractor.getName() + " solicitou ajustes na entrega",
                    contractor.getName()
            );
            persistedNotificationService.notifyAdjustmentsRequested(serviceRequest, contractor);
        }

        log.info("Ajuste {} criado com sucesso para solicitação {}", adjustment.getId(), serviceRequestId);

        return toResponse(adjustment);
    }

    @Transactional(readOnly = true)
    public List<DeliveryAdjustmentResponse> getAdjustmentsByServiceRequest(Long serviceRequestId, String userEmail) {
        ServiceRequest serviceRequest = serviceRequestRepository.findById(serviceRequestId)
                .orElseThrow(() -> new ResourceNotFoundException("Solicitação não encontrada"));
        assertParticipant(serviceRequest, userEmail);
        return adjustmentRepository.findByServiceRequestIdOrderByCreatedAtDesc(serviceRequestId)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<DeliveryAdjustmentResponse> getAdjustmentsByFreelancer(String freelancerEmail) {
        return adjustmentRepository.findByServiceRequestFreelancerEmailOrderByCreatedAtDesc(freelancerEmail)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<DeliveryAdjustmentResponse> getAdjustmentsByContractor(String contractorEmail) {
        return adjustmentRepository.findByRequestedByEmailOrderByCreatedAtDesc(contractorEmail)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public DeliveryAdjustmentResponse getById(Long adjustmentId, String userEmail) {
        DeliveryAdjustment adjustment = adjustmentRepository.findById(adjustmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Ajuste não encontrado"));
        assertParticipant(adjustment.getServiceRequest(), userEmail);
        return toResponse(adjustment);
    }

    // M3: leitura de ajustes exige que o solicitante seja parte da solicitação.
    private void assertParticipant(ServiceRequest serviceRequest, String email) {
        boolean isContractor = serviceRequest.getContractor() != null
                && serviceRequest.getContractor().getEmail().equals(email);
        boolean isFreelancer = serviceRequest.getFreelancer() != null
                && serviceRequest.getFreelancer().getEmail().equals(email);
        if (!isContractor && !isFreelancer) {
            throw new ResourceNotFoundException("Ajuste não encontrado");
        }
    }

    @Transactional
    public DeliveryAdjustmentResponse updateStatus(Long adjustmentId, AdjustmentStatusUpdate update, String freelancerEmail) {
        log.info("Freelancer {} atualizando status do ajuste {}", freelancerEmail, adjustmentId);

        DeliveryAdjustment adjustment = adjustmentRepository.findById(adjustmentId)
                .orElseThrow(() -> new RuntimeException("Ajuste não encontrado"));

        // Verificar se o freelancer é o responsável
        if (!adjustment.getServiceRequest().getFreelancer().getEmail().equals(freelancerEmail)) {
            throw new RuntimeException("Você não tem permissão para atualizar este ajuste");
        }

        // Atualizar status
        adjustment.setStatus(DeliveryAdjustment.Status.valueOf(update.getStatus()));
        
        if (update.getResponse() != null && !update.getResponse().isBlank()) {
            adjustment.setFreelancerResponse(update.getResponse());
            adjustment.setRespondedAt(LocalDateTime.now());
        }

        adjustment = adjustmentRepository.save(adjustment);

        // Registrar no histórico
        User freelancer = adjustment.getServiceRequest().getFreelancer();
        String statusLabel = adjustment.getStatus().getLabel();
        historyService.record(
                adjustment.getServiceRequest(),
                freelancer,
                ServiceRequestHistory.OperationType.ADJUSTMENT_STATUS_UPDATED,
                statusLabel
        );

        // Notificar cliente via WebSocket
        notificationService.notifyServiceRequestUpdate(
                adjustment.getRequestedBy().getEmail(),
                adjustment.getServiceRequest().getId(),
                "ADJUSTMENT_STATUS_CHANGED",
                "Status do ajuste atualizado: " + statusLabel,
                freelancer.getName()
        );
        persistedNotificationService.notifyStatusUpdated(
                adjustment.getServiceRequest(),
                adjustment.getRequestedBy(),
                freelancer,
                "Status do ajuste atualizado: " + statusLabel,
                adjustment.getStatus().name(),
                "cliente-project-detail"
        );

        log.info("Status do ajuste {} atualizado para {}", adjustmentId, update.getStatus());

        return toResponse(adjustment);
    }

    @Transactional
    public void deleteAdjustment(Long adjustmentId, String userEmail) {
        DeliveryAdjustment adjustment = adjustmentRepository.findById(adjustmentId)
                .orElseThrow(() -> new RuntimeException("Ajuste não encontrado"));

        // Apenas o criador pode deletar (e apenas se estiver PENDING)
        if (!adjustment.getRequestedBy().getEmail().equals(userEmail)) {
            throw new RuntimeException("Você não tem permissão para deletar este ajuste");
        }

        if (adjustment.getStatus() != DeliveryAdjustment.Status.PENDING) {
            throw new RuntimeException("Só é possível deletar ajustes pendentes");
        }

        DeliveryAdjustmentResponse deletedAdjustment = toResponse(adjustment);

        // Notificar freelancer via WebSocket antes de deletar
        if (adjustment.getServiceRequest().getFreelancer() != null) {
            notificationService.notifyServiceRequestUpdate(
                    adjustment.getServiceRequest().getFreelancer().getEmail(),
                    adjustment.getServiceRequest().getId(),
                    "ADJUSTMENT_DELETED",
                    "O cliente removeu a solicitação de ajuste",
                    adjustment.getRequestedBy().getName());
        }

        adjustmentRepository.delete(adjustment);

        log.info("Ajuste {} deletado por {}", adjustmentId, userEmail);

        auditLogger.recordSemantic(
                "ADJUSTMENT_DELETED",
                "ADJUSTMENT",
                String.valueOf(adjustmentId),
                "Ajuste " + adjustmentId,
                com.servija.servija.model.AuditLog.Result.SUCCESS,
                null,
                deletedAdjustment,
                null,
                null);
    }

    private DeliveryAdjustmentResponse toResponse(DeliveryAdjustment adjustment) {
        return DeliveryAdjustmentResponse.builder()
                .id(adjustment.getId())
                .serviceRequestId(adjustment.getServiceRequest().getId())
                .requestedByName(adjustment.getRequestedBy().getName())
                .requestedByEmail(adjustment.getRequestedBy().getEmail())
                .reason(adjustment.getReason())
                .status(adjustment.getStatus().name())
                .freelancerResponse(adjustment.getFreelancerResponse())
                .respondedAt(adjustment.getRespondedAt())
                .createdAt(adjustment.getCreatedAt())
                .build();
    }
}
