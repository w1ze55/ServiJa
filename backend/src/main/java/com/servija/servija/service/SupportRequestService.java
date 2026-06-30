package com.servija.servija.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.servija.servija.dto.SupportAttachmentDto;
import com.servija.servija.dto.SupportRequestCreateRequest;
import com.servija.servija.dto.SupportRequestResponse;
import com.servija.servija.exception.ResourceNotFoundException;
import com.servija.servija.model.SupportRequest;
import com.servija.servija.model.User;
import com.servija.servija.repository.SupportRequestRepository;
import com.servija.servija.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class SupportRequestService {

    private static final TypeReference<List<SupportAttachmentDto>> SUPPORT_ATTACHMENT_LIST_TYPE =
            new TypeReference<>() {};

    private final SupportRequestRepository supportRequestRepository;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;
    private final EmailService emailService;

    @Transactional
    public SupportRequestResponse create(String requesterEmail, SupportRequestCreateRequest request) {
        User requester = getUserByEmail(requesterEmail);

        SupportRequest supportRequest = SupportRequest.builder()
                .requester(requester)
                .category(request.getSupportCategory())
                .description(request.getDescription().trim())
                .attachments(serializeAttachments(request.getAttachments()))
                .status(SupportRequest.Status.PENDING)
                .build();

        SupportRequest savedRequest = supportRequestRepository.save(supportRequest);
        emailService.sendSupportRequestCreatedEmail(
                requester.getEmail(),
                requester.getName(),
                savedRequest.getId(),
                savedRequest.getCategory());

        return toResponse(savedRequest);
    }

    @Transactional(readOnly = true)
    public List<SupportRequestResponse> getMyRequests(String requesterEmail) {
        User requester = getUserByEmail(requesterEmail);
        return supportRequestRepository.findByRequesterIdOrderByCreatedAtDesc(requester.getId())
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public SupportRequestResponse getByIdForUser(Long id, String requesterEmail) {
        SupportRequest supportRequest = getSupportRequestById(id);
        User requester = getUserByEmail(requesterEmail);

        boolean isOwner = supportRequest.getRequester() != null
                && supportRequest.getRequester().getId().equals(requester.getId());
        boolean isAdmin = requester.getUserType() == User.UserType.ADMIN;

        if (!isOwner && !isAdmin) {
            throw new AccessDeniedException("Você não tem permissão para visualizar esta solicitação");
        }

        return toResponse(supportRequest);
    }

    @Transactional(readOnly = true)
    public List<SupportRequestResponse> listAll() {
        return supportRequestRepository.findAllByOrderByCreatedAtDesc()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public SupportRequestResponse getByIdForAdmin(Long id) {
        return toResponse(getSupportRequestById(id));
    }

    @Transactional
    public SupportRequestResponse approve(Long id, String adminEmail, String responseMessage) {
        return applyAdminAction(id, adminEmail, SupportRequest.Status.APPROVED, responseMessage);
    }

    @Transactional
    public SupportRequestResponse reject(Long id, String adminEmail, String responseMessage) {
        return applyAdminAction(id, adminEmail, SupportRequest.Status.REJECTED, responseMessage);
    }

    private SupportRequestResponse applyAdminAction(
            Long id,
            String adminEmail,
            SupportRequest.Status status,
            String responseMessage) {

        String normalizedMessage = normalizeResponseMessage(responseMessage);
        if (normalizedMessage == null) {
            throw new IllegalArgumentException("A mensagem de resposta é obrigatória para esta ação");
        }

        User admin = getUserByEmail(adminEmail);
        if (admin.getUserType() != User.UserType.ADMIN) {
            throw new AccessDeniedException("Apenas administradores podem tratar solicitações de suporte");
        }

        SupportRequest supportRequest = getSupportRequestById(id);
        if (supportRequest.getStatus() != SupportRequest.Status.PENDING) {
            throw new IllegalStateException(
                    "Esta solicitação já foi respondida e agora está disponível apenas para visualização. "
                            + "Se necessário, oriente o usuário a abrir uma nova solicitação.");
        }

        supportRequest.setAdminUser(admin);
        supportRequest.setStatus(status);
        supportRequest.setAdminResponse(normalizedMessage);
        supportRequest.setAnsweredAt(LocalDateTime.now());

        SupportRequest savedRequest = supportRequestRepository.save(supportRequest);
        User requester = savedRequest.getRequester();
        if (requester != null) {
            emailService.sendSupportRequestDecisionEmail(
                    requester.getEmail(),
                    requester.getName(),
                    savedRequest.getId(),
                    savedRequest.getStatus(),
                    normalizedMessage);
        }

        return toResponse(savedRequest);
    }

    private User getUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado"));
    }

    private SupportRequest getSupportRequestById(Long id) {
        return supportRequestRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Solicitação de suporte não encontrada"));
    }

    private String normalizeResponseMessage(String responseMessage) {
        if (responseMessage == null) {
            return null;
        }

        String trimmed = responseMessage.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String serializeAttachments(List<SupportAttachmentDto> attachments) {
        if (attachments == null || attachments.isEmpty()) {
            return null;
        }

        try {
            return objectMapper.writeValueAsString(attachments);
        } catch (Exception ex) {
            log.warn("Não foi possível serializar anexos da solicitação de suporte", ex);
            throw new IllegalArgumentException("Não foi possível processar os anexos enviados");
        }
    }

    private List<SupportAttachmentDto> parseAttachments(String attachmentsJson) {
        if (attachmentsJson == null || attachmentsJson.isBlank()) {
            return Collections.emptyList();
        }

        try {
            return objectMapper.readValue(attachmentsJson, SUPPORT_ATTACHMENT_LIST_TYPE);
        } catch (Exception ex) {
            log.warn("Não foi possível deserializar anexos da solicitação de suporte", ex);
            return Collections.emptyList();
        }
    }

    private SupportRequestResponse toResponse(SupportRequest supportRequest) {
        User requester = supportRequest.getRequester();
        User adminUser = supportRequest.getAdminUser();

        return SupportRequestResponse.builder()
                .id(supportRequest.getId())
                .userId(requester != null ? requester.getId() : null)
                .userName(requester != null ? requester.getName() : null)
                .userEmail(requester != null ? requester.getEmail() : null)
                .category(supportRequest.getCategory())
                .description(supportRequest.getDescription())
                .attachments(parseAttachments(supportRequest.getAttachments()))
                .status(supportRequest.getStatus())
                .adminResponse(supportRequest.getAdminResponse())
                .adminUserId(adminUser != null ? adminUser.getId() : null)
                .adminName(adminUser != null ? adminUser.getName() : null)
                .createdAt(supportRequest.getCreatedAt())
                .updatedAt(supportRequest.getUpdatedAt())
                .answeredAt(supportRequest.getAnsweredAt())
                .build();
    }
}
