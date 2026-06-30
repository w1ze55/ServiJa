package com.servija.servija.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.servija.servija.dto.NotificationListResponse;
import com.servija.servija.dto.NotificationResponse;
import com.servija.servija.dto.NotificationStreamEvent;
import com.servija.servija.exception.ResourceNotFoundException;
import com.servija.servija.model.Notification;
import com.servija.servija.model.NotificationCategory;
import com.servija.servija.model.NotificationType;
import com.servija.servija.model.Payment;
import com.servija.servija.model.ServiceRequest;
import com.servija.servija.model.User;
import com.servija.servija.repository.NotificationRepository;
import com.servija.servija.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {

    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {};

    private static final String ROUTE_FREELANCER_PROPOSAL_DETAIL = "freelancer-proposal-detail";
    private static final String ROUTE_FREELANCER_PROJECT_DETAIL = "freelancer-project-detail";
    private static final String ROUTE_CLIENT_REQUEST_DETAIL = "cliente-request-detail";
    private static final String ROUTE_CLIENT_PROJECT_DETAIL = "cliente-project-detail";
    private static final String ROUTE_CLIENT_DELIVERY_APPROVAL = "cliente-delivery-approval";
    private static final String ROUTE_CHECKOUT = "checkout";

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;
    private final WebSocketService webSocketService;

    @Transactional(readOnly = true)
    public NotificationListResponse getNotifications(String userEmail, Integer limit, Boolean read, String category) {
        User user = getUserByEmail(userEmail);
        NotificationCategory parsedCategory = NotificationCategory.fromValue(category);

        List<NotificationResponse> items = notificationRepository.findByRecipientIdOrderByCreatedAtDesc(user.getId())
                .stream()
                .filter(notification -> read == null || Objects.equals(notification.getIsRead(), read))
                .filter(notification -> parsedCategory == null || notification.getCategory() == parsedCategory)
                .limit(resolveLimit(limit))
                .map(this::toResponse)
                .toList();

        return NotificationListResponse.builder()
                .items(items)
                .unreadCount(notificationRepository.countByRecipientIdAndIsReadFalse(user.getId()))
                .build();
    }

    @Transactional
    public NotificationResponse markAsRead(Long id, String userEmail) {
        User user = getUserByEmail(userEmail);
        Notification notification = notificationRepository.findByIdAndRecipientId(id, user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Notificação não encontrada"));

        if (!Boolean.TRUE.equals(notification.getIsRead())) {
            notification.setIsRead(true);
            notification.setReadAt(LocalDateTime.now());
            notification = notificationRepository.save(notification);
            publishEvent(user.getEmail(), "UPDATED", notification, notification.getId());
        }

        return toResponse(notification);
    }

    @Transactional
    public void markAllAsRead(String userEmail) {
        User user = getUserByEmail(userEmail);
        List<Notification> notifications = notificationRepository.findByRecipientIdOrderByCreatedAtDesc(user.getId());
        boolean changed = false;

        for (Notification notification : notifications) {
            if (Boolean.TRUE.equals(notification.getIsRead())) {
                continue;
            }

            notification.setIsRead(true);
            notification.setReadAt(LocalDateTime.now());
            changed = true;
        }

        if (changed) {
            notificationRepository.saveAll(notifications);
        }

        webSocketService.publishNotificationEvent(
                user.getEmail(),
                NotificationStreamEvent.builder()
                        .eventType("READ_ALL")
                        .unreadCount(0)
                        .build()
        );
    }

    @Transactional
    public void deleteNotification(Long id, String userEmail) {
        User user = getUserByEmail(userEmail);
        long deleted = notificationRepository.deleteByIdAndRecipientId(id, user.getId());
        if (deleted == 0) {
            throw new ResourceNotFoundException("Notificação não encontrada");
        }

        webSocketService.publishNotificationEvent(
                user.getEmail(),
                NotificationStreamEvent.builder()
                        .eventType("DELETED")
                        .notificationId(id)
                        .unreadCount(notificationRepository.countByRecipientIdAndIsReadFalse(user.getId()))
                        .build()
        );
    }

    @Transactional
    public void clearAll(String userEmail) {
        User user = getUserByEmail(userEmail);
        notificationRepository.deleteByRecipientId(user.getId());

        webSocketService.publishNotificationEvent(
                user.getEmail(),
                NotificationStreamEvent.builder()
                        .eventType("CLEARED")
                        .unreadCount(0)
                        .build()
        );
    }

    @Transactional
    public void notifyNewServiceRequest(ServiceRequest serviceRequest, User actor) {
        User recipient = serviceRequest.getFreelancer();
        if (recipient == null) {
            return;
        }

        createUniqueNotification(
                recipient,
                actor,
                NotificationType.NEW_SERVICE_REQUEST,
                NotificationCategory.PROJECTS,
                "Nova solicitação de serviço",
                actor.getName() + " solicitou o serviço " + serviceRequest.getServiceName() + ".",
                metadataForServiceRequest(serviceRequest, ROUTE_FREELANCER_PROPOSAL_DETAIL)
        );
    }

    @Transactional
    public void notifyNewProposal(ServiceRequest serviceRequest, User freelancer) {
        createUniqueNotification(
                serviceRequest.getContractor(),
                freelancer,
                NotificationType.NEW_PROPOSAL,
                NotificationCategory.PROJECTS,
                "Novo orçamento",
                freelancer.getName() + " enviou um orçamento para " + serviceRequest.getServiceName() + ".",
                metadataForServiceRequest(serviceRequest, ROUTE_CLIENT_REQUEST_DETAIL)
        );
    }

    @Transactional
    public void notifyRequestRejected(ServiceRequest serviceRequest, User freelancer, String reason) {
        String description = freelancer.getName() + " recusou sua solicitação.";
        if (StringUtils.hasText(reason)) {
            description += " Motivo: " + reason.trim();
        }

        createUniqueNotification(
                serviceRequest.getContractor(),
                freelancer,
                NotificationType.REQUEST_REJECTED,
                NotificationCategory.PROJECTS,
                "Solicitação recusada",
                description,
                metadataForServiceRequest(serviceRequest, ROUTE_CLIENT_REQUEST_DETAIL)
        );
    }

    @Transactional
    public void notifyProposalAccepted(ServiceRequest serviceRequest) {
        createUniqueNotification(
                serviceRequest.getFreelancer(),
                serviceRequest.getContractor(),
                NotificationType.PROPOSAL_ACCEPTED,
                NotificationCategory.PROJECTS,
                "Orçamento aceito",
                serviceRequest.getContractor().getName() + " aceitou seu orçamento.",
                metadataForServiceRequest(serviceRequest, ROUTE_FREELANCER_PROPOSAL_DETAIL)
        );

        createUniqueNotification(
                serviceRequest.getContractor(),
                serviceRequest.getFreelancer(),
                NotificationType.PAYMENT_REQUIRED,
                NotificationCategory.PAYMENTS,
                "Pagamento necessário",
                "Seu orçamento foi aceito. Realize o pagamento para iniciar o projeto.",
                metadataForServiceRequest(serviceRequest, ROUTE_CHECKOUT)
        );
    }

    @Transactional
    public void notifyProjectStarted(Payment payment) {
        if (payment == null
                || payment.getServiceRequest() == null
                || payment.getContractor() == null
                || payment.getFreelancer() == null) {
            return;
        }

        ServiceRequest serviceRequest = payment.getServiceRequest();
        Map<String, Object> paymentMetadata = metadataForPayment(serviceRequest, payment.getId(), ROUTE_CLIENT_PROJECT_DETAIL);
        Map<String, Object> projectMetadataForContractor = metadataForPayment(serviceRequest, payment.getId(), ROUTE_CLIENT_PROJECT_DETAIL);
        Map<String, Object> paymentMetadataForFreelancer = metadataForPayment(serviceRequest, payment.getId(), ROUTE_FREELANCER_PROJECT_DETAIL);
        Map<String, Object> projectMetadataForFreelancer = metadataForPayment(serviceRequest, payment.getId(), ROUTE_FREELANCER_PROJECT_DETAIL);

        createUniqueNotification(
                payment.getContractor(),
                payment.getFreelancer(),
                NotificationType.PAYMENT_APPROVED,
                NotificationCategory.PAYMENTS,
                "Pagamento aprovado",
                "Seu pagamento foi aprovado e o projeto já pode começar.",
                paymentMetadata
        );

        createUniqueNotification(
                payment.getFreelancer(),
                payment.getContractor(),
                NotificationType.PAYMENT_RECEIVED,
                NotificationCategory.PAYMENTS,
                "Pagamento recebido",
                "O pagamento do projeto foi confirmado e você já pode iniciar o trabalho.",
                paymentMetadataForFreelancer
        );

        createUniqueNotification(
                payment.getContractor(),
                payment.getFreelancer(),
                NotificationType.PROJECT_STARTED,
                NotificationCategory.PROJECTS,
                "Projeto iniciado",
                "Pagamento confirmado. " + payment.getFreelancer().getName() + " já pode iniciar o projeto.",
                projectMetadataForContractor
        );

        createUniqueNotification(
                payment.getFreelancer(),
                payment.getContractor(),
                NotificationType.PROJECT_STARTED,
                NotificationCategory.PROJECTS,
                "Projeto iniciado",
                "Pagamento confirmado. Você já pode iniciar o projeto.",
                projectMetadataForFreelancer
        );
    }

    @Transactional
    public void notifyProjectCompleted(ServiceRequest serviceRequest, User freelancer) {
        Map<String, Object> metadata = metadataForServiceRequest(serviceRequest, ROUTE_CLIENT_DELIVERY_APPROVAL);
        metadata.put("status", ServiceRequest.Status.DELIVERED.name());

        createNotification(
                serviceRequest.getContractor(),
                freelancer,
                NotificationType.PROJECT_COMPLETED,
                NotificationCategory.PROJECTS,
                "Projeto concluído",
                "O freelancer marcou a entrega e aguarda sua aprovação.",
                metadata
        );
    }

    @Transactional
    public void notifyDeliveryApproved(ServiceRequest serviceRequest, User contractor) {
        Map<String, Object> metadata = metadataForServiceRequest(serviceRequest, ROUTE_FREELANCER_PROJECT_DETAIL);
        metadata.put("status", ServiceRequest.Status.COMPLETED.name());

        createUniqueNotification(
                serviceRequest.getFreelancer(),
                contractor,
                NotificationType.DELIVERY_APPROVED,
                NotificationCategory.PROJECTS,
                "Entrega aprovada",
                contractor.getName() + " aprovou a entrega do projeto.",
                metadata
        );
    }

    @Transactional
    public void notifyAdjustmentsRequested(ServiceRequest serviceRequest, User contractor) {
        createNotification(
                serviceRequest.getFreelancer(),
                contractor,
                NotificationType.ADJUSTMENTS_REQUESTED,
                NotificationCategory.PROJECTS,
                "Ajustes solicitados",
                contractor.getName() + " solicitou ajustes na entrega.",
                metadataForServiceRequest(serviceRequest, ROUTE_FREELANCER_PROJECT_DETAIL)
        );
    }

    @Transactional
    public void notifyStatusUpdated(ServiceRequest serviceRequest, User recipient, User actor, String description, String status, String routeHint) {
        Map<String, Object> metadata = metadataForServiceRequest(serviceRequest, routeHint);
        if (StringUtils.hasText(status)) {
            metadata.put("status", status.toUpperCase(Locale.ROOT));
        }

        createNotification(
                recipient,
                actor,
                NotificationType.STATUS_UPDATED,
                NotificationCategory.PROJECTS,
                "Status atualizado",
                description,
                metadata
        );
    }

    @Transactional
    public void notifyAttachmentAdded(ServiceRequest serviceRequest, User actor, int attachmentCount) {
        boolean sentByContractor = serviceRequest.getContractor() != null
                && actor.getId().equals(serviceRequest.getContractor().getId());
        User recipient = sentByContractor ? serviceRequest.getFreelancer() : serviceRequest.getContractor();
        if (recipient == null || recipient.getId().equals(actor.getId())) {
            return;
        }

        String routeHint = sentByContractor
                ? resolveFreelancerRouteHint(serviceRequest)
                : resolveContractorRouteHint(serviceRequest);

        Map<String, Object> metadata = metadataForServiceRequest(serviceRequest, routeHint);
        metadata.put("attachmentCount", attachmentCount);

        createNotification(
                recipient,
                actor,
                NotificationType.ATTACHMENT_ADDED,
                NotificationCategory.PROJECTS,
                "Novo anexo adicionado",
                actor.getName() + " adicionou " + attachmentCount + " anexo" + (attachmentCount > 1 ? "s" : "") + " ao projeto.",
                metadata
        );
    }

    private void createUniqueNotification(
            User recipient,
            User actor,
            NotificationType type,
            NotificationCategory category,
            String title,
            String description,
            Map<String, Object> metadata
    ) {
        createNotification(recipient, actor, type, category, title, description, metadata, true);
    }

    private void createNotification(
            User recipient,
            User actor,
            NotificationType type,
            NotificationCategory category,
            String title,
            String description,
            Map<String, Object> metadata
    ) {
        createNotification(recipient, actor, type, category, title, description, metadata, false);
    }

    private void createNotification(
            User recipient,
            User actor,
            NotificationType type,
            NotificationCategory category,
            String title,
            String description,
            Map<String, Object> metadata,
            boolean uniqueByTypeAndMetadata
    ) {
        if (recipient == null) {
            return;
        }

        String metadataJson = writeMetadata(metadata);
        if (uniqueByTypeAndMetadata
                && notificationRepository.existsByRecipientIdAndTypeAndMetadataJson(recipient.getId(), type, metadataJson)) {
            return;
        }

        Notification notification = Notification.builder()
                .recipient(recipient)
                .actor(actor)
                .type(type)
                .category(category)
                .title(title)
                .description(description)
                .metadataJson(metadataJson)
                .build();

        notification = notificationRepository.save(notification);
        publishEvent(recipient.getEmail(), "CREATED", notification, notification.getId());
    }

    private void publishEvent(String userEmail, String eventType, Notification notification, Long notificationId) {
        Long recipientId = notification != null && notification.getRecipient() != null
                ? notification.getRecipient().getId()
                : null;
        long unreadCount = recipientId != null
                ? notificationRepository.countByRecipientIdAndIsReadFalse(recipientId)
                : 0;
        webSocketService.publishNotificationEvent(
                userEmail,
                NotificationStreamEvent.builder()
                        .eventType(eventType)
                        .notification(notification != null ? toResponse(notification) : null)
                        .notificationId(notificationId)
                        .unreadCount(unreadCount)
                        .build()
        );
    }

    private User getUserByEmail(String userEmail) {
        return userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado"));
    }

    private long resolveLimit(Integer limit) {
        if (limit == null || limit <= 0) {
            return 100;
        }

        return Math.min(limit, 200);
    }

    private NotificationResponse toResponse(Notification notification) {
        return NotificationResponse.builder()
                .id(notification.getId())
                .type(notification.getType().name())
                .category(notification.getCategory().getValue())
                .title(notification.getTitle())
                .description(notification.getDescription())
                .isRead(notification.getIsRead())
                .createdAt(notification.getCreatedAt())
                .readAt(notification.getReadAt())
                .actorName(notification.getActor() != null ? notification.getActor().getName() : null)
                .actorAvatar(notification.getActor() != null ? notification.getActor().getAvatarUrl() : null)
                .metadata(readMetadata(notification.getMetadataJson()))
                .build();
    }

    private String writeMetadata(Map<String, Object> metadata) {
        try {
            return objectMapper.writeValueAsString(metadata != null ? metadata : Collections.emptyMap());
        } catch (Exception ex) {
            log.error("Erro ao serializar metadata da notificação", ex);
            return "{}";
        }
    }

    private Map<String, Object> readMetadata(String metadataJson) {
        if (!StringUtils.hasText(metadataJson)) {
            return Collections.emptyMap();
        }

        try {
            return objectMapper.readValue(metadataJson, MAP_TYPE);
        } catch (Exception ex) {
            log.warn("Erro ao desserializar metadata da notificação: {}", ex.getMessage());
            return Collections.emptyMap();
        }
    }

    private Map<String, Object> metadataForPayment(ServiceRequest serviceRequest, Long paymentId, String routeHint) {
        Map<String, Object> metadata = metadataForServiceRequest(serviceRequest, routeHint);
        metadata.put("paymentId", paymentId);
        metadata.put("status", ServiceRequest.Status.IN_PROGRESS.name());
        return metadata;
    }

    private Map<String, Object> metadataForServiceRequest(ServiceRequest serviceRequest, String routeHint) {
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("serviceRequestId", serviceRequest.getId());
        metadata.put("status", serviceRequest.getStatus().name());
        metadata.put("routeHint", routeHint);
        return metadata;
    }

    private String resolveContractorRouteHint(ServiceRequest serviceRequest) {
        if (serviceRequest.getStatus() == ServiceRequest.Status.DELIVERED) {
            return ROUTE_CLIENT_DELIVERY_APPROVAL;
        }

        if (serviceRequest.getStatus() == ServiceRequest.Status.IN_PROGRESS
                || serviceRequest.getStatus() == ServiceRequest.Status.COMPLETED) {
            return ROUTE_CLIENT_PROJECT_DETAIL;
        }

        return ROUTE_CLIENT_REQUEST_DETAIL;
    }

    private String resolveFreelancerRouteHint(ServiceRequest serviceRequest) {
        if (serviceRequest.getStatus() == ServiceRequest.Status.IN_PROGRESS
                || serviceRequest.getStatus() == ServiceRequest.Status.DELIVERED
                || serviceRequest.getStatus() == ServiceRequest.Status.COMPLETED) {
            return ROUTE_FREELANCER_PROJECT_DETAIL;
        }

        return ROUTE_FREELANCER_PROPOSAL_DETAIL;
    }
}
