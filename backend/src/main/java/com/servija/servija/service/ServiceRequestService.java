package com.servija.servija.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.servija.servija.dto.CounterProposalDTO;
import com.servija.servija.dto.FreelancerResponseDTO;
import com.servija.servija.dto.ServiceRequestDTO;
import com.servija.servija.dto.ServiceRequestResponse;
import com.servija.servija.exception.ResourceNotFoundException;
import com.servija.servija.model.ServiceRequest;
import com.servija.servija.model.ServiceRequest.Status;
import com.servija.servija.model.ServiceRequestHistory.OperationType;
import com.servija.servija.repository.ServiceRepository;
import com.servija.servija.model.User;
import com.servija.servija.repository.ServiceRequestRepository;
import com.servija.servija.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class ServiceRequestService {

    private final ServiceRequestRepository serviceRequestRepository;
    private final UserRepository userRepository;
    private final ServiceRepository serviceRepository;
    private final FileStorageService fileStorageService;
    private final ServiceRequestHistoryService historyService;
    private final WebSocketService webSocketService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Transactional
    public ServiceRequestResponse create(String contractorEmail, ServiceRequestDTO dto) {
        User contractor = userRepository.findByEmail(contractorEmail)
                .orElseThrow(() -> new RuntimeException("Contractor not found"));

        com.servija.servija.model.Service selectedService = null;
        User freelancer = null;

        if (dto.getServiceId() != null) {
            selectedService = serviceRepository.findById(dto.getServiceId())
                    .orElseThrow(() -> new RuntimeException("Service not found"));
            freelancer = selectedService.getFreelancer();
        } else if (dto.getFreelancerId() != null) {
            freelancer = userRepository.findById(dto.getFreelancerId())
                    .orElseThrow(() -> new RuntimeException("Freelancer not found"));
        }

        if (selectedService != null && dto.getFreelancerId() != null
                && !selectedService.getFreelancer().getId().equals(dto.getFreelancerId())) {
            throw new IllegalArgumentException("O serviço selecionado não pertence ao freelancer informado");
        }

        String normalizedAddress = StringUtils.hasText(dto.getAddress()) ? dto.getAddress().trim() : null;

        com.servija.servija.model.Service.WorkModel requestWorkModel = resolveServiceWorkModel(selectedService);
        if (requestWorkModel == com.servija.servija.model.Service.WorkModel.PRESENCIAL
                || requestWorkModel == com.servija.servija.model.Service.WorkModel.HIBRIDO) {
            if (!StringUtils.hasText(normalizedAddress)) {
                throw new IllegalArgumentException("Informe o local onde o serviço será realizado");
            }
        } else {
            normalizedAddress = null;
        }

        String attachmentsJson = null;
        if (dto.getAttachments() != null && !dto.getAttachments().isEmpty()) {
            try {
                attachmentsJson = objectMapper.writeValueAsString(dto.getAttachments());
            } catch (Exception e) {
                log.warn("Erro ao serializar attachments: {}", e.getMessage());
            }
        }

        ServiceRequest request = ServiceRequest.builder()
                .contractor(contractor)
                .freelancer(freelancer)
                .serviceName(dto.getServiceName())
                .category(dto.getCategory())
                .description(dto.getDescription())
                .budget(dto.getBudget())
                .deadline(dto.getDeadline())
                .priority(ServiceRequest.Priority.valueOf(dto.getPriority().toUpperCase()))
                .address(normalizedAddress)
                .desiredDate(dto.getDesiredDate() != null ? LocalDate.parse(dto.getDesiredDate()) : null)
                .attachments(attachmentsJson)
                .build();

        ServiceRequest saved = serviceRequestRepository.save(request);
        historyService.record(saved, contractor, OperationType.REQUEST_OPENED);

        // Notificar freelancer sobre nova solicitação
        if (freelancer != null) {
            webSocketService.notifyNewServiceRequest(
                    saved.getId(),
                    freelancer.getEmail(),
                    contractor.getName(),
                    dto.getServiceName());
        }

        log.info("Service request created: {} for freelancer: {}", saved.getId(),
                freelancer != null ? freelancer.getId() : "any");
        return toResponse(saved);
    }

    public List<ServiceRequestResponse> getByContractorEmail(String email) {
        User contractor = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Contractor not found"));

        return serviceRequestRepository.findByContractorIdOrderByCreatedAtDesc(contractor.getId())
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public List<ServiceRequestResponse> getByFreelancerEmail(String email) {
        User freelancer = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Freelancer not found"));

        return serviceRequestRepository.findByFreelancerIdOrderByCreatedAtDesc(freelancer.getId())
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public ServiceRequestResponse getById(Long id) {
        ServiceRequest request = serviceRequestRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Service request not found"));
        return toResponse(request);
    }

    @Transactional
    public ServiceRequestResponse updateStatus(Long id, String status, String reason, String userEmail) {
        ServiceRequest request = serviceRequestRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Service request not found"));

        User actionUser = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("User not found"));

        ServiceRequest.Status newStatus = ServiceRequest.Status.valueOf(status.toUpperCase());

        if (newStatus == Status.DELIVERED || newStatus == Status.COMPLETED) {
            throw new IllegalStateException(
                    "O status " + newStatus + " deve ser alterado através do endpoint de conclusão");
        }

        request.setStatus(newStatus);

        if (newStatus == ServiceRequest.Status.CANCELLED && reason != null) {
            request.setCancelReason(reason);
        }
        if (newStatus == ServiceRequest.Status.REJECTED && reason != null) {
            request.setRejectReason(reason);
        }

        ServiceRequest saved = serviceRequestRepository.save(request);

        if (newStatus == ServiceRequest.Status.REJECTED) {
            String details = reason != null ? "Motivo: " + reason : null;
            historyService.record(saved, actionUser, OperationType.REQUEST_REJECTED, details);
        } else if (newStatus == ServiceRequest.Status.ACCEPTED) {
            historyService.record(saved, actionUser, OperationType.BUDGET_ACCEPTED);
        } else if (newStatus == ServiceRequest.Status.NEGOTIATING) {
            historyService.record(saved, actionUser, OperationType.COUNTER_PROPOSAL_SENT);
        } else if (newStatus == ServiceRequest.Status.CANCELLED) {
            String details = reason != null ? "Motivo: " + reason : null;
            historyService.record(saved, actionUser, OperationType.CANCELLED, details);
        }

        return toResponse(saved);
    }

    @Transactional
    public ServiceRequestResponse respondToRequest(Long id, FreelancerResponseDTO dto, String freelancerEmail) {
        ServiceRequest request = serviceRequestRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Service request not found"));

        User freelancer = userRepository.findByEmail(freelancerEmail)
                .orElseThrow(() -> new RuntimeException("Freelancer not found"));

        request.setResponseValue(dto.getValue());
        request.setResponseDays(dto.getExecutionDays());
        request.setResponseScope(dto.getScope());
        request.setResponseNotes(dto.getNotes());
        request.setRespondedAt(LocalDateTime.now());
        request.setStatus(ServiceRequest.Status.NEGOTIATING);

        if (dto.getAttachments() != null && !dto.getAttachments().isEmpty()) {
            try {
                request.setResponseAttachments(objectMapper.writeValueAsString(dto.getAttachments()));
            } catch (Exception e) {
                throw new RuntimeException("Error serializing attachments", e);
            }
        }

        ServiceRequest saved = serviceRequestRepository.save(request);
        String details = String.format("Valor: R$ %.2f | Prazo: %d dias", dto.getValue(), dto.getExecutionDays());
        historyService.record(saved, freelancer, OperationType.BUDGET_SENT, details);

        // Notificar contratante sobre nova proposta
        webSocketService.notifyNewProposal(
                saved.getId(),
                request.getContractor().getEmail(),
                freelancer.getName(),
                String.format("R$ %.2f", dto.getValue()));

        return toResponse(saved);
    }

    @Transactional
    public ServiceRequestResponse counterProposal(Long id, CounterProposalDTO dto, String contractorEmail) {
        ServiceRequest request = serviceRequestRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Service request not found"));

        User contractor = userRepository.findByEmail(contractorEmail)
                .orElseThrow(() -> new RuntimeException("Contractor not found"));

        request.setCounterValue(dto.getValue());
        request.setCounterDays(dto.getExecutionDays());
        request.setCounterNotes(dto.getNotes());
        request.setStatus(ServiceRequest.Status.NEGOTIATING);

        ServiceRequest saved = serviceRequestRepository.save(request);
        String details = String.format("Valor proposto: R$ %.2f | Prazo: %d dias", dto.getValue(),
                dto.getExecutionDays());
        historyService.record(saved, contractor, OperationType.COUNTER_PROPOSAL_SENT, details);

        // Notificar freelancer sobre contra-proposta
        webSocketService.notifyCounterProposal(
                saved.getId(),
                request.getFreelancer().getEmail(),
                contractor.getName(),
                String.format("R$ %.2f", dto.getValue()));

        return toResponse(saved);
    }

    @Transactional
    public ServiceRequestResponse acceptCounterProposal(Long id, String freelancerEmail) {
        ServiceRequest request = serviceRequestRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Service request not found"));

        User freelancer = userRepository.findByEmail(freelancerEmail)
                .orElseThrow(() -> new RuntimeException("Freelancer not found"));

        if (request.getCounterValue() != null) {
            request.setResponseValue(request.getCounterValue());
            request.setResponseDays(request.getCounterDays());
            request.setResponseNotes(request.getCounterNotes());

            // Clear counter fields after accepting
            request.setCounterValue(null);
            request.setCounterDays(null);
            request.setCounterNotes(null);

            request.setStatus(ServiceRequest.Status.ACCEPTED);

            ServiceRequest saved = serviceRequestRepository.save(request);
            String details = String.format("Contra proposta aceita. Novo valor: R$ %.2f", request.getResponseValue());
            historyService.record(saved, freelancer, OperationType.COUNTER_PROPOSAL_ACCEPTED, details);

            // Notificar ambas as partes sobre aceitação
            webSocketService.notifyProposalAccepted(
                    saved.getId(),
                    freelancer.getEmail(),
                    request.getContractor().getEmail());

            return toResponse(saved);
        }

        throw new RuntimeException("No counter proposal to accept");
    }

    @Transactional
    public ServiceRequestResponse rejectCounterProposal(Long id, String reason, String freelancerEmail) {
        ServiceRequest request = serviceRequestRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Service request not found"));

        User freelancer = userRepository.findByEmail(freelancerEmail)
                .orElseThrow(() -> new RuntimeException("Freelancer not found"));

        // Clear counter fields
        request.setCounterValue(null);
        request.setCounterDays(null);
        request.setCounterNotes(null);

        // Status remains NEGOTIATING as we still have the original proposal
        request.setStatus(ServiceRequest.Status.NEGOTIATING);

        ServiceRequest saved = serviceRequestRepository.save(request);

        String details = "Contra proposta recusada. Mantendo termos originais.";
        if (reason != null && !reason.trim().isEmpty()) {
            details += " Motivo: " + reason;
        }
        historyService.record(saved, freelancer, OperationType.COUNTER_PROPOSAL_REJECTED, details);

        return toResponse(saved);
    }

    @Transactional
    public ServiceRequestResponse completeService(Long id, String userEmail) {
        ServiceRequest request = serviceRequestRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Solicitação não encontrada"));

        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado"));

        // Phase 1: Freelancer marks as Delivered
        if (request.getFreelancer() != null && request.getFreelancer().getId().equals(user.getId())) {
            if (request.getStatus() != Status.IN_PROGRESS) {
                throw new IllegalStateException("O serviço só pode ser entregue quando está em andamento");
            }

            request.setStatus(Status.DELIVERED);
            ServiceRequest saved = serviceRequestRepository.save(request);

            historyService.record(saved, user, OperationType.SERVICE_DELIVERED,
                    "Serviço marcado como entregue pelo profissional");

            // Notificar o cliente sobre a entrega
            webSocketService.notifyServiceRequestStatusChange(
                    saved.getId(), "DELIVERED",
                    request.getContractor().getEmail(), userEmail);

            return toResponse(saved);
        }

        // Phase 2: Client marks as Completed
        if (request.getContractor().getId().equals(user.getId())) {
            if (request.getStatus() != Status.DELIVERED) {
                throw new IllegalStateException("O serviço só pode ser concluído após a entrega pelo profissional");
            }

            request.setStatus(Status.COMPLETED);
            ServiceRequest saved = serviceRequestRepository.save(request);

            historyService.record(saved, user, OperationType.SERVICE_COMPLETED,
                    "Serviço marcado como concluído pelo cliente");

            log.info("Serviço concluído pelo cliente - ID: {}, Contratante: {}", id, userEmail);

            // Notificar o freelancer sobre a conclusão
            webSocketService.notifyServiceRequestStatusChange(
                    saved.getId(), "COMPLETED",
                    userEmail, request.getFreelancer().getEmail());

            return toResponse(saved);
        }

        throw new IllegalStateException("Usuário não autorizado a realizar esta ação");
    }

    @Transactional
    public void delete(Long id) {
        ServiceRequest request = serviceRequestRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Service request not found"));

        String attachments = request.getAttachments();
        serviceRequestRepository.delete(request);

        // Deletar anexos do S3 se existirem
        if (attachments != null && !attachments.isEmpty()) {
            // Supondo que anexos são armazenados como lista de URLs (ex: JSON ou CSV)
            // Analisando como possível JSON ou apenas uma URL
            String[] urls;
            if (attachments.startsWith("[")) {
                // Muito simplificado, o ideal seria usar ObjectMapper mas vamos ser genéricos
                urls = attachments.replace("[", "").replace("]", "").replace("\"", "").split(",");
            } else {
                urls = attachments.split(",");
            }

            for (String url : urls) {
                String trimmedUrl = url.trim();
                if (isS3Url(trimmedUrl)) {
                    try {
                        fileStorageService.deleteFile(trimmedUrl);
                    } catch (Exception e) {
                        log.warn("Falha ao deletar anexo de solicitação do S3: {}. Erro: {}", trimmedUrl,
                                e.getMessage());
                    }
                }
            }
        }
    }

    @Transactional
    public ServiceRequestResponse addFreelancerAttachments(Long id, List<String> urls, String freelancerEmail) {
        ServiceRequest request = serviceRequestRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Solicitação não encontrada"));

        User freelancer = userRepository.findByEmail(freelancerEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado"));

        if (request.getFreelancer() == null || !request.getFreelancer().getId().equals(freelancer.getId())) {
            throw new IllegalStateException("Apenas o freelancer da solicitação pode enviar arquivos");
        }

        List<String> existing = parseAttachments(request.getFreelancerAttachments());
        existing.addAll(urls);

        try {
            request.setFreelancerAttachments(objectMapper.writeValueAsString(existing));
        } catch (Exception e) {
            log.error("Erro ao serializar freelancer attachments", e);
            throw new RuntimeException("Erro ao salvar arquivos");
        }

        ServiceRequest saved = serviceRequestRepository.save(request);
        log.info("Freelancer attachments adicionados à solicitação {}: {} arquivos", id, urls.size());
        return toResponse(saved);
    }

    private List<String> parseAttachments(String json) {
        if (json == null || json.isEmpty())
            return new ArrayList<>();
        try {
            return objectMapper.readValue(json, new TypeReference<List<String>>() {
            });
        } catch (Exception e) {
            log.warn("Erro ao deserializar attachments: {}", e.getMessage());
            return new ArrayList<>();
        }
    }

    private boolean isS3Url(String url) {
        return url != null && url.contains(".s3.") && url.contains("amazonaws.com");
    }

    private com.servija.servija.model.Service.WorkModel resolveServiceWorkModel(
            com.servija.servija.model.Service service) {
        if (service == null) {
            return null;
        }

        return service.getWorkModel();
    }

    private ServiceRequestResponse toResponse(ServiceRequest request) {
        return ServiceRequestResponse.builder()
                .id(request.getId())
                .contractorId(request.getContractor().getId())
                .contractorName(request.getContractor().getName())
                .contractorAvatar(request.getContractor().getAvatarUrl())
                .freelancerId(request.getFreelancer() != null ? request.getFreelancer().getId() : null)
                .freelancerName(request.getFreelancer() != null ? request.getFreelancer().getName() : null)
                .freelancerAvatar(request.getFreelancer() != null ? request.getFreelancer().getAvatarUrl() : null)
                .serviceName(request.getServiceName())
                .category(request.getCategory())
                .description(request.getDescription())
                .budget(request.getBudget())
                .deadline(request.getDeadline())
                .priority(request.getPriority().name())
                .status(request.getStatus().name())
                .address(request.getAddress())
                .desiredDate(request.getDesiredDate())
                .responseValue(request.getResponseValue())
                .responseDays(request.getResponseDays())
                .responseScope(request.getResponseScope())
                .responseNotes(request.getResponseNotes())
                .respondedAt(request.getRespondedAt())
                .counterValue(request.getCounterValue())
                .counterDays(request.getCounterDays())
                .counterNotes(request.getCounterNotes())
                .cancelReason(request.getCancelReason())
                .rejectReason(request.getRejectReason())
                .attachments(parseAttachments(request.getAttachments()))
                .freelancerAttachments(parseAttachments(request.getFreelancerAttachments()))
                .responseAttachments(parseAttachments(request.getResponseAttachments()))
                .createdAt(request.getCreatedAt())
                .updatedAt(request.getUpdatedAt())
                .build();
    }
}
