package com.servija.servija.service;

import com.servija.servija.dto.ServiceRequestHistoryResponse;
import com.servija.servija.model.DeliveryAdjustment;
import com.servija.servija.model.ServiceRequest;
import com.servija.servija.model.ServiceRequestHistory;
import com.servija.servija.model.ServiceRequestHistory.OperationType;
import com.servija.servija.model.User;
import com.servija.servija.repository.ServiceRequestHistoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class ServiceRequestHistoryService {

        private final ServiceRequestHistoryRepository historyRepository;

        private static final ZoneId BRAZIL_ZONE = ZoneId.of("America/Sao_Paulo");
        private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
        private static final String ADJUSTMENT_STATUS_PREFIX = "Status do ajuste atualizado para:";

        @Transactional
        public void record(ServiceRequest serviceRequest, User user, OperationType operationType) {
                record(serviceRequest, user, operationType, null);
        }

        @Transactional
        public void record(ServiceRequest serviceRequest, User user, OperationType operationType, String details) {
                ServiceRequestHistory history = ServiceRequestHistory.builder()
                                .serviceRequest(serviceRequest)
                                .user(user)
                                .operationType(operationType)
                                .details(details)
                                .build();

                historyRepository.save(history);
                log.info("History recorded: {} by user {} on request {}",
                                operationType, user.getId(), serviceRequest.getId());
        }

        public List<ServiceRequestHistoryResponse> getHistory(Long serviceRequestId) {
                return historyRepository.findByServiceRequestIdOrderByCreatedAtDesc(serviceRequestId)
                                .stream()
                                .map(this::toResponse)
                                .toList();
        }

        private ServiceRequestHistoryResponse toResponse(ServiceRequestHistory history) {
                ZonedDateTime brazilTime = history.getCreatedAt()
                                .atZone(ZoneOffset.UTC)
                                .withZoneSameInstant(BRAZIL_ZONE);

                String details = history.getDetails();
                if (details == null) {
                        ServiceRequest request = history.getServiceRequest();
                        if (history.getOperationType() == OperationType.REQUEST_REJECTED) {
                                details = request.getRejectReason() != null ? "Motivo: " + request.getRejectReason()
                                                : null;
                        } else if (history.getOperationType() == OperationType.CANCELLED) {
                                details = request.getCancelReason() != null ? "Motivo: " + request.getCancelReason()
                                                : null;
                        }
                }

                if (history.getOperationType() == OperationType.ADJUSTMENT_STATUS_UPDATED) {
                        details = translateAdjustmentStatusDetails(details);
                }

                return ServiceRequestHistoryResponse.builder()
                                .id(history.getId())
                                .operationType(history.getOperationType().getLabel())
                                .operationKey(history.getOperationType().name())
                                .userName(history.getUser().getName())
                                .formattedDateTime(brazilTime.format(FORMATTER))
                                .details(details)
                                .build();
        }

        private String translateAdjustmentStatusDetails(String details) {
                if (details == null || details.isBlank()) {
                        return details;
                }

                String rawStatus = details.startsWith(ADJUSTMENT_STATUS_PREFIX)
                                ? details.substring(ADJUSTMENT_STATUS_PREFIX.length()).trim()
                                : details.trim();
                if (rawStatus.isEmpty()) {
                        return details;
                }

                return DeliveryAdjustment.Status.getLabel(rawStatus);
        }
}
