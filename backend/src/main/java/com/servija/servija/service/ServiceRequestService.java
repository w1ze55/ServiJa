package com.servija.servija.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.servija.servija.dto.CounterProposalDTO;
import com.servija.servija.dto.DeliverySubmissionDTO;
import com.servija.servija.dto.FreelancerAttachmentDTO;
import com.servija.servija.dto.FreelancerAttachmentsRequest;
import com.servija.servija.dto.FreelancerResponseDTO;
import com.servija.servija.dto.ProjectAttachmentsRequest;
import com.servija.servija.dto.ResponseMilestoneDTO;
import com.servija.servija.dto.ServiceRequestDTO;
import com.servija.servija.dto.ServiceRequestResponse;
import com.servija.servija.exception.ResourceNotFoundException;
import com.servija.servija.model.DeliveryAdjustment;
import com.servija.servija.model.Payment;
import com.servija.servija.model.ServiceRequest;
import com.servija.servija.model.ServiceRequest.Status;
import com.servija.servija.model.ServiceRequestHistory.OperationType;
import com.servija.servija.repository.DeliveryAdjustmentRepository;
import com.servija.servija.repository.PaymentRepository;
import com.servija.servija.repository.ServiceRepository;
import com.servija.servija.model.User;
import com.servija.servija.repository.ServiceRequestRepository;
import com.servija.servija.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.math.BigDecimal;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.regex.Pattern;

@Service
@Slf4j
@RequiredArgsConstructor
public class ServiceRequestService {

    private static final Pattern LEGACY_SERVICE_DESCRIPTION_PREFIX = Pattern.compile(
            "^\\[Solicitação referente ao serviço: [^\\]]+\\]");

    private final ServiceRequestRepository serviceRequestRepository;
    private final DeliveryAdjustmentRepository deliveryAdjustmentRepository;
    private final PaymentRepository paymentRepository;
    private final UserRepository userRepository;
    private final ServiceRepository serviceRepository;
    private final FileStorageService fileStorageService;
    private final ServiceRequestHistoryService historyService;
    private final WebSocketService webSocketService;
    private final NotificationService notificationService;
    private final FreelancerWalletService walletService;
    private final PlatformWalletService platformWalletService;
    private final CategoryCatalogService categoryCatalogService;
    private final ChatService chatService;
    private final AuditLogger auditLogger;
    private final CurrentUserContextService currentUserContextService;
    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private static final BigDecimal MIN_NEGOTIATED_AMOUNT = new BigDecimal("40.00");
    private static final BigDecimal MAX_NEGOTIATED_AMOUNT = new BigDecimal("1000000.00");
    // Mantém o valor histórico (R$ 20,00) usado como placeholder do orçamento legado,
    // desacoplado do mínimo atual para não alterar registros antigos.
    private static final BigDecimal LEGACY_BUDGET_PLACEHOLDER = new BigDecimal("20.00");
    private static final List<String> VALID_MILESTONE_STATUSES = List.of("pending", "in_progress", "completed");

