<script setup lang="ts">
import { ref, watch, computed, onMounted } from 'vue';
import { Search, X, Users, ChevronLeft, ChevronRight } from 'lucide-vue-next';
import AdminLayout from '@/components/layout/AdminLayout.vue';
import UserDrawer from './UserDrawer.vue';
import { useAdminUserStore } from '@/stores/adminUserStore';
import { type AdminUserSummary } from '@/api/services/adminUserService';

const store = useAdminUserStore();

const searchInput = ref('');
const typeFilter = ref('');
const statusFilter = ref('');
const dateFrom = ref('');
const dateTo = ref('');
const includeDeleted = ref(false);

const drawerOpen = ref(false);
const drawerUserId = ref<number | null>(null);

let debounceTimer: ReturnType<typeof setTimeout> | null = null;

const applyFilters = (page = 0) => {
  store.filters.search = searchInput.value;
  store.filters.type = typeFilter.value as '' | 'CONTRACTOR' | 'FREELANCER' | 'ADMIN';
  store.filters.status = statusFilter.value as '' | 'active' | 'inactive';
  store.filters.dateFrom = dateFrom.value;
  store.filters.dateTo = dateTo.value;
  store.filters.includeDeleted = includeDeleted.value;
  store.filters.page = page;
  store.loadUsers();
};

watch(searchInput, () => {
  if (debounceTimer) clearTimeout(debounceTimer);
  debounceTimer = setTimeout(() => applyFilters(0), 300);
});

watch([typeFilter, statusFilter, dateFrom, dateTo, includeDeleted], () => {
  applyFilters(0);
});

const clearFilters = () => {
  searchInput.value = '';
  typeFilter.value = '';
  statusFilter.value = '';
  dateFrom.value = '';
  dateTo.value = '';
  includeDeleted.value = false;
  applyFilters(0);
};

const goToPage = (page: number) => {
  applyFilters(page);
};

const openDrawer = async (user: AdminUserSummary) => {
  drawerUserId.value = user.id;
  drawerOpen.value = true;
  await store.loadUserById(user.id);
};

const closeDrawer = () => {
  drawerOpen.value = false;
  store.clearSelectedUser();
};

const onRefresh = () => {
  applyFilters(store.currentPage);
  if (drawerUserId.value !== null) {
    store.loadUserById(drawerUserId.value);
  }
};

const getInitials = (name: string) => {
  return name
    .split(' ')
    .slice(0, 2)
    .map((n) => n[0])
    .join('')
    .toUpperCase();
};

const avatarColors = [
  'bg-blue-500', 'bg-purple-500', 'bg-emerald-500',
  'bg-orange-500', 'bg-rose-500', 'bg-cyan-500',
];
const getAvatarColor = (id: number) => avatarColors[id % avatarColors.length];

const userTypeBadge = (type: string) => {
  switch (type) {
    case 'FREELANCER': return { label: 'Freelancer', cls: 'bg-blue-50 text-blue-700' };
    case 'CONTRACTOR': return { label: 'Contratante', cls: 'bg-purple-50 text-purple-700' };
    case 'ADMIN': return { label: 'Admin', cls: 'bg-red-50 text-red-700' };
    default: return { label: type, cls: 'bg-slate-100 text-slate-600' };
  }
};

const formatDate = (dateStr: string) => {
  return new Date(dateStr).toLocaleDateString('pt-BR', {
    day: '2-digit', month: '2-digit', year: 'numeric',
  });
};

const pageNumbers = computed(() => {
  const total = store.totalPages;
  const current = store.currentPage;
  if (total <= 7) return Array.from({ length: total }, (_, i) => i);
  const pages: (number | '...')[] = [];
  if (current <= 3) {
    pages.push(0, 1, 2, 3, 4, '...', total - 1);
  } else if (current >= total - 4) {
    pages.push(0, '...', total - 5, total - 4, total - 3, total - 2, total - 1);
  } else {
    pages.push(0, '...', current - 1, current, current + 1, '...', total - 1);
  }
  return pages;
});

onMounted(() => applyFilters(0));
</script>

