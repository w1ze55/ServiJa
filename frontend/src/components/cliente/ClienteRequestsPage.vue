<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted, watch } from 'vue';
import {
  FileText,
  Search,
  Clock,
  DollarSign,
  MapPin,
  Calendar,
  Eye,
  MessageSquare,
  CheckCircle,
  XCircle,
  Archive,
  Plus,
  RefreshCw,
  Send,
  X,
} from 'lucide-vue-next';
import Button from '../ui/button.vue';
import Input from '../ui/input.vue';
import Card from '../ui/card.vue';
import Badge from '../ui/badge.vue';
import Avatar from '../ui/avatar.vue';
import AvatarFallback from '../ui/avatar-fallback.vue';
import AvatarImage from '../ui/avatar-image.vue';
import Select from '../ui/select.vue';
import SelectContent from '../ui/select-content.vue';
import SelectItem from '../ui/select-item.vue';
import SelectTrigger from '../ui/select-trigger.vue';
import SelectValue from '../ui/select-value.vue';
import Tabs from '../ui/tabs.vue';
import TabsContent from '../ui/tabs-content.vue';
import TabsList from '../ui/tabs-list.vue';
import TabsTrigger from '../ui/tabs-trigger.vue';
import Dialog from '../ui/dialog.vue';
import DialogContent from '../ui/dialog-content.vue';
import DialogDescription from '../ui/dialog-description.vue';
import DialogFooter from '../ui/dialog-footer.vue';
import DialogHeader from '../ui/dialog-header.vue';
import DialogTitle from '../ui/dialog-title.vue';
import AlertDialog from '../ui/alert-dialog.vue';
import AlertDialogAction from '../ui/alert-dialog-action.vue';
import AlertDialogCancel from '../ui/alert-dialog-cancel.vue';
import AlertDialogContent from '../ui/alert-dialog-content.vue';
import AlertDialogDescription from '../ui/alert-dialog-description.vue';
import AlertDialogFooter from '../ui/alert-dialog-footer.vue';
import AlertDialogHeader from '../ui/alert-dialog-header.vue';
import AlertDialogTitle from '../ui/alert-dialog-title.vue';
import Textarea from '../ui/textarea.vue';
import Label from '../ui/label.vue';
import WorkModelBadge from '../shared/WorkModelBadge.vue';
import { useClientRequestStore, type ServiceRequest } from '@/stores/clientRequestStore';
import { useServiceRequestWebSocket } from '@/composables/useServiceRequestWebSocket';
import { formatApiDate, getApiDateTimestamp } from '@/utils/apiDate';
import { toast } from '@/utils/toast';
import { getStatusLabel } from '@/utils/statusTranslations';
import { validateValueRange, extractApiErrorMessage } from '@/utils/valueValidation';
import { type WorkModelValue } from '@/utils/workModel';

interface Props {
  onNavigate: (page: string, params?: any) => void;
}

const props = defineProps<Props>();
const store = useClientRequestStore();

const searchQuery = ref('');
const categoryFilter = ref('all');
const sortBy = ref('recent');
const currentTab = ref('all');
const currentPage = ref(1);
const itemsPerPage = 6;

// Create dialog
const showCreateDialog = ref(false);
const isCreating = ref(false);
const newRequest = ref({
  service: '',
  category: '',
  description: '',
  budget: 0,
  deadline: '',
  workModel: '' as WorkModelValue | '',
  address: '',
  desiredDate: '',
});

// Cancel dialog
const showCancelDialog = ref(false);
const selectedRequest = ref<ServiceRequest | null>(null);
const cancelReason = ref('');

// WebSocket
let wsConnection: any = null;

const categories = ['Eletricista', 'Encanador', 'Pintor', 'Pedreiro', 'Designer', 'Desenvolvedor'];

// Mapeamento de valores de ordenação para português
const sortOptions = {
  recent: 'Mais recentes',
  'budget-high': 'Maior orçamento',
  'budget-low': 'Menor orçamento'
};

const getSortLabel = (value: string) => {
  return sortOptions[value as keyof typeof sortOptions] || value;
};

