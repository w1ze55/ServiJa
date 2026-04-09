export const MIN_VALUE = 20;
export const MAX_VALUE = 1_000_000;

export const VALUE_ERRORS = {
  min: `O valor mínimo é R$ ${MIN_VALUE.toLocaleString('pt-BR', { minimumFractionDigits: 2 })}`,
  max: `O valor máximo é R$ ${MAX_VALUE.toLocaleString('pt-BR', { minimumFractionDigits: 2 })}`,
} as const;

export function validateValueRange(value: number): string | null {
  if (value < MIN_VALUE) return VALUE_ERRORS.min;
  if (value > MAX_VALUE) return VALUE_ERRORS.max;
  return null;
}

export function extractApiErrorMessage(err: unknown): string {
  if (!err || typeof err !== 'object') return '';

  const axiosError = err as any;
  const data = axiosError?.response?.data;
  if (!data) return '';

  // ValidationErrorResponse: { errors: { field: message } }
  if (data.errors && typeof data.errors === 'object') {
    const messages = Object.values(data.errors) as string[];
    if (messages.length > 0) return messages[0];
  }

  // ErrorResponse: { message: "..." }
  if (data.message && typeof data.message === 'string' && data.message !== 'Erro de validação') {
    return data.message;
  }

  return '';
}
