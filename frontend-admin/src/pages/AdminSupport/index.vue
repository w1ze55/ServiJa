<script setup lang="ts">
import { computed, onMounted, ref } from 'vue';
import {
  CircleSlash,
  Eye,
  Inbox,
  MessageSquareText,
  RefreshCw,
  Search,
  ShieldCheck,
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
import { toast } from '@/utils/toast';
import SupportAdminResponseModal from './SupportAdminResponseModal.vue';
import SupportRequestDetailsModal from './SupportRequestDetailsModal.vue';

type SupportAdminActionMode = 'approve' | 'reply' | 'reject' | null;

const isLoading = ref(false);
const isLoadingDetails = ref(false);
const isSubmittingAction = ref(false);
const requests = ref<SupportRequest[]>([]);
const searchTerm = ref('');
const statusFilter = ref<'ALL' | (typeof SUPPORT_STATUS_OPTIONS)[number]['value']>('ALL');
const categoryFilter = ref<'ALL' | (typeof SUPPORT_CATEGORY_OPTIONS)[number]['value']>('ALL');
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
  const answered = requests.value.filter((request) => request.status === 'ANSWERED').length;
  const rejected = requests.value.filter((request) => request.status === 'REJECTED').length;

  return {
    total: requests.value.length,
    pending,
    approved,
    answered,
    rejected,
  };
});

const filteredRequests = computed(() => {
  const normalizedSearch = searchTerm.value.trim().toLowerCase();

  return requests.value.filter((request) => {
    const matchesStatus = statusFilter.value === 'ALL' || request.status === statusFilter.value;
    const matchesCategory = categoryFilter.value === 'ALL' || request.category === categoryFilter.value;

    if (!matchesStatus || !matchesCategory) {
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

const closeResponseModal = () => {
  if (isSubmittingAction.value) return;
  forceCloseResponseModal();
};

const forceCloseResponseModal = () => {
  isResponseModalOpen.value = false;
  currentAction.value = null;
};

const handleActionSubmit = async (message: string) => {
  if (!selectedRequest.value || !currentAction.value) {
    return;
  }

  isSubmittingAction.value = true;

  try {
    let updatedRequest: SupportRequest;

    if (currentAction.value === 'approve') {
      updatedRequest = await supportService.approveSupportRequest(selectedRequest.value.id, message || undefined);
      toast.success('Solicitação aprovada com sucesso.');
    } else if (currentAction.value === 'reject') {
      updatedRequest = await supportService.rejectSupportRequest(selectedRequest.value.id, message);
      toast.success('Solicitação recusada com sucesso.');
    } else {
      updatedRequest = await supportService.replySupportRequest(selectedRequest.value.id, message);
      toast.success('Resposta enviada com sucesso.');
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
            Acompanhe, responda, aprove ou recuse chamados enviados pelos usuários.
          </p>
        </div>
        <button
          type="button"
          class="inline-flex items-center gap-2 rounded-lg border border-slate-200 bg-white px-4 py-2 text-sm font-medium text-slate-700 transition hover:bg-slate-50"
          @click="loadRequests"
        >
          <RefreshCw class="h-4 w-4" />
          Atualizar
        </button>
      </div>

      <div class="grid gap-4 md:grid-cols-2 xl:grid-cols-5">
        <div class="rounded-xl border border-slate-200 bg-white p-4 shadow-sm">
          <p class="text-sm text-slate-500">Total</p>
          <p class="mt-2 text-2xl font-semibold text-slate-900">{{ summary.total }}</p>
        </div>
        <div class="rounded-xl border border-amber-100 bg-amber-50/70 p-4 shadow-sm">
          <p class="text-sm text-amber-700">Pendentes</p>
          <p class="mt-2 text-2xl font-semibold text-amber-800">{{ summary.pending }}</p>
        </div>
        <div class="rounded-xl border border-emerald-100 bg-emerald-50/70 p-4 shadow-sm">
          <p class="text-sm text-emerald-700">Aprovadas</p>
          <p class="mt-2 text-2xl font-semibold text-emerald-800">{{ summary.approved }}</p>
        </div>
        <div class="rounded-xl border border-blue-100 bg-blue-50/70 p-4 shadow-sm">
          <p class="text-sm text-blue-700">Respondidas</p>
          <p class="mt-2 text-2xl font-semibold text-blue-800">{{ summary.answered }}</p>
        </div>
        <div class="rounded-xl border border-red-100 bg-red-50/70 p-4 shadow-sm">
          <p class="text-sm text-red-700">Recusadas</p>
          <p class="mt-2 text-2xl font-semibold text-red-800">{{ summary.rejected }}</p>
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

          <select
            v-model="statusFilter"
            class="rounded-lg border border-slate-200 px-3 py-2 text-sm outline-none focus:border-blue-400 focus:ring-2 focus:ring-blue-100"
          >
            <option value="ALL">Todos os status</option>
            <option v-for="status in SUPPORT_STATUS_OPTIONS" :key="status.value" :value="status.value">
              {{ status.label }}
            </option>
          </select>

          <select
            v-model="categoryFilter"
            class="rounded-lg border border-slate-200 px-3 py-2 text-sm outline-none focus:border-blue-400 focus:ring-2 focus:ring-blue-100"
          >
            <option value="ALL">Todas as categorias</option>
            <option
              v-for="category in SUPPORT_CATEGORY_OPTIONS"
              :key="category.value"
              :value="category.value"
            >
              {{ category.label }}
            </option>
          </select>

          <button
            type="button"
            class="rounded-lg border border-slate-200 px-3 py-2 text-sm font-medium text-slate-700 transition hover:bg-slate-50"
            @click="clearFilters"
          >
            Limpar filtros
          </button>
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
                <th class="px-6 py-3 text-left text-xs font-semibold uppercase tracking-wide text-slate-500">Solicitação</th>
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
                <td class="px-6 py-4">
                  <p class="text-sm font-medium text-slate-900">#{{ request.id }}</p>
                  <p class="text-xs text-slate-500">Usuário #{{ request.userId }}</p>
                </td>
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
                      class="inline-flex items-center gap-1.5 rounded-lg border border-blue-200 px-3 py-1.5 text-xs font-medium text-blue-700 transition hover:bg-blue-50"
                      @click="openActionModal('reply', request)"
                    >
                      <MessageSquareText class="h-3.5 w-3.5" />
                      Responder
                    </button>
                    <button
                      type="button"
                      class="inline-flex items-center gap-1.5 rounded-lg border border-red-200 px-3 py-1.5 text-xs font-medium text-red-700 transition hover:bg-red-50"
                      @click="openActionModal('reject', request)"
                    >
                      <CircleSlash class="h-3.5 w-3.5" />
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
                class="inline-flex items-center gap-1.5 rounded-lg border border-blue-200 px-3 py-1.5 text-xs font-medium text-blue-700 transition hover:bg-blue-50"
                @click="openActionModal('reply', request)"
              >
                <MessageSquareText class="h-3.5 w-3.5" />
                Responder
              </button>
              <button
                type="button"
                class="inline-flex items-center gap-1.5 rounded-lg border border-red-200 px-3 py-1.5 text-xs font-medium text-red-700 transition hover:bg-red-50"
                @click="openActionModal('reject', request)"
              >
                <CircleSlash class="h-3.5 w-3.5" />
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
      @reply="selectedRequest && openActionModal('reply', selectedRequest)"
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
