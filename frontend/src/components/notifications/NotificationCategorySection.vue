<script setup lang="ts">
import NotificationList from './NotificationList.vue';
import type { NotificationCategory, NotificationItem } from '@/types/notifications';
import { getNotificationCategoryLabel } from '@/utils/notifications';

interface Props {
  category: NotificationCategory;
  notifications: NotificationItem[];
  pendingIds?: number[];
}

withDefaults(defineProps<Props>(), {
  pendingIds: () => [],
});

const emit = defineEmits<{
  open: [notification: NotificationItem];
  markRead: [notification: NotificationItem];
  delete: [notification: NotificationItem];
}>();
</script>

<template>
  <section class="space-y-4 rounded-2xl border border-slate-200 bg-white p-5 shadow-sm">
    <div class="flex items-center justify-between gap-3">
      <div>
        <h2 class="text-lg font-semibold text-[#263238]">
          {{ getNotificationCategoryLabel(category) }}
        </h2>
        <p class="text-sm text-[#435574]">
          {{ notifications.length }} {{ notifications.length === 1 ? 'notificação' : 'notificações' }}
        </p>
      </div>
    </div>

    <NotificationList
      :notifications="notifications"
      empty-title="Nada por aqui"
      empty-description="Quando houver novas atualizações nesta categoria, elas aparecerão aqui."
      :pending-ids="pendingIds"
      @open="emit('open', $event)"
      @mark-read="emit('markRead', $event)"
      @delete="emit('delete', $event)"
    />
  </section>
</template>
