<script setup lang="ts">
import { ref, computed, onMounted, watch } from 'vue';
import {
  FileText,
  XCircle,
  CheckCircle,
  Send,
  MessageSquare,
  Clock,
  FileEdit,
  CreditCard,
  DollarSign,
  AlertCircle,
  CheckCircle2,
  Loader2,
} from 'lucide-vue-next';
import Card from '../ui/card.vue';
import api from '@/api/config/axios';

export interface HistoryEntry {
  id: string;
  action: string;
  operationKey?: string;
  user: string;
  timestamp: string;
  details?: string;
}

interface Props {
  entries?: HistoryEntry[];
  requestId?: string | number;
  title?: string;
}

const props = withDefaults(defineProps<Props>(), {
  title: 'Histórico de Atividades',
});

const fetchedEntries = ref<HistoryEntry[]>([]);
const isLoading = ref(false);

const resolvedEntries = computed(() => {
  if (props.entries && props.entries.length > 0) return props.entries;
  return fetchedEntries.value;
});

const fetchHistory = async () => {
  if (!props.requestId) return;
  isLoading.value = true;
  try {
    const response = await api.get(`/service-requests/${props.requestId}/history`);
    fetchedEntries.value = response.data.map((entry: any) => ({
      id: String(entry.id),
      action: entry.operationType,
      operationKey: entry.operationKey,
      user: entry.userName,
      timestamp: entry.formattedDateTime,
      details: entry.details,
    }));
  } catch (err) {
    console.error('Erro ao carregar histórico:', err);
  } finally {
    isLoading.value = false;
  }
};

onMounted(() => {
  if (props.requestId && (!props.entries || props.entries.length === 0)) {
    fetchHistory();
  }
});

watch(() => props.requestId, (newId) => {
  if (newId) fetchHistory();
});

const getDotClass = (key?: string) => {
  if (key === 'REQUEST_OPENED') return 'bg-blue-100 ring-2 ring-blue-300';
  if (key === 'REQUEST_REJECTED' || key === 'CANCELLED') return 'bg-red-100 ring-2 ring-red-300';
  if (key === 'BUDGET_ACCEPTED' || key === 'COUNTER_PROPOSAL_ACCEPTED') return 'bg-green-100 ring-2 ring-green-300';
  if (key === 'COUNTER_PROPOSAL_REJECTED') return 'bg-red-100 ring-2 ring-red-300';
  if (key === 'COUNTER_PROPOSAL_SENT') return 'bg-purple-100 ring-2 ring-purple-300';
  // Payment events
  if (key === 'PAYMENT_CREATED') return 'bg-blue-100 ring-2 ring-blue-300';
  if (key === 'PAYMENT_PENDING') return 'bg-yellow-100 ring-2 ring-yellow-300';
  if (key === 'PAYMENT_CONFIRMED') return 'bg-green-100 ring-2 ring-green-300';
  if (key === 'PAYMENT_CANCELLED' || key === 'PAYMENT_FAILED') return 'bg-red-100 ring-2 ring-red-300';
  if (key === 'SERVICE_COMPLETED') return 'bg-green-100 ring-2 ring-green-300';
  return 'bg-amber-100 ring-2 ring-amber-300';
};

const getLabelClass = (key?: string) => {
  if (key === 'REQUEST_OPENED') return 'text-blue-700';
  if (key === 'REQUEST_REJECTED' || key === 'COUNTER_PROPOSAL_REJECTED' || key === 'CANCELLED') return 'text-red-700';
  if (key === 'BUDGET_ACCEPTED' || key === 'COUNTER_PROPOSAL_ACCEPTED') return 'text-green-700';
  if (key === 'COUNTER_PROPOSAL_SENT') return 'text-purple-700';
  // Payment events
  if (key === 'PAYMENT_CREATED') return 'text-blue-700';
  if (key === 'PAYMENT_PENDING') return 'text-yellow-700';
  if (key === 'PAYMENT_CONFIRMED' || key === 'SERVICE_COMPLETED') return 'text-green-700';
  if (key === 'PAYMENT_CANCELLED' || key === 'PAYMENT_FAILED') return 'text-red-700';
  return 'text-amber-700';
};
</script>

<template>
  <Card class="p-6">
    <h3 class="text-lg font-semibold text-[#263238] mb-4">{{ title }}</h3>

    <div v-if="isLoading" class="flex items-center justify-center py-8">
      <Loader2 class="h-8 w-8 text-[#286AD8] animate-spin" />
    </div>

    <div v-else-if="!resolvedEntries || resolvedEntries.length === 0" class="text-center py-8">
      <Clock class="h-12 w-12 text-gray-300 mx-auto mb-3" />
      <p class="text-[#435574]">Nenhuma ação registrada</p>
    </div>

    <div v-else class="space-y-0">
      <div
        v-for="(log, index) in resolvedEntries"
        :key="log.id"
        class="relative pl-10 pb-8 last:pb-0"
      >
        <!-- Timeline line -->
        <div
          v-if="index < resolvedEntries.length - 1"
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
          <XCircle v-else-if="log.operationKey === 'REQUEST_REJECTED' || log.operationKey === 'COUNTER_PROPOSAL_REJECTED' || log.operationKey === 'CANCELLED'" class="h-4 w-4 text-red-600" />
          <CheckCircle v-else-if="log.operationKey === 'BUDGET_ACCEPTED' || log.operationKey === 'COUNTER_PROPOSAL_ACCEPTED'" class="h-4 w-4 text-green-600" />
          <Send v-else-if="log.operationKey === 'BUDGET_SENT'" class="h-4 w-4 text-amber-600" />
          <FileEdit v-else-if="log.operationKey === 'COUNTER_PROPOSAL_SENT'" class="h-4 w-4 text-purple-600" />
          <!-- Payment events -->
          <CreditCard v-else-if="log.operationKey === 'PAYMENT_CREATED'" class="h-4 w-4 text-blue-600" />
          <Clock v-else-if="log.operationKey === 'PAYMENT_PENDING'" class="h-4 w-4 text-yellow-600" />
          <DollarSign v-else-if="log.operationKey === 'PAYMENT_CONFIRMED'" class="h-4 w-4 text-green-600" />
          <AlertCircle v-else-if="log.operationKey === 'PAYMENT_CANCELLED' || log.operationKey === 'PAYMENT_FAILED'" class="h-4 w-4 text-red-600" />
          <CheckCircle2 v-else-if="log.operationKey === 'SERVICE_COMPLETED'" class="h-4 w-4 text-green-600" />
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
