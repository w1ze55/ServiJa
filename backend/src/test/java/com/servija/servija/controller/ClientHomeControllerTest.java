package com.servija.servija.controller;

import com.servija.servija.dto.ClientHomeResponse;
import com.servija.servija.exception.GlobalExceptionHandler;
import com.servija.servija.service.ClientHomeService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class ClientHomeControllerTest {

    @Mock
    private ClientHomeService clientHomeService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new ClientHomeController(clientHomeService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("Deve retornar 200 ao buscar a home do cliente")
    void getHome_returnsOk() throws Exception {
        when(clientHomeService.getHome()).thenReturn(ClientHomeResponse.builder()
                .region(ClientHomeResponse.RegionInfo.builder()
                        .displayName("São Paulo, SP")
                        .localized(true)
                        .build())
                .build());

        mockMvc.perform(get("/api/client-home"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.region.displayName").value("São Paulo, SP"));
    }

    @Test
    @DisplayName("Deve retornar 400 quando o usuário não puder acessar a home do cliente")
    void getHome_returnsBadRequestForInvalidUserType() throws Exception {
        when(clientHomeService.getHome())
                .thenThrow(new IllegalStateException("Usuário não é um contratante"));

        mockMvc.perform(get("/api/client-home"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Usuário não é um contratante"));
    }
}
