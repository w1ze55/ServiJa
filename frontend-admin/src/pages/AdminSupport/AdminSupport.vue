<script setup lang="ts">
import { computed, onMounted, ref } from 'vue';
import {
  CheckCircle,
  Clock,
  Eye,
  Inbox,
  Search,
  ShieldCheck,
  X,
  XCircle,
} from 'lucide-vue-next';
import AdminLayout from '@/layouts/AdminLayout.vue';
import { supportService, type SupportRequest } from '@/api/services/supportService';
import {
  SUPPORT_CATEGORY_OPTIONS,
  SUPPORT_STATUS_OPTIONS,
  getSupportCategoryLabel,
  getSupportStatusBadgeClass,
  getSupportStatusLabel,
} from '@/constants/support';
import Select from '@/components/ui/select.vue';
import SelectContent from '@/components/ui/select-content.vue';
import SelectItem from '@/components/ui/select-item.vue';
import SelectTrigger from '@/components/ui/select-trigger.vue';
import SelectValue from '@/components/ui/select-value.vue';
import { toast } from '@/utils/toast';
import SupportAdminResponseModal from './SupportAdminResponseModal.vue';
import SupportRequestDetailsModal from './SupportRequestDetailsModal.vue';

type SupportAdminActionMode = 'approve' | 'reject' | null;
type SupportStatusCardKey = 'ALL' | 'PENDING' | 'APPROVED' | 'REJECTED';

const TIME_FILTER_OPTIONS = [
  { value: 'Hoje', label: 'Hoje' },
  { value: 'Ontem', label: 'Ontem' },
  { value: 'Ultimos 7 dias', label: 'Últimos 7 dias' },
  { value: 'Ultimos 30 dias', label: 'Últimos 30 dias' },
  { value: 'Mes atual', label: 'Mês atual' },
  { value: 'Ultimo mes', label: 'Último mês' },
  { value: 'Intervalo personalizado', label: 'Intervalo personalizado' },
] as const;

const isLoading = ref(false);
const isLoadingDetails = ref(false);
const isSubmittingAction = ref(false);
const requests = ref<SupportRequest[]>([]);
const searchTerm = ref('');
const statusFilter = ref<'ALL' | (typeof SUPPORT_STATUS_OPTIONS)[number]['value']>('ALL');
const categoryFilter = ref<'ALL' | (typeof SUPPORT_CATEGORY_OPTIONS)[number]['value']>('ALL');
const timeFilter = ref('Ultimos 30 dias');
const customDateFrom = ref('');
const customDateTo = ref('');
const isDetailsOpen = ref(false);
const isResponseModalOpen = ref(false);
const selectedRequest = ref<SupportRequest | null>(null);
const currentAction = ref<SupportAdminActionMode>(null);

const loadRequests = async () => {
  isLoading.value = true;

  try {
    requests.value = await supportService.getSupportRequests();
  } catch (error) {
    console.error(error);
    toast.error('Não foi possível carregar as solicitações de suporte.');
  } finally {
    isLoading.value = false;
  }
};

const summary = computed(() => {
  const pending = requests.value.filter((request) => request.status === 'PENDING').length;
  const approved = requests.value.filter((request) => request.status === 'APPROVED').length;
  const rejected = requests.value.filter((request) => request.status === 'REJECTED').length;

  return {
    total: requests.value.length,
    pending,
    approved,
    rejected,
  };
});

const supportStatusCards = computed(() => [
  {
    key: 'ALL' as const,
    cardLabel: 'Total',
    count: summary.value.total,
    cardIconContainerClass: 'bg-slate-100',
    cardIconClass: 'text-slate-600',
    icon: Inbox,
  },
  {
    key: 'PENDING' as const,
    cardLabel: 'Pendentes',
    count: summary.value.pending,
    cardIconContainerClass: 'bg-amber-100',
    cardIconClass: 'text-amber-600',
    icon: Clock,
  },
  {
    key: 'APPROVED' as const,
    cardLabel: 'Aprovadas',
    count: summary.value.approved,
    cardIconContainerClass: 'bg-emerald-100',
    cardIconClass: 'text-emerald-600',
    icon: CheckCircle,
  },
  {
    key: 'REJECTED' as const,
    cardLabel: 'Recusadas',
    count: summary.value.rejected,
    cardIconContainerClass: 'bg-red-100',
    cardIconClass: 'text-red-600',
    icon: XCircle,
  },
]);

const selectedStatusLabel = computed(() =>
  statusFilter.value === 'ALL'
    ? 'Todos os status'
    : SUPPORT_STATUS_OPTIONS.find((option) => option.value === statusFilter.value)?.label || 'Todos os status'
);

