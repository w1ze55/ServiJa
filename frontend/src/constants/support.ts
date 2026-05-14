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

export const SUPPORT_ATTACHMENT_ACCEPT =
  'image/*,.pdf,.doc,.docx,.xls,.xlsx,.txt,.ppt,.pptx';

const SUPPORT_ALLOWED_MIME_TYPES = new Set([
  'image/jpeg',
  'image/png',
  'image/gif',
  'image/webp',
  'application/pdf',
  'application/msword',
  'application/vnd.openxmlformats-officedocument.wordprocessingml.document',
  'application/vnd.ms-excel',
  'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet',
  'application/vnd.ms-powerpoint',
  'application/vnd.openxmlformats-officedocument.presentationml.presentation',
  'text/plain',
]);

const SUPPORT_ALLOWED_EXTENSIONS = new Set([
  '.jpg',
  '.jpeg',
  '.png',
  '.gif',
  '.webp',
  '.pdf',
  '.doc',
  '.docx',
  '.xls',
  '.xlsx',
  '.txt',
  '.ppt',
  '.pptx',
]);

export const getSupportCategoryLabel = (value?: string | null) =>
  SUPPORT_CATEGORY_OPTIONS.find((option) => option.value === value)?.label || 'Categoria desconhecida';

export const getSupportStatusLabel = (value?: string | null) =>
  SUPPORT_STATUS_OPTIONS.find((option) => option.value === value)?.label || 'Status desconhecido';

export const isAllowedSupportFile = (file: File) => {
  if (file.type && SUPPORT_ALLOWED_MIME_TYPES.has(file.type)) {
    return true;
  }

  const lowerName = file.name.toLowerCase();
  return Array.from(SUPPORT_ALLOWED_EXTENSIONS).some((extension) => lowerName.endsWith(extension));
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
