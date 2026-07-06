const BRAZIL_TIME_ZONE = 'America/Sao_Paulo';

/**
 * O backend serializa timestamps (LocalDateTime) sem offset, ex.: "2024-06-30T00:00:00",
 * mas o instante real é UTC. Sem tratamento, `new Date()` interpreta uma data-hora sem
 * offset como horário local do navegador, mostrando o relógio UTC literalmente (+3h em
 * relação a Brasília, virando o dia para eventos do fim da noite).
 *
 * `parseApiDate` anexa "Z" quando o valor não tem fuso explícito, garantindo a leitura
 * correta como UTC. Os formatadores fixam o fuso em America/Sao_Paulo, então a exibição
 * fica sempre em Brasília — independente do fuso da máquina do admin.
 */
export const parseApiDate = (value?: string | null): Date => {
  if (!value) {
    return new Date(Number.NaN);
  }

  const trimmedValue = value.trim();
  if (!trimmedValue) {
    return new Date(Number.NaN);
  }

  const normalizedValue = trimmedValue.includes(' ') && !trimmedValue.includes('T')
    ? trimmedValue.replace(' ', 'T')
    : trimmedValue;

  const hasExplicitTimezone = /(?:[zZ]|[+\-]\d{2}:\d{2})$/.test(normalizedValue);
  const isLocalDateTime = /^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}(?::\d{2}(?:\.\d{1,9})?)?$/.test(normalizedValue);

  const parseableValue = isLocalDateTime && !hasExplicitTimezone
    ? `${normalizedValue}Z`
    : normalizedValue;

  return new Date(parseableValue);
};

const withBrazilTimeZone = (
  options: Intl.DateTimeFormatOptions = {}
): Intl.DateTimeFormatOptions => ({ timeZone: BRAZIL_TIME_ZONE, ...options });

/** Data + hora em horário de Brasília (toLocaleString). */
export const formatApiDate = (
  value?: string | null,
  options: Intl.DateTimeFormatOptions = {}
): string => {
  const parsedDate = parseApiDate(value);
  if (Number.isNaN(parsedDate.getTime())) {
    return '';
  }
  return parsedDate.toLocaleString('pt-BR', withBrazilTimeZone(options));
};

/** Somente a data em horário de Brasília (toLocaleDateString). */
export const formatApiDateOnly = (
  value?: string | null,
  options: Intl.DateTimeFormatOptions = {}
): string => {
  const parsedDate = parseApiDate(value);
  if (Number.isNaN(parsedDate.getTime())) {
    return '';
  }
  return parsedDate.toLocaleDateString('pt-BR', withBrazilTimeZone(options));
};

/** Somente a hora em horário de Brasília (toLocaleTimeString). */
export const formatApiTimeOnly = (
  value?: string | null,
  options: Intl.DateTimeFormatOptions = {}
): string => {
  const parsedDate = parseApiDate(value);
  if (Number.isNaN(parsedDate.getTime())) {
    return '';
  }
  return parsedDate.toLocaleTimeString('pt-BR', withBrazilTimeZone(options));
};
