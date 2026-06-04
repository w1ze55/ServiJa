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

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminSchemaCompatibilityServiceTest {

    @Mock
    private JdbcTemplate jdbcTemplate;

    @InjectMocks
    private AdminSchemaCompatibilityService service;

    @Test
    @DisplayName("run remove check legado e adiciona constraint com ADMIN")
    void runUpdatesConstraintWhenAdminIsMissing() {
        when(jdbcTemplate.queryForList(org.mockito.ArgumentMatchers.anyString()))
                .thenReturn(List.of(Map.of(
                        "CONSTRAINT_NAME", "chk_users_user_type_old",
                        "CHECK_CLAUSE", "user_type in ('CONTRACTOR','FREELANCER')")));

        service.run(null);

        verify(jdbcTemplate).execute("ALTER TABLE users DROP CHECK `chk_users_user_type_old`");
        verify(jdbcTemplate).execute(org.mockito.ArgumentMatchers.contains("ADD CONSTRAINT chk_users_user_type"));
    }
}
