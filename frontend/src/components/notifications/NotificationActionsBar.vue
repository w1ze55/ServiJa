<script setup lang="ts">
import { CheckCheck, Trash2 } from 'lucide-vue-next';
import Button from '@/components/ui/button.vue';

interface Props {
  totalCount: number;
  unreadCount: number;
  isBusy?: boolean;
}

withDefaults(defineProps<Props>(), {
  isBusy: false,
});

const emit = defineEmits<{
  markAllRead: [];
  clearAll: [];
}>();
</script>

<template>
  <div class="flex flex-wrap items-center justify-between gap-3 rounded-2xl border border-slate-200 bg-white p-4 shadow-sm">
    <div>
      <p class="text-sm font-semibold text-[#263238]">
        {{ totalCount }} {{ totalCount === 1 ? 'notificação' : 'notificações' }}
      </p>
      <p class="text-sm text-[#435574]">
        {{ unreadCount }} {{ unreadCount === 1 ? 'não lida' : 'não lidas' }}
      </p>
    </div>

    <div class="flex flex-wrap items-center gap-2">
      <Button
        variant="outline"
        size="sm"
        :disabled="isBusy || unreadCount === 0"
        @click="emit('markAllRead')"
      >
        <CheckCheck class="mr-2 h-4 w-4" />
        Marcar todas como lidas
      </Button>
      <Button
        variant="outline"
        size="sm"
        class="border-red-200 text-red-600 hover:bg-red-50 hover:text-red-700"
        :disabled="isBusy || totalCount === 0"
        @click="emit('clearAll')"
      >
        <Trash2 class="mr-2 h-4 w-4" />
        Limpar todas
      </Button>
    </div>
  </div>
</template>