    @Transactional
    public ServiceRequestResponse create(String contractorEmail, ServiceRequestDTO dto) {
        User contractor = currentUserContextService.requireContractorMarketplaceAccess();

        com.servija.servija.model.Service selectedService = null;
        User freelancer = null;

        if (dto.getServiceId() != null) {
            selectedService = serviceRepository.findById(dto.getServiceId())
                    .orElseThrow(() -> new RuntimeException("Service not found"));
            freelancer = selectedService.getFreelancer() != null
                    ? selectedService.getFreelancer().getUser()
                    : null;
        } else if (dto.getFreelancerId() != null) {
            freelancer = userRepository.findById(dto.getFreelancerId())
                    .orElseThrow(() -> new RuntimeException("Freelancer not found"));
        }

        if (selectedService != null && dto.getFreelancerId() != null
                && !selectedService.getFreelancer().getId().equals(dto.getFreelancerId())) {
            throw new IllegalArgumentException("O serviço selecionado não pertence ao freelancer informado");
        }

        if (freelancer != null && contractor.getId().equals(freelancer.getId())) {
            throw new IllegalStateException("Você não pode enviar uma solicitação para si mesmo");
        }

        String normalizedAddress = StringUtils.hasText(dto.getAddress()) ? dto.getAddress().trim() : null;

        ServiceRequest.WorkModel requestWorkModel = resolveRequestWorkModel(dto, selectedService);
        if (requestWorkModel == ServiceRequest.WorkModel.PRESENCIAL
                || requestWorkModel == ServiceRequest.WorkModel.HIBRIDO) {
            if (!StringUtils.hasText(normalizedAddress)) {
                throw new IllegalArgumentException("Informe o local onde o serviço será realizado");
            }
        } else {
            normalizedAddress = null;
        }

        String normalizedDescription = normalizeRequestDescription(dto.getDescription());
        if (!StringUtils.hasText(normalizedDescription)) {
            throw new IllegalArgumentException("A descrição é obrigatória");
        }

        String attachmentsJson = null;
        if (dto.getAttachments() != null && !dto.getAttachments().isEmpty()) {
            try {
                attachmentsJson = objectMapper.writeValueAsString(dto.getAttachments());
            } catch (Exception e) {
                log.warn("Erro ao serializar attachments: {}", e.getMessage());
            }
        }

        // Validar orçamento (budget)
        if (dto.getBudget() != null) {
            if (dto.getBudget().compareTo(MIN_NEGOTIATED_AMOUNT) < 0) {
                throw new IllegalArgumentException("O orçamento mínimo é R$ 40,00");
            }
            if (dto.getBudget().compareTo(MAX_NEGOTIATED_AMOUNT) > 0) {
                throw new IllegalArgumentException("O orçamento máximo é R$ 1.000.000,00");
            }
        }

        String normalizedDeadline = dto.getDeadline() != null ? dto.getDeadline().trim() : null;
        boolean deadlineIsIsoDate = normalizedDeadline != null
                && normalizedDeadline.matches("^\\d{4}-\\d{2}-\\d{2}$");

        // Validar prazo estimado (deadline)
        if (normalizedDeadline != null && !normalizedDeadline.isEmpty() && !deadlineIsIsoDate) {
            // Se for prazo estimado (ex: "30 dias"), extrair número e validar
            String deadlineStr = normalizedDeadline.replaceAll("[^0-9]", "");
            if (!deadlineStr.isEmpty()) {
                try {
                    int days = Integer.parseInt(deadlineStr);
                    if (days < 1) {
                        throw new IllegalArgumentException("O prazo mínimo é de 1 dia");
                    }
                    if (days > 365) {
                        throw new IllegalArgumentException("O prazo máximo é de 365 dias");
                    }
                } catch (NumberFormatException e) {
                    // Se não for número, pode ser um texto livre - ignorar validação de dias
                }
            }
        }

        // Validar desiredDate - não pode ser no passado
        LocalDate parsedDesiredDate = null;
        String desiredDateValue = dto.getDesiredDate();
        if ((desiredDateValue == null || desiredDateValue.isBlank()) && deadlineIsIsoDate) {
            desiredDateValue = normalizedDeadline;
        }

        if (desiredDateValue != null && !desiredDateValue.isBlank()) {
            try {
                parsedDesiredDate = LocalDate.parse(desiredDateValue);
                if (parsedDesiredDate.isBefore(LocalDate.now())) {
                    throw new IllegalArgumentException("A data desejada não pode ser anterior à data atual");
                }
            } catch (java.time.format.DateTimeParseException e) {
                throw new IllegalArgumentException("Formato de data inválido. Use o formato AAAA-MM-DD");
            }
        }

        CategoryCatalogService.CategorySelection categorySelection = selectedService != null
                ? resolveSelectionFromService(selectedService)
                : categoryCatalogService.resolveSelection(
                        dto.getCategoryId(),
                        dto.getSubcategoryId(),
                        dto.getCategory(),
                        dto.getSubcategory(),
                        true,
                        true,
                        false,
                        false);

        String requestServiceName = StringUtils.hasText(dto.getServiceName())
                ? dto.getServiceName().trim()
                : selectedService != null ? selectedService.getTitle() : null;

        ServiceRequest request = ServiceRequest.builder()
                .contractor(contractor)
                .freelancer(freelancer)
                .service(selectedService)
                .serviceName(requestServiceName)
                .category(categorySelection.getCategoryLabel())
                .subcategory(categorySelection.getSubcategoryLabel())
                .serviceCategory(categorySelection.getCategory())
                .serviceSubcategory(categorySelection.getSubcategory())
                .description(normalizedDescription)
                .deadline(normalizedDeadline)
                .workModel(requestWorkModel)
                .address(normalizedAddress)
                .desiredDate(parsedDesiredDate)
                .attachments(attachmentsJson)
                .legacyLowValueAllowed(false)
                .build();

        ServiceRequest saved = saveNewRequest(request);
        historyService.record(saved, contractor, OperationType.REQUEST_OPENED);

        // Notificar freelancer sobre nova solicitação
        if (freelancer != null) {
            webSocketService.notifyNewServiceRequest(
                    saved.getId(),
                    freelancer.getEmail(),
                    contractor.getName(),
                    requestServiceName);
            notificationService.notifyNewServiceRequest(saved, contractor);
        }

        log.info("Service request created: {} for freelancer: {}", saved.getId(),
                freelancer != null ? freelancer.getId() : "any");
        return toResponse(saved);
    }

    private ServiceRequest saveNewRequest(ServiceRequest request) {
        if (!hasLegacyBudgetColumn()) {
            return serviceRequestRepository.save(request);
        }

        log.warn("Tabela service_requests ainda possui a coluna legada budget. Aplicando insert compatível.");
        Long generatedId = insertRequestWithLegacyBudget(request);
        return serviceRequestRepository.findById(generatedId)
                .orElseThrow(() -> new IllegalStateException("Não foi possível carregar a solicitação criada"));
    }

    private boolean hasLegacyBudgetColumn() {
        Integer count = jdbcTemplate.queryForObject(
                """
                        SELECT COUNT(*)
                        FROM information_schema.COLUMNS
                        WHERE TABLE_SCHEMA = DATABASE()
                          AND TABLE_NAME = 'service_requests'
                          AND COLUMN_NAME = 'budget'
                        """,
                Integer.class);
        return count != null && count > 0;
    }

