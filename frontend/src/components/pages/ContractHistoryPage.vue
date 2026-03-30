<script setup lang="ts">
import { ref, computed } from 'vue';
import {
  Download,
  Eye,
  RefreshCw,
  Calendar,
  DollarSign,
  Filter,
  X,
  CheckCircle,
  AlertCircle,
  Clock,
} from 'lucide-vue-next';
import Button from '../ui/button.vue';
import Badge from '../ui/badge.vue';
import Card from '../ui/card.vue';
import Label from '../ui/label.vue';
import Select from '../ui/select.vue';
import SelectContent from '../ui/select-content.vue';
import SelectItem from '../ui/select-item.vue';
import SelectTrigger from '../ui/select-trigger.vue';
import SelectValue from '../ui/select-value.vue';
import Sheet from '../ui/sheet.vue';
import SheetContent from '../ui/sheet-content.vue';
import SheetDescription from '../ui/sheet-description.vue';
import SheetHeader from '../ui/sheet-header.vue';
import SheetTitle from '../ui/sheet-title.vue';
import SheetTrigger from '../ui/sheet-trigger.vue';
import Avatar from '../ui/avatar.vue';
import AvatarFallback from '../ui/avatar-fallback.vue';
import AvatarImage from '../ui/avatar-image.vue';

interface Contract {
  id: string;
  freelancerId: string;
  freelancerName: string;
  freelancerAvatar?: string;
  service: string;
  category: string;
  amount: number;
  completedDate: string;
  status: 'completed' | 'cancelled';
  rating?: number;
  hasReceipt: boolean;
}

interface Props {
  onNavigate: (page: string, params?: any) => void;
}

const props = defineProps<Props>();

const showFilters = ref(false);
const statusFilter = ref('all');
const categoryFilter = ref('all');
const periodFilter = ref('all');
const currentPage = ref(1);
const itemsPerPage = 10;

// Mock data - substituir por dados reais da API
const contracts = ref<Contract[]>([
  {
    id: '1',
    freelancerId: '1',
    freelancerName: 'João Silva',
    freelancerAvatar: '',
    service: 'Instalação Elétrica Residencial',
    category: 'Eletricista',
    amount: 3200,
    completedDate: '2026-11-05',
    status: 'completed',
    rating: 5,
    hasReceipt: true,
  },
  {
    id: '2',
    freelancerId: '2',
    freelancerName: 'Maria Santos',
    freelancerAvatar: '',
    service: 'Design de Logo e Identidade Visual',
    category: 'Design',
    amount: 1800,
    completedDate: '2026-10-28',
    status: 'completed',
    rating: 5,
    hasReceipt: true,
  },
  {
    id: '3',
    freelancerId: '3',
    freelancerName: 'Pedro Oliveira',
    freelancerAvatar: '',
    service: 'Reparo de Vazamento',
    category: 'Encanamento',
    amount: 450,
    completedDate: '2026-10-15',
    status: 'completed',
    rating: 4,
    hasReceipt: true,
  },
  {
    id: '4',
    freelancerId: '4',
    freelancerName: 'Ana Costa',
    freelancerAvatar: '',
    service: 'Desenvolvimento de Website',
    category: 'Tecnologia',
    amount: 5500,
    completedDate: '2026-09-20',
    status: 'completed',
    rating: 5,
    hasReceipt: true,
  },
  {
    id: '5',
    freelancerId: '5',
    freelancerName: 'Carlos Mendes',
    freelancerAvatar: '',
    service: 'Pintura de Apartamento',
    category: 'Pintura',
    amount: 2800,
    completedDate: '2026-08-10',
    status: 'cancelled',
    hasReceipt: false,
  },
]);

const categories = ['Eletricista', 'Design', 'Encanamento', 'Tecnologia', 'Pintura'];

// Filtrar contratos
const filteredContracts = computed(() =>
  contracts.value.filter((contract) => {
    if (statusFilter.value !== 'all' && contract.status !== statusFilter.value) return false;
    if (categoryFilter.value !== 'all' && contract.category !== categoryFilter.value)
      return false;

    if (periodFilter.value !== 'all') {
      const contractDate = new Date(contract.completedDate);
      const now = new Date();
      const monthsAgo = new Date();

      switch (periodFilter.value) {
        case 'last-30':
          monthsAgo.setDate(now.getDate() - 30);
          if (contractDate < monthsAgo) return false;
          break;
        case 'last-60':
          monthsAgo.setDate(now.getDate() - 60);
          if (contractDate < monthsAgo) return false;
          break;
        case 'last-90':
          monthsAgo.setDate(now.getDate() - 90);
          if (contractDate < monthsAgo) return false;
          break;
      }
    }

    return true;
  })
);

