package com.servija.servija.service;

import com.servija.servija.dto.FreelancerResponseDTO;
import com.servija.servija.dto.ServiceRequestDTO;
import com.servija.servija.dto.ServiceRequestResponse;
import com.servija.servija.model.ServiceRequest;
import com.servija.servija.model.User;
import com.servija.servija.repository.ServiceRequestRepository;
import com.servija.servija.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ServiceRequestService {

    private final ServiceRequestRepository serviceRequestRepository;
    private final UserRepository userRepository;

    @Transactional
    public ServiceRequestResponse create(Long contractorId, ServiceRequestDTO dto) {
        User contractor = userRepository.findById(contractorId)
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
        return toResponse(saved);
    }

    public List<ServiceRequestResponse> getByContractor(Long contractorId) {
        return serviceRequestRepository.findByContractorIdOrderByCreatedAtDesc(contractorId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public List<ServiceRequestResponse> getByFreelancer(Long freelancerId) {
        return serviceRequestRepository.findByFreelancerIdOrderByCreatedAtDesc(freelancerId)
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
        serviceRequestRepository.deleteById(id);
    }

    private ServiceRequestResponse toResponse(ServiceRequest request) {
        return ServiceRequestResponse.builder()
                .id(request.getId())
                .contractorId(request.getContractor().getId())
                .contractorName(request.getContractor().getName())
                .freelancerId(request.getFreelancer() != null ? request.getFreelancer().getId() : null)
                .freelancerName(request.getFreelancer() != null ? request.getFreelancer().getName() : null)
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
