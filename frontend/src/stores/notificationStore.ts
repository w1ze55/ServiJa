import { defineStore } from 'pinia';
import { computed, ref } from 'vue';
import notificationService from '@/api/services/notificationService';
import type {
  NotificationItem,
  NotificationListResponse,
  NotificationStreamEvent,
} from '@/types/notifications';
import { sortNotifications } from '@/utils/notifications';
import { toast } from '@/utils/toast';

const PREVIEW_LIMIT = 8;
const PAGE_LIMIT = 100;

const cloneNotifications = (notifications: NotificationItem[]) => notifications.map((item) => ({
  ...item,
  metadata: { ...(item.metadata || {}) },
}));

export const useNotificationStore = defineStore('notifications', () => {
  const notifications = ref<NotificationItem[]>([]);
  const unreadCount = ref(0);
  const isLoadingPreview = ref(false);
  const isLoadingPage = ref(false);
  const isMutating = ref(false);
  const lastSyncAt = ref<string | null>(null);
  const hasLoadedPreview = ref(false);
  const hasLoadedPage = ref(false);
  const pendingNotificationIds = ref<number[]>([]);

  const sortedNotifications = computed(() => sortNotifications(notifications.value));
  const dropdownNotifications = computed(() => sortedNotifications.value.slice(0, PREVIEW_LIMIT));
  const hasNotifications = computed(() => notifications.value.length > 0);
  const hasUnread = computed(() => unreadCount.value > 0);

  const setNotificationsFromResponse = (
    response: NotificationListResponse,
    mode: 'preview' | 'page'
  ) => {
    unreadCount.value = response.unreadCount;
    lastSyncAt.value = new Date().toISOString();

    if (mode === 'page' || !hasLoadedPage.value) {
      notifications.value = sortNotifications(response.items);
    }

    if (mode === 'preview') {
      hasLoadedPreview.value = true;
    } else {
      hasLoadedPage.value = true;
      hasLoadedPreview.value = true;
    }
  };

  const ensurePreviewLoaded = async (force = false) => {
    if ((hasLoadedPreview.value || hasLoadedPage.value) && !force) {
      return;
    }

    isLoadingPreview.value = true;
    try {
      const response = await notificationService.getNotifications({ limit: PREVIEW_LIMIT });
      setNotificationsFromResponse(response, 'preview');
    } catch (error) {
      console.error('Erro ao carregar preview de notificações:', error);
    } finally {
      isLoadingPreview.value = false;
    }
  };

  const loadPageNotifications = async (force = true) => {
    if (hasLoadedPage.value && !force) {
      return;
    }

    isLoadingPage.value = true;
    try {
      const response = await notificationService.getNotifications({ limit: PAGE_LIMIT });
      setNotificationsFromResponse(response, 'page');
    } catch (error) {
      console.error('Erro ao carregar notificações:', error);
      toast.error('Erro ao carregar notificações');
    } finally {
      isLoadingPage.value = false;
    }
  };

  const upsertNotification = (notification: NotificationItem) => {
    const index = notifications.value.findIndex((item) => item.id === notification.id);
    if (index === -1) {
      notifications.value = sortNotifications([notification, ...notifications.value]);
      return;
    }

    const next = [...notifications.value];
    next[index] = notification;
    notifications.value = sortNotifications(next);
  };

  const removeNotificationLocally = (notificationId: number) => {
    notifications.value = notifications.value.filter((item) => item.id !== notificationId);
  };

  const setPending = (notificationId: number, active: boolean) => {
    if (active) {
      if (!pendingNotificationIds.value.includes(notificationId)) {
        pendingNotificationIds.value = [...pendingNotificationIds.value, notificationId];
      }
      return;
    }

    pendingNotificationIds.value = pendingNotificationIds.value.filter((id) => id !== notificationId);
  };

  const markNotificationAsRead = async (notificationId: number) => {
    const current = notifications.value.find((item) => item.id === notificationId);
    if (!current || current.isRead) {
      return;
    }

    const snapshot = cloneNotifications(notifications.value);
    const previousUnreadCount = unreadCount.value;
    setPending(notificationId, true);

    notifications.value = notifications.value.map((item) =>
      item.id === notificationId
        ? { ...item, isRead: true, readAt: new Date().toISOString() }
        : item
    );
    unreadCount.value = Math.max(0, unreadCount.value - 1);

    try {
      const updated = await notificationService.markAsRead(notificationId);
      upsertNotification(updated);
    } catch (error) {
      notifications.value = snapshot;
      unreadCount.value = previousUnreadCount;
      console.error('Erro ao marcar notificação como lida:', error);
      toast.error('Erro ao marcar notificação como lida');
    } finally {
      setPending(notificationId, false);
    }
  };

  const markAllNotificationsAsRead = async () => {
    if (!hasUnread.value) {
      return;
    }

    const snapshot = cloneNotifications(notifications.value);
    const previousUnreadCount = unreadCount.value;
    isMutating.value = true;

    notifications.value = notifications.value.map((item) => ({
      ...item,
      isRead: true,
      readAt: item.readAt || new Date().toISOString(),
    }));
    unreadCount.value = 0;

    try {
      await notificationService.markAllAsRead();
    } catch (error) {
      notifications.value = snapshot;
      unreadCount.value = previousUnreadCount;
      console.error('Erro ao marcar todas as notificações como lidas:', error);
      toast.error('Erro ao marcar todas como lidas');
    } finally {
      isMutating.value = false;
    }
  };

  const deleteNotification = async (notificationId: number) => {
    const target = notifications.value.find((item) => item.id === notificationId);
    if (!target) {
      return;
    }

    const snapshot = cloneNotifications(notifications.value);
    const previousUnreadCount = unreadCount.value;
    setPending(notificationId, true);

    removeNotificationLocally(notificationId);
    if (!target.isRead) {
      unreadCount.value = Math.max(0, unreadCount.value - 1);
    }

    try {
      await notificationService.deleteNotification(notificationId);
    } catch (error) {
      notifications.value = snapshot;
      unreadCount.value = previousUnreadCount;
      console.error('Erro ao excluir notificação:', error);
      toast.error('Erro ao excluir notificação');
    } finally {
      setPending(notificationId, false);
    }
  };

  const clearAllNotifications = async () => {
    if (!hasNotifications.value) {
      return;
    }

    const snapshot = cloneNotifications(notifications.value);
    const previousUnreadCount = unreadCount.value;
    isMutating.value = true;
    notifications.value = [];
    unreadCount.value = 0;

    try {
      await notificationService.clearAll();
    } catch (error) {
      notifications.value = snapshot;
      unreadCount.value = previousUnreadCount;
      console.error('Erro ao limpar notificações:', error);
      toast.error('Erro ao limpar notificações');
    } finally {
      isMutating.value = false;
    }
  };

  const applyStreamEvent = (event: NotificationStreamEvent) => {
    if (!event) {
      return;
    }

    unreadCount.value = event.unreadCount;
    lastSyncAt.value = new Date().toISOString();

    switch (event.eventType) {
      case 'CREATED':
      case 'UPDATED':
        if (event.notification) {
          upsertNotification(event.notification);
        }
        break;
      case 'DELETED':
        if (event.notificationId) {
          removeNotificationLocally(event.notificationId);
        }
        break;
      case 'READ_ALL':
        notifications.value = notifications.value.map((item) => ({
          ...item,
          isRead: true,
          readAt: item.readAt || new Date().toISOString(),
        }));
        break;
      case 'CLEARED':
        notifications.value = [];
        break;
    }
  };

  return {
    notifications,
    unreadCount,
    isLoadingPreview,
    isLoadingPage,
    isMutating,
    hasLoadedPreview,
    hasLoadedPage,
    lastSyncAt,
    pendingNotificationIds,
    sortedNotifications,
    dropdownNotifications,
    hasNotifications,
    hasUnread,
    ensurePreviewLoaded,
    loadPageNotifications,
    markNotificationAsRead,
    markAllNotificationsAsRead,
    deleteNotification,
    clearAllNotifications,
    applyStreamEvent,
  };
});