const selectedCategoryLabel = computed(() =>
  categoryFilter.value === 'ALL'
    ? 'Todas as categorias'
    : SUPPORT_CATEGORY_OPTIONS.find((option) => option.value === categoryFilter.value)?.label || 'Todas as categorias'
);

const selectedTimeFilterLabel = computed(() =>
  TIME_FILTER_OPTIONS.find((option) => option.value === timeFilter.value)?.label || 'Últimos 30 dias'
);

const matchesSelectedTimeFilter = (dateValue: string) => {
  const requestDate = new Date(dateValue);
  if (Number.isNaN(requestDate.getTime())) {
    return false;
  }

  const now = new Date();
  const startOfToday = new Date(now.getFullYear(), now.getMonth(), now.getDate());

  switch (timeFilter.value) {
    case 'Hoje':
      return requestDate >= startOfToday;
    case 'Ontem': {
      const startOfYesterday = new Date(startOfToday);
      startOfYesterday.setDate(startOfYesterday.getDate() - 1);
      return requestDate >= startOfYesterday && requestDate < startOfToday;
    }
    case 'Ultimos 7 dias': {
      const sevenDaysAgo = new Date(startOfToday);
      sevenDaysAgo.setDate(sevenDaysAgo.getDate() - 7);
      return requestDate >= sevenDaysAgo;
    }
    case 'Ultimos 30 dias': {
      const thirtyDaysAgo = new Date(startOfToday);
      thirtyDaysAgo.setDate(thirtyDaysAgo.getDate() - 30);
      return requestDate >= thirtyDaysAgo;
    }
    case 'Mes atual':
      return requestDate.getMonth() === now.getMonth() && requestDate.getFullYear() === now.getFullYear();
    case 'Ultimo mes': {
      const lastMonth = new Date(now.getFullYear(), now.getMonth() - 1, 1);
      const endOfLastMonth = new Date(now.getFullYear(), now.getMonth(), 0, 23, 59, 59);
      return requestDate >= lastMonth && requestDate <= endOfLastMonth;
    }
    case 'Intervalo personalizado': {
      const from = customDateFrom.value ? new Date(`${customDateFrom.value}T00:00:00`) : null;
      const to = customDateTo.value ? new Date(`${customDateTo.value}T23:59:59`) : null;

      if (from && to) return requestDate >= from && requestDate <= to;
      if (from) return requestDate >= from;
      if (to) return requestDate <= to;
      return true;
    }
    default:
      return true;
  }
};

const handleStatusFilterChange = (value: string) => {
  statusFilter.value = value as typeof statusFilter.value;
};

const handleCategoryFilterChange = (value: string) => {
  categoryFilter.value = value as typeof categoryFilter.value;
};

const handleTimeFilterChange = (value: string) => {
  timeFilter.value = value;
};

const handleStatusCardClick = (status: SupportStatusCardKey) => {
  statusFilter.value = status;
};

const filteredRequests = computed(() => {
  const normalizedSearch = searchTerm.value.trim().toLowerCase();

  return requests.value.filter((request) => {
    const matchesStatus = statusFilter.value === 'ALL' || request.status === statusFilter.value;
    const matchesCategory = categoryFilter.value === 'ALL' || request.category === categoryFilter.value;
    const matchesTime = matchesSelectedTimeFilter(request.createdAt);

    if (!matchesStatus || !matchesCategory || !matchesTime) {
      return false;
    }

    if (!normalizedSearch) {
      return true;
    }

    return (
      String(request.id).includes(normalizedSearch) ||
      String(request.userId).includes(normalizedSearch) ||
      request.userName.toLowerCase().includes(normalizedSearch) ||
      request.userEmail.toLowerCase().includes(normalizedSearch)
    );
  });
});

const formatDate = (value: string) =>
  new Date(value).toLocaleString('pt-BR', {
    day: '2-digit',
    month: '2-digit',
    year: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
  });

const upsertRequest = (updatedRequest: SupportRequest) => {
  requests.value = requests.value.map((request) =>
    request.id === updatedRequest.id ? updatedRequest : request
  );

  if (selectedRequest.value?.id === updatedRequest.id) {
    selectedRequest.value = updatedRequest;
  }
};

const openDetails = async (requestId: number) => {
  isDetailsOpen.value = true;
  isLoadingDetails.value = true;
  selectedRequest.value = requests.value.find((request) => request.id === requestId) || null;

  try {
    const detailedRequest = await supportService.getSupportRequestById(requestId);
    selectedRequest.value = detailedRequest;
    upsertRequest(detailedRequest);
  } catch (error) {
    console.error(error);
    toast.error('Não foi possível carregar os detalhes da solicitação.');
  } finally {
    isLoadingDetails.value = false;
  }
};

