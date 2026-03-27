package com.servija.servija.service;

import com.servija.servija.dto.NotificationStreamEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class WebSocketService {

    private final SimpMessagingTemplate messagingTemplate;

    /**
     * Notifica sobre atualização de status de pagamento
     */
    public void notifyPaymentStatusChange(Long paymentId, String status, String userEmail) {
        Map<String, Object> message = new HashMap<>();
        message.put("type", "PAYMENT_STATUS_CHANGED");
        message.put("paymentId", paymentId);
        message.put("status", status);
        message.put("timestamp", System.currentTimeMillis());
        
        // Enviar para tópico público baseado no email (mesmo padrão das solicitações)
        String destination = "/topic/payment/" + userEmail;
        messagingTemplate.convertAndSend(destination, (Object) message);
        
        log.info("Notificação WebSocket enviada para {}: pagamento {} - status {}", 
                userEmail, paymentId, status);
    }

    /**
     * Notifica sobre confirmação de pagamento PIX
     */
    public void notifyPixPaymentConfirmed(Long paymentId, String userEmail) {
        Map<String, Object> message = new HashMap<>();
        message.put("type", "PIX_CONFIRMED");
        message.put("paymentId", paymentId);
        message.put("message", "Pagamento PIX confirmado com sucesso!");
        message.put("timestamp", System.currentTimeMillis());
        
        // Enviar para tópico público baseado no email (mesmo padrão das solicitações)
        String destination = "/topic/payment/" + userEmail;
        messagingTemplate.convertAndSend(destination, (Object) message);
        
        log.info("Notificação PIX confirmado enviada para {}: pagamento {}", 
                userEmail, paymentId);
    }

    /**
     * Notifica sobre falha no pagamento
     */
    public void notifyPaymentFailed(Long paymentId, String userEmail, String reason) {
        Map<String, Object> message = new HashMap<>();
        message.put("type", "PAYMENT_FAILED");
        message.put("paymentId", paymentId);
        message.put("message", "Falha no pagamento: " + reason);
        message.put("reason", reason);
        message.put("timestamp", System.currentTimeMillis());
        
        // Enviar para tópico público baseado no email (mesmo padrão das solicitações)
        String destination = "/topic/payment/" + userEmail;
        messagingTemplate.convertAndSend(destination, (Object) message);
        
        log.warn("Notificação de falha no pagamento enviada para {}: pagamento {} - {}", 
                userEmail, paymentId, reason);
    }

    /**
     * Notifica sobre nova solicitação de serviço para o freelancer
     */
    public void notifyNewServiceRequest(Long serviceRequestId, String freelancerEmail, String contractorName, String serviceName) {
        Map<String, Object> message = new HashMap<>();
        message.put("type", "NEW_SERVICE_REQUEST");
        message.put("serviceRequestId", serviceRequestId);
        message.put("contractorName", contractorName);
        message.put("serviceName", serviceName);
        message.put("message", contractorName + " solicitou o serviço: " + serviceName);
        message.put("timestamp", System.currentTimeMillis());
        
        String destination = "/topic/service-request/" + freelancerEmail;
        messagingTemplate.convertAndSend(destination, (Object) message);
        
        log.info("Notificação de nova solicitação enviada para {}: solicitação {}", 
                freelancerEmail, serviceRequestId);
    }

    /**
     * Notifica sobre mudança de status da solicitação
     */
    public void notifyServiceRequestStatusChange(Long serviceRequestId, String status, String contractorEmail, String freelancerEmail) {
        Map<String, Object> message = new HashMap<>();
        message.put("type", "SERVICE_REQUEST_STATUS_CHANGED");
        message.put("serviceRequestId", serviceRequestId);
        message.put("status", status);
        message.put("timestamp", System.currentTimeMillis());
        
        // Notificar ambas as partes
        String destinationContractor = "/topic/service-request/" + contractorEmail;
        String destinationFreelancer = "/topic/service-request/" + freelancerEmail;
        messagingTemplate.convertAndSend(destinationContractor, (Object) message);
        messagingTemplate.convertAndSend(destinationFreelancer, (Object) message);
        
        log.info("Notificação de mudança de status enviada: solicitação {} - status {}", 
                serviceRequestId, status);
    }

    /**
     * Notifica sobre nova proposta do freelancer
     */
    public void notifyNewProposal(Long serviceRequestId, String contractorEmail, String freelancerName, String proposalAmount) {
        Map<String, Object> message = new HashMap<>();
        message.put("type", "NEW_PROPOSAL");
        message.put("serviceRequestId", serviceRequestId);
        message.put("freelancerName", freelancerName);
        message.put("proposalAmount", proposalAmount);
        message.put("message", freelancerName + " enviou uma proposta de " + proposalAmount);
        message.put("timestamp", System.currentTimeMillis());
        
        String destination = "/topic/service-request/" + contractorEmail;
        messagingTemplate.convertAndSend(destination, (Object) message);
        
        log.info("Notificação de nova proposta enviada para {}: solicitação {}", 
                contractorEmail, serviceRequestId);
    }

    /**
     * Notifica sobre contra-proposta do contratante
     */
    public void notifyCounterProposal(Long serviceRequestId, String freelancerEmail, String contractorName, String counterProposalAmount) {
        Map<String, Object> message = new HashMap<>();
        message.put("type", "COUNTER_PROPOSAL");
        message.put("serviceRequestId", serviceRequestId);
        message.put("contractorName", contractorName);
        message.put("counterProposalAmount", counterProposalAmount);
        message.put("message", contractorName + " fez uma contra-proposta de " + counterProposalAmount);
        message.put("timestamp", System.currentTimeMillis());
        
        String destination = "/topic/service-request/" + freelancerEmail;
        messagingTemplate.convertAndSend(destination, (Object) message);
        
        log.info("Notificação de contra-proposta enviada para {}: solicitação {}", 
                freelancerEmail, serviceRequestId);
    }

    /**
     * Notifica sobre rejeição de contra-proposta pelo freelancer
     */
    public void notifyCounterProposalRejected(Long serviceRequestId, String contractorEmail, String freelancerName, String reason) {
        Map<String, Object> message = new HashMap<>();
        message.put("type", "COUNTER_PROPOSAL_REJECTED");
        message.put("serviceRequestId", serviceRequestId);
        message.put("freelancerName", freelancerName);
        message.put("reason", reason);
        String messageText = freelancerName + " recusou sua contra-proposta";
        if (reason != null && !reason.trim().isEmpty()) {
            messageText += ". Motivo: " + reason;
        }
        message.put("message", messageText);
        message.put("timestamp", System.currentTimeMillis());
        
        String destination = "/topic/service-request/" + contractorEmail;
        messagingTemplate.convertAndSend(destination, (Object) message);
        
        log.info("Notificação de contra-proposta recusada enviada para {}: solicitação {}", 
                contractorEmail, serviceRequestId);
    }

    /**
     * Notifica sobre proposta aceita
     */
    public void notifyProposalAccepted(Long serviceRequestId, String freelancerEmail, String contractorEmail) {
        Map<String, Object> messageFreelancer = new HashMap<>();
        messageFreelancer.put("type", "PROPOSAL_ACCEPTED");
        messageFreelancer.put("serviceRequestId", serviceRequestId);
        messageFreelancer.put("message", "Sua proposta foi aceita!");
        messageFreelancer.put("timestamp", System.currentTimeMillis());
        
        Map<String, Object> messageContractor = new HashMap<>();
        messageContractor.put("type", "PROPOSAL_ACCEPTED");
        messageContractor.put("serviceRequestId", serviceRequestId);
        messageContractor.put("message", "Proposta aceita! Prossiga para o pagamento.");
        messageContractor.put("timestamp", System.currentTimeMillis());
        
        String destinationFreelancer = "/topic/service-request/" + freelancerEmail;
        String destinationContractor = "/topic/service-request/" + contractorEmail;
        messagingTemplate.convertAndSend(destinationFreelancer, (Object) messageFreelancer);
        messagingTemplate.convertAndSend(destinationContractor, (Object) messageContractor);
        
        log.info("Notificação de proposta aceita enviada: solicitação {}", serviceRequestId);
    }

    /**
     * Notifica sobre solicitação cancelada
     */
    public void notifyServiceRequestCancelled(Long serviceRequestId, String reason, String freelancerEmail, String contractorEmail) {
        Map<String, Object> message = new HashMap<>();
        message.put("type", "SERVICE_REQUEST_CANCELLED");
        message.put("serviceRequestId", serviceRequestId);
        message.put("reason", reason);
        message.put("message", "Solicitação cancelada");
        message.put("timestamp", System.currentTimeMillis());
        
        String destinationFreelancer = "/topic/service-request/" + freelancerEmail;
        String destinationContractor = "/topic/service-request/" + contractorEmail;
        messagingTemplate.convertAndSend(destinationFreelancer, (Object) message);
        messagingTemplate.convertAndSend(destinationContractor, (Object) message);
        
        log.info("Notificação de cancelamento enviada: solicitação {}", serviceRequestId);
    }

    /**
     * Notifica sobre nova avaliação recebida
     */
    public void notifyNewReview(Long serviceRequestId, Long reviewId, String reviewedEmail, String reviewerName, int rating) {
        Map<String, Object> message = new HashMap<>();
        message.put("type", "NEW_REVIEW");
        message.put("serviceRequestId", serviceRequestId);
        message.put("reviewId", reviewId);
        message.put("reviewerName", reviewerName);
        message.put("rating", rating);
        message.put("message", reviewerName + " enviou uma avaliação de " + rating + " estrelas");
        message.put("timestamp", System.currentTimeMillis());

        String destination = "/topic/service-request/" + reviewedEmail;
        messagingTemplate.convertAndSend(destination, (Object) message);

        log.info("Notificação de nova avaliação enviada para {}: solicitação {} - {} estrelas",
                reviewedEmail, serviceRequestId, rating);
    }

    /**
     * Notifica que o serviço foi concluído e a avaliação está pendente
     */
    public void notifyReviewPending(Long serviceRequestId, String contractorEmail, String freelancerEmail, String serviceName) {
        Map<String, Object> messageContractor = new HashMap<>();
        messageContractor.put("type", "SERVICE_COMPLETED_REVIEW_PENDING");
        messageContractor.put("serviceRequestId", serviceRequestId);
        messageContractor.put("serviceName", serviceName);
        messageContractor.put("message", "Serviço concluído! Avalie o freelancer.");
        messageContractor.put("timestamp", System.currentTimeMillis());

        Map<String, Object> messageFreelancer = new HashMap<>();
        messageFreelancer.put("type", "SERVICE_COMPLETED_REVIEW_PENDING");
        messageFreelancer.put("serviceRequestId", serviceRequestId);
        messageFreelancer.put("serviceName", serviceName);
        messageFreelancer.put("message", "Serviço concluído! Avalie o cliente.");
        messageFreelancer.put("timestamp", System.currentTimeMillis());

        String destinationContractor = "/topic/service-request/" + contractorEmail;
        String destinationFreelancer = "/topic/service-request/" + freelancerEmail;
        messagingTemplate.convertAndSend(destinationContractor, (Object) messageContractor);
        messagingTemplate.convertAndSend(destinationFreelancer, (Object) messageFreelancer);

        log.info("Notificação de avaliação pendente enviada: solicitação {}", serviceRequestId);
    }

    /**
     * Notifica sobre atualizações gerais de solicitação de serviço
     */
    public void notifyServiceRequestUpdate(String userEmail, Long serviceRequestId, String eventType, String message, String actorName) {
        Map<String, Object> notification = new HashMap<>();
        notification.put("type", eventType);
        notification.put("serviceRequestId", serviceRequestId);
        notification.put("message", message);
        notification.put("actorName", actorName);
        notification.put("timestamp", System.currentTimeMillis());
        
        String destination = "/topic/service-request/" + userEmail;
        messagingTemplate.convertAndSend(destination, (Object) notification);
        
        log.info("Notificação {} enviada para {}: solicitação {}", eventType, userEmail, serviceRequestId);
    }

    /**
     * Notifica sobre atualização de imagem de perfil do usuário.
     */
    public void notifyProfileImageUpdate(String userEmail, String avatarUrl) {
        Map<String, Object> message = new HashMap<>();
        message.put("type", "PROFILE_IMAGE_UPDATED");
        message.put("avatarUrl", avatarUrl);
        message.put("timestamp", System.currentTimeMillis());

        String destination = "/topic/profile-update/" + userEmail;
        messagingTemplate.convertAndSend(destination, (Object) message);

        log.info("Notificação de atualização de avatar enviada para {}", userEmail);
    }

    /**
     * Publica eventos do stream de notificações persistidas.
     */
    public void publishNotificationEvent(String userEmail, NotificationStreamEvent event) {
        String destination = "/topic/notifications/" + userEmail;
        messagingTemplate.convertAndSend(destination, event);

        log.info("Evento de notificação {} enviado para {}",
                event != null ? event.getEventType() : "UNKNOWN",
                userEmail);
    }
}
