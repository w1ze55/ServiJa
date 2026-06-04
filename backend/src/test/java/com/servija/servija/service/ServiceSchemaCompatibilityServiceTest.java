package com.servija.servija.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;
import java.util.Map;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ServiceSchemaCompatibilityServiceTest {

    @Mock
    private JdbcTemplate jdbcTemplate;

    @InjectMocks
    private ServiceSchemaCompatibilityService service;

    @Test
    @DisplayName("run ajusta enum quando a coluna delivery_unit ainda nao aceita MONTHS")
    void runAdjustsEnumWhenMonthsAreMissing() {
        when(jdbcTemplate.queryForList(org.mockito.ArgumentMatchers.anyString()))
                .thenReturn(List.of(Map.of("COLUMN_TYPE", "enum('DAYS','WEEKS')")));

        service.run(null);

        verify(jdbcTemplate).execute(org.mockito.ArgumentMatchers.contains("ALTER TABLE services MODIFY COLUMN delivery_unit"));
    }

    @Test
    @DisplayName("run nao altera nada quando a coluna ja aceita MONTHS")
    void runSkipsWhenColumnAlreadySupportsMonths() {
        when(jdbcTemplate.queryForList(org.mockito.ArgumentMatchers.anyString()))
                .thenReturn(List.of(Map.of("COLUMN_TYPE", "enum('DAYS','WEEKS','MONTHS')")));

        service.run(null);

        verify(jdbcTemplate, never()).execute(org.mockito.ArgumentMatchers.anyString());
    }
}