// Paginação
const totalPages = computed(() => Math.ceil(filteredContracts.value.length / itemsPerPage));
const startIndex = computed(() => (currentPage.value - 1) * itemsPerPage);
const endIndex = computed(() => startIndex.value + itemsPerPage);
const currentContracts = computed(() =>
  filteredContracts.value.slice(startIndex.value, endIndex.value)
);

const getInitials = (name: string) => {
  return name
    .split(' ')
    .map((n) => n[0])
    .join('')
    .toUpperCase()
    .slice(0, 2);
};

const formatDate = (dateString: string) => {
  const date = new Date(dateString);
  return date.toLocaleDateString('pt-BR');
};

const formatCurrency = (value: number) => {
  return new Intl.NumberFormat('pt-BR', {
    style: 'currency',
    currency: 'BRL',
  }).format(value);
};

const handleDownloadReceipt = (contractId: string) => {
  alert(`Baixando recibo do contrato ${contractId}...`);
};

const handleRepeatContract = (contract: Contract) => {
  props.onNavigate('request-quote', {
    freelancerId: contract.freelancerId,
    freelancerName: contract.freelancerName,
    service: contract.service,
  });
};

const activeFiltersCount = computed(
  () =>
    (statusFilter.value !== 'all' ? 1 : 0) +
    (categoryFilter.value !== 'all' ? 1 : 0) +
    (periodFilter.value !== 'all' ? 1 : 0)
);

const clearFilters = () => {
  statusFilter.value = 'all';
  categoryFilter.value = 'all';
  periodFilter.value = 'all';
};

const totalSpent = computed(() =>
  contracts.value
    .filter((c) => c.status === 'completed')
    .reduce((sum, c) => sum + c.amount, 0)
);

const completedCount = computed(() =>
  contracts.value.filter((c) => c.status === 'completed').length
);

const averageAmount = computed(() =>
  completedCount.value > 0 ? totalSpent.value / completedCount.value : 0
);
</script>

