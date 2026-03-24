package com.servija.servija.controller;

import com.servija.servija.dto.FreelancerAttachmentDTO;
import com.servija.servija.dto.FreelancerAttachmentsRequest;
import com.servija.servija.dto.ServiceRequestResponse;
import com.servija.servija.service.ServiceRequestHistoryService;
import com.servija.servija.service.ServiceRequestService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.Authentication;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ServiceRequestControllerTest {

    @Mock
    private ServiceRequestService serviceRequestService;

    @Mock
    private ServiceRequestHistoryService historyService;

    @InjectMocks
    private ServiceRequestController controller;

    @Test
    @DisplayName("Deve aceitar payload novo de anexos detalhados")
    void addFreelancerAttachments_acceptsDetailedAttachmentsPayload() {
        Authentication authentication = new TestingAuthenticationToken("freela@servija.com", null);
        FreelancerAttachmentsRequest body = FreelancerAttachmentsRequest.builder()
                .attachments(List.of(FreelancerAttachmentDTO.builder()
                        .url("https://cdn.servija.com/entrega.zip")
                        .name("Entrega.zip")
                        .description("Versão final")
                        .build()))
                .build();

        when(serviceRequestService.addFreelancerAttachments(eq(15L), eq(body), eq("freela@servija.com")))
                .thenReturn(ServiceRequestResponse.builder().id(15L).build());

        var response = controller.addFreelancerAttachments(15L, body, authentication);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(serviceRequestService).addFreelancerAttachments(15L, body, "freela@servija.com");
    }

    @Test
    @DisplayName("Deve aceitar payload legado com urls")
    void addFreelancerAttachments_acceptsLegacyUrlsPayload() {
        Authentication authentication = new TestingAuthenticationToken("freela@servija.com", null);
        FreelancerAttachmentsRequest body = FreelancerAttachmentsRequest.builder()
                .urls(List.of("https://cdn.servija.com/entrega.zip"))
                .build();

        when(serviceRequestService.addFreelancerAttachments(eq(15L), eq(body), eq("freela@servija.com")))
                .thenReturn(ServiceRequestResponse.builder().id(15L).build());

        var response = controller.addFreelancerAttachments(15L, body, authentication);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(serviceRequestService).addFreelancerAttachments(15L, body, "freela@servija.com");
    }

    @Test
    @DisplayName("Deve rejeitar payload sem anexos nem urls")
    void addFreelancerAttachments_rejectsEmptyPayload() {
        Authentication authentication = new TestingAuthenticationToken("freela@servija.com", null);

        var response = controller.addFreelancerAttachments(15L, FreelancerAttachmentsRequest.builder().build(), authentication);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }
}
