export type WorkModelValue = 'PRESENCIAL' | 'REMOTO' | 'HIBRIDO';

export const normalizeWorkModel = (value?: string | null): WorkModelValue | null => {
  const normalized = value?.trim().toUpperCase();

  if (normalized === 'PRESENCIAL') return 'PRESENCIAL';
  if (normalized === 'REMOTO') return 'REMOTO';
  if (normalized === 'HIBRIDO') return 'HIBRIDO';

  return null;
};

export const getWorkModelLabel = (value?: string | null): string => {
  const normalized = normalizeWorkModel(value);

  if (normalized === 'PRESENCIAL') return 'Presencial';
  if (normalized === 'HIBRIDO') return 'Híbrido';
  if (normalized === 'REMOTO') return 'Remoto';

  return 'Não informado';
};

export const getWorkModelBadgeClass = (value?: string | null): string => {
  const normalized = normalizeWorkModel(value);

  if (normalized === 'PRESENCIAL') return 'bg-emerald-100 text-emerald-700 hover:bg-emerald-100';
  if (normalized === 'HIBRIDO') return 'bg-amber-100 text-amber-700 hover:bg-amber-100';
  if (normalized === 'REMOTO') return 'bg-sky-100 text-sky-700 hover:bg-sky-100';

  return 'bg-gray-100 text-gray-700 hover:bg-gray-100';
};