<template>
  <AdminLayout>
    <div class="space-y-6">
      <!-- Header -->
      <div>
        <h1 class="text-2xl font-bold text-slate-900">Gestão de Usuários</h1>
        <p class="mt-1 text-sm text-slate-500">
          Gerencie todos os usuários da plataforma
        </p>
      </div>

      <!-- Filtros -->
      <div class="rounded-xl border border-slate-200 bg-white p-4 shadow-sm">
        <div class="flex flex-wrap gap-3">
          <!-- Search -->
          <div class="relative min-w-[220px] flex-1">
            <Search class="absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-slate-400" />
            <input
              v-model="searchInput"
              type="text"
              placeholder="Buscar por nome ou email..."
              class="w-full rounded-lg border border-slate-200 py-2 pl-9 pr-3 text-sm outline-none transition focus:border-blue-400 focus:ring-2 focus:ring-blue-100"
            />
          </div>

          <!-- Type filter -->
          <select
            v-model="typeFilter"
            class="rounded-lg border border-slate-200 px-3 py-2 text-sm outline-none focus:border-blue-400 focus:ring-2 focus:ring-blue-100"
          >
            <option value="">Todos os tipos</option>
            <option value="FREELANCER">Freelancer</option>
            <option value="CONTRACTOR">Contratante</option>
            <option value="ADMIN">Admin</option>
          </select>

          <!-- Status filter -->
          <select
            v-model="statusFilter"
            class="rounded-lg border border-slate-200 px-3 py-2 text-sm outline-none focus:border-blue-400 focus:ring-2 focus:ring-blue-100"
          >
            <option value="">Todos os status</option>
            <option value="active">Ativo</option>
            <option value="inactive">Bloqueado</option>
          </select>

          <!-- Date range -->
          <input
            v-model="dateFrom"
            type="date"
            title="De"
            class="rounded-lg border border-slate-200 px-3 py-2 text-sm outline-none focus:border-blue-400 focus:ring-2 focus:ring-blue-100"
          />
          <input
            v-model="dateTo"
            type="date"
            title="Até"
            class="rounded-lg border border-slate-200 px-3 py-2 text-sm outline-none focus:border-blue-400 focus:ring-2 focus:ring-blue-100"
          />

          <!-- Include deleted -->
          <label class="flex cursor-pointer items-center gap-2 rounded-lg border border-slate-200 px-3 py-2 text-sm">
            <input v-model="includeDeleted" type="checkbox" class="h-4 w-4 rounded border-slate-300" />
            Incluir desativados
          </label>

          <!-- Clear filters -->
          <button
            class="flex items-center gap-1.5 rounded-lg border border-slate-200 px-3 py-2 text-sm text-slate-600 transition hover:bg-slate-50"
            @click="clearFilters"
          >
            <X class="h-4 w-4" />
            Limpar filtros
          </button>
        </div>
      </div>

      <!-- Table -->
      <div class="overflow-hidden rounded-xl border border-slate-200 bg-white shadow-sm">
        <!-- Loading skeleton -->
        <template v-if="store.isLoading">
          <div class="divide-y divide-slate-100">
            <div v-for="i in 8" :key="i" class="flex items-center gap-4 px-6 py-4">
              <div class="h-9 w-9 animate-pulse rounded-full bg-slate-200" />
              <div class="flex-1 space-y-2">
                <div class="h-3.5 w-36 animate-pulse rounded bg-slate-200" />
                <div class="h-3 w-48 animate-pulse rounded bg-slate-200" />
              </div>
              <div class="h-5 w-20 animate-pulse rounded-full bg-slate-200" />
              <div class="h-5 w-16 animate-pulse rounded-full bg-slate-200" />
              <div class="h-3 w-24 animate-pulse rounded bg-slate-200" />
            </div>
          </div>
        </template>

        <!-- Empty state -->
        <template v-else-if="store.users.length === 0">
          <div class="flex flex-col items-center justify-center px-6 py-16 text-center">
            <div class="mb-4 flex h-16 w-16 items-center justify-center rounded-full bg-slate-100">
              <Users class="h-8 w-8 text-slate-400" />
            </div>
            <p class="text-base font-medium text-slate-900">Nenhum usuário encontrado</p>
            <p class="mt-1 text-sm text-slate-500">Tente ajustar os filtros ou busca.</p>
          </div>
        </template>

        <!-- Table content -->
        <template v-else>
          <table class="min-w-full divide-y divide-slate-100">
            <thead class="bg-slate-50">
              <tr>
                <th class="px-6 py-3 text-left text-xs font-semibold uppercase tracking-wide text-slate-500">Usuário</th>
                <th class="px-4 py-3 text-left text-xs font-semibold uppercase tracking-wide text-slate-500">Tipo</th>
                <th class="px-4 py-3 text-left text-xs font-semibold uppercase tracking-wide text-slate-500">Status</th>
                <th class="px-4 py-3 text-left text-xs font-semibold uppercase tracking-wide text-slate-500">Cadastro</th>
              </tr>
            </thead>
            <tbody class="divide-y divide-slate-100">
              <tr
                v-for="user in store.users"
                :key="user.id"
                class="cursor-pointer transition hover:bg-slate-50"
                :class="{ 'opacity-60': user.deletedAt !== null }"
                @click="openDrawer(user)"
              >
                <td class="px-6 py-4">
                  <div class="flex items-center gap-3">
                    <div
                      v-if="user.avatarUrl"
                      class="h-9 w-9 overflow-hidden rounded-full"
                    >
                      <img :src="user.avatarUrl" :alt="user.name" class="h-full w-full object-cover" />
                    </div>
                    <div
                      v-else
                      :class="['flex h-9 w-9 items-center justify-center rounded-full text-xs font-semibold text-white', getAvatarColor(user.id)]"
                    >
                      {{ getInitials(user.name) }}
                    </div>
                    <div>
                      <p class="text-sm font-medium text-slate-900">{{ user.name }}</p>
                      <p class="text-xs text-slate-500">{{ user.email }}</p>
                    </div>
                  </div>
                </td>
                <td class="px-4 py-4">
                  <span :class="['inline-flex items-center rounded-full px-2.5 py-0.5 text-xs font-medium', userTypeBadge(user.userType).cls]">
                    {{ userTypeBadge(user.userType).label }}
                  </span>
                </td>
                <td class="px-4 py-4">
                  <span v-if="user.deletedAt" class="inline-flex items-center rounded-full border border-red-200 bg-red-50 px-2.5 py-0.5 text-xs font-medium text-red-700">
                    Deletado
                  </span>
                  <span v-else-if="user.active" class="inline-flex items-center rounded-full bg-emerald-50 px-2.5 py-0.5 text-xs font-medium text-emerald-700">
                    Ativo
                  </span>
                  <span v-else class="inline-flex items-center rounded-full bg-slate-100 px-2.5 py-0.5 text-xs font-medium text-slate-600">
                    Bloqueado
                  </span>
                </td>
                <td class="px-4 py-4 text-xs text-slate-500">
                  {{ formatDate(user.createdAt) }}
                </td>
              </tr>
            </tbody>
          </table>
        </template>
      </div>

      <!-- Pagination -->
      <div v-if="store.totalPages > 1" class="flex items-center justify-between">
        <p class="text-sm text-slate-500">
          {{ store.totalElements }} usuários no total
        </p>
        <div class="flex items-center gap-1">
          <button
            class="flex h-8 w-8 items-center justify-center rounded-lg border border-slate-200 text-slate-600 transition hover:bg-slate-50 disabled:opacity-40"
            :disabled="store.currentPage === 0"
            @click="goToPage(store.currentPage - 1)"
          >
            <ChevronLeft class="h-4 w-4" />
          </button>

          <template v-for="(pg, idx) in pageNumbers" :key="idx">
            <span v-if="pg === '...'" class="flex h-8 w-8 items-center justify-center text-sm text-slate-400">…</span>
            <button
              v-else
              :class="[
                'flex h-8 w-8 items-center justify-center rounded-lg border text-sm transition',
                pg === store.currentPage
                  ? 'border-blue-500 bg-blue-500 text-white'
                  : 'border-slate-200 text-slate-700 hover:bg-slate-50',
              ]"
              @click="goToPage(pg as number)"
            >
              {{ (pg as number) + 1 }}
            </button>
          </template>

          <button
            class="flex h-8 w-8 items-center justify-center rounded-lg border border-slate-200 text-slate-600 transition hover:bg-slate-50 disabled:opacity-40"
            :disabled="store.currentPage >= store.totalPages - 1"
            @click="goToPage(store.currentPage + 1)"
          >
            <ChevronRight class="h-4 w-4" />
          </button>
        </div>
      </div>
    </div>

    <!-- User Drawer -->
    <UserDrawer
      :user="store.selectedUser"
      :open="drawerOpen"
      @close="closeDrawer"
      @refresh="onRefresh"
    />
  </AdminLayout>
</template>
