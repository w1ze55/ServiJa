<script setup lang="ts">
import { ref, computed, onMounted } from 'vue';
import { Clock, CheckCircle, XCircle, AlertCircle, Ban, DollarSign } from 'lucide-vue-next';

interface PaymentHistoryItem {
  id: number;
  status: string;
  asaasEvent: string;
  details: string;
  createdAt: string;
}

interface Props {
  paymentId: number;
}

const props = defineProps<Props>();

const history = ref<PaymentHistoryItem[]>([]);
const isLoading = ref(true);
const BASE_URL = import.meta.env.VITE_API_URL;

const statusConfig: Record<string, {
  label: string;
  icon: any;
  color: string;
  bgColor: string;
  description: string;
}> = {
  PENDING: {
    label: 'Aguardando Pagamento',
    icon: Clock,
    color: 'text-yellow-600',
    bgColor: 'bg-yellow-100',
    description: 'Pagamento criado e aguardando confirmação'
  },
  PROCESSING: {
    label: 'Processando',
    icon: Clock,
    color: 'text-blue-600',
    bgColor: 'bg-blue-100',
    description: 'Pagamento em processamento'
  },
  COMPLETED: {
    label: 'Pagamento Recebido',
    icon: CheckCircle,
    color: 'text-green-600',
    bgColor: 'bg-green-100',
    description: 'Pagamento confirmado e aprovado ✅'
  },
  FAILED: {
    label: 'Pagamento Falhou',
    icon: XCircle,
    color: 'text-red-600',
    bgColor: 'bg-red-100',
    description: 'Pagamento recusado ou falhou'
  },
  CANCELLED: {
    label: 'Pagamento Cancelado',
    icon: Ban,
    color: 'text-gray-600',
    bgColor: 'bg-gray-100',
    description: 'Pagamento foi cancelado'
  },
  OVERDUE: {
    label: 'Pagamento Vencido',
    icon: AlertCircle,
    color: 'text-orange-600',
    bgColor: 'bg-orange-100',
    description: 'Prazo de pagamento expirado'
  },
  REFUNDED: {
    label: 'Reembolsado',
    icon: DollarSign,
    color: 'text-purple-600',
    bgColor: 'bg-purple-100',
    description: 'Pagamento foi estornado'
  },
  CHARGEBACK: {
    label: 'Contestado',
    icon: AlertCircle,
    color: 'text-red-700',
    bgColor: 'bg-red-100',
    description: 'Chargeback solicitado'
  }
};

const fetchHistory = async () => {
  try {
    isLoading.value = true;
    const token = localStorage.getItem('token');
    const response = await fetch(`${BASE_URL}/api/payments/${props.paymentId}/history`, {
      headers: {
        'Authorization': `Bearer ${token}`,
        'Content-Type': 'application/json'
      }
    });

    if (response.ok) {
      history.value = await response.json();
    } else {
      console.error('Erro ao buscar histórico:', response.status);
    }
  } catch (error) {
    console.error('Erro ao buscar histórico:', error);
  } finally {
    isLoading.value = false;
  }
};

const formatDate = (dateString: string) => {
  try {
    const date = new Date(dateString);
    const day = String(date.getDate()).padStart(2, '0');
    const month = String(date.getMonth() + 1).padStart(2, '0');
    const year = date.getFullYear();
    const hours = String(date.getHours()).padStart(2, '0');
    const minutes = String(date.getMinutes()).padStart(2, '0');
    return `${day}/${month}/${year} às ${hours}:${minutes}`;
  } catch (e) {
    return dateString;
  }
};

const currentStatus = computed(() => {
  if (history.value.length === 0) return null;
  return history.value[history.value.length - 1].status;
});

onMounted(() => {
  fetchHistory();
});
</script>

<template>
  <div class="payment-timeline">
    <div v-if="isLoading" class="flex items-center justify-center py-8">
      <div class="animate-spin rounded-full h-8 w-8 border-b-2 border-blue-600"></div>
    </div>

    <div v-else-if="history.length === 0" class="text-center py-8 text-gray-500">
      Nenhum histórico disponível
    </div>

    <div v-else class="space-y-4">
      <!-- Status Atual -->
      <div v-if="currentStatus" class="mb-6">
        <div 
          :class="[
            'flex items-center gap-3 p-4 rounded-lg border-2',
            statusConfig[currentStatus]?.bgColor || 'bg-gray-100'
          ]"
        >
          <component 
            :is="statusConfig[currentStatus]?.icon || Clock"
            :class="[
              'w-6 h-6',
              statusConfig[currentStatus]?.color || 'text-gray-600'
            ]"
          />
          <div class="flex-1">
            <p class="font-semibold text-gray-900">
              {{ statusConfig[currentStatus]?.label || currentStatus }}
            </p>
            <p class="text-sm text-gray-600">
              {{ statusConfig[currentStatus]?.description || '' }}
            </p>
          </div>
        </div>
      </div>

      <!-- Linha do Tempo -->
      <div class="relative">
        <div 
          class="absolute left-4 top-0 bottom-0 w-0.5 bg-gray-200"
          style="top: 20px; bottom: 20px;"
        ></div>

        <div class="space-y-6">
          <div 
            v-for="(item, index) in history" 
            :key="item.id"
            class="relative flex gap-4"
          >
            <!-- Ícone -->
            <div 
              :class="[
                'relative z-10 flex items-center justify-center w-8 h-8 rounded-full border-2 border-white',
                statusConfig[item.status]?.bgColor || 'bg-gray-100'
              ]"
            >
              <component 
                :is="statusConfig[item.status]?.icon || Clock"
                :class="[
                  'w-4 h-4',
                  statusConfig[item.status]?.color || 'text-gray-600'
                ]"
              />
            </div>

            <!-- Conteúdo -->
            <div class="flex-1 pb-6">
              <div class="flex items-start justify-between gap-4">
                <div>
                  <p class="font-medium text-gray-900">
                    {{ statusConfig[item.status]?.label || item.status }}
                  </p>
                  <p v-if="item.details" class="text-sm text-gray-600 mt-1">
                    {{ item.details }}
                  </p>
                  <p class="text-xs text-gray-500 mt-2">
                    {{ formatDate(item.createdAt) }}
                  </p>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.payment-timeline {
  max-width: 600px;
}
</style>
