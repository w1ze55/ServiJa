<script setup lang="ts">
import { ref, computed, onMounted } from 'vue';
import {
  FileText,
  Clock,
  CheckCircle,
  XCircle,
  MessageSquare,
  ArrowUpDown,
  Filter,
  Eye,
  Trash2,
  RefreshCw,
} from 'lucide-vue-next';
import Button from '../ui/button.vue';
import Badge from '../ui/badge.vue';
import Select from '../ui/select.vue';
import SelectContent from '../ui/select-content.vue';
import SelectItem from '../ui/select-item.vue';
import SelectTrigger from '../ui/select-trigger.vue';
import SelectValue from '../ui/select-value.vue';
import AlertDialog from '../ui/alert-dialog.vue';
import AlertDialogAction from '../ui/alert-dialog-action.vue';
import AlertDialogCancel from '../ui/alert-dialog-cancel.vue';
import AlertDialogContent from '../ui/alert-dialog-content.vue';
import AlertDialogDescription from '../ui/alert-dialog-description.vue';
import AlertDialogFooter from '../ui/alert-dialog-footer.vue';
import AlertDialogHeader from '../ui/alert-dialog-header.vue';
import AlertDialogTitle from '../ui/alert-dialog-title.vue';
import { toast } from '../../utils/toast';
import api from '@/api/config/axios';

interface QuoteRequest {
  id: string;
  freelancerId: string;
  freelancerName: string;
  freelancerAvatar?: string;
  service: string;
  description: string;
  budget?: number;
  deadline?: string;
  status: 'pending' | 'sent' | 'negotiating' | 'responded' | 'accepted' | 'in_progress' | 'rejected' | 'completed' | 'cancelled';
  createdAt: string;
  lastMessage?: string;
  lastMessageDate?: string;
  hasUnreadMessages?: boolean;
}

interface Props {
  onNavigate: (page: string, params?: any) => void;
}

const props = defineProps<Props>();

const filterStatus = ref<string>('all');
const sortBy = ref<string>('date-desc');
const cancelRequest = ref<QuoteRequest | null>(null);
const isLoading = ref(false);

const requests = ref<QuoteRequest[]>([]);

const loadRequests = async () => {
  isLoading.value = true;
  try {
    const response = await api.get('/service-requests/my');
    requests.value = response.data.map((req: any) => ({
      id: String(req.id),
      freelancerId: String(req.freelancerId ?? ''),
      freelancerName: req.freelancerName ?? 'Freelancer',
      freelancerAvatar: req.freelancerAvatar,
      service: req.serviceName ?? req.service ?? '',
      description: req.description ?? '',
      budget: req.budget,
      deadline: req.deadline,
      status: (req.status ?? 'pending').toLowerCase() as QuoteRequest['status'],
      createdAt: req.createdAt,
      lastMessage: req.responseScope,
      lastMessageDate: req.respondedAt,
      hasUnreadMessages: req.status?.toLowerCase() === 'negotiating' && !!req.responseScope,
    }));
  } catch (err: any) {
    toast.error('Erro ao carregar solicitações');
    console.error('Erro ao carregar solicitações:', err);
  } finally {
    isLoading.value = false;
  }
};

onMounted(() => {
  loadRequests();
});

