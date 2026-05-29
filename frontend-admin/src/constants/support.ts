export const SUPPORT_CATEGORY_OPTIONS = [
  { value: 'BUG', label: 'Erro ou bug' },
  { value: 'CATEGORY_SUBCATEGORY_REQUEST', label: 'Cadastro de nova categoria ou subcategoria' },
  { value: 'PAYMENT_ISSUE', label: 'Problemas com pagamento' },
  { value: 'SERVICE_ISSUE', label: 'Problemas com serviço' },
  { value: 'FRAUD', label: 'Fraude' },
  { value: 'USER_REPORT', label: 'Denúncia de usuário' },
] as const;

export type SupportCategoryValue = (typeof SUPPORT_CATEGORY_OPTIONS)[number]['value'];

export const SUPPORT_STATUS_OPTIONS = [
  { value: 'PENDING', label: 'Pendente' },
  { value: 'APPROVED', label: 'Aprovado' },
  { value: 'REJECTED', label: 'Recusado' },
  { value: 'ANSWERED', label: 'Respondido' },
] as const;

export type SupportStatusValue = (typeof SUPPORT_STATUS_OPTIONS)[number]['value'];

export interface SupportAttachment {
  id?: string;
  name: string;
  url?: string;
  type?: string;
  size?: number;
}

export const getSupportCategoryLabel = (value?: string | null) =>
  SUPPORT_CATEGORY_OPTIONS.find((option) => option.value === value)?.label || 'Categoria desconhecida';

export const getSupportStatusLabel = (value?: string | null) =>
  SUPPORT_STATUS_OPTIONS.find((option) => option.value === value)?.label || 'Status desconhecido';

export const getSupportStatusBadgeClass = (value?: string | null) => {
  switch (value) {
    case 'APPROVED':
      return 'bg-emerald-50 text-emerald-700';
    case 'REJECTED':
      return 'bg-red-50 text-red-700';
    case 'ANSWERED':
      return 'bg-blue-50 text-blue-700';
    case 'PENDING':
    default:
      return 'bg-amber-50 text-amber-700';
  }
};

export const isImageAttachment = (attachment?: SupportAttachment | null) => {
  if (!attachment) return false;
  if (attachment.type?.startsWith('image/')) return true;

  const lowerUrl = attachment.url?.toLowerCase() || attachment.name.toLowerCase();
  return ['.jpg', '.jpeg', '.png', '.gif', '.webp'].some((extension) => lowerUrl.endsWith(extension));
};

export const formatSupportFileSize = (size?: number | null) => {
  if (!size || size <= 0) return '';

  if (size < 1024) return `${size} B`;
  if (size < 1024 * 1024) return `${(size / 1024).toFixed(1)} KB`;
  return `${(size / (1024 * 1024)).toFixed(1)} MB`;
};