// Obter mensagem de estado vazio baseada na tab atual
const getEmptyMessage = () => {
  const messages = {
    all: 'Você ainda não criou nenhuma solicitação',
    pending: 'Não há solicitações pendentes no momento',
    negotiating: 'Não há solicitações em negociação',
    accepted: 'Não há solicitações em andamento',
    completed: 'Não há solicitações concluídas',
    cancelled: 'Não há solicitações canceladas'
  };

  return messages[currentTab.value as keyof typeof messages] || 'Nenhuma solicitação encontrada';
};

onMounted(() => {
  store.loadRequests();

  // Conectar WebSocket
  const userEmail = getUserEmail();
  console.log('🔍 Tentando conectar WebSocket para:', userEmail);

  if (userEmail) {
    wsConnection = useServiceRequestWebSocket(userEmail);
    wsConnection.connect().catch((error: any) => {
      console.error('❌ Erro ao conectar WebSocket:', error);
    });

    console.log('✅ WebSocket inicializado para solicitações (Cliente)');

    // Escutar notificações de solicitações
    window.addEventListener('service-request-notification', handleServiceRequestNotification);
  } else {
    console.error('❌ Não foi possível extrair email do token');
  }
});

onUnmounted(() => {
  console.log('🔌 Desconectando WebSocket de solicitações (Cliente)');
  window.removeEventListener('service-request-notification', handleServiceRequestNotification);
  if (wsConnection) {
    wsConnection.disconnect();
  }
});

// Extrair email do usuário do JWT
const getUserEmail = () => {
  const token = localStorage.getItem('token');
  if (!token) return null;

  try {
    const payload = JSON.parse(atob(token.split('.')[1]));
    return payload.sub || payload.email;
  } catch (error) {
    console.error('Erro ao extrair email do token:', error);
    return null;
  }
};

// Lidar com notificações de solicitações via WebSocket
const handleServiceRequestNotification = (event: any) => {
  const notification = event.detail;
  console.log('📩 Notificação recebida:', notification);

  // Mostrar toast baseado no tipo de notificação
  switch (notification.type) {
    case 'NEW_PROPOSAL':
      toast.success(`${notification.freelancerName} enviou uma proposta de ${notification.proposalAmount}!`);
      // Recarregar lista de solicitações
      store.loadRequests();
      break;
    case 'COUNTER_PROPOSAL_REJECTED':
      toast.info(`${notification.freelancerName} recusou sua contra-proposta`);
      store.loadRequests();
      break;
    case 'PROPOSAL_ACCEPTED':
      toast.success(notification.message);
      store.loadRequests();
      break;
    case 'SERVICE_REQUEST_CANCELLED':
      toast.warning('Solicitação cancelada');
      store.loadRequests();
      break;
    case 'SERVICE_REQUEST_STATUS_CHANGED':
      if (notification.status === 'DELIVERED') {
        toast.success('Serviço entregue! O freelancer marcou o serviço como concluído');
      } else if (notification.status === 'IN_PROGRESS') {
        toast.success('Pagamento confirmado! O serviço está em andamento');
      } else if (notification.status === 'COMPLETED') {
        toast.success('Serviço concluído com sucesso!');
      } else {
        toast.info(`Status atualizado: ${getStatusLabel(notification.status)}`);
      }
      store.loadRequests();
      break;
    case 'SERVICE_COMPLETED_REVIEW_PENDING':
      toast.success('Serviço concluído! Avalie o freelancer.');
      break;
    case 'NEW_REVIEW':
      toast.info(notification.reviewerName + ' enviou uma avaliação!');
      break;
  }
};

