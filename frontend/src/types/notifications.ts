export type NotificationCategory = 'messages' | 'projects' | 'payments';

export type NotificationType =
  | 'NEW_SERVICE_REQUEST'
  | 'NEW_PROPOSAL'
  | 'REQUEST_REJECTED'
  | 'PROPOSAL_ACCEPTED'
  | 'PAYMENT_REQUIRED'
  | 'PAYMENT_APPROVED'
  | 'PROJECT_STARTED'
  | 'PROJECT_COMPLETED'
  | 'STATUS_UPDATED'
  | 'ADJUSTMENTS_REQUESTED'
  | 'DELIVERY_APPROVED'
  | 'ATTACHMENT_ADDED'
  | 'MESSAGE_RECEIVED'
  | 'PAYMENT_RECEIVED';

export interface NotificationMetadata {
  serviceRequestId?: number;
  paymentId?: number;
  status?: string;
  attachmentCount?: number;
  routeHint?: string;
  [key: string]: unknown;
}

export interface NotificationItem {
  id: number;
  type: NotificationType;
  category: NotificationCategory;
  title: string;
  description: string;
  isRead: boolean;
  createdAt: string;
  readAt?: string | null;
  actorName?: string | null;
  actorAvatar?: string | null;
  metadata: NotificationMetadata;
}

export interface NotificationListResponse {
  items: NotificationItem[];
  unreadCount: number;
}

export type NotificationStreamEventType = 'CREATED' | 'UPDATED' | 'DELETED' | 'READ_ALL' | 'CLEARED';

export interface NotificationStreamEvent {
  eventType: NotificationStreamEventType;
  notification?: NotificationItem | null;
  notificationId?: number | null;
  unreadCount: number;
}

export interface NotificationQueryParams {
  limit?: number;
  read?: boolean;
  category?: NotificationCategory;
}
