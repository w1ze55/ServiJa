<script setup lang="ts">
import { onMounted, onUnmounted, ref } from 'vue';
import NotificationEmptyState from './NotificationEmptyState.vue';
import NotificationListItem from './NotificationListItem.vue';
import type { NotificationItem } from '@/types/notifications';

interface Props {
  notifications: NotificationItem[];
  compact?: boolean;
  showCategory?: boolean;
  emptyTitle: string;
  emptyDescription: string;
  pendingIds?: number[];
}

const props = withDefaults(defineProps<Props>(), {
  compact: false,
  showCategory: false,
  pendingIds: () => [],
});

const nowTimestamp = ref(Date.now());
let timerId: number | null = null;

const emit = defineEmits<{
  open: [notification: NotificationItem];
  markRead: [notification: NotificationItem];
  delete: [notification: NotificationItem];
}>();

onMounted(() => {
  timerId = window.setInterval(() => {
    nowTimestamp.value = Date.now();
  }, 60000);
});

onUnmounted(() => {
  if (timerId !== null) {
    window.clearInterval(timerId);
  }
});
</script>

<template>
  <div class="space-y-3">
    <NotificationEmptyState
      v-if="notifications.length === 0"
      :title="emptyTitle"
      :description="emptyDescription"
      :compact="compact"
    />

    <NotificationListItem
      v-for="notification in notifications"
      v-else
      :key="notification.id"
      :notification="notification"
      :now-timestamp="nowTimestamp"
      :compact="compact"
      :show-category="showCategory"
      :pending="pendingIds.includes(notification.id)"
      @open="emit('open', $event)"
      @mark-read="emit('markRead', $event)"
      @delete="emit('delete', $event)"
    />
  </div>
</template>
