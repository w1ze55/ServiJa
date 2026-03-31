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

export const getApiDateTimestamp = (value?: string | null): number => {
  const parsedDate = parseApiDate(value);
  const timestamp = parsedDate.getTime();
  return Number.isNaN(timestamp) ? 0 : timestamp;
};

export const formatApiDate = (
  value: string,
  options: Intl.DateTimeFormatOptions
): string => {
  const parsedDate = parseApiDate(value);
  if (Number.isNaN(parsedDate.getTime())) {
    return '';
  }

  return parsedDate.toLocaleString('pt-BR', options);
};

export const formatTimeAgoFromApiDate = (value: string): string => {
  const timestamp = getApiDateTimestamp(value);
  if (!timestamp) {
    return 'Atualizado agora';
  }

  const diffMs = Math.max(0, Date.now() - timestamp);
  const diffMins = Math.floor(diffMs / 60000);
  const diffHours = Math.floor(diffMins / 60);
  const diffDays = Math.floor(diffHours / 24);

  if (diffMins < 1) return 'Atualizado agora';
  if (diffMins < 60) return `Atualizado há ${diffMins} minuto${diffMins !== 1 ? 's' : ''}`;
  if (diffHours < 24) return `Atualizado há ${diffHours} hora${diffHours !== 1 ? 's' : ''}`;
  if (diffDays === 1) return 'Atualizado há 1 dia';
  return `Atualizado há ${diffDays} dias`;
};