const filteredRequests = computed(() => {
  let filtered = store.requests.filter((request) => {
    const matchesSearch =
      request.service.toLowerCase().includes(searchQuery.value.toLowerCase()) ||
      request.description.toLowerCase().includes(searchQuery.value.toLowerCase()) ||
      (request.freelancerName || '').toLowerCase().includes(searchQuery.value.toLowerCase());
    const matchesCategory = categoryFilter.value === 'all' || request.category === categoryFilter.value;
    const matchesTab =
      currentTab.value === 'all' ||
      (currentTab.value === 'pending' && (request.status === 'pending' || request.status === 'sent')) ||
      (currentTab.value === 'negotiating' && (request.status === 'negotiating' || request.status === 'accepted')) ||
      (currentTab.value === 'in_progress' && (request.status === 'in_progress' || request.status === 'delivered')) ||
      (currentTab.value === 'completed' && request.status === 'completed') ||
      (currentTab.value === 'cancelled' && (request.status === 'cancelled' || request.status === 'rejected'));

    return matchesSearch && matchesCategory && matchesTab;
  });

  if (sortBy.value === 'recent') {
    filtered.sort((a, b) => getApiDateTimestamp(getLastActivityAt(b)) - getApiDateTimestamp(getLastActivityAt(a)));
  } else if (sortBy.value === 'budget-high') {
    filtered.sort((a, b) => b.budget - a.budget);
  } else if (sortBy.value === 'budget-low') {
    filtered.sort((a, b) => a.budget - b.budget);
  }

  return filtered;
});

const totalPages = computed(() => Math.ceil(filteredRequests.value.length / itemsPerPage));
const startIndex = computed(() => (currentPage.value - 1) * itemsPerPage);
const currentRequests = computed(() =>
  filteredRequests.value.slice(startIndex.value, startIndex.value + itemsPerPage)
);

const formatCurrency = (value: number) => {
  return new Intl.NumberFormat('pt-BR', {
    style: 'currency',
    currency: 'BRL',
  }).format(value);
};

const formatDate = (dateString: string) => {
  return formatApiDate(dateString, {
    day: '2-digit',
    month: '2-digit',
    year: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
  });
};

const getLastActivityAt = (request: ServiceRequest) => request.updatedAt || request.createdAt;

const formatShortDate = (dateString: string) => {
  return formatApiDate(dateString, {
    day: '2-digit',
    month: '2-digit',
    year: 'numeric',
  });
};

const getInitials = (name: string) => {
  return name
    .split(' ')
    .map((n) => n[0])
    .join('')
    .toUpperCase()
    .slice(0, 2);
};

const handleSearchChange = (value: string) => {
  searchQuery.value = value;
  currentPage.value = 1;
};

const handleViewDetails = (request: ServiceRequest) => {
  props.onNavigate('cliente-request-detail', { requestId: request.id });
};

const handleOpenCreateDialog = () => {
  newRequest.value = {
    service: '',
    category: '',
    description: '',
    budget: 0,
    deadline: '',
    workModel: '',
    address: '',
    desiredDate: '',
  };
  showCreateDialog.value = true;
};

const handleCreateRequest = async () => {
  if (!newRequest.value.service.trim()) {
    toast.error('Informe o serviço desejado');
    return;
  }
  if (!newRequest.value.category) {
    toast.error('Selecione uma categoria');
    return;
  }
  if (!newRequest.value.description.trim()) {
    toast.error('Descreva o que precisa');
    return;
  }
  if (!newRequest.value.workModel) {
    toast.error('Selecione o modelo de trabalho');
    return;
  }
  if (newRequest.value.budget <= 0) {
    toast.error('Informe um orçamento válido');
    return;
  }

  const rangeError = validateValueRange(newRequest.value.budget);
  if (rangeError) {
    toast.error(rangeError);
    return;
  }

  // Validar que a data desejada não é no passado
  if (newRequest.value.desiredDate) {
    const selectedDate = new Date(newRequest.value.desiredDate);
    const today = new Date();
    today.setHours(0, 0, 0, 0);

    if (selectedDate < today) {
      toast.error('A data desejada não pode ser anterior à data atual');
      return;
    }
  }

  if (
    ['PRESENCIAL', 'HIBRIDO'].includes(newRequest.value.workModel) &&
    !newRequest.value.address.trim()
  ) {
    toast.error('Informe o endereço para serviços presenciais ou híbridos');
    return;
  }

  isCreating.value = true;
  try {
    const result = await store.createRequest({
      service: newRequest.value.service,
      category: newRequest.value.category,
      description: newRequest.value.description,
      budget: newRequest.value.budget,
      deadline: newRequest.value.deadline,
      workModel: newRequest.value.workModel as WorkModelValue,
      address: newRequest.value.address,
      desiredDate: newRequest.value.desiredDate,
    });

    if (result) {
      toast.success('Solicitação criada com sucesso!');
      showCreateDialog.value = false;
    } else {
      toast.error('Erro ao criar solicitação');
    }
  } catch (err: any) {
    console.error('Erro ao criar solicitação:', err);
    const backendMsg = extractApiErrorMessage(err);
    toast.error(backendMsg || 'Erro ao criar solicitação. Verifique os dados e tente novamente.');
  } finally {
    isCreating.value = false;
  }
};

