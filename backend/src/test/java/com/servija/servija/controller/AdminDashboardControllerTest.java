package com.servija.servija.controller;

import com.servija.servija.dto.AdminDashboardResponse;
import com.servija.servija.exception.GlobalExceptionHandler;
import com.servija.servija.service.AdminDashboardService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class AdminDashboardControllerTest {

    @Mock
    private AdminDashboardService adminDashboardService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new AdminDashboardController(adminDashboardService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("Deve converter os parametros de data e retornar 200 no dashboard admin")
    void getDashboard_returnsOk() throws Exception {
        when(adminDashboardService.getDashboard(LocalDate.of(2026, 5, 1), LocalDate.of(2026, 5, 31)))
                .thenReturn(AdminDashboardResponse.builder().build());

        mockMvc.perform(get("/api/admin/dashboard")
                        .param("dateFrom", "2026-05-01")
                        .param("dateTo", "2026-05-31"))
                .andExpect(status().isOk());

        verify(adminDashboardService).getDashboard(LocalDate.of(2026, 5, 1), LocalDate.of(2026, 5, 31));
    }

    @Test
    @DisplayName("Deve proteger o controller do dashboard com role admin")
    void controllerShouldRequireAdminRole() {
        PreAuthorize annotation = AdminDashboardController.class.getAnnotation(PreAuthorize.class);

        assertThat(annotation).isNotNull();
        assertThat(annotation.value()).isEqualTo("hasRole('ADMIN')");
    }
}
