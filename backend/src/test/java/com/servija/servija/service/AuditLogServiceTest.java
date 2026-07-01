package com.servija.servija.service;

import com.servija.servija.dto.AuditLogDto;
import com.servija.servija.dto.AuditLogPageResponse;
import com.servija.servija.exception.ResourceNotFoundException;
import com.servija.servija.model.AuditLog;
import com.servija.servija.repository.AuditLogRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuditLogServiceTest {

    @Mock
    private AuditLogRepository auditLogRepository;

    @InjectMocks
    private AuditLogService service;

    @Test
    @DisplayName("getLog retorna detalhes completos do log")
    void getLogReturnsDetailedDto() {
        AuditLog log = auditLog();
        when(auditLogRepository.findById(1L)).thenReturn(Optional.of(log));

        AuditLogDto result = service.getLog(1L);

        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getResponse()).isEqualTo("{\"ok\":true}");
        assertThat(result.getOldValue()).isEqualTo("{\"before\":1}");
        assertThat(result.getSeverity()).isNotNull();
    }

    @Test
    @DisplayName("getLog lança ResourceNotFoundException quando o log não existe")
    void getLogThrowsWhenLogDoesNotExist() {
        when(auditLogRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getLog(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("auditoria");
    }

    @Test
    @DisplayName("listLogs pagina resultados e omite campos pesados na listagem")
    void listLogsReturnsPaginatedSummary() {
        AuditLog log = auditLog();
        when(auditLogRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(log)));

        AuditLogPageResponse result = service.listLogs(
                "actor",
                "USER_LOGIN",
                "USER",
                AuditLog.ActorType.ADMIN,
                AuditLog.Result.SUCCESS,
                null,
                null,
                null,
                0,
                20);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getResponse()).isNull();
        assertThat(result.getContent().get(0).getOldValue()).isNull();
        assertThat(result.getCurrentPage()).isZero();

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(auditLogRepository).findAll(any(Specification.class), pageableCaptor.capture());
        assertThat(pageableCaptor.getValue().getPageSize()).isEqualTo(20);
    }

    private AuditLog auditLog() {
        return AuditLog.builder()
                .id(1L)
                .timestamp(LocalDateTime.of(2026, 1, 10, 10, 0))
                .actorId(10L)
                .actorName("Admin")
                .actorEmail("admin@test.com")
                .actorType(AuditLog.ActorType.ADMIN)
                .action("USER_LOGIN")
                .entityType("USER")
                .entityId("10")
                .entityLabel("Admin")
                .httpMethod("POST")
                .httpPath("/auth/login")
                .httpStatus(200)
                .result(AuditLog.Result.SUCCESS)
                .ipAddress("127.0.0.1")
                .userAgent("JUnit")
                .payload("{\"email\":\"admin@test.com\"}")
                .response("{\"ok\":true}")
                .oldValue("{\"before\":1}")
                .newValue("{\"after\":2}")
                .build();
    }
}
