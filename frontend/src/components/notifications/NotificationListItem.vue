<script setup lang="ts">
import { computed } from 'vue';
import { Check, Trash2 } from 'lucide-vue-next';
import Badge from '@/components/ui/badge.vue';
import Button from '@/components/ui/button.vue';
import NotificationIcon from './NotificationIcon.vue';
import type { NotificationItem } from '@/types/notifications';
import {
  formatNotificationTimestamp,
  getNotificationDescription,
  getNotificationCategoryLabel,
} from '@/utils/notifications';

interface Props {
  notification: NotificationItem;
  nowTimestamp?: number;
  compact?: boolean;
  showCategory?: boolean;
  pending?: boolean;
}

const props = withDefaults(defineProps<Props>(), {
  nowTimestamp: () => Date.now(),
  compact: false,
  showCategory: false,
  pending: false,
});

const emit = defineEmits<{
  open: [notification: NotificationItem];
  markRead: [notification: NotificationItem];
  delete: [notification: NotificationItem];
}>();

const isRejectedNotification = computed(() => props.notification.type === 'REQUEST_REJECTED');

const itemClasses = computed(() => [
  'group rounded-xl border transition-all',
  props.compact ? 'p-3' : 'p-4',
  props.notification.isRead
    ? 'border-slate-200 bg-white hover:border-slate-300 hover:shadow-sm'
    : isRejectedNotification.value
      ? 'border-red-200 bg-red-50/70 hover:border-red-300 hover:shadow-sm'
      : 'border-blue-200 bg-blue-50/70 hover:border-blue-300 hover:shadow-sm',
  props.pending ? 'opacity-70 pointer-events-none' : '',
]);

const markReadButtonClasses = computed(() =>
  isRejectedNotification.value
    ? 'h-8 w-8 text-slate-500 hover:bg-red-100 hover:text-red-700'
    : 'h-8 w-8 text-slate-500 hover:bg-blue-100 hover:text-blue-700'
);

const unreadIndicatorClasses = computed(() =>
  isRejectedNotification.value ? 'ml-auto h-2.5 w-2.5 rounded-full bg-red-500' : 'ml-auto h-2.5 w-2.5 rounded-full bg-[#286AD8]'
);

const timestamp = computed(() =>
  formatNotificationTimestamp(props.notification.createdAt, props.nowTimestamp)
);

const description = computed(() => getNotificationDescription(props.notification));
</script>

<template>
  <div
    :class="itemClasses"
    role="button"
    tabindex="0"
    @click="emit('open', notification)"
    @keydown.enter.prevent="emit('open', notification)"
  >
    <div
      v-if="compact"
      class="flex items-center gap-3"
    >
      <NotificationIcon :notification="notification" :compact="compact" />

      <div class="min-w-0 flex-1">
        <div class="flex items-start justify-between gap-3">
          <div class="min-w-0">
            <div class="flex items-center gap-2">
              <p
                :class="[
                  'truncate text-sm',
                  notification.isRead ? 'text-[#435574]' : 'font-semibold text-[#263238]',
                ]"
              >
                {{ notification.title }}
              </p>
              <Badge
                v-if="showCategory"
                variant="secondary"
                class="bg-slate-100 text-slate-600 hover:bg-slate-100"
              >
                {{ getNotificationCategoryLabel(notification.category) }}
              </Badge>
            </div>

            <p class="mt-1 text-sm text-[#435574]">
              {{ description }}
            </p>
          </div>

          <div class="flex min-w-[40px] flex-col items-center gap-1">
            <div
              v-if="!notification.isRead"
              :class="unreadIndicatorClasses"
            />
            <Button
              variant="ghost"
              size="icon"
              class="h-8 w-8 text-slate-500 hover:bg-red-100 hover:text-red-700"
              @click.stop="emit('delete', notification)"
            >
              <Trash2 class="h-4 w-4" />
            </Button>
          </div>
        </div>

        <div class="mt-1 flex items-center justify-between gap-3">
          <Button
            variant="link"
            size="sm"
            class="h-auto p-0 text-[#286AD8] hover:text-[#1f4ea7]"
            @click.stop="emit('open', notification)"
          >
            Ver detalhes
          </Button>

          <span class="min-w-[72px] text-center text-xs text-slate-500">
            {{ timestamp }}
          </span>
        </div>
      </div>
    </div>

    <div
      v-else
      class="flex items-center gap-3"
    >
      <NotificationIcon :notification="notification" :compact="compact" />

      <div class="min-w-0 flex-1">
        <div class="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
          <div class="min-w-0">
            <div class="flex flex-wrap items-center gap-2">
              <p
                :class="[
                  'truncate text-[15px]',
                  notification.isRead ? 'text-[#435574]' : 'font-semibold text-[#263238]',
                ]"
              >
                {{ notification.title }}
              </p>
              <Badge
                v-if="showCategory"
                variant="secondary"
                class="bg-slate-100 text-slate-600 hover:bg-slate-100"
              >
                {{ getNotificationCategoryLabel(notification.category) }}
              </Badge>
            </div>

            <p class="mt-1 text-sm text-[#435574]">
              {{ description }}
            </p>

            <Button
              variant="link"
              size="sm"
              class="mt-1 h-auto p-0 text-[#286AD8] hover:text-[#1f4ea7]"
              @click.stop="emit('open', notification)"
            >
              Ver detalhes
            </Button>
          </div>

          <div class="flex items-center gap-2 self-start sm:self-center">
            <span class="text-xs text-slate-500">{{ timestamp }}</span>
            <div
              v-if="!notification.isRead"
              :class="unreadIndicatorClasses"
            />
            <Button
              v-if="!notification.isRead"
              variant="ghost"
              size="icon"
              :class="markReadButtonClasses"
              @click.stop="emit('markRead', notification)"
            >
              <Check class="h-4 w-4" />
            </Button>
            <Button
              variant="ghost"
              size="icon"
              class="h-8 w-8 text-slate-500 hover:bg-red-100 hover:text-red-700"
              @click.stop="emit('delete', notification)"
            >
              <Trash2 class="h-4 w-4" />
            </Button>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>