const openActionModal = (mode: SupportAdminActionMode, request: SupportRequest) => {
  selectedRequest.value = request;
  currentAction.value = mode;
  isResponseModalOpen.value = true;
};

const forceCloseResponseModal = () => {
  isResponseModalOpen.value = false;
  currentAction.value = null;
};

const closeResponseModal = () => {
  if (isSubmittingAction.value) {
    return;
  }

  forceCloseResponseModal();
};

const handleActionSubmit = async (message: string) => {
  if (!selectedRequest.value || !currentAction.value) {
    return;
  }

  isSubmittingAction.value = true;

  try {
    let updatedRequest: SupportRequest;

    if (currentAction.value === 'approve') {
      updatedRequest = await supportService.approveSupportRequest(selectedRequest.value.id, message);
      toast.success('Solicitação aprovada com sucesso.');
    } else {
      updatedRequest = await supportService.rejectSupportRequest(selectedRequest.value.id, message);
      toast.success('Solicitação recusada com sucesso.');
    }

    upsertRequest(updatedRequest);
    forceCloseResponseModal();
  } catch (error) {
    console.error(error);
    const messageFromApi = (error as { response?: { data?: { message?: string } } })?.response?.data?.message;
    toast.error(messageFromApi || 'Não foi possível concluir a ação.');
  } finally {
    isSubmittingAction.value = false;
  }
};

const clearFilters = () => {
  searchTerm.value = '';
  statusFilter.value = 'ALL';
  categoryFilter.value = 'ALL';
  timeFilter.value = 'Ultimos 30 dias';
  customDateFrom.value = '';
  customDateTo.value = '';
};

onMounted(() => {
  loadRequests();
});
</script>

