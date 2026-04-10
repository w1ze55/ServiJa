import type {
  NotificationCategory,
  NotificationItem,
  NotificationType,
  NotificationMetadata,
} from '@/types/notifications';
import { getApiDateTimestamp, parseApiDate } from '@/utils/apiDate';

export type NotificationUserType = 'provider' | 'contractor';
export type NotificationTone = 'green' | 'purple' | 'red' | 'blue' | 'orange';

export interface NotificationRouteTarget {
  path: string;
  query?: Record<string, string>;
}

export const notificationCategoryOrder: NotificationCategory[] = ['projects', 'payments', 'messages'];

const categoryLabels: Record<NotificationCategory, string> = {
  projects: 'Projetos',
  payments: 'Pagamentos',
  messages: 'Mensagens',
};

const withActor = (actorName: string | null | undefined, fallback: string) => actorName || fallback;

const POSITIVE_TYPES: NotificationType[] = [
  'PROPOSAL_ACCEPTED',
  'PAYMENT_APPROVED',
  'PAYMENT_RECEIVED',
  'DELIVERY_APPROVED',
];

const NEGATIVE_TYPES: NotificationType[] = ['REQUEST_REJECTED'];

const INFO_TYPES: NotificationType[] = ['MESSAGE_RECEIVED', 'ATTACHMENT_ADDED'];

const PENDING_TYPES: NotificationType[] = [
  'NEW_SERVICE_REQUEST',
  'NEW_PROPOSAL',
  'PAYMENT_REQUIRED',
  'PROJECT_COMPLETED',
  'ADJUSTMENTS_REQUESTED',
];

const POSITIVE_STATUS_KEYWORDS = ['ACCEPTED', 'APPROVED', 'COMPLETED'];
const NEGATIVE_STATUS_KEYWORDS = ['REJECTED', 'REPROVED', 'FAILED', 'REFUSED', 'CANCELLED', 'OVERDUE', 'CHARGEBACK'];
const PENDING_STATUS_KEYWORDS = ['PENDING', 'NEGOTIATING', 'SENT', 'DELIVERED', 'PROCESSING'];
const STARTED_STATUS_KEYWORDS = ['IN_PROGRESS'];

export const getNotificationCategoryLabel = (category: NotificationCategory) => {
  return categoryLabels[category];
};

const resolveToneByStatus = (status: unknown): NotificationTone | null => {
  if (typeof status !== 'string' || !status.trim()) {
    return null;
  }

  const normalizedStatus = status.trim().toUpperCase();

  if (STARTED_STATUS_KEYWORDS.some((keyword) => normalizedStatus.includes(keyword))) {
    return 'blue';
  }

  if (NEGATIVE_STATUS_KEYWORDS.some((keyword) => normalizedStatus.includes(keyword))) {
    return 'red';
  }

  if (POSITIVE_STATUS_KEYWORDS.some((keyword) => normalizedStatus.includes(keyword))) {
    return 'green';
  }

  if (PENDING_STATUS_KEYWORDS.some((keyword) => normalizedStatus.includes(keyword))) {
    return 'orange';
  }

  return null;
};

export const getNotificationTone = (notification: NotificationItem): NotificationTone => {
  if (notification.type === 'PROJECT_STARTED') {
    return 'blue';
  }

  if (POSITIVE_TYPES.includes(notification.type)) {
    return 'green';
  }

  if (NEGATIVE_TYPES.includes(notification.type)) {
    return 'red';
  }

  if (INFO_TYPES.includes(notification.type)) {
    return 'blue';
  }

  if (PENDING_TYPES.includes(notification.type)) {
    return 'orange';
  }

  const statusTone = resolveToneByStatus(notification.metadata?.status);
  if (statusTone) {
    return statusTone;
  }

  switch (notification.category) {
    case 'payments':
      return 'green';
    case 'messages':
      return 'blue';
    case 'projects':
    default:
      return 'orange';
  }
};

export const getNotificationDescription = (notification: NotificationItem) => {
  const actorName = notification.actorName;
  const attachmentCount = Number(notification.metadata.attachmentCount || 0);

  switch (notification.type) {
    case 'NEW_SERVICE_REQUEST':
      return `${withActor(actorName, 'Um cliente')} enviou uma solicitação de serviço.`;
    case 'NEW_PROPOSAL':
      return `${withActor(actorName, 'Um freelancer')} enviou um orçamento.`;
    case 'REQUEST_REJECTED':
      return `${withActor(actorName, 'O freelancer')} recusou a sua solicitação de serviço.`;
    case 'PROPOSAL_ACCEPTED':
      return `${withActor(actorName, 'O cliente')} aceitou o seu orçamento.`;
    case 'PAYMENT_REQUIRED':
      return 'O pagamento é necessário para iniciar o projeto.';
    case 'PAYMENT_APPROVED':
      return 'O pagamento do projeto foi aprovado.';
    case 'PROJECT_STARTED':
      return 'O projeto foi iniciado.';
    case 'PROJECT_COMPLETED':
      return 'O projeto foi concluído e aguarda a sua revisão.';
    case 'STATUS_UPDATED':
      return 'O status da solicitação foi atualizado.';
    case 'ADJUSTMENTS_REQUESTED':
      return `${withActor(actorName, 'O cliente')} solicitou ajustes no projeto.`;
    case 'DELIVERY_APPROVED':
      return `${withActor(actorName, 'O cliente')} aprovou a entrega do projeto.`;
    case 'ATTACHMENT_ADDED':
      return `${withActor(actorName, 'Um usuário')} adicionou ${
        attachmentCount > 1 ? `${attachmentCount} anexos` : 'um anexo'
      } ao projeto.`;
    case 'MESSAGE_RECEIVED':
      return `${withActor(actorName, 'Um usuário')} enviou uma nova mensagem.`;
    case 'PAYMENT_RECEIVED':
      return 'Você recebeu um novo pagamento.';
    default:
      return notification.description;
  }
};

