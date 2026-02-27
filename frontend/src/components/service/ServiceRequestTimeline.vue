<script setup lang="ts">
import {
  FileText,
  XCircle,
  CheckCircle,
  Send,
  MessageSquare,
  Clock,
} from 'lucide-vue-next';
import Card from '../ui/card.vue';

export interface HistoryEntry {
  id: string;
  action: string;
  operationKey?: string;
  user: string;
  timestamp: string;
  details?: string;
}

interface Props {
  entries: HistoryEntry[];
  title?: string;
}

const props = withDefaults(defineProps<Props>(), {
  title: 'Histórico de Ações',
});

const getDotClass = (key?: string) => {
  if (key === 'REQUEST_OPENED') return 'bg-blue-100 ring-2 ring-blue-300';
  if (key === 'REQUEST_REJECTED') return 'bg-red-100 ring-2 ring-red-300';
  if (key === 'BUDGET_ACCEPTED') return 'bg-green-100 ring-2 ring-green-300';
  return 'bg-amber-100 ring-2 ring-amber-300';
};

const getLabelClass = (key?: string) => {
  if (key === 'REQUEST_OPENED') return 'text-blue-700';
  if (key === 'REQUEST_REJECTED') return 'text-red-700';
  if (key === 'BUDGET_ACCEPTED') return 'text-green-700';
  return 'text-amber-700';
};
</script>

<template>
  <Card class="p-6">
    <h3 class="text-lg font-semibold text-[#263238] mb-4">{{ title }}</h3>

    <div v-if="!entries || entries.length === 0" class="text-center py-8">
      <Clock class="h-12 w-12 text-gray-300 mx-auto mb-3" />
      <p class="text-[#435574]">Nenhuma ação registrada</p>
    </div>

    <div v-else class="space-y-0">
      <div
        v-for="(log, index) in entries"
        :key="log.id"
        class="relative pl-10 pb-8 last:pb-0"
      >
        <!-- Timeline line -->
        <div
          v-if="index < entries.length - 1"
          class="absolute left-[15px] top-8 w-0.5 h-[calc(100%-16px)] bg-gray-200"
        />

        <!-- Timeline dot with icon -->
        <div
          :class="[
            'absolute left-0 top-0.5 w-8 h-8 rounded-full flex items-center justify-center shadow-sm',
            getDotClass(log.operationKey),
          ]"
        >
          <FileText v-if="log.operationKey === 'REQUEST_OPENED'" class="h-4 w-4 text-blue-600" />
          <XCircle v-else-if="log.operationKey === 'REQUEST_REJECTED'" class="h-4 w-4 text-red-600" />
          <CheckCircle v-else-if="log.operationKey === 'BUDGET_ACCEPTED'" class="h-4 w-4 text-green-600" />
          <Send v-else-if="log.operationKey === 'BUDGET_SENT'" class="h-4 w-4 text-amber-600" />
          <MessageSquare v-else class="h-4 w-4 text-amber-600" />
        </div>

        <!-- Content -->
        <div class="min-h-[2rem]">
          <div class="flex items-start justify-between gap-4">
            <p :class="['font-medium text-sm', getLabelClass(log.operationKey)]">
              {{ log.action }}
            </p>
            <span class="text-xs text-[#435574] whitespace-nowrap">{{ log.timestamp }}</span>
          </div>
          <p class="text-xs text-[#435574] mt-0.5">{{ log.user }}</p>
          <p v-if="log.details" class="text-sm text-[#435574] mt-1">{{ log.details }}</p>
        </div>
      </div>
    </div>
  </Card>
</template>