    private Long insertRequestWithLegacyBudget(ServiceRequest request) {
        KeyHolder keyHolder = new GeneratedKeyHolder();

        jdbcTemplate.update(connection -> {
            PreparedStatement statement = connection.prepareStatement(
                    """
                            INSERT INTO service_requests (
                                contractor_id,
                                freelancer_id,
                                service_id,
                                service_name,
                                category,
                                subcategory,
                                category_id,
                                subcategory_id,
                                description,
                                budget,
                                deadline,
                                work_model,
                                status,
                                chat_enabled,
                                address,
                                desired_date,
                                attachments,
                                legacy_low_value_allowed,
                                created_at,
                                updated_at
                            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                            """,
                    Statement.RETURN_GENERATED_KEYS);

            statement.setLong(1, request.getContractor().getId());
            if (request.getFreelancer() != null) {
                statement.setLong(2, request.getFreelancer().getId());
            } else {
                statement.setNull(2, java.sql.Types.BIGINT);
            }
            if (request.getService() != null) {
                statement.setLong(3, request.getService().getId());
            } else {
                statement.setNull(3, java.sql.Types.BIGINT);
            }
            statement.setString(4, request.getServiceName());
            statement.setString(5, request.getCategory());
            statement.setString(6, request.getSubcategory());
            if (request.getServiceCategory() != null) {
                statement.setLong(7, request.getServiceCategory().getId());
            } else {
                statement.setNull(7, java.sql.Types.BIGINT);
            }
            if (request.getServiceSubcategory() != null) {
                statement.setLong(8, request.getServiceSubcategory().getId());
            } else {
                statement.setNull(8, java.sql.Types.BIGINT);
            }
            statement.setString(9, request.getDescription());
            statement.setBigDecimal(10, LEGACY_BUDGET_PLACEHOLDER);
            statement.setString(11, request.getDeadline());
            statement.setString(12, request.getWorkModel().name());
            statement.setString(13,
                    request.getStatus() != null ? request.getStatus().name() : ServiceRequest.Status.PENDING.name());
            statement.setBoolean(14, Boolean.TRUE.equals(request.getChatEnabled()));
            statement.setString(15, request.getAddress());
            if (request.getDesiredDate() != null) {
                statement.setDate(16, Date.valueOf(request.getDesiredDate()));
            } else {
                statement.setNull(16, java.sql.Types.DATE);
            }
            statement.setString(17, request.getAttachments());
            statement.setBoolean(18, Boolean.TRUE.equals(request.getLegacyLowValueAllowed()));
            return statement;
        }, keyHolder);

        Number key = keyHolder.getKey();
        if (key == null) {
            throw new IllegalStateException("Não foi possível obter o ID da solicitação criada");
        }
        return key.longValue();
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

    // M3: leitura exige que o solicitante seja parte da solicitação.
    public ServiceRequestResponse getById(Long id, String userEmail) {
        return toResponse(loadAsParticipant(id, userEmail));
    }

    public void assertParticipant(Long id, String userEmail) {
        loadAsParticipant(id, userEmail);
    }

    private ServiceRequest loadAsParticipant(Long id, String userEmail) {
        ServiceRequest request = serviceRequestRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Solicitação não encontrada"));
        boolean isContractor = request.getContractor() != null
                && request.getContractor().getEmail().equals(userEmail);
        boolean isFreelancer = request.getFreelancer() != null
                && request.getFreelancer().getEmail().equals(userEmail);
        if (!isContractor && !isFreelancer) {
            throw new ResourceNotFoundException("Solicitação não encontrada");
        }
        return request;
    }

    @Transactional
    public ServiceRequestResponse updateStatus(Long id, String status, String reason, String userEmail) {
        ServiceRequest request = serviceRequestRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Service request not found"));

        User actionUser = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // H1: apenas as partes (contratante ou freelancer) podem alterar o status.
        boolean isContractorParty = request.getContractor() != null
                && request.getContractor().getId().equals(actionUser.getId());
        boolean isFreelancerParty = request.getFreelancer() != null
                && request.getFreelancer().getId().equals(actionUser.getId());
        if (!isContractorParty && !isFreelancerParty) {
            throw new ResourceNotFoundException("Solicitação não encontrada");
        }

        ServiceRequest.Status newStatus = ServiceRequest.Status.valueOf(status.toUpperCase());

        if (newStatus == Status.DELIVERED || newStatus == Status.COMPLETED) {
            throw new IllegalStateException(
                    "O status " + newStatus + " deve ser alterado através do endpoint de conclusão");
        }

        if (newStatus == ServiceRequest.Status.ACCEPTED) {
            BigDecimal acceptedValue = resolveAcceptedValue(request);
            validateAcceptedAmount(acceptedValue, Boolean.TRUE.equals(request.getLegacyLowValueAllowed()));
            request.setLegacyLowValueAllowed(isBelowMinimumNegotiatedAmount(acceptedValue));
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
            if (request.getFreelancer() != null && request.getFreelancer().getId().equals(actionUser.getId())) {
                notificationService.notifyRequestRejected(saved, actionUser, reason);
                webSocketService.notifyServiceRequestStatusChange(
                        saved.getId(),
                        ServiceRequest.Status.REJECTED.name(),
                        request.getContractor().getEmail(),
                        request.getFreelancer().getEmail());
            }
        } else if (newStatus == ServiceRequest.Status.ACCEPTED) {
            historyService.record(saved, actionUser, OperationType.BUDGET_ACCEPTED);
            if (request.getFreelancer() != null) {
                webSocketService.notifyProposalAccepted(
                        saved.getId(),
                        request.getFreelancer().getEmail(),
                        request.getContractor().getEmail());
                notificationService.notifyProposalAccepted(saved);
            }
        } else if (newStatus == ServiceRequest.Status.NEGOTIATING) {
            historyService.record(saved, actionUser, OperationType.COUNTER_PROPOSAL_SENT);
        } else if (newStatus == ServiceRequest.Status.CANCELLED) {
            String details = reason != null ? "Motivo: " + reason : null;
            historyService.record(saved, actionUser, OperationType.CANCELLED, details);
            if (request.getFreelancer() != null) {
                webSocketService.notifyServiceRequestCancelled(
                        saved.getId(),
                        reason,
                        request.getFreelancer().getEmail(),
                        request.getContractor().getEmail());
            }
        }

        return toResponse(saved);
    }

    @Transactional
    public ServiceRequestResponse respondToRequest(Long id, FreelancerResponseDTO dto, String freelancerEmail) {
        ServiceRequest request = serviceRequestRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Service request not found"));

        User freelancer = userRepository.findByEmail(freelancerEmail)
                .orElseThrow(() -> new RuntimeException("Freelancer not found"));

        // H1: somente o freelancer designado pode responder. Se a solicitação está
        // aberta (sem freelancer), vincula quem está respondendo.
        if (request.getFreelancer() == null) {
            request.setFreelancer(freelancer);
        } else if (!request.getFreelancer().getId().equals(freelancer.getId())) {
            throw new ResourceNotFoundException("Solicitação não encontrada");
        }

        validateNewNegotiatedAmount(dto.getValue());

        request.setResponseValue(dto.getValue());
        request.setResponseDays(dto.getExecutionDays());
        request.setResponseScope(dto.getScope());
        request.setResponseNotes(dto.getNotes());
        request.setRespondedAt(LocalDateTime.now());
        request.setStatus(ServiceRequest.Status.NEGOTIATING);
        request.setLegacyLowValueAllowed(false);

        if (dto.getAttachments() != null && !dto.getAttachments().isEmpty()) {
            try {
                request.setResponseAttachments(objectMapper.writeValueAsString(dto.getAttachments()));
            } catch (Exception e) {
                throw new RuntimeException("Error serializing attachments", e);
            }
        } else {
            request.setResponseAttachments(null);
        }

        request.setResponseMilestones(serializeResponseMilestones(dto.getMilestones()));

        ServiceRequest saved = serviceRequestRepository.save(request);
        String details = String.format("Valor: R$ %.2f | Prazo: %d dias", dto.getValue(), dto.getExecutionDays());
        historyService.record(saved, freelancer, OperationType.BUDGET_SENT, details);

        // Notificar contratante sobre nova proposta
        webSocketService.notifyNewProposal(
                saved.getId(),
                request.getContractor().getEmail(),
                freelancer.getName(),
                String.format("R$ %.2f", dto.getValue()));
        notificationService.notifyNewProposal(saved, freelancer);

        return toResponse(saved);
    }

    @Transactional
    public ServiceRequestResponse updateMilestoneStatus(Long id, String milestoneId, String status, String freelancerEmail) {
        if (!StringUtils.hasText(status)) {
            throw new IllegalArgumentException("Informe o status do marco");
        }

        ServiceRequest request = serviceRequestRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Solicitação não encontrada"));

        User freelancer = userRepository.findByEmail(freelancerEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Freelancer não encontrado"));

        if (request.getFreelancer() == null || !request.getFreelancer().getId().equals(freelancer.getId())) {
            throw new IllegalStateException("Apenas o freelancer responsável pode atualizar marcos");
        }

        List<ResponseMilestoneDTO> milestones = parseResponseMilestones(request.getResponseMilestones());
        if (milestones.isEmpty()) {
            throw new ResourceNotFoundException("Não há marcos cadastrados para esta solicitação");
        }

        String normalizedMilestoneId = milestoneId != null ? milestoneId.trim() : "";
        String normalizedStatus = normalizeMilestoneStatus(status);
        boolean updated = false;

        for (ResponseMilestoneDTO milestone : milestones) {
            if (normalizedMilestoneId.equals(milestone.getId())) {
                milestone.setStatus(normalizedStatus);
                updated = true;
                break;
            }
        }

        if (!updated) {
            throw new ResourceNotFoundException("Marco não encontrado");
        }

        request.setResponseMilestones(serializeResponseMilestones(milestones));
        ServiceRequest saved = serviceRequestRepository.save(request);
        return toResponse(saved);
    }

    @Transactional
    public ServiceRequestResponse counterProposal(Long id, CounterProposalDTO dto, String contractorEmail) {
        ServiceRequest request = serviceRequestRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Service request not found"));

        User contractor = userRepository.findByEmail(contractorEmail)
                .orElseThrow(() -> new RuntimeException("Contractor not found"));

        // H1: apenas o contratante dono da solicitação pode contrapropor.
        if (request.getContractor() == null || !request.getContractor().getId().equals(contractor.getId())) {
            throw new ResourceNotFoundException("Solicitação não encontrada");
        }

        validateNewNegotiatedAmount(dto.getValue());

        request.setCounterValue(dto.getValue());
        request.setCounterDays(dto.getExecutionDays());
        request.setCounterNotes(dto.getNotes());
        request.setStatus(ServiceRequest.Status.NEGOTIATING);
        request.setLegacyLowValueAllowed(false);

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

        // H1: apenas o freelancer designado pode aceitar a contraproposta.
        if (request.getFreelancer() == null || !request.getFreelancer().getId().equals(freelancer.getId())) {
            throw new ResourceNotFoundException("Solicitação não encontrada");
        }

        if (request.getCounterValue() != null) {
            BigDecimal acceptedValue = request.getCounterValue();
            validateAcceptedAmount(acceptedValue, Boolean.TRUE.equals(request.getLegacyLowValueAllowed()));

            request.setResponseValue(acceptedValue);
            request.setResponseDays(request.getCounterDays());
            request.setResponseNotes(request.getCounterNotes());

            // Clear counter fields after accepting
            request.setCounterValue(null);
            request.setCounterDays(null);
            request.setCounterNotes(null);

            request.setStatus(ServiceRequest.Status.ACCEPTED);
            request.setLegacyLowValueAllowed(isBelowMinimumNegotiatedAmount(acceptedValue));

            ServiceRequest saved = serviceRequestRepository.save(request);
            String details = String.format("Contra proposta aceita. Novo valor: R$ %.2f", request.getResponseValue());
            historyService.record(saved, freelancer, OperationType.COUNTER_PROPOSAL_ACCEPTED, details);

            // Notificar ambas as partes sobre aceitação
            webSocketService.notifyProposalAccepted(
                    saved.getId(),
                    freelancer.getEmail(),
                    request.getContractor().getEmail());
            notificationService.notifyProposalAccepted(saved);

            return toResponse(saved);
        }

        throw new RuntimeException("No counter proposal to accept");
    }

    private void validateNewNegotiatedAmount(BigDecimal value) {
        if (value == null) {
            throw new IllegalArgumentException("O valor negociado é obrigatório");
        }

        if (value.compareTo(MIN_NEGOTIATED_AMOUNT) < 0) {
            throw new IllegalArgumentException("O valor mínimo é R$ 40,00");
        }

        if (value.compareTo(MAX_NEGOTIATED_AMOUNT) > 0) {
            throw new IllegalArgumentException("O valor máximo é R$ 1.000.000,00");
        }
    }

    private void validateAcceptedAmount(BigDecimal value, boolean legacyAllowed) {
        if (value == null) {
            throw new IllegalStateException("A solicitação não possui valor negociado para ser aceita");
        }

        if (!legacyAllowed && isBelowMinimumNegotiatedAmount(value)) {
            throw new IllegalStateException("O valor mínimo para novos orçamentos é R$ 40,00");
        }

        if (value.compareTo(MAX_NEGOTIATED_AMOUNT) > 0) {
            throw new IllegalStateException("O valor máximo é R$ 1.000.000,00");
        }
    }

    private BigDecimal resolveAcceptedValue(ServiceRequest request) {
        if (request.getResponseValue() != null) {
            return request.getResponseValue();
        }

        return request.getCounterValue();
    }

    private boolean isBelowMinimumNegotiatedAmount(BigDecimal value) {
        return value != null && value.compareTo(MIN_NEGOTIATED_AMOUNT) < 0;
    }

    @Transactional
    public ServiceRequestResponse rejectCounterProposal(Long id, String reason, String freelancerEmail) {
        ServiceRequest request = serviceRequestRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Service request not found"));

        User freelancer = userRepository.findByEmail(freelancerEmail)
                .orElseThrow(() -> new RuntimeException("Freelancer not found"));

        // H1: apenas o freelancer designado pode recusar a contraproposta.
        if (request.getFreelancer() == null || !request.getFreelancer().getId().equals(freelancer.getId())) {
            throw new ResourceNotFoundException("Solicitação não encontrada");
        }

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

        // Notificar contratante sobre rejeição da contra proposta
        webSocketService.notifyCounterProposalRejected(
                saved.getId(),
                request.getContractor().getEmail(),
                freelancer.getName(),
                reason);
        notificationService.notifyStatusUpdated(
                saved,
                request.getContractor(),
                freelancer,
                StringUtils.hasText(reason)
                        ? freelancer.getName() + " recusou sua contra-proposta. Motivo: " + reason.trim()
                        : freelancer.getName() + " recusou sua contra-proposta. O orçamento original segue ativo.",
                ServiceRequest.Status.NEGOTIATING.name(),
                "cliente-request-detail");

        return toResponse(saved);
    }

    @Transactional
    public ServiceRequestResponse completeService(Long id, DeliverySubmissionDTO delivery, String userEmail) {
        ServiceRequest request = serviceRequestRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Solicitação não encontrada"));

        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado"));

        // Phase 1: Freelancer marks as Delivered
        if (request.getFreelancer() != null && request.getFreelancer().getId().equals(user.getId())) {
            if (request.getStatus() != Status.IN_PROGRESS) {
                throw new IllegalStateException("O serviço só pode ser entregue quando está em andamento");
            }

            applyDeliverySubmission(request, delivery);

            request.setStatus(Status.DELIVERED);
            ServiceRequest saved = serviceRequestRepository.save(request);

            historyService.record(saved, user, OperationType.SERVICE_DELIVERED,
                    "Serviço marcado como entregue pelo profissional");

            // Notificar o cliente sobre a entrega
            webSocketService.notifyServiceRequestStatusChange(
                    saved.getId(), "DELIVERED",
                    request.getContractor().getEmail(), userEmail);
            notificationService.notifyProjectCompleted(saved, user);

            return toResponse(saved);
        }

        // Phase 2: Client marks as Completed
        if (request.getContractor().getId().equals(user.getId())) {
            if (request.getStatus() != Status.DELIVERED) {
                throw new IllegalStateException("O serviço só pode ser concluído após a entrega pelo profissional");
            }

            boolean hasActiveAdjustment = deliveryAdjustmentRepository.existsByServiceRequestIdAndStatusIn(
                    request.getId(),
                    java.util.List.of(DeliveryAdjustment.Status.PENDING, DeliveryAdjustment.Status.ACKNOWLEDGED));
            if (hasActiveAdjustment) {
                throw new IllegalStateException(
                        "Aguarde o profissional reenviar a entrega com os ajustes solicitados antes de aprovar");
            }

            request.setStatus(Status.COMPLETED);
            ServiceRequest saved = serviceRequestRepository.save(request);
            Payment payment = releaseFreelancerBalanceIfNeeded(saved);
            releasePlatformFeeIfNeeded(payment);

            chatService.lockConversationByServiceRequest(saved.getId());

            historyService.record(saved, user, OperationType.SERVICE_COMPLETED,
                    "Serviço marcado como concluído pelo cliente");

            log.info("Serviço concluído pelo cliente - ID: {}, Contratante: {}", id, userEmail);

            // Notificar o freelancer sobre a conclusão
            webSocketService.notifyServiceRequestStatusChange(
                    saved.getId(), "COMPLETED",
                    userEmail, request.getFreelancer().getEmail());
            notificationService.notifyDeliveryApproved(saved, user);

            // Notificar ambos que a avaliação está pendente
            webSocketService.notifyReviewPending(
                    saved.getId(),
                    userEmail,
                    request.getFreelancer().getEmail(),
                    saved.getServiceName());

            return toResponse(saved);
        }

        throw new IllegalStateException("Usuário não autorizado a realizar esta ação");
    }

    private void applyDeliverySubmission(ServiceRequest request, DeliverySubmissionDTO delivery) {
        if (delivery == null) {
            return;
        }

        if (!StringUtils.hasText(delivery.getDescription())) {
            throw new IllegalArgumentException("A descrição da entrega é obrigatória");
        }

        request.setDeliveryDescription(delivery.getDescription().trim());

        if (delivery.getAttachments() != null && !delivery.getAttachments().isEmpty()) {
            try {
                request.setDeliveryAttachments(objectMapper.writeValueAsString(delivery.getAttachments()));
            } catch (Exception e) {
                throw new RuntimeException("Error serializing delivery attachments", e);
            }
        } else {
            request.setDeliveryAttachments(null);
        }

        request.setDeliveredAt(LocalDateTime.now());
    }

    private Payment releaseFreelancerBalanceIfNeeded(ServiceRequest request) {
        List<Payment> payments = paymentRepository.findByServiceRequestId(request.getId());
        if (payments.isEmpty()) {
            log.warn("Solicitação {} concluída sem pagamento associado para liberar saldo", request.getId());
            return null;
        }

        Payment payment = payments.get(0);
        if (payment.getStatus() != Payment.PaymentStatus.COMPLETED) {
            log.warn(
                    "Solicitação {} concluída, mas pagamento {} ainda não está COMPLETED. Status atual: {}",
                    request.getId(),
                    payment.getId(),
                    payment.getStatus());
            return payment;
        }

        walletService.releasePendingBalance(payment);
        return payment;
    }

    private void releasePlatformFeeIfNeeded(Payment payment) {
        if (payment == null) {
            return;
        }

        platformWalletService.releasePlatformFeeIfNeeded(payment);
    }

    @Transactional
    public void delete(Long id, String userEmail) {
        ServiceRequest request = serviceRequestRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Service request not found"));

        User actionUser = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado"));

        // H2: apenas o contratante dono da solicitação pode excluí-la.
        if (request.getContractor() == null || !request.getContractor().getId().equals(actionUser.getId())) {
            throw new ResourceNotFoundException("Solicitação não encontrada");
        }

        String attachments = request.getAttachments();
        ServiceRequestResponse deletedRequest = toResponse(request);
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

        auditLogger.recordSemantic(
                "SERVICE_REQUEST_DELETED",
                "SERVICE_REQUEST",
                String.valueOf(id),
                deletedRequest.getServiceName(),
                com.servija.servija.model.AuditLog.Result.SUCCESS,
                null,
                deletedRequest,
                null,
                null);
    }

    @Transactional
    public ServiceRequestResponse addFreelancerAttachments(Long id, FreelancerAttachmentsRequest body, String freelancerEmail) {
        ServiceRequest request = serviceRequestRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Solicitação não encontrada"));

        User freelancer = userRepository.findByEmail(freelancerEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado"));

        if (request.getFreelancer() == null || !request.getFreelancer().getId().equals(freelancer.getId())) {
            throw new IllegalStateException("Apenas o freelancer da solicitação pode enviar arquivos");
        }

        List<FreelancerAttachmentDTO> attachmentsToAdd = resolveFreelancerAttachments(body);
        if (attachmentsToAdd.isEmpty()) {
            throw new IllegalArgumentException("Informe pelo menos um arquivo");
        }

        List<FreelancerAttachmentDTO> existing = parseFreelancerAttachmentDetails(request.getFreelancerAttachments());
        existing.addAll(attachmentsToAdd);

        try {
            request.setFreelancerAttachments(objectMapper.writeValueAsString(existing));
        } catch (Exception e) {
            log.error("Erro ao serializar freelancer attachments", e);
            throw new RuntimeException("Erro ao salvar arquivos");
        }

        ServiceRequest saved = serviceRequestRepository.save(request);
        notificationService.notifyAttachmentAdded(saved, freelancer, attachmentsToAdd.size());
        log.info("Freelancer attachments adicionados à solicitação {}: {} arquivos", id, attachmentsToAdd.size());
        return toResponse(saved);
    }

    @Transactional
    public ServiceRequestResponse addContractorAttachments(Long id, ProjectAttachmentsRequest body, String contractorEmail) {
        ServiceRequest request = serviceRequestRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Solicitação não encontrada"));

        User contractor = userRepository.findByEmail(contractorEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado"));

        if (request.getContractor() == null || !request.getContractor().getId().equals(contractor.getId())) {
            throw new IllegalStateException("Apenas o cliente da solicitação pode enviar arquivos");
        }

        List<String> attachmentsToAdd = resolveProjectAttachmentUrls(body);
        if (attachmentsToAdd.isEmpty()) {
            throw new IllegalArgumentException("Informe pelo menos um arquivo");
        }

        List<String> existing = parseAttachmentUrls(request.getAttachments());
        existing.addAll(attachmentsToAdd);

        try {
            request.setAttachments(objectMapper.writeValueAsString(existing));
        } catch (Exception e) {
            log.error("Erro ao serializar attachments do cliente", e);
            throw new RuntimeException("Erro ao salvar arquivos");
        }

        ServiceRequest saved = serviceRequestRepository.save(request);
        notificationService.notifyAttachmentAdded(saved, contractor, attachmentsToAdd.size());
        log.info("Contractor attachments adicionados à solicitação {}: {} arquivos", id, attachmentsToAdd.size());
        return toResponse(saved);
    }

    private List<String> parseAttachmentUrls(String json) {
        if (!StringUtils.hasText(json)) {
            return new ArrayList<>();
        }

        try {
            JsonNode root = objectMapper.readTree(json);
            if (!root.isArray()) {
                return new ArrayList<>();
            }

            List<String> urls = new ArrayList<>();
            for (JsonNode node : root) {
                if (node.isTextual() && StringUtils.hasText(node.asText())) {
                    urls.add(node.asText());
                } else if (node.isObject()) {
                    String url = node.path("url").asText(null);
                    if (StringUtils.hasText(url)) {
                        urls.add(url);
                    }
                }
            }
            return urls;
        } catch (Exception e) {
            log.warn("Erro ao deserializar attachments: {}", e.getMessage());
            return new ArrayList<>();
        }
    }

    private List<FreelancerAttachmentDTO> parseFreelancerAttachmentDetails(String json) {
        if (!StringUtils.hasText(json)) {
            return new ArrayList<>();
        }

        try {
            JsonNode root = objectMapper.readTree(json);
            if (!root.isArray()) {
                return new ArrayList<>();
            }

            List<FreelancerAttachmentDTO> attachments = new ArrayList<>();
            for (JsonNode node : root) {
                if (node.isTextual()) {
                    String url = node.asText();
                    if (StringUtils.hasText(url)) {
                        attachments.add(buildFreelancerAttachment(url, null, null));
                    }
                    continue;
                }

                if (!node.isObject()) {
                    continue;
                }

                String url = node.path("url").asText(null);
                if (!StringUtils.hasText(url)) {
                    continue;
                }

                attachments.add(buildFreelancerAttachment(
                        url,
                        node.path("name").asText(null),
                        node.path("description").asText(null)));
            }
            return attachments;
        } catch (Exception e) {
            log.warn("Erro ao deserializar freelancer attachment details: {}", e.getMessage());
            return new ArrayList<>();
        }
    }

    private List<FreelancerAttachmentDTO> resolveFreelancerAttachments(FreelancerAttachmentsRequest body) {
        List<FreelancerAttachmentDTO> resolved = new ArrayList<>();
        if (body == null) {
            return resolved;
        }

        if (body.getAttachments() != null && !body.getAttachments().isEmpty()) {
            for (FreelancerAttachmentDTO attachment : body.getAttachments()) {
                if (attachment == null || !StringUtils.hasText(attachment.getUrl())) {
                    continue;
                }
                resolved.add(buildFreelancerAttachment(
                        attachment.getUrl(),
                        attachment.getName(),
                        attachment.getDescription()));
            }
        }

        if (resolved.isEmpty() && body.getUrls() != null && !body.getUrls().isEmpty()) {
            for (String url : body.getUrls()) {
                if (!StringUtils.hasText(url)) {
                    continue;
                }
                resolved.add(buildFreelancerAttachment(url, null, null));
            }
        }

        return resolved;
    }

    private List<String> resolveProjectAttachmentUrls(ProjectAttachmentsRequest body) {
        List<String> resolved = new ArrayList<>();
        if (body == null || body.getUrls() == null || body.getUrls().isEmpty()) {
            return resolved;
        }

        for (String url : body.getUrls()) {
            if (StringUtils.hasText(url)) {
                resolved.add(url.trim());
            }
        }

        return resolved;
    }

    private FreelancerAttachmentDTO buildFreelancerAttachment(String url, String name, String description) {
        String normalizedUrl = url.trim();
        String normalizedName = StringUtils.hasText(name) ? name.trim() : extractFileName(normalizedUrl);
        String normalizedDescription = StringUtils.hasText(description) ? description.trim() : null;

        return FreelancerAttachmentDTO.builder()
                .url(normalizedUrl)
                .name(normalizedName)
                .description(normalizedDescription)
                .build();
    }

    private String extractFileName(String url) {
        try {
            String path = java.net.URI.create(url).getPath();
            if (!StringUtils.hasText(path)) {
                return "arquivo";
            }

            String encoded = path.substring(path.lastIndexOf('/') + 1);
            return StringUtils.hasText(encoded)
                    ? URLDecoder.decode(encoded, StandardCharsets.UTF_8)
                    : "arquivo";
        } catch (Exception e) {
            String fallback = url.substring(url.lastIndexOf('/') + 1);
            return StringUtils.hasText(fallback) ? fallback : "arquivo";
        }
    }

    private boolean isS3Url(String url) {
        return url != null && url.contains(".s3.") && url.contains("amazonaws.com");
    }

    private String serializeResponseMilestones(List<ResponseMilestoneDTO> milestones) {
        List<ResponseMilestoneDTO> normalizedMilestones = normalizeResponseMilestones(milestones);
        if (normalizedMilestones.isEmpty()) {
            return null;
        }

        try {
            return objectMapper.writeValueAsString(normalizedMilestones);
        } catch (Exception e) {
            log.error("Erro ao serializar response milestones", e);
            throw new RuntimeException("Erro ao salvar marcos da proposta");
        }
    }

    private List<ResponseMilestoneDTO> parseResponseMilestones(String json) {
        if (!StringUtils.hasText(json)) {
            return new ArrayList<>();
        }

        try {
            JsonNode root = objectMapper.readTree(json);
            if (!root.isArray()) {
                return new ArrayList<>();
            }

            List<ResponseMilestoneDTO> milestones = new ArrayList<>();
            for (JsonNode node : root) {
                if (!node.isObject()) {
                    continue;
                }

                String title = node.path("title").asText(null);
                if (!StringUtils.hasText(title)) {
                    continue;
                }

                milestones.add(ResponseMilestoneDTO.builder()
                        .id(StringUtils.hasText(node.path("id").asText(null))
                                ? node.path("id").asText().trim()
                                : UUID.randomUUID().toString())
                        .title(title.trim())
                        .description(StringUtils.hasText(node.path("description").asText(null))
                                ? node.path("description").asText().trim()
                                : null)
                        .status(normalizeMilestoneStatus(node.path("status").asText(null)))
                        .build());
            }
            return milestones;
        } catch (Exception e) {
            log.warn("Erro ao deserializar response milestones: {}", e.getMessage());
            return new ArrayList<>();
        }
    }

    private List<ResponseMilestoneDTO> normalizeResponseMilestones(List<ResponseMilestoneDTO> milestones) {
        List<ResponseMilestoneDTO> normalized = new ArrayList<>();
        if (milestones == null) {
            return normalized;
        }

        for (ResponseMilestoneDTO milestone : milestones) {
            if (milestone == null || !StringUtils.hasText(milestone.getTitle())) {
                continue;
            }

            normalized.add(ResponseMilestoneDTO.builder()
                    .id(StringUtils.hasText(milestone.getId()) ? milestone.getId().trim() : UUID.randomUUID().toString())
                    .title(milestone.getTitle().trim())
                    .description(StringUtils.hasText(milestone.getDescription()) ? milestone.getDescription().trim() : null)
                    .status(normalizeMilestoneStatus(milestone.getStatus()))
                    .build());
        }

        return normalized;
    }

    private String normalizeMilestoneStatus(String status) {
        if (!StringUtils.hasText(status)) {
            return "pending";
        }

        String normalized = status.trim().toLowerCase();
        return VALID_MILESTONE_STATUSES.contains(normalized) ? normalized : "pending";
    }

    private ServiceRequest.WorkModel resolveRequestWorkModel(
            ServiceRequestDTO dto,
            com.servija.servija.model.Service service) {
        if (service != null && service.getWorkModel() != null) {
            return ServiceRequest.WorkModel.valueOf(service.getWorkModel().name());
        }

        if (!StringUtils.hasText(dto.getWorkModel())) {
            throw new IllegalArgumentException("Informe o modelo de trabalho");
        }

        try {
            return ServiceRequest.WorkModel.valueOf(dto.getWorkModel().trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("Modelo de trabalho inválido");
        }
    }

    private CategoryCatalogService.CategorySelection resolveSelectionFromService(
            com.servija.servija.model.Service service) {
        return categoryCatalogService.resolveSelection(
                service.getServiceCategory() != null ? service.getServiceCategory().getId() : null,
                service.getServiceSubcategory() != null ? service.getServiceSubcategory().getId() : null,
                service.getCategory(),
                null,
                true,
                false,
                true,
                false);
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
                .categoryId(request.getServiceCategory() != null ? request.getServiceCategory().getId() : null)
                .categorySlug(request.getServiceCategory() != null ? request.getServiceCategory().getSlug() : null)
                .categoryLabel(request.getServiceCategory() != null ? request.getServiceCategory().getName()
                        : request.getCategory())
                .category(request.getCategory())
                .subcategoryId(
                        request.getServiceSubcategory() != null ? request.getServiceSubcategory().getId() : null)
                .subcategorySlug(
                        request.getServiceSubcategory() != null ? request.getServiceSubcategory().getSlug() : null)
                .subcategoryLabel(
                        request.getServiceSubcategory() != null ? request.getServiceSubcategory().getName()
                                : request.getSubcategory())
                .subcategory(request.getSubcategory())
                .description(normalizeRequestDescription(request.getDescription()))
                .deadline(request.getDeadline())
                .workModel(request.getWorkModel() != null ? request.getWorkModel().name() : null)
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
                .deliveryDescription(request.getDeliveryDescription())
                .deliveryAttachments(parseFreelancerAttachmentDetails(request.getDeliveryAttachments()))
                .deliveredAt(request.getDeliveredAt())
                .attachments(parseAttachmentUrls(request.getAttachments()))
                .freelancerAttachments(parseAttachmentUrls(request.getFreelancerAttachments()))
                .freelancerAttachmentDetails(parseFreelancerAttachmentDetails(request.getFreelancerAttachments()))
                .responseAttachments(parseAttachmentUrls(request.getResponseAttachments()))
                .milestones(parseResponseMilestones(request.getResponseMilestones()))
                .createdAt(request.getCreatedAt())
                .updatedAt(request.getUpdatedAt())
                .build();
    }

    private String normalizeRequestDescription(String description) {
        if (!StringUtils.hasText(description)) {
            return description;
        }

        String withoutLegacyPrefix = LEGACY_SERVICE_DESCRIPTION_PREFIX.matcher(description).replaceFirst("");
        return withoutLegacyPrefix.stripLeading();
    }
}
