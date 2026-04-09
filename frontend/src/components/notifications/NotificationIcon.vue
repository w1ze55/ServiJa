<script setup lang="ts">
import { computed } from 'vue';
import {
  Bell,
  BriefcaseBusiness,
  CheckCheck,
  CircleDollarSign,
  FileText,
  MessageSquare,
  Paperclip,
  RefreshCcw,
  X,
} from 'lucide-vue-next';
import type { NotificationItem } from '@/types/notifications';

interface Props {
  notification: NotificationItem;
  compact?: boolean;
}

const props = withDefaults(defineProps<Props>(), {
  compact: false,
});

const iconComponent = computed(() => {
  switch (props.notification.type) {
    case 'NEW_SERVICE_REQUEST':
      return BriefcaseBusiness;
    case 'NEW_PROPOSAL':
    case 'PROPOSAL_ACCEPTED':
      return FileText;
    case 'REQUEST_REJECTED':
      return X;
    case 'PAYMENT_REQUIRED':
    case 'PAYMENT_APPROVED':
    case 'PAYMENT_RECEIVED':
      return CircleDollarSign;
    case 'MESSAGE_RECEIVED':
      return MessageSquare;
    case 'ATTACHMENT_ADDED':
      return Paperclip;
    case 'STATUS_UPDATED':
    case 'ADJUSTMENTS_REQUESTED':
      return RefreshCcw;
    case 'PROJECT_STARTED':
    case 'PROJECT_COMPLETED':
    case 'DELIVERY_APPROVED':
      return CheckCheck;
    default:
      return Bell;
  }
});

const palette = computed(() => {
  if (props.notification.type === 'REQUEST_REJECTED') {
    return {
      wrapper: 'bg-red-100',
      icon: 'text-red-700',
    };
  }

  switch (props.notification.category) {
    case 'payments':
      return {
        wrapper: 'bg-emerald-100',
        icon: 'text-emerald-700',
      };
    case 'messages':
      return {
        wrapper: 'bg-sky-100',
        icon: 'text-sky-700',
      };
    default:
      return {
        wrapper: 'bg-blue-100',
        icon: 'text-blue-700',
      };
  }
});
</script>

<template>
  <div
    :class="[
      'flex items-center justify-center rounded-full flex-shrink-0',
      compact ? 'h-9 w-9' : 'h-11 w-11',
      palette.wrapper,
    ]"
  >
    <component :is="iconComponent" :class="[compact ? 'h-4 w-4' : 'h-5 w-5', palette.icon]" />
  </div>
</template>