export const sortNotifications = (notifications: NotificationItem[]) => {
  return [...notifications].sort(
    (left, right) => getApiDateTimestamp(right.createdAt) - getApiDateTimestamp(left.createdAt)
  );
};

export const groupNotificationsByCategory = (notifications: NotificationItem[]) => {
  const groups: Record<NotificationCategory, NotificationItem[]> = {
    projects: [],
    payments: [],
    messages: [],
  };

  for (const notification of sortNotifications(notifications)) {
    groups[notification.category].push(notification);
  }

  return groups;
};

export const formatNotificationTimestamp = (timestamp: string, nowTimestamp = Date.now()) => {
  const createdAt = parseApiDate(timestamp);
  if (Number.isNaN(createdAt.getTime())) {
    return '';
  }

  const now = new Date(nowTimestamp);
  const diffInMs = createdAt.getTime() - now.getTime();
  const isFuture = diffInMs > 0;
  const absMinutes = Math.round(Math.abs(diffInMs) / 60000);

  const formatRelativeValue = (value: number, singular: string, plural: string) => {
    const label = value === 1 ? singular : plural;
    return `${isFuture ? 'Em' : 'Há'} ${value} ${label}`;
  };

  if (absMinutes < 1) {
    return 'Agora';
  }

  if (absMinutes < 60) {
    return formatRelativeValue(absMinutes, 'minuto', 'minutos');
  }

  const absHours = Math.round(absMinutes / 60);
  if (absHours < 24) {
    return formatRelativeValue(absHours, 'hora', 'horas');
  }

  return createdAt.toLocaleString('pt-BR', {
    day: '2-digit',
    month: '2-digit',
    year: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
  });
};

const buildQuery = (key: string, value?: string | number | null): Record<string, string> | undefined => {
  if (value === undefined || value === null || value === '') {
    return undefined;
  }

  return { [key]: String(value) };
};

const resolveRouteByHint = (hint: string | undefined, metadata: NotificationMetadata): NotificationRouteTarget | null => {
  const serviceRequestId = metadata.serviceRequestId;

  switch (hint) {
    case 'checkout':
      return {
        path: '/checkout',
        query: buildQuery('requestId', serviceRequestId),
      };
    case 'cliente-request-detail':
      return {
        path: '/cliente-request-detail',
        query: buildQuery('requestId', serviceRequestId),
      };
    case 'cliente-project-detail':
      return {
        path: '/cliente-project-detail',
        query: buildQuery('requestId', serviceRequestId),
      };
    case 'cliente-delivery-approval':
      return {
        path: '/cliente-delivery-approval',
        query: buildQuery('requestId', serviceRequestId),
      };
    case 'freelancer-proposal-detail':
      return {
        path: '/freelancer-proposal-detail',
        query: buildQuery('requestId', serviceRequestId),
      };
    case 'freelancer-project-detail':
    case 'freelancer-deliveries':
      return {
        path: serviceRequestId ? '/freelancer-project-detail' : '/freelancer-deliveries',
        query: buildQuery('requestId', serviceRequestId),
      };
    case 'messages':
      return {
        path: '/messages',
        query: buildQuery('projectId', serviceRequestId),
      };
    default:
      return null;
  }
};

export const resolveNotificationRoute = (
  notification: NotificationItem,
  userType: NotificationUserType
): NotificationRouteTarget => {
  const routeByHint = resolveRouteByHint(notification.metadata.routeHint as string | undefined, notification.metadata);
  if (routeByHint) {
    return routeByHint;
  }

  if (userType === 'provider') {
    const status = typeof notification.metadata.status === 'string'
      ? notification.metadata.status.toUpperCase()
      : '';

    if (['IN_PROGRESS', 'DELIVERED', 'COMPLETED'].includes(status)) {
      return {
        path: '/freelancer-project-detail',
        query: buildQuery('requestId', notification.metadata.serviceRequestId),
      };
    }

    return {
      path: '/freelancer-proposal-detail',
      query: buildQuery('requestId', notification.metadata.serviceRequestId),
    };
  }

  return {
    path: '/notifications',
  };
};
