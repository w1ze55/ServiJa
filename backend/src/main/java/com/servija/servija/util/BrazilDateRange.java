package com.servija.servija.util;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;

/**
 * Converte um dia-calendário no horário de Brasília (America/Sao_Paulo) para os
 * limites equivalentes em UTC.
 *
 * <p>Os timestamps são gravados em UTC (o JVM roda em UTC e as colunas são
 * {@code DATETIME}/{@code LocalDateTime} sem fuso). Já os filtros enviados pelo
 * front representam o dia-calendário de Brasília. Sem esta conversão, um evento
 * ocorrido às 21h–23h59 de Brasília (00h–02h59 UTC do dia seguinte) cairia no
 * dia errado ao filtrar.
 */
public final class BrazilDateRange {

    private static final ZoneId BRAZIL = ZoneId.of("America/Sao_Paulo");

    private BrazilDateRange() {
    }

    /** Início do dia (00:00 Brasília) convertido para UTC. */
    public static LocalDateTime startOfDayUtc(LocalDate date) {
        return date.atStartOfDay(BRAZIL).withZoneSameInstant(ZoneOffset.UTC).toLocalDateTime();
    }

    /** Início do dia seguinte (limite superior exclusivo) convertido para UTC. */
    public static LocalDateTime endExclusiveUtc(LocalDate date) {
        return date.plusDays(1).atStartOfDay(BRAZIL).withZoneSameInstant(ZoneOffset.UTC).toLocalDateTime();
    }
}