const getStatusConfig = (status: QuoteRequest['status']) => {
  const configs = {
    pending: {
      label: 'Pendente',
      icon: Clock,
      color: 'bg-yellow-100 text-yellow-700 border-yellow-200',
      iconColor: 'text-yellow-600',
    },
    sent: {
      label: 'Enviada',
      icon: Clock,
      color: 'bg-blue-50 text-blue-600 border-blue-100',
      iconColor: 'text-blue-500',
    },
    negotiating: {
      label: 'Negociando',
      icon: MessageSquare,
      color: 'bg-amber-100 text-amber-700 border-amber-200',
      iconColor: 'text-amber-600',
    },
    responded: {
      label: 'Respondido',
      icon: MessageSquare,
      color: 'bg-blue-100 text-blue-700 border-blue-200',
      iconColor: 'text-blue-600',
    },
    accepted: {
      label: 'Aceito',
      icon: CheckCircle,
      color: 'bg-green-100 text-green-700 border-green-200',
      iconColor: 'text-green-600',
    },
    in_progress: {
      label: 'Em Andamento',
      icon: Clock,
      color: 'bg-purple-100 text-purple-700 border-purple-200',
      iconColor: 'text-purple-600',
    },
    rejected: {
      label: 'Recusado',
      icon: XCircle,
      color: 'bg-red-100 text-red-700 border-red-200',
      iconColor: 'text-red-600',
    },
    completed: {
      label: 'Concluído',
      icon: CheckCircle,
      color: 'bg-teal-100 text-teal-700 border-teal-200',
      iconColor: 'text-teal-600',
    },
    cancelled: {
      label: 'Cancelado',
      icon: XCircle,
      color: 'bg-gray-100 text-gray-700 border-gray-200',
      iconColor: 'text-gray-600',
    },
  };
  return configs[status] ?? configs['pending'];
};

const handleCancelRequest = (request: QuoteRequest) => {
  cancelRequest.value = request;
};

const confirmCancel = () => {
  if (cancelRequest.value) {
    requests.value = requests.value.map((r) =>
      r.id === cancelRequest.value!.id ? { ...r, status: 'cancelled' as const } : r
    );
    toast.success('Solicitação cancelada com sucesso');
    cancelRequest.value = null;
  }
};

const filteredRequests = computed(() =>
  requests.value.filter((r) => {
    if (filterStatus.value === 'all') return true;
    return r.status === filterStatus.value;
  })
);

const sortedRequests = computed(() => {
  const filtered = [...filteredRequests.value];
  switch (sortBy.value) {
    case 'date-desc':
      return filtered.sort(
        (a, b) => new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime()
      );
    case 'date-asc':
      return filtered.sort(
        (a, b) => new Date(a.createdAt).getTime() - new Date(b.createdAt).getTime()
      );
    case 'name-asc':
      return filtered.sort((a, b) => a.freelancerName.localeCompare(b.freelancerName));
    case 'name-desc':
      return filtered.sort((a, b) => b.freelancerName.localeCompare(a.freelancerName));
    default:
      return filtered;
  }
});

const formatDate = (dateString: string) => {
  const date = new Date(dateString);
  const now = new Date();
  const diffMs = now.getTime() - date.getTime();
  const diffHours = Math.floor(diffMs / (1000 * 60 * 60));
  const diffDays = Math.floor(diffHours / 24);

  if (diffHours < 1) return 'Agora';
  if (diffHours < 24) return `${diffHours}h atrás`;
  if (diffDays < 7) return `${diffDays}d atrás`;
  return date.toLocaleDateString('pt-BR');
};

const getSortLabel = (value: string) => {
  const options: Record<string, string> = {
    'date-desc': 'Mais recentes',
    'date-asc': 'Mais antigas',
    'name-asc': 'Nome (A-Z)',
    'name-desc': 'Nome (Z-A)',
  };
  return options[value] || value;
};

const getFilterStatusLabel = (value: string) => {
  const labels: Record<string, string> = {
    all: 'Todos os status',
    pending: 'Pendente',
    responded: 'Respondido',
    accepted: 'Aceito',
    rejected: 'Recusado',
    cancelled: 'Cancelado',
  };
  return labels[value] || value;
};
</script>

