package com.servija.servija.service;

import com.servija.servija.dto.AdjustmentStatusUpdate;
import com.servija.servija.dto.DeliveryAdjustmentRequest;
import com.servija.servija.dto.DeliveryAdjustmentResponse;
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
        }

        log.info("Ajuste {} criado com sucesso para solicitação {}", adjustment.getId(), serviceRequestId);

        return toResponse(adjustment);
    }

    @Transactional(readOnly = true)
    public List<DeliveryAdjustmentResponse> getAdjustmentsByServiceRequest(Long serviceRequestId) {
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
    public DeliveryAdjustmentResponse getById(Long adjustmentId) {
        DeliveryAdjustment adjustment = adjustmentRepository.findById(adjustmentId)
                .orElseThrow(() -> new RuntimeException("Ajuste não encontrado"));
        return toResponse(adjustment);
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
        historyService.record(
                adjustment.getServiceRequest(),
                freelancer,
                ServiceRequestHistory.OperationType.ADJUSTMENT_STATUS_UPDATED,
                "Status do ajuste atualizado para: " + update.getStatus()
        );

        // Notificar cliente via WebSocket
        notificationService.notifyServiceRequestUpdate(
                adjustment.getRequestedBy().getEmail(),
                adjustment.getServiceRequest().getId(),
                "ADJUSTMENT_STATUS_CHANGED",
                "Status do ajuste atualizado: " + update.getStatus(),
                freelancer.getName()
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

        adjustmentRepository.delete(adjustment);

        log.info("Ajuste {} deletado por {}", adjustmentId, userEmail);
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
