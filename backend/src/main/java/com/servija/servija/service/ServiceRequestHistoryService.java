package com.servija.servija.service;

import com.servija.servija.dto.ServiceRequestHistoryResponse;
import com.servija.servija.model.ServiceRequest;
import com.servija.servija.model.ServiceRequestHistory;
import com.servija.servija.model.ServiceRequestHistory.OperationType;
import com.servija.servija.model.User;
import com.servija.servija.repository.ServiceRequestHistoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class ServiceRequestHistoryService {

        private final ServiceRequestHistoryRepository historyRepository;

        private static final ZoneId BRAZIL_ZONE = ZoneId.of("America/Sao_Paulo");
        private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

        @Transactional
        public void record(ServiceRequest serviceRequest, User user, OperationType operationType) {
                ServiceRequestHistory history = ServiceRequestHistory.builder()
                                .serviceRequest(serviceRequest)
                                .user(user)
                                .operationType(operationType)
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
                return ServiceRequestHistoryResponse.builder()
                                .id(history.getId())
                                .operationType(history.getOperationType().getLabel())
                                .operationKey(history.getOperationType().name())
                                .userName(history.getUser().getName())
                                .formattedDateTime(brazilTime.format(FORMATTER))
                                .build();
        }
}