const handleOpenCancelDialog = (request: ServiceRequest) => {
  selectedRequest.value = request;
  cancelReason.value = '';
  showCancelDialog.value = true;
};

const handleCancelRequest = async () => {
  if (!selectedRequest.value) return;

  if (!cancelReason.value.trim()) {
    toast.error('Informe o motivo do cancelamento');
    return;
  }

  const success = await store.cancelRequest(selectedRequest.value.id, cancelReason.value);
  if (success) {
    toast.success('Solicitação cancelada');
    showCancelDialog.value = false;
    selectedRequest.value = null;
  } else {
    toast.error('Erro ao cancelar solicitação');
  }
};

const handleRefresh = () => {
  store.loadRequests();
  toast.info('Atualizando solicitações...');
};

watch(currentTab, () => {
  currentPage.value = 1;
});
</script>

<template>
  <div class="max-w-7xl mx-auto space-y-6">
    <!-- Header -->
    <div class="flex items-start justify-between flex-wrap gap-4">
      <div>
        <h1 class="text-2xl font-bold text-[#263238] mb-2">Minhas Solicitações</h1>
        <p class="text-[#435574]">Gerencie seus pedidos de orçamento enviados a profissionais</p>
      </div>
    </div>

    <!-- Stats Cards -->
    <div class="grid grid-cols-2 md:grid-cols-5 gap-4">
      <Card
        class="p-4 cursor-pointer hover:shadow-md transition-shadow"
        @click="currentTab = 'pending'"
      >
        <div class="flex items-center gap-3">
          <div class="w-10 h-10 rounded-full bg-blue-100 flex items-center justify-center">
            <Clock class="h-5 w-5 text-blue-600" />
          </div>
          <div>
            <p class="text-sm text-[#435574]">Pendentes</p>
            <p class="text-xl font-bold text-[#263238]">{{ store.stats.pending }}</p>
          </div>
        </div>
      </Card>

      <Card
        class="p-4 cursor-pointer hover:shadow-md transition-shadow"
        @click="currentTab = 'negotiating'"
      >
        <div class="flex items-center gap-3">
          <div class="w-10 h-10 rounded-full bg-amber-100 flex items-center justify-center">
            <MessageSquare class="h-5 w-5 text-amber-600" />
          </div>
          <div>
            <p class="text-sm text-[#435574]">Negociando</p>
            <p class="text-xl font-bold text-[#263238]">{{ store.stats.negotiating }}</p>
          </div>
        </div>
      </Card>

      <Card
        class="p-4 cursor-pointer hover:shadow-md transition-shadow"
        @click="currentTab = 'accepted'"
      >
        <div class="flex items-center gap-3">
          <div class="w-10 h-10 rounded-full bg-green-100 flex items-center justify-center">
            <CheckCircle class="h-5 w-5 text-green-600" />
          </div>
          <div>
            <p class="text-sm text-[#435574]">Em Andamento</p>
            <p class="text-xl font-bold text-[#263238]">{{ store.stats.inProgress }}</p>
          </div>
        </div>
      </Card>

      <Card
        class="p-4 cursor-pointer hover:shadow-md transition-shadow"
        @click="currentTab = 'completed'"
      >
        <div class="flex items-center gap-3">
          <div class="w-10 h-10 rounded-full bg-teal-100 flex items-center justify-center">
            <Archive class="h-5 w-5 text-teal-600" />
          </div>
          <div>
            <p class="text-sm text-[#435574]">Concluídas</p>
            <p class="text-xl font-bold text-[#263238]">{{ store.stats.completed }}</p>
          </div>
        </div>
      </Card>

      <Card
        class="p-4 cursor-pointer hover:shadow-md transition-shadow col-span-2 md:col-span-1"
        @click="currentTab = 'cancelled'"
      >
        <div class="flex items-center gap-3">
          <div class="w-10 h-10 rounded-full bg-red-100 flex items-center justify-center">
            <XCircle class="h-5 w-5 text-red-600" />
          </div>
          <div>
            <p class="text-sm text-[#435574]">Canceladas</p>
            <p class="text-xl font-bold text-[#263238]">{{ store.stats.cancelled }}</p>
          </div>
        </div>
      </Card>
    </div>

    <!-- Filters -->
    <Card class="p-4">
      <div class="grid grid-cols-1 md:grid-cols-3 gap-4">
        <div class="relative">
          <Search class="absolute left-3 top-1/2 -translate-y-1/2 h-5 w-5 text-[#435574]" />
          <Input
            placeholder="Buscar solicitações..."
            :model-value="searchQuery"
            @update:model-value="handleSearchChange"
            class="pl-10"
          />
        </div>

        <Select :model-value="categoryFilter" @update:model-value="(val: string) => categoryFilter = val">
          <SelectTrigger>
            <SelectValue :label="categoryFilter === 'all' ? 'Todas as categorias' : categoryFilter" placeholder="Todas as categorias" />
          </SelectTrigger>
          <SelectContent>
            <SelectItem value="all">Todas as categorias</SelectItem>
            <SelectItem v-for="cat in categories" :key="cat" :value="cat">
              {{ cat }}
            </SelectItem>
          </SelectContent>
        </Select>

        <Select :model-value="sortBy" @update:model-value="(val: string) => sortBy = val">
          <SelectTrigger>
            <SelectValue :label="getSortLabel(sortBy)" :placeholder="getSortLabel(sortBy)" />
          </SelectTrigger>
          <SelectContent>
            <SelectItem value="recent">Mais recentes</SelectItem>
            <SelectItem value="budget-high">Maior orçamento</SelectItem>
            <SelectItem value="budget-low">Menor orçamento</SelectItem>
          </SelectContent>
        </Select>
      </div>
    </Card>

    <!-- Tabs -->
    <Tabs :model-value="currentTab" @update:model-value="(val: string) => currentTab = val">
      <TabsList class="tabs-list grid w-full grid-cols-6">
        <TabsTrigger value="all">Todas ({{ store.requests.length }})</TabsTrigger>
        <TabsTrigger value="pending">Pendentes ({{ store.stats.pending }})</TabsTrigger>
        <TabsTrigger value="negotiating">Negociando ({{ store.stats.negotiating }})</TabsTrigger>
        <TabsTrigger value="in_progress">Em Andamento ({{ store.stats.inProgress }})</TabsTrigger>
        <TabsTrigger value="completed">Concluídas ({{ store.stats.completed }})</TabsTrigger>
        <TabsTrigger value="cancelled">Canceladas ({{ store.stats.cancelled }})</TabsTrigger>
      </TabsList>

      <TabsContent :value="currentTab" class="mt-6">
        <!-- Loading State -->
        <div v-if="store.isLoading" class="flex justify-center py-12">
          <div class="animate-spin h-8 w-8 border-4 border-[#286AD8] border-t-transparent rounded-full"></div>
        </div>

        <!-- Empty State -->
        <Card v-else-if="currentRequests.length === 0" class="p-12 text-center">
          <FileText class="h-12 w-12 text-gray-400 mx-auto mb-4" />
          <h3 class="text-lg font-semibold text-[#263238] mb-2">
            {{ searchQuery || categoryFilter !== 'all' ? 'Nenhuma solicitação encontrada' : getEmptyMessage() }}
          </h3>
          <p class="text-[#435574] mb-4">
            {{ searchQuery || categoryFilter !== 'all'
              ? 'Tente ajustar os filtros de busca'
              : 'Clique em um profissional para solicitar um orçamento' }}
          </p>
        </Card>

        <template v-else>
          <!-- Requests List -->
          <div class="grid grid-cols-1 gap-4">
            <Card
              v-for="request in currentRequests"
              :key="request.id"
              :class="`p-6 hover:shadow-md transition-all cursor-pointer ${
                request.freelancerResponse && request.status === 'negotiating'
                  ? 'border-l-4 border-l-amber-500 bg-amber-50/30'
                  : ''
              }`"
              @click="handleViewDetails(request)"
            >
              <div class="flex items-start gap-4">
                <Avatar class="h-12 w-12 hidden sm:flex">
                  <AvatarImage :src="request.freelancerAvatar" />
                  <AvatarFallback class="bg-[#286AD8] text-white">
                    {{ getInitials(request.freelancerName || request.service) }}
                  </AvatarFallback>
                </Avatar>

                <div class="flex-1 min-w-0">
                  <div class="flex items-start justify-between mb-2">
                    <div class="flex-1">
                      <div class="flex items-center gap-2 mb-1 flex-wrap">
                        <h3 class="font-semibold text-[#263238] hover:text-[#286AD8]">{{ request.service }}</h3>
                        <Badge
                          v-if="request.freelancerResponse && request.status === 'negotiating'"
                          class="bg-amber-100 text-amber-700 hover:bg-amber-100 text-xs"
                        >
                          Resposta recebida
                        </Badge>
                      </div>
                      <div class="flex items-center gap-3 text-sm text-[#435574] flex-wrap">
                        <span v-if="request.freelancerName" class="flex items-center gap-1">
                          {{ request.freelancerName }}
                        </span>
                        <span v-if="request.address" class="flex items-center gap-1">
                          <MapPin class="h-3 w-3" />
                          {{ request.address.split(' - ')[0] }}
                        </span>
                        <span class="flex items-center gap-1">
                          <Calendar class="h-3 w-3" />
                          {{ formatDate(getLastActivityAt(request)) }}
                        </span>
                      </div>
                    </div>
                    <div class="flex items-center gap-2">
                      <div class="flex flex-col items-end gap-2">
                        <!-- Status Badge -->
                        <template v-if="request.status === 'pending' || request.status === 'sent'">
                          <Badge class="bg-blue-100 text-blue-700 hover:bg-blue-100">
                            <Clock class="h-3 w-3 mr-1" />
                            {{ getStatusLabel(request.status) }}
                          </Badge>
                        </template>
                        <template v-else-if="request.status === 'negotiating'">
                          <Badge class="bg-amber-100 text-amber-700 hover:bg-amber-100">
                            <MessageSquare class="h-3 w-3 mr-1" />
                            Negociando
                          </Badge>
                        </template>
                        <template v-else-if="request.status === 'accepted'">
                          <Badge class="bg-green-100 text-green-700 hover:bg-green-100">
                            <CheckCircle class="h-3 w-3 mr-1" />
                            Aceita
                          </Badge>
                        </template>
                        <template v-else-if="request.status === 'in_progress'">
                          <Badge class="bg-purple-100 text-purple-700 hover:bg-purple-100">
                            <Clock class="h-3 w-3 mr-1" />
                            Em Andamento
                          </Badge>
                        </template>
                        <template v-else-if="request.status === 'delivered'">
                          <Badge class="bg-amber-100 text-amber-700 hover:bg-amber-100">
                            <Send class="h-3 w-3 mr-1" />
                            Entregue
                          </Badge>
                        </template>
                        <template v-else-if="request.status === 'completed'">
                          <Badge class="bg-teal-100 text-teal-700 hover:bg-teal-100">
                            <CheckCircle class="h-3 w-3 mr-1" />
                            Concluída
                          </Badge>
                        </template>
                        <template v-else-if="request.status === 'rejected'">
                          <Badge class="bg-red-100 text-red-700 hover:bg-red-100">
                            <XCircle class="h-3 w-3 mr-1" />
                            Recusada
                          </Badge>
                        </template>
                        <template v-else-if="request.status === 'cancelled'">
                          <Badge variant="secondary">
                            <XCircle class="h-3 w-3 mr-1" />
                            Cancelada
                          </Badge>
                        </template>

                        <WorkModelBadge v-if="request.workModel" :work-model="request.workModel" />
                      </div>
                    </div>
                  </div>

                  <p class="text-sm text-[#435574] mb-3 line-clamp-2">
                    {{ request.description }}
                  </p>

                  <!-- Freelancer response preview -->
                  <div
                    v-if="request.freelancerResponse && request.status === 'negotiating'"
                    class="bg-white border border-amber-200 rounded-lg p-3 mb-3"
                  >
                    <div class="flex items-center gap-2 text-sm text-amber-700 font-medium mb-1">
                      <Send class="h-3.5 w-3.5" />
                      Resposta do profissional
                    </div>
                    <div class="flex items-center gap-4 text-sm">
                      <span class="font-medium text-[#263238]">
                        {{ formatCurrency(request.freelancerResponse.value) }}
                      </span>
                      <span class="text-[#435574]">
                        Prazo: {{ request.freelancerResponse.executionDays }} dias
                      </span>
                    </div>
                  </div>

                  <div class="flex items-center justify-between">
                    <div class="flex items-center gap-4 text-sm flex-wrap">
                      <span class="flex items-center gap-1 font-medium text-[#263238]">
                        <DollarSign class="h-4 w-4 text-green-600" />
                        {{ formatCurrency(['accepted', 'in_progress', 'completed'].includes(request.status) && request.freelancerResponse ? request.freelancerResponse.value : request.budget) }}
                      </span>
                      <span class="flex items-center gap-1 text-[#435574]">
                        <Clock class="h-4 w-4" />
                        Prazo: {{ ['accepted', 'in_progress', 'completed'].includes(request.status) && request.freelancerResponse ? request.freelancerResponse.executionDays + ' dias' : request.deadline }}
                      </span>
                      <span
                        v-if="request.attachments && request.attachments.length > 0"
                        class="flex items-center gap-1 text-[#435574]"
                      >
                        <FileText class="h-4 w-4" />
                        {{ request.attachments.length }} anexo{{ request.attachments.length > 1 ? 's' : '' }}
                      </span>
                    </div>

                    <Button
                      @click.stop="handleViewDetails(request)"
                      variant="outline"
                      size="sm"
                    >
                      <Eye class="h-4 w-4 mr-2" />
                      Ver Detalhes
                    </Button>
                  </div>
                </div>
              </div>
            </Card>
          </div>

          <!-- Pagination -->
          <div v-if="totalPages > 1" class="flex items-center justify-center gap-2 mt-6">
            <Button
              @click="currentPage = Math.max(1, currentPage - 1)"
              :disabled="currentPage === 1"
              variant="outline"
              size="sm"
            >
              Anterior
            </Button>
            <div class="flex items-center gap-1">
              <Button
                v-for="pageNum in Array.from({ length: Math.min(5, totalPages) }, (_, i) => {
                  if (totalPages <= 5) return i + 1;
                  if (currentPage <= 3) return i + 1;
                  if (currentPage >= totalPages - 2) return totalPages - 4 + i;
                  return currentPage - 2 + i;
                })"
                :key="pageNum"
                @click="currentPage = pageNum"
                :variant="currentPage === pageNum ? 'default' : 'outline'"
                size="sm"
                :class="currentPage === pageNum ? 'bg-[#286AD8] hover:bg-[#435574]' : ''"
              >
                {{ pageNum }}
              </Button>
            </div>
            <Button
              @click="currentPage = Math.min(totalPages, currentPage + 1)"
              :disabled="currentPage === totalPages"
              variant="outline"
              size="sm"
            >
              Próxima
            </Button>
          </div>
        </template>
      </TabsContent>
    </Tabs>

    <!-- Create Request Dialog -->
    <Dialog :open="showCreateDialog" @update:open="(val) => showCreateDialog = val">
      <DialogContent class="sm:max-w-lg max-h-[90vh] overflow-y-auto">
        <DialogHeader>
          <DialogTitle>Nova Solicitação de Orçamento</DialogTitle>
          <DialogDescription>
            Preencha os detalhes do serviço que você precisa
          </DialogDescription>
        </DialogHeader>
        <div class="space-y-4">
          <div class="space-y-2">
            <Label for="service">Serviço *</Label>
            <Input
              id="service"
              :model-value="newRequest.service"
              @update:model-value="(val) => newRequest.service = val as string"
              placeholder="Ex: Instalação Elétrica Residencial"
            />
          </div>

          <div class="space-y-2">
            <Label for="category">Categoria *</Label>
            <Select :model-value="newRequest.category" @update:model-value="(val: string) => newRequest.category = val">
              <SelectTrigger>
                <SelectValue placeholder="Selecione a categoria" />
              </SelectTrigger>
              <SelectContent>
                <SelectItem v-for="cat in categories" :key="cat" :value="cat">
                  {{ cat }}
                </SelectItem>
              </SelectContent>
            </Select>
          </div>

          <div class="space-y-2">
            <Label for="description">Descrição *</Label>
            <Textarea
              id="description"
              :model-value="newRequest.description"
              @update:model-value="(val) => newRequest.description = val as string"
              placeholder="Descreva detalhadamente o que você precisa..."
              :rows="4"
            />
          </div>

          <div class="grid grid-cols-2 gap-4">
            <div class="space-y-2">
              <Label for="budget">Orçamento (R$) *</Label>
              <Input
                id="budget"
                type="number"
                :model-value="newRequest.budget"
                @update:model-value="(val) => newRequest.budget = Number(val)"
                placeholder="0,00"
                min="0"
              />
            </div>
            <div class="space-y-2">
              <Label for="deadline">Prazo</Label>
              <Input
                id="deadline"
                :model-value="newRequest.deadline"
                @update:model-value="(val) => newRequest.deadline = val as string"
                placeholder="Ex: 15 dias"
              />
            </div>
          </div>

          <div class="grid grid-cols-2 gap-4">
            <div class="space-y-2">
              <Label for="work-model">Modelo de trabalho</Label>
              <Select :model-value="newRequest.workModel" @update:model-value="(val: string) => newRequest.workModel = val as WorkModelValue">
                <SelectTrigger>
                  <SelectValue placeholder="Selecione" />
                </SelectTrigger>
                <SelectContent>
                  <SelectItem value="REMOTO">Remoto</SelectItem>
                  <SelectItem value="PRESENCIAL">Presencial</SelectItem>
                  <SelectItem value="HIBRIDO">Híbrido</SelectItem>
                </SelectContent>
              </Select>
            </div>
            <div class="space-y-2">
              <Label for="desiredDate">Data desejada</Label>
              <Input
                id="desiredDate"
                type="date"
                :min="new Date().toISOString().split('T')[0]"
                :model-value="newRequest.desiredDate"
                @update:model-value="(val) => newRequest.desiredDate = val as string"
              />
            </div>
          </div>

          <div class="space-y-2">
            <Label for="address">Endereço do serviço</Label>
            <Input
              id="address"
              :model-value="newRequest.address"
              @update:model-value="(val) => newRequest.address = val as string"
              placeholder="Rua, número - Bairro, Cidade - UF"
            />
          </div>
        </div>
        <DialogFooter>
          <Button @click="showCreateDialog = false" variant="outline">
            Cancelar
          </Button>
          <Button
            @click="handleCreateRequest"
            class="bg-[#286AD8] hover:bg-[#435574]"
            :disabled="isCreating"
          >
            <Plus class="h-4 w-4 mr-2" />
            {{ isCreating ? 'Criando...' : 'Criar Solicitação' }}
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>

    <!-- Cancel Dialog -->
    <Dialog :open="showCancelDialog" @update:open="(val) => showCancelDialog = val">
      <DialogContent class="sm:max-w-md">
        <DialogHeader>
          <DialogTitle>Cancelar Solicitação</DialogTitle>
          <DialogDescription>
            Tem certeza que deseja cancelar "{{ selectedRequest?.service }}"?
          </DialogDescription>
        </DialogHeader>
        <div class="space-y-4">
          <div class="space-y-2">
            <Label for="cancel-reason">Motivo do cancelamento *</Label>
            <Textarea
              id="cancel-reason"
              :model-value="cancelReason"
              @update:model-value="(val) => cancelReason = val as string"
              placeholder="Por que você quer cancelar esta solicitação?"
              :rows="3"
            />
          </div>
        </div>
        <DialogFooter>
          <Button @click="showCancelDialog = false" variant="outline">
            Manter
          </Button>
          <Button @click="handleCancelRequest" class="bg-red-600 hover:bg-red-700">
            Confirmar Cancelamento
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  </div>
</template>

<style scoped>
@media (max-width: 768px) {
  .tabs-list {
    grid-template-columns: repeat(3, 1fr) !important;
    gap: 0.25rem;
  }
}

@media (max-width: 480px) {
  .tabs-list {
    grid-template-columns: repeat(2, 1fr) !important;
  }
}
</style>
