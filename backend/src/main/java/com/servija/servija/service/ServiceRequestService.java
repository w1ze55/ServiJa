package com.servija.servija.service;

import com.servija.servija.dto.FreelancerResponseDTO;
import com.servija.servija.dto.ServiceRequestDTO;
import com.servija.servija.dto.ServiceRequestResponse;
import com.servija.servija.model.ServiceRequest;
import com.servija.servija.model.User;
import com.servija.servija.repository.ServiceRequestRepository;
import com.servija.servija.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class ServiceRequestService {

    private final ServiceRequestRepository serviceRequestRepository;
    private final UserRepository userRepository;
    private final FileStorageService fileStorageService;

    @Transactional
    public ServiceRequestResponse create(String contractorEmail, ServiceRequestDTO dto) {
        User contractor = userRepository.findByEmail(contractorEmail)
                .orElseThrow(() -> new RuntimeException("Contractor not found"));

        User freelancer = null;
        if (dto.getFreelancerId() != null) {
            freelancer = userRepository.findById(dto.getFreelancerId())
                    .orElseThrow(() -> new RuntimeException("Freelancer not found"));
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
                .address(dto.getAddress())
                .desiredDate(dto.getDesiredDate() != null ? LocalDate.parse(dto.getDesiredDate()) : null)
                .urgency(dto.getUrgency())
                .build();

        ServiceRequest saved = serviceRequestRepository.save(request);
        log.info("Service request created: {} for freelancer: {}", saved.getId(), freelancer != null ? freelancer.getId() : "any");
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
    public ServiceRequestResponse updateStatus(Long id, String status, String reason) {
        ServiceRequest request = serviceRequestRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Service request not found"));

        ServiceRequest.Status newStatus = ServiceRequest.Status.valueOf(status.toUpperCase());
        request.setStatus(newStatus);

        if (newStatus == ServiceRequest.Status.CANCELLED && reason != null) {
            request.setCancelReason(reason);
        }
        if (newStatus == ServiceRequest.Status.REJECTED && reason != null) {
            request.setRejectReason(reason);
        }

        return toResponse(serviceRequestRepository.save(request));
    }

    @Transactional
    public ServiceRequestResponse respondToRequest(Long id, FreelancerResponseDTO dto) {
        ServiceRequest request = serviceRequestRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Service request not found"));

        request.setResponseValue(dto.getValue());
        request.setResponseDays(dto.getExecutionDays());
        request.setResponseScope(dto.getScope());
        request.setResponseNotes(dto.getNotes());
        request.setRespondedAt(LocalDateTime.now());
        request.setStatus(ServiceRequest.Status.NEGOTIATING);

        return toResponse(serviceRequestRepository.save(request));
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

    private boolean isS3Url(String url) {
        return url != null && url.contains(".s3.") && url.contains("amazonaws.com");
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
                .urgency(request.getUrgency())
                .responseValue(request.getResponseValue())
                .responseDays(request.getResponseDays())
                .responseScope(request.getResponseScope())
                .responseNotes(request.getResponseNotes())
                .respondedAt(request.getRespondedAt())
                .cancelReason(request.getCancelReason())
                .rejectReason(request.getRejectReason())
                .createdAt(request.getCreatedAt())
                .updatedAt(request.getUpdatedAt())
                .build();
    }
}
