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

        return toResponse(supportRequestRepository.save(supportRequest));
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
            throw new AccessDeniedException("Voce nao tem permissao para visualizar esta solicitacao");
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
        return applyAdminAction(id, adminEmail, SupportRequest.Status.APPROVED, responseMessage, false);
    }

    @Transactional
    public SupportRequestResponse reject(Long id, String adminEmail, String responseMessage) {
        return applyAdminAction(id, adminEmail, SupportRequest.Status.REJECTED, responseMessage, true);
    }

    @Transactional
    public SupportRequestResponse reply(Long id, String adminEmail, String responseMessage) {
        return applyAdminAction(id, adminEmail, SupportRequest.Status.ANSWERED, responseMessage, true);
    }

    private SupportRequestResponse applyAdminAction(
            Long id,
            String adminEmail,
            SupportRequest.Status status,
            String responseMessage,
            boolean messageRequired) {

        String normalizedMessage = normalizeResponseMessage(responseMessage);
        if (messageRequired && normalizedMessage == null) {
            throw new IllegalArgumentException("A mensagem de resposta e obrigatoria para esta acao");
        }

        User admin = getUserByEmail(adminEmail);
        if (admin.getUserType() != User.UserType.ADMIN) {
            throw new AccessDeniedException("Apenas administradores podem responder solicitacoes de suporte");
        }

        SupportRequest supportRequest = getSupportRequestById(id);
        supportRequest.setAdminUser(admin);
        supportRequest.setStatus(status);
        supportRequest.setAdminResponse(normalizedMessage);
        supportRequest.setAnsweredAt(LocalDateTime.now());

        return toResponse(supportRequestRepository.save(supportRequest));
    }

    private User getUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario nao encontrado"));
    }

    private SupportRequest getSupportRequestById(Long id) {
        return supportRequestRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Solicitacao de suporte nao encontrada"));
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
            log.warn("Nao foi possivel serializar anexos da solicitacao de suporte", ex);
            throw new IllegalArgumentException("Nao foi possivel processar os anexos enviados");
        }
    }

    private List<SupportAttachmentDto> parseAttachments(String attachmentsJson) {
        if (attachmentsJson == null || attachmentsJson.isBlank()) {
            return Collections.emptyList();
        }

        try {
            return objectMapper.readValue(attachmentsJson, SUPPORT_ATTACHMENT_LIST_TYPE);
        } catch (Exception ex) {
            log.warn("Nao foi possivel deserializar anexos da solicitacao de suporte", ex);
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
