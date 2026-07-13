<script setup lang="ts">
import { computed, onMounted } from 'vue';
import { useRouter } from 'vue-router';
import { Bell, CheckCheck } from 'lucide-vue-next';
import DropdownMenu from '@/components/ui/dropdown-menu.vue';
import DropdownMenuContent from '@/components/ui/dropdown-menu-content.vue';
import DropdownMenuTrigger from '@/components/ui/dropdown-menu-trigger.vue';
import Button from '@/components/ui/button.vue';
import NotificationBadge from './NotificationBadge.vue';
import NotificationEmptyState from './NotificationEmptyState.vue';
import NotificationList from './NotificationList.vue';
import NotificationSkeleton from './NotificationSkeleton.vue';
import { useNotificationStore } from '@/stores/notificationStore';
import type { NotificationItem } from '@/types/notifications';
import {
  resolveNotificationRoute,
  type NotificationUserType,
} from '@/utils/notifications';

interface Props {
  userType: NotificationUserType;
  open?: boolean;
}

const props = defineProps<Props>();
const emit = defineEmits<{
  'update:open': [value: boolean];
  'content-enter': [];
  'content-leave': [];
}>();

const router = useRouter();
const notificationStore = useNotificationStore();

const isInitialLoading = computed(
  () => notificationStore.isLoadingPreview && notificationStore.dropdownNotifications.length === 0
);

const openNotification = async (notification: NotificationItem) => {
  if (!notification.isRead) {
    await notificationStore.markNotificationAsRead(notification.id);
  }

  const target = resolveNotificationRoute(notification, props.userType);
  router.push({
    path: target.path,
    query: target.query,
  });
};

onMounted(async () => {
  await notificationStore.ensurePreviewLoaded();
});
</script>

<template>
  <DropdownMenu
    :open="props.open"
    @update:open="(value) => emit('update:open', value)"
  >
    <DropdownMenuTrigger>
      <button
        class="relative rounded-xl p-2 text-[#435574] transition-colors hover:bg-gray-100"
        @click="notificationStore.ensurePreviewLoaded(true)"
      >
        <Bell class="h-6 w-6" />
        <NotificationBadge :count="notificationStore.unreadCount" />
      </button>
    </DropdownMenuTrigger>

    <DropdownMenuContent
      align="end"
      class="w-[23.75rem] space-y-4 p-4 z-[70]"
      @mouseenter="emit('content-enter')"
      @mouseleave="emit('content-leave')"
    >
      <div class="flex items-center justify-between gap-3">
        <div>
          <h3 class="text-base font-semibold text-[#263238]">Notificações</h3>
          <p class="text-sm text-[#435574]">
            {{ notificationStore.unreadCount }} não lidas
          </p>
        </div>

        <Button
          variant="ghost"
          size="sm"
          class="text-[#286AD8] hover:bg-blue-50 hover:text-[#286AD8]"
          :disabled="notificationStore.unreadCount === 0 || notificationStore.isMutating"
          @click.stop="notificationStore.markAllNotificationsAsRead"
        >
          <CheckCheck class="mr-2 h-4 w-4" />
          Marcar todas
        </Button>
      </div>

      <div class="max-h-[26.25rem] overflow-y-auto pr-1">
        <NotificationSkeleton v-if="isInitialLoading" compact />

        <NotificationEmptyState
          v-else-if="notificationStore.dropdownNotifications.length === 0"
          title="Nenhuma notificação"
          description="Quando houver novidades, elas aparecerão aqui."
          compact
        />

        <NotificationList
          v-else
          :notifications="notificationStore.dropdownNotifications"
          compact
          show-category
          empty-title="Nenhuma notificação"
          empty-description="Quando houver novidades, elas aparecerão aqui."
          :pending-ids="notificationStore.pendingNotificationIds"
          @open="openNotification"
          @mark-read="notificationStore.markNotificationAsRead($event.id)"
          @delete="notificationStore.deleteNotification($event.id)"
        />
      </div>

      <Button
        variant="outline"
        class="w-full border-slate-200 text-[#263238] hover:bg-slate-50"
        @click="router.push('/notifications')"
      >
        Ver todas as notificações
      </Button>
    </DropdownMenuContent>
  </DropdownMenu>
</template>
