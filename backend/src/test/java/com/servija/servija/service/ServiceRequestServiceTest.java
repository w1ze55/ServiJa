package com.servija.servija.service;

import com.servija.servija.dto.FreelancerAttachmentDTO;
import com.servija.servija.dto.FreelancerAttachmentsRequest;
import com.servija.servija.dto.ServiceRequestResponse;
import com.servija.servija.model.ServiceRequest;
import com.servija.servija.model.User;
import com.servija.servija.repository.ServiceRepository;
import com.servija.servija.repository.ServiceRequestRepository;
import com.servija.servija.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.jdbc.core.JdbcTemplate;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ServiceRequestServiceTest {

    @Mock
    private ServiceRequestRepository serviceRequestRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ServiceRepository serviceRepository;

    @Mock
    private FileStorageService fileStorageService;

    @Mock
    private ServiceRequestHistoryService historyService;

    @Mock
    private WebSocketService webSocketService;

    @Mock
    private NotificationService notificationService;

    @Mock
    private JdbcTemplate jdbcTemplate;

    @InjectMocks
    private ServiceRequestService serviceRequestService;

    @Test
    @DisplayName("Deve converter anexos antigos do freelancer em detalhes ao carregar a solicitação")
    void getById_mapsLegacyFreelancerAttachmentsToDetailedResponse() {
        ServiceRequest request = buildRequest("[\"https://cdn.servija.com/entrega-final.pdf\"]");

        when(serviceRequestRepository.findById(11L)).thenReturn(Optional.of(request));

        ServiceRequestResponse response = serviceRequestService.getById(11L);

        assertThat(response.getFreelancerAttachments())
                .containsExactly("https://cdn.servija.com/entrega-final.pdf");
        assertThat(response.getFreelancerAttachmentDetails()).singleElement().satisfies(attachment -> {
            assertThat(attachment.getUrl()).isEqualTo("https://cdn.servija.com/entrega-final.pdf");
            assertThat(attachment.getName()).isEqualTo("entrega-final.pdf");
            assertThat(attachment.getDescription()).isNull();
        });
    }

    @Test
    @DisplayName("Deve salvar anexos detalhados e manter compatibilidade com anexos antigos")
    void addFreelancerAttachments_persistsDetailedAttachmentsAndKeepsLegacyEntries() {
        User freelancer = buildFreelancer();
        ServiceRequest request = buildRequest("[\"https://cdn.servija.com/entrega-antiga.zip\"]");

        FreelancerAttachmentsRequest body = FreelancerAttachmentsRequest.builder()
                .attachments(List.of(FreelancerAttachmentDTO.builder()
                        .url("https://cdn.servija.com/entrega-v2.zip")
                        .name("Entrega v2.zip")
                        .description("Versão com ajustes aplicados")
                        .build()))
                .build();

        when(serviceRequestRepository.findById(11L)).thenReturn(Optional.of(request));
        when(userRepository.findByEmail("freela@servija.com")).thenReturn(Optional.of(freelancer));
        when(serviceRequestRepository.save(any(ServiceRequest.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ServiceRequestResponse response = serviceRequestService.addFreelancerAttachments(11L, body, "freela@servija.com");

        assertThat(request.getFreelancerAttachments())
                .contains("\"url\":\"https://cdn.servija.com/entrega-v2.zip\"")
                .contains("\"description\":\"Versão com ajustes aplicados\"");

        assertThat(response.getFreelancerAttachments()).containsExactly(
                "https://cdn.servija.com/entrega-antiga.zip",
                "https://cdn.servija.com/entrega-v2.zip");

        assertThat(response.getFreelancerAttachmentDetails()).hasSize(2);
        assertThat(response.getFreelancerAttachmentDetails().get(0).getName()).isEqualTo("entrega-antiga.zip");
        assertThat(response.getFreelancerAttachmentDetails().get(1).getName()).isEqualTo("Entrega v2.zip");
        assertThat(response.getFreelancerAttachmentDetails().get(1).getDescription())
                .isEqualTo("Versão com ajustes aplicados");
    }

    private ServiceRequest buildRequest(String freelancerAttachmentsJson) {
        return ServiceRequest.builder()
                .id(11L)
                .contractor(User.builder()
                        .id(21L)
                        .name("Cliente")
                        .email("cliente@servija.com")
                        .build())
                .freelancer(buildFreelancer())
                .serviceName("Desenvolvimento Web")
                .category("Desenvolvedor Web")
                .description("Landing page institucional")
                .deadline("7 dias")
                .workModel(ServiceRequest.WorkModel.REMOTO)
                .status(ServiceRequest.Status.IN_PROGRESS)
                .freelancerAttachments(freelancerAttachmentsJson)
                .build();
    }

    private User buildFreelancer() {
        return User.builder()
                .id(31L)
                .name("Freelancer")
                .email("freela@servija.com")
                .build();
    }
}