<template>
  <div class="max-w-7xl mx-auto space-y-6">
    <!-- Header -->
    <div class="flex items-start justify-between">
      <div>
        <h1 class="text-[#263238] mb-2">Histórico de Contratações</h1>
        <p class="text-[#435574]">Gerencie seus contratos concluídos e baixe recibos</p>
      </div>

      <!-- Filters Button -->
      <Sheet :open="showFilters" @update:open="(val) => showFilters = val">
        <SheetTrigger>
          <Button variant="outline" class="relative">
            <Filter class="h-4 w-4 mr-2" />
            Filtros
            <Badge v-if="activeFiltersCount > 0" class="ml-2 bg-primary hover:bg-primary">
              {{ activeFiltersCount }}
            </Badge>
          </Button>
        </SheetTrigger>
        <SheetContent>
          <SheetHeader>
            <SheetTitle>Filtros</SheetTitle>
            <SheetDescription>Filtre o histórico de contratações</SheetDescription>
          </SheetHeader>

          <div class="space-y-6 mt-6">
            <!-- Status Filter -->
            <div class="space-y-2">
              <Label>Status</Label>
              <Select :model-value="statusFilter" @update:model-value="(val) => statusFilter = val as string">
                <SelectTrigger>
                  <SelectValue />
                </SelectTrigger>
                <SelectContent>
                  <SelectItem value="all">Todos</SelectItem>
                  <SelectItem value="completed">Concluídos</SelectItem>
                  <SelectItem value="cancelled">Cancelados</SelectItem>
                </SelectContent>
              </Select>
            </div>

            <!-- Category Filter -->
            <div class="space-y-2">
              <Label>Categoria</Label>
              <Select
                :model-value="categoryFilter"
                @update:model-value="(val) => categoryFilter = val as string"
              >
                <SelectTrigger>
                  <SelectValue />
                </SelectTrigger>
                <SelectContent>
                  <SelectItem value="all">Todas</SelectItem>
                  <SelectItem v-for="cat in categories" :key="cat" :value="cat">
                    {{ cat }}
                  </SelectItem>
                </SelectContent>
              </Select>
            </div>

            <!-- Period Filter -->
            <div class="space-y-2">
              <Label>Período</Label>
              <Select
                :model-value="periodFilter"
                @update:model-value="(val) => periodFilter = val as string"
              >
                <SelectTrigger>
                  <SelectValue />
                </SelectTrigger>
                <SelectContent>
                  <SelectItem value="all">Todo o período</SelectItem>
                  <SelectItem value="last-30">Últimos 30 dias</SelectItem>
                  <SelectItem value="last-60">Últimos 60 dias</SelectItem>
                  <SelectItem value="last-90">Últimos 90 dias</SelectItem>
                </SelectContent>
              </Select>
            </div>

            <!-- Clear Filters -->
            <Button v-if="activeFiltersCount > 0" @click="clearFilters" variant="outline" class="w-full">
              <X class="h-4 w-4 mr-2" />
              Limpar Filtros
            </Button>
          </div>
        </SheetContent>
      </Sheet>
    </div>

    <!-- Stats Cards -->
    <div class="grid grid-cols-1 md:grid-cols-3 gap-4">
      <Card class="p-4">
        <div class="flex items-center gap-3">
          <div class="w-10 h-10 rounded-full bg-green-100 flex items-center justify-center">
            <CheckCircle class="h-5 w-5 text-green-600" />
          </div>
          <div>
            <p class="text-sm text-[#435574]">Contratos Concluídos</p>
            <p class="text-xl text-[#263238]">{{ completedCount }}</p>
          </div>
        </div>
      </Card>

      <Card class="p-4">
        <div class="flex items-center gap-3">
          <div class="w-10 h-10 rounded-full bg-blue-100 flex items-center justify-center">
            <DollarSign class="h-5 w-5 text-[#286AD8]" />
          </div>
          <div>
            <p class="text-sm text-[#435574]">Total Investido</p>
            <p class="text-xl text-[#263238]">{{ formatCurrency(totalSpent) }}</p>
          </div>
        </div>
      </Card>

      <Card class="p-4">
        <div class="flex items-center gap-3">
          <div class="w-10 h-10 rounded-full bg-purple-100 flex items-center justify-center">
            <RefreshCw class="h-5 w-5 text-purple-600" />
          </div>
          <div>
            <p class="text-sm text-[#435574]">Média por Contrato</p>
            <p class="text-xl text-[#263238]">{{ formatCurrency(averageAmount) }}</p>
          </div>
        </div>
      </Card>
    </div>

    <!-- Contracts Table -->
    <Card v-if="currentContracts.length === 0" class="p-12 text-center">
      <div class="flex flex-col items-center gap-4">
        <Clock class="h-12 w-12 text-gray-400" />
        <div>
          <h3 class="text-[#263238] mb-2">Nenhum contrato encontrado</h3>
          <p class="text-[#435574]">
            {{ activeFiltersCount > 0 ? 'Tente ajustar os filtros' : 'Você ainda não concluiu nenhuma contratação' }}
          </p>
        </div>
      </div>
    </Card>
    <Card v-else class="overflow-hidden">
      <!-- Desktop Table -->
      <div class="hidden md:block overflow-x-auto">
        <table class="w-full">
          <thead class="bg-gray-50 border-b">
            <tr>
              <th class="px-6 py-3 text-left text-xs text-[#435574] uppercase tracking-wider">
                Freelancer
              </th>
              <th class="px-6 py-3 text-left text-xs text-[#435574] uppercase tracking-wider">
                Serviço
              </th>
              <th class="px-6 py-3 text-left text-xs text-[#435574] uppercase tracking-wider">
                Valor
              </th>
              <th class="px-6 py-3 text-left text-xs text-[#435574] uppercase tracking-wider">
                Data
              </th>
              <th class="px-6 py-3 text-left text-xs text-[#435574] uppercase tracking-wider">
                Status
              </th>
              <th class="px-6 py-3 text-left text-xs text-[#435574] uppercase tracking-wider">
                Ações
              </th>
            </tr>
          </thead>
          <tbody class="divide-y divide-gray-200">
            <tr v-for="contract in currentContracts" :key="contract.id" class="hover:bg-gray-50">
              <td class="px-6 py-4 whitespace-nowrap">
                <div class="flex items-center gap-3">
                  <Avatar class="h-10 w-10">
                    <AvatarImage :src="contract.freelancerAvatar" :alt="contract.freelancerName" />
                    <AvatarFallback class="bg-[#286AD8] text-white text-sm">
                      {{ getInitials(contract.freelancerName) }}
                    </AvatarFallback>
                  </Avatar>
                  <div>
                    <p class="text-sm text-[#263238]">{{ contract.freelancerName }}</p>
                    <div v-if="contract.rating" class="flex items-center gap-1 text-xs text-[#435574]">
                      <span>⭐</span>
                      <span>{{ contract.rating.toFixed(1) }}</span>
                    </div>
                  </div>
                </div>
              </td>
              <td class="px-6 py-4">
                <p class="text-sm text-[#263238] max-w-xs truncate">{{ contract.service }}</p>
                <p class="text-xs text-[#435574]">{{ contract.category }}</p>
              </td>
              <td class="px-6 py-4 whitespace-nowrap">
                <p class="text-sm text-[#263238]">{{ formatCurrency(contract.amount) }}</p>
              </td>
              <td class="px-6 py-4 whitespace-nowrap">
                <p class="text-sm text-[#435574]">{{ formatDate(contract.completedDate) }}</p>
              </td>
              <td class="px-6 py-4 whitespace-nowrap">
                <Badge
                  :class="
                    contract.status === 'completed'
                      ? 'bg-green-100 text-green-700 hover:bg-green-100'
                      : 'bg-red-100 text-red-700 hover:bg-red-100'
                  "
                >
                  {{ contract.status === 'completed' ? 'Concluído' : 'Cancelado' }}
                </Badge>
              </td>
              <td class="px-6 py-4 whitespace-nowrap">
                <div class="flex items-center gap-2">
                  <Button
                    v-if="contract.hasReceipt"
                    @click="handleDownloadReceipt(contract.id)"
                    variant="ghost"
                    size="sm"
                    title="Baixar Recibo"
                  >
                    <Download class="h-4 w-4" />
                  </Button>
                  <Button
                    @click="handleRepeatContract(contract)"
                    variant="ghost"
                    size="sm"
                    title="Repetir Contratação"
                  >
                    <RefreshCw class="h-4 w-4" />
                  </Button>
                </div>
              </td>
            </tr>
          </tbody>
        </table>
      </div>

      <!-- Mobile Cards -->
      <div class="md:hidden divide-y">
        <div v-for="contract in currentContracts" :key="contract.id" class="p-4 space-y-3">
          <div class="flex items-center justify-between">
            <div class="flex items-center gap-3">
              <Avatar class="h-10 w-10">
                <AvatarImage :src="contract.freelancerAvatar" :alt="contract.freelancerName" />
                <AvatarFallback class="bg-[#286AD8] text-white text-sm">
                  {{ getInitials(contract.freelancerName) }}
                </AvatarFallback>
              </Avatar>
              <div>
                <p class="text-sm text-[#263238]">{{ contract.freelancerName }}</p>
                <p class="text-xs text-[#435574]">{{ formatDate(contract.completedDate) }}</p>
              </div>
            </div>
            <Badge
              :class="
                contract.status === 'completed'
                  ? 'bg-green-100 text-green-700 hover:bg-green-100'
                  : 'bg-red-100 text-red-700 hover:bg-red-100'
              "
            >
              {{ contract.status === 'completed' ? 'Concluído' : 'Cancelado' }}
            </Badge>
          </div>

          <p class="text-sm text-[#263238]">{{ contract.service }}</p>
          <p class="text-[#263238]">{{ formatCurrency(contract.amount) }}</p>

          <div class="flex gap-2 pt-2">
            <Button
              v-if="contract.hasReceipt"
              @click="handleDownloadReceipt(contract.id)"
              variant="outline"
              size="sm"
              class="flex-1"
            >
              <Download class="h-4 w-4 mr-2" />
              Recibo
            </Button>
            <Button
              @click="handleRepeatContract(contract)"
              variant="outline"
              size="sm"
              class="flex-1"
            >
              <RefreshCw class="h-4 w-4 mr-2" />
              Repetir
            </Button>
          </div>
        </div>
      </div>
    </Card>

    <!-- Pagination -->
    <div v-if="totalPages > 1" class="flex items-center justify-center gap-2">
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
  </div>
</template>

