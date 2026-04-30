package com.servija.servija.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.servija.servija.exception.GlobalExceptionHandler;
import com.servija.servija.exception.IbgeServiceUnavailableException;
import com.servija.servija.service.FreelancerService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class FreelancerControllerTest {

    @Mock
    private FreelancerService freelancerService;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        mockMvc = MockMvcBuilders.standaloneSetup(new FreelancerController(freelancerService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("Deve retornar 400 quando a validação das cidades falhar")
    void updateProfile_returnsBadRequestForInvalidCityPayload() throws Exception {
        when(freelancerService.updateProfile(any()))
                .thenThrow(new IllegalArgumentException("Cidade de atendimento inválida."));

        mockMvc.perform(put("/api/freelancer/profile")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(Map.of("serviceCities", java.util.List.of()))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Cidade de atendimento inválida."));
    }

    @Test
    @DisplayName("Deve retornar 503 quando o IBGE estiver indisponível")
    void updateProfile_returnsServiceUnavailableWhenIbgeFails() throws Exception {
        when(freelancerService.updateProfile(any()))
                .thenThrow(new IbgeServiceUnavailableException("Não foi possível consultar o IBGE."));

        mockMvc.perform(put("/api/freelancer/profile")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(Map.of("serviceCities", java.util.List.of()))))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.message").value("Não foi possível consultar o IBGE."));
    }
}