<template>
  <AdminLayout>
    <div class="space-y-6">
      <div class="flex flex-col gap-3 md:flex-row md:items-center md:justify-between">
        <div>
          <h1 class="text-2xl font-bold text-slate-900">Suporte</h1>
          <p class="mt-1 text-sm text-slate-500">
            Acompanhe, visualize, aprove ou recuse chamados enviados pelos usuários.
          </p>
        </div>
      </div>

      <div class="grid grid-cols-1 gap-4 md:grid-cols-2 xl:grid-cols-4">
        <div
          v-for="statusCard in supportStatusCards"
          :key="statusCard.key"
          :class="[
            'cursor-pointer rounded-xl border bg-white p-4 shadow-sm transition-shadow hover:shadow-md',
            statusFilter === statusCard.key
              ? 'border-blue-200 ring-2 ring-blue-100'
              : 'border-slate-200',
          ]"
          @click="handleStatusCardClick(statusCard.key)"
        >
          <div class="flex items-center gap-3">
            <div
              :class="[
                'flex h-10 w-10 items-center justify-center rounded-full',
                statusCard.cardIconContainerClass,
              ]"
            >
              <component
                :is="statusCard.icon"
                :class="['h-5 w-5', statusCard.cardIconClass]"
              />
            </div>
            <div>
              <p class="text-sm text-[#435574]">{{ statusCard.cardLabel }}</p>
              <p class="text-xl font-bold text-[#263238]">{{ statusCard.count }}</p>
            </div>
          </div>
        </div>
      </div>

      <div class="rounded-xl border border-slate-200 bg-white p-4 shadow-sm">
        <div class="flex flex-wrap gap-3">
          <div class="relative min-w-[240px] flex-1">
            <Search class="absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-slate-400" />
            <input
              v-model="searchTerm"
              type="text"
              placeholder="Buscar por ID da solicitação, ID do usuário, nome ou email..."
              class="w-full rounded-lg border border-slate-200 py-2 pl-9 pr-3 text-sm outline-none transition focus:border-blue-400 focus:ring-2 focus:ring-blue-100"
            />
          </div>

          <Select
            :model-value="statusFilter"
            @update:model-value="handleStatusFilterChange"
          >
            <SelectTrigger class="min-w-[180px]">
              <SelectValue
                :label="selectedStatusLabel"
                placeholder="Todos os status"
              />
            </SelectTrigger>
            <SelectContent>
              <SelectItem value="ALL">Todos os status</SelectItem>
              <SelectItem
                v-for="status in SUPPORT_STATUS_OPTIONS"
                :key="status.value"
                :value="status.value"
              >
                {{ status.label }}
              </SelectItem>
            </SelectContent>
          </Select>

          <Select
            :model-value="categoryFilter"
            @update:model-value="handleCategoryFilterChange"
          >
            <SelectTrigger class="min-w-[220px]">
              <SelectValue
                :label="selectedCategoryLabel"
                placeholder="Todas as categorias"
              />
            </SelectTrigger>
            <SelectContent>
              <SelectItem value="ALL">Todas as categorias</SelectItem>
              <SelectItem
                v-for="category in SUPPORT_CATEGORY_OPTIONS"
                :key="category.value"
                :value="category.value"
              >
                {{ category.label }}
              </SelectItem>
            </SelectContent>
          </Select>

          <Select
            :model-value="timeFilter"
            @update:model-value="handleTimeFilterChange"
          >
            <SelectTrigger class="min-w-[220px]">
              <SelectValue
                :label="selectedTimeFilterLabel"
                placeholder="Últimos 30 dias"
              />
            </SelectTrigger>
            <SelectContent>
              <SelectItem
                v-for="timeOption in TIME_FILTER_OPTIONS"
                :key="timeOption.value"
                :value="timeOption.value"
              >
                {{ timeOption.label }}
              </SelectItem>
            </SelectContent>
          </Select>

          <button
            type="button"
            class="flex items-center gap-1.5 rounded-lg border border-slate-200 px-3 py-2 text-sm text-slate-600 transition hover:bg-slate-50"
            @click="clearFilters"
          >
            <X class="h-4 w-4" />
            Limpar filtros
          </button>
        </div>

        <div
          v-if="timeFilter === 'Intervalo personalizado'"
          class="mt-4 grid grid-cols-1 gap-4 border-t border-slate-200 pt-4 md:grid-cols-2"
        >
          <div class="space-y-1">
            <label class="text-sm font-medium text-slate-700">De</label>
            <input
              v-model="customDateFrom"
              type="date"
              class="w-full rounded-lg border border-slate-200 px-3 py-2 text-sm outline-none transition focus:border-blue-400 focus:ring-2 focus:ring-blue-100"
            />
          </div>

          <div class="space-y-1">
            <label class="text-sm font-medium text-slate-700">Até</label>
            <input
              v-model="customDateTo"
              type="date"
              class="w-full rounded-lg border border-slate-200 px-3 py-2 text-sm outline-none transition focus:border-blue-400 focus:ring-2 focus:ring-blue-100"
            />
          </div>
        </div>
      </div>

      <div
        v-if="isLoading"
        class="rounded-xl border border-dashed border-slate-300 bg-white p-12 text-center text-slate-500"
      >
        Carregando solicitações de suporte...
      </div>

      <div
        v-else-if="filteredRequests.length === 0"
        class="rounded-xl border border-slate-200 bg-white px-6 py-16 text-center shadow-sm"
      >
        <div class="mx-auto mb-4 flex h-16 w-16 items-center justify-center rounded-full bg-slate-100">
          <Inbox class="h-8 w-8 text-slate-400" />
        </div>
        <p class="text-base font-medium text-slate-900">Nenhuma solicitação encontrada</p>
        <p class="mt-1 text-sm text-slate-500">
          Ajuste os filtros ou aguarde novos chamados chegarem.
        </p>
      </div>

      <template v-else>
        <div class="hidden overflow-hidden rounded-xl border border-slate-200 bg-white shadow-sm lg:block">
          <table class="min-w-full divide-y divide-slate-100">
            <thead class="bg-slate-50">
              <tr>
                <th class="px-4 py-3 text-left text-xs font-semibold uppercase tracking-wide text-slate-500">Usuário</th>
                <th class="px-4 py-3 text-left text-xs font-semibold uppercase tracking-wide text-slate-500">Categoria</th>
                <th class="px-4 py-3 text-left text-xs font-semibold uppercase tracking-wide text-slate-500">Status</th>
                <th class="px-4 py-3 text-left text-xs font-semibold uppercase tracking-wide text-slate-500">Criação</th>
                <th class="px-4 py-3 text-left text-xs font-semibold uppercase tracking-wide text-slate-500">Ações</th>
              </tr>
            </thead>
            <tbody class="divide-y divide-slate-100">
              <tr
                v-for="request in filteredRequests"
                :key="request.id"
                class="transition hover:bg-slate-50"
              >
                <td class="px-4 py-4">
                  <p class="text-sm font-medium text-slate-900">{{ request.userName }}</p>
                  <p class="text-xs text-slate-500">{{ request.userEmail }}</p>
                </td>
                <td class="px-4 py-4">
                  <p class="max-w-xs text-sm text-slate-700">
                    {{ getSupportCategoryLabel(request.category) }}
                  </p>
                </td>
                <td class="px-4 py-4">
                  <span
                    :class="['inline-flex rounded-full px-2.5 py-1 text-xs font-semibold', getSupportStatusBadgeClass(request.status)]"
                  >
                    {{ getSupportStatusLabel(request.status) }}
                  </span>
                </td>
                <td class="px-4 py-4 text-sm text-slate-500">
                  {{ formatDate(request.createdAt) }}
                </td>
                <td class="px-4 py-4">
                  <div class="flex flex-wrap gap-2">
                    <button
                      type="button"
                      class="inline-flex items-center gap-1.5 rounded-lg border border-slate-200 px-3 py-1.5 text-xs font-medium text-slate-700 transition hover:bg-slate-50"
                      @click="openDetails(request.id)"
                    >
                      <Eye class="h-3.5 w-3.5" />
                      Visualizar
                    </button>
                    <button
                      type="button"
                      class="inline-flex items-center gap-1.5 rounded-lg border border-emerald-200 px-3 py-1.5 text-xs font-medium text-emerald-700 transition hover:bg-emerald-50"
                      @click="openActionModal('approve', request)"
                    >
                      <ShieldCheck class="h-3.5 w-3.5" />
                      Aprovar
                    </button>
                    <button
                      type="button"
                      class="inline-flex items-center gap-1.5 rounded-lg border border-red-200 px-3 py-1.5 text-xs font-medium text-red-700 transition hover:bg-red-50"
                      @click="openActionModal('reject', request)"
                    >
                      <XCircle class="h-3.5 w-3.5" />
                      Recusar
                    </button>
                  </div>
                </td>
              </tr>
            </tbody>
          </table>
        </div>

        <div class="space-y-4 lg:hidden">
          <div
            v-for="request in filteredRequests"
            :key="request.id"
            class="rounded-xl border border-slate-200 bg-white p-4 shadow-sm"
          >
            <div class="flex items-start justify-between gap-3">
              <div>
                <p class="text-sm font-semibold text-slate-900">Solicitação #{{ request.id }}</p>
                <p class="text-xs text-slate-500">Usuário #{{ request.userId }}</p>
              </div>
              <span
                :class="['inline-flex rounded-full px-2.5 py-1 text-xs font-semibold', getSupportStatusBadgeClass(request.status)]"
              >
                {{ getSupportStatusLabel(request.status) }}
              </span>
            </div>

            <div class="mt-4 space-y-2 text-sm text-slate-600">
              <p><span class="font-medium text-slate-900">{{ request.userName }}</span></p>
              <p>{{ request.userEmail }}</p>
              <p>{{ getSupportCategoryLabel(request.category) }}</p>
              <p>{{ formatDate(request.createdAt) }}</p>
            </div>

            <div class="mt-4 flex flex-wrap gap-2">
              <button
                type="button"
                class="inline-flex items-center gap-1.5 rounded-lg border border-slate-200 px-3 py-1.5 text-xs font-medium text-slate-700 transition hover:bg-slate-50"
                @click="openDetails(request.id)"
              >
                <Eye class="h-3.5 w-3.5" />
                Visualizar
              </button>
              <button
                type="button"
                class="inline-flex items-center gap-1.5 rounded-lg border border-emerald-200 px-3 py-1.5 text-xs font-medium text-emerald-700 transition hover:bg-emerald-50"
                @click="openActionModal('approve', request)"
              >
                <ShieldCheck class="h-3.5 w-3.5" />
                Aprovar
              </button>
              <button
                type="button"
                class="inline-flex items-center gap-1.5 rounded-lg border border-red-200 px-3 py-1.5 text-xs font-medium text-red-700 transition hover:bg-red-50"
                @click="openActionModal('reject', request)"
              >
                <XCircle class="h-3.5 w-3.5" />
                Recusar
              </button>
            </div>
          </div>
        </div>
      </template>
    </div>

    <SupportRequestDetailsModal
      :open="isDetailsOpen"
      :request="selectedRequest"
      :is-loading="isLoadingDetails"
      @close="isDetailsOpen = false"
      @approve="selectedRequest && openActionModal('approve', selectedRequest)"
      @reject="selectedRequest && openActionModal('reject', selectedRequest)"
    />

    <SupportAdminResponseModal
      :open="isResponseModalOpen"
      :request="selectedRequest"
      :mode="currentAction"
      :is-submitting="isSubmittingAction"
      @close="closeResponseModal"
      @submit="handleActionSubmit"
    />
  </AdminLayout>
</template>
