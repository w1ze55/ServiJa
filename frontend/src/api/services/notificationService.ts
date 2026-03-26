import api from '../config/axios';
import type {
  NotificationListResponse,
  NotificationQueryParams,
  NotificationItem,
} from '@/types/notifications';

const notificationService = {
  async getNotifications(params: NotificationQueryParams = {}): Promise<NotificationListResponse> {
    const response = await api.get<NotificationListResponse>('/notifications', { params });
    return response.data;
  },

  async markAsRead(notificationId: number): Promise<NotificationItem> {
    const response = await api.patch<NotificationItem>(`/notifications/${notificationId}/read`);
    return response.data;
  },

  async markAllAsRead(): Promise<void> {
    await api.patch('/notifications/read-all');
  },

  async deleteNotification(notificationId: number): Promise<void> {
    await api.delete(`/notifications/${notificationId}`);
  },

  async clearAll(): Promise<void> {
    await api.delete('/notifications');
  },
};

export default notificationService;
