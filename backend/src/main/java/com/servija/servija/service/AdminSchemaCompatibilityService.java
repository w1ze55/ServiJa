package com.servija.servija.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

@Component
@Order(5)
@RequiredArgsConstructor
@Slf4j
public class AdminSchemaCompatibilityService implements ApplicationRunner {

    private static final Pattern SAFE_IDENTIFIER = Pattern.compile("[A-Za-z0-9_]+");
    private static final String ADMIN_USER_TYPE_CHECK =
            "CHECK (user_type IN ('CONTRACTOR', 'FREELANCER', 'ADMIN'))";

    private final JdbcTemplate jdbcTemplate;

    @Override
    public void run(ApplicationArguments args) {
        try {
            ensureAdminUserTypeConstraint();
        } catch (Exception ex) {
            log.warn("Não foi possível ajustar a constraint users.user_type antes do bootstrap do admin.", ex);
        }
    }

    private void ensureAdminUserTypeConstraint() {
        List<Map<String, Object>> constraints = jdbcTemplate.queryForList("""
                SELECT tc.CONSTRAINT_NAME, cc.CHECK_CLAUSE
                FROM INFORMATION_SCHEMA.TABLE_CONSTRAINTS tc
                JOIN INFORMATION_SCHEMA.CHECK_CONSTRAINTS cc
                  ON cc.CONSTRAINT_SCHEMA = tc.CONSTRAINT_SCHEMA
                 AND cc.CONSTRAINT_NAME = tc.CONSTRAINT_NAME
                WHERE tc.TABLE_SCHEMA = DATABASE()
                  AND tc.TABLE_NAME = 'users'
                  AND tc.CONSTRAINT_TYPE = 'CHECK'
                  AND LOWER(cc.CHECK_CLAUSE) LIKE '%user_type%'
                """);

        boolean adminAlreadyAllowed = constraints.stream()
                .map(row -> String.valueOf(row.get("CHECK_CLAUSE")))
                .anyMatch(clause -> clause.toUpperCase().contains("'ADMIN'"));

        if (adminAlreadyAllowed) {
            return;
        }

        for (Map<String, Object> constraint : constraints) {
            String constraintName = String.valueOf(constraint.get("CONSTRAINT_NAME"));
            if (!SAFE_IDENTIFIER.matcher(constraintName).matches()) {
                log.warn("Ignorando DROP CHECK com nome inesperado: {}", constraintName);
                continue;
            }

            jdbcTemplate.execute("ALTER TABLE users DROP CHECK `" + constraintName + "`");
            log.info("Constraint {} removida de users.user_type para compatibilidade com ADMIN.", constraintName);
        }

        jdbcTemplate.execute("ALTER TABLE users ADD CONSTRAINT chk_users_user_type " + ADMIN_USER_TYPE_CHECK);
        log.info("Constraint users.user_type atualizada para aceitar ADMIN.");
    }
}