<template>
  <div class="max-w-7xl mx-auto space-y-6">
    <!-- Header -->
    <div>
      <h1 class="text-[#263238] mb-2">Minhas Solicitações</h1>
      <p class="text-[#435574]">Acompanhe todas as suas solicitações de orçamento</p>
    </div>

    <!-- Filters -->
    <div class="bg-white rounded-lg border border-gray-200 p-4">
      <div class="flex flex-col md:flex-row gap-4">
        <div class="flex-1">
          <Select :model-value="filterStatus" @update:model-value="(val: string) => filterStatus = val">
            <SelectTrigger>
              <Filter class="h-4 w-4 mr-2" />
              <SelectValue :label="getFilterStatusLabel(filterStatus)" />
            </SelectTrigger>
            <SelectContent>
              <SelectItem value="all">Todos os status</SelectItem>
              <SelectItem value="pending">Pendente</SelectItem>
              <SelectItem value="responded">Respondido</SelectItem>
              <SelectItem value="accepted">Aceito</SelectItem>
              <SelectItem value="rejected">Recusado</SelectItem>
              <SelectItem value="cancelled">Cancelado</SelectItem>
            </SelectContent>
          </Select>
        </div>

        <div class="flex-1">
          <Select :model-value="sortBy" @update:model-value="(val: string) => sortBy = val">
            <SelectTrigger>
              <ArrowUpDown class="h-4 w-4 mr-2" />
              <SelectValue :label="getSortLabel(sortBy)" />
            </SelectTrigger>
            <SelectContent>
              <SelectItem value="date-desc">Mais recentes</SelectItem>
              <SelectItem value="date-asc">Mais antigas</SelectItem>
              <SelectItem value="name-asc">Nome (A-Z)</SelectItem>
              <SelectItem value="name-desc">Nome (Z-A)</SelectItem>
            </SelectContent>
          </Select>
        </div>
      </div>
    </div>

    <!-- Results count -->
    <p class="text-[#435574]">
      {{ sortedRequests.length }}
      {{ sortedRequests.length === 1 ? 'solicitação' : 'solicitações' }}
    </p>

    <!-- Requests Table - Desktop -->
    <div class="hidden md:block bg-white rounded-lg border border-gray-200 overflow-hidden">
      <table class="w-full">
        <thead class="bg-gray-50 border-b border-gray-200">
          <tr>
            <th class="text-left py-3 px-4 text-[#263238]">Freelancer / Serviço</th>
            <th class="text-left py-3 px-4 text-[#263238]">Status</th>
            <th class="text-left py-3 px-4 text-[#263238]">Última atualização</th>
            <th class="text-right py-3 px-4 text-[#263238]">Ações</th>
          </tr>
        </thead>
        <tbody>
          <tr
            v-for="request in sortedRequests"
            :key="request.id"
            class="border-b border-gray-100 hover:bg-gray-50 transition-colors"
          >
            <td class="py-4 px-4">
              <div>
                <p class="text-[#263238] mb-1">{{ request.freelancerName }}</p>
                <p class="text-sm text-[#435574]">{{ request.service }}</p>
                <p v-if="request.lastMessage" class="text-sm text-[#435574] mt-1 line-clamp-1 italic">
                  "{{ request.lastMessage }}"
                </p>
              </div>
            </td>
            <td class="py-4 px-4">
              <Badge
                :class="`${getStatusConfig(request.status).color} border flex items-center gap-1 w-fit`"
              >
                <component
                  :is="getStatusConfig(request.status).icon"
                  :class="`h-3 w-3 ${getStatusConfig(request.status).iconColor}`"
                />
                {{ getStatusConfig(request.status).label }}
              </Badge>
            </td>
            <td class="py-4 px-4">
              <div>
                <p class="text-sm text-[#435574]">
                  {{ formatDate(request.lastMessageDate || request.createdAt) }}
                </p>
                <div v-if="request.hasUnreadMessages" class="flex items-center gap-1 mt-1">
                  <div class="w-2 h-2 bg-[#286AD8] rounded-full" />
                  <span class="text-xs text-[#286AD8]">Nova mensagem</span>
                </div>
              </div>
            </td>
            <td class="py-4 px-4">
              <div class="flex items-center justify-end gap-2">
                <Button
                  @click="
                    props.onNavigate('quote-detail', {
                      requestId: request.id,
                      freelancerId: request.freelancerId,
                    })
                  "
                  size="sm"
                  class="bg-[#286AD8] hover:bg-[#435574]"
                >
                  <Eye class="h-4 w-4 mr-2" />
                  Ver detalhes
                </Button>
                <Button
                  v-if="request.status === 'pending'"
                  @click="handleCancelRequest(request)"
                  size="sm"
                  variant="outline"
                  class="text-red-600 border-red-600 hover:bg-red-50"
                >
                  <Trash2 class="h-4 w-4" />
                </Button>
              </div>
            </td>
          </tr>
        </tbody>
      </table>

      <div v-if="sortedRequests.length === 0" class="text-center py-12">
        <FileText class="h-12 w-12 text-[#435574] mx-auto mb-4" />
        <p class="text-[#435574] mb-2">Nenhuma solicitação encontrada</p>
        <p class="text-sm text-[#435574]">
          {{ filterStatus !== 'all' ? 'Tente alterar os filtros aplicados' : 'Você ainda não fez nenhuma solicitação de orçamento' }}
        </p>
      </div>
    </div>

    <!-- Requests Cards - Mobile -->
    <div class="md:hidden space-y-4">
      <div
        v-for="request in sortedRequests"
        :key="request.id"
        class="bg-white rounded-lg border border-gray-200 p-4 space-y-3"
      >
        <div class="flex items-start justify-between">
          <div class="flex-1">
            <h4 class="text-[#263238] mb-1">{{ request.freelancerName }}</h4>
            <p class="text-sm text-[#435574]">{{ request.service }}</p>
          </div>
          <Badge
            :class="`${getStatusConfig(request.status).color} border flex items-center gap-1`"
          >
            <component
              :is="getStatusConfig(request.status).icon"
              :class="`h-3 w-3 ${getStatusConfig(request.status).iconColor}`"
            />
            {{ getStatusConfig(request.status).label }}
          </Badge>
        </div>

        <p v-if="request.lastMessage" class="text-sm text-[#435574] italic line-clamp-2">
          "{{ request.lastMessage }}"
        </p>

        <div class="flex items-center justify-between text-sm text-[#435574] pt-2 border-t border-gray-200">
          <span>{{ formatDate(request.lastMessageDate || request.createdAt) }}</span>
          <div v-if="request.hasUnreadMessages" class="flex items-center gap-1">
            <div class="w-2 h-2 bg-[#286AD8] rounded-full" />
            <span class="text-xs text-[#286AD8]">Nova mensagem</span>
          </div>
        </div>

        <div class="flex gap-2">
          <Button
            @click="
              props.onNavigate('quote-detail', {
                requestId: request.id,
                freelancerId: request.freelancerId,
              })
            "
            size="sm"
            class="flex-1 bg-[#286AD8] hover:bg-[#435574]"
          >
            Ver detalhes
          </Button>
          <Button
            v-if="request.status === 'pending'"
            @click="handleCancelRequest(request)"
            size="sm"
            variant="outline"
            class="text-red-600 border-red-600 hover:bg-red-50"
          >
            Cancelar
          </Button>
        </div>
      </div>

      <div v-if="sortedRequests.length === 0" class="text-center py-12 bg-white rounded-lg border border-gray-200">
        <FileText class="h-12 w-12 text-[#435574] mx-auto mb-4" />
        <p class="text-[#435574] mb-2">Nenhuma solicitação encontrada</p>
        <p class="text-sm text-[#435574]">
          {{ filterStatus !== 'all' ? 'Tente alterar os filtros aplicados' : 'Você ainda não fez nenhuma solicitação de orçamento' }}
        </p>
      </div>
    </div>

    <!-- Cancel Confirmation Dialog -->
    <AlertDialog :open="cancelRequest !== null" @update:open="(val) => cancelRequest = val ? cancelRequest : null">
      <AlertDialogContent>
        <AlertDialogHeader>
          <AlertDialogTitle>Cancelar solicitação</AlertDialogTitle>
          <AlertDialogDescription>
            Tem certeza que deseja cancelar a solicitação para
            <strong>{{ cancelRequest?.freelancerName }}</strong>?
            <br />
            <br />
            Esta ação não pode ser desfeita.
          </AlertDialogDescription>
        </AlertDialogHeader>
        <AlertDialogFooter>
          <AlertDialogCancel>Voltar</AlertDialogCancel>
          <AlertDialogAction @click="confirmCancel" class="bg-red-600 hover:bg-red-700">
            Confirmar cancelamento
          </AlertDialogAction>
        </AlertDialogFooter>
      </AlertDialogContent>
    </AlertDialog>
  </div>
</template>



