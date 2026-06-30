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

@Component
@Order(6)
@RequiredArgsConstructor
@Slf4j
public class ServiceSchemaCompatibilityService implements ApplicationRunner {

    private static final String DELIVERY_UNIT_COLUMN_SQL = """
            SELECT COLUMN_TYPE
            FROM INFORMATION_SCHEMA.COLUMNS
            WHERE TABLE_SCHEMA = DATABASE()
              AND TABLE_NAME = 'services'
              AND COLUMN_NAME = 'delivery_unit'
            """;

    private static final String DELIVERY_UNIT_ENUM_WITH_MONTHS =
            "ALTER TABLE services MODIFY COLUMN delivery_unit ENUM('DAYS','WEEKS','MONTHS') NOT NULL DEFAULT 'DAYS'";

    private final JdbcTemplate jdbcTemplate;

    @Override
    public void run(ApplicationArguments args) {
        try {
            ensureDeliveryUnitSupportsMonths();
        } catch (Exception ex) {
            log.warn("Não foi possível ajustar a coluna services.delivery_unit para compatibilidade com MONTHS.", ex);
        }
    }

    private void ensureDeliveryUnitSupportsMonths() {
        List<Map<String, Object>> columns = jdbcTemplate.queryForList(DELIVERY_UNIT_COLUMN_SQL);
        if (columns.isEmpty()) {
            log.warn("Coluna services.delivery_unit não encontrada para verificação de compatibilidade.");
            return;
        }

        String columnType = String.valueOf(columns.get(0).get("COLUMN_TYPE"));
        if (columnType.toUpperCase().contains("'MONTHS'")) {
            log.info("Coluna services.delivery_unit ja aceita MONTHS: {}", columnType);
            return;
        }

        jdbcTemplate.execute(DELIVERY_UNIT_ENUM_WITH_MONTHS);
        log.info("Coluna services.delivery_unit ajustada para aceitar DAYS, WEEKS e MONTHS.");
    }
}
