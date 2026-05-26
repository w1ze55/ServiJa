package com.servija.servija.service;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AuditStateDiffUtilsTest {

    @Test
    void ignoresBigDecimalScaleWhenComparingChangedFields() {
        List<String> changedFields = AuditStateDiffUtils.changedFields(
                Map.of(
                        "title", "Nome antigo",
                        "price", new BigDecimal("1000")),
                Map.of(
                        "title", "Nome novo",
                        "price", new BigDecimal("1000.00")));

        assertEquals(List.of("title"), changedFields);
    }
}
