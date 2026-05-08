<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted } from 'vue';
import { useServiceRequestWebSocket, type ServiceRequestNotification } from '@/composables/useServiceRequestWebSocket';
import {
  TrendingUp,
  DollarSign,
  Briefcase,
  Clock,
  Star,
  ArrowUp,
  ArrowDown,
  FileText,
  CheckCircle,
  AlertCircle,
  Eye,
  MessageSquare,
  Calendar,
} from 'lucide-vue-next';
import { useProposalStore, type Proposal } from '@/stores/proposalStore';
import reviewService, { type ReviewResponse } from '@/api/services/reviewService';
import { formatTimeAgoFromApiDate, getApiDateTimestamp } from '@/utils/apiDate';
import Card from '../ui/card.vue';
import Badge from '../ui/badge.vue';
import Button from '../ui/button.vue';
import Avatar from '../ui/avatar.vue';
import AvatarImage from '../ui/avatar-image.vue';
import AvatarFallback from '../ui/avatar-fallback.vue';
import Progress from '../ui/progress.vue';
import { toast } from '@/utils/toast';

interface Props {
  onNavigate: (page: string, params?: any) => void;
}

const props = defineProps<Props>();

let wsConnection: ReturnType<typeof useServiceRequestWebSocket> | null = null;
const proposalStore = useProposalStore();
const reviews = ref<ReviewResponse[]>([]);
const isLoadingReviews = ref(false);

const selectedPeriod = ref<'week' | 'month' | 'year'>('month');

const getWindowDays = () => {
  switch (selectedPeriod.value) {
    case 'week':
      return 7;
    case 'year':
      return 365;
    case 'month':
    default:
      return 30;
  }
};

const parseDate = (value: string) => {
  const parsed = new Date(value);
  return Number.isNaN(parsed.getTime()) ? null : parsed;
};

const getProposalAmount = (proposal: Proposal) =>
  proposal.freelancerResponse?.value ?? proposal.budget ?? 0;

const periodProposals = computed(() => {
  const windowDays = getWindowDays();
  const now = new Date();
  const start = new Date(now);
  start.setDate(now.getDate() - windowDays);

  return proposalStore.proposals.filter((proposal) => {
    const date = parseDate(proposal.receivedAt);
    return date !== null && date >= start && date <= now;
  });
});

const previousPeriodProposals = computed(() => {
  const windowDays = getWindowDays();
  const now = new Date();
  const end = new Date(now);
  end.setDate(now.getDate() - windowDays);
  const start = new Date(end);
  start.setDate(end.getDate() - windowDays);

  return proposalStore.proposals.filter((proposal) => {
    const date = parseDate(proposal.receivedAt);
    return date !== null && date >= start && date < end;
  });
});

const periodReviews = computed(() => {
  const windowDays = getWindowDays();
  const now = new Date();
  const start = new Date(now);
  start.setDate(now.getDate() - windowDays);

  return reviews.value.filter((review) => {
    const date = parseDate(review.createdAt);
    return date !== null && date >= start && date <= now;
  });
});

const previousPeriodReviews = computed(() => {
  const windowDays = getWindowDays();
  const now = new Date();
  const end = new Date(now);
  end.setDate(now.getDate() - windowDays);
  const start = new Date(end);
  start.setDate(end.getDate() - windowDays);

  return reviews.value.filter((review) => {
    const date = parseDate(review.createdAt);
    return date !== null && date >= start && date < end;
  });
});

const averageRating = (list: ReviewResponse[]) => {
  if (list.length === 0) return 0;
  return list.reduce((sum, review) => sum + review.rating, 0) / list.length;
};

const calculateChange = (current: number, previous: number) => {
  if (previous === 0) return current > 0 ? 100 : 0;
  return Number((((current - previous) / previous) * 100).toFixed(1));
};

const comparisonLabel = computed(() => {
  switch (selectedPeriod.value) {
    case 'week':
      return 'semana anterior';
    case 'year':
      return 'ano anterior';
    case 'month':
    default:
      return 'mês anterior';
  }
});

const stats = computed(() => {
  const activeStatuses: Proposal['status'][] = ['accepted', 'in_progress', 'delivered'];
  const completedStatuses: Proposal['status'][] = ['completed'];
  const revenueStatuses: Proposal['status'][] = ['accepted', 'in_progress', 'delivered', 'completed'];

  const currentRevenue = periodProposals.value
    .filter((proposal) => revenueStatuses.includes(proposal.status))
    .reduce((sum, proposal) => sum + getProposalAmount(proposal), 0);
  const previousRevenue = previousPeriodProposals.value
    .filter((proposal) => revenueStatuses.includes(proposal.status))
    .reduce((sum, proposal) => sum + getProposalAmount(proposal), 0);

  const currentActiveServices = periodProposals.value.filter((proposal) =>
    activeStatuses.includes(proposal.status),
  ).length;
  const previousActiveServices = previousPeriodProposals.value.filter((proposal) =>
    activeStatuses.includes(proposal.status),
  ).length;

  const currentCompletedJobs = periodProposals.value.filter((proposal) =>
    completedStatuses.includes(proposal.status),
  ).length;
  const previousCompletedJobs = previousPeriodProposals.value.filter((proposal) =>
    completedStatuses.includes(proposal.status),
  ).length;

  const currentRating = averageRating(periodReviews.value);
  const previousRating = averageRating(previousPeriodReviews.value);

  return {
    revenue: {
      current: currentRevenue,
      previous: previousRevenue,
      change: calculateChange(currentRevenue, previousRevenue),
    },
    activeServices: {
      current: currentActiveServices,
      previous: previousActiveServices,
      change: calculateChange(currentActiveServices, previousActiveServices),
    },
    completedJobs: {
      current: currentCompletedJobs,
      previous: previousCompletedJobs,
      change: calculateChange(currentCompletedJobs, previousCompletedJobs),
    },
    rating: {
      current: currentRating,
      previous: previousRating,
      change: calculateChange(currentRating, previousRating),
    },
  };
});

const revenueData = computed(() => {
  const now = new Date();
  const months = Array.from({ length: 6 }, (_, index) => {
    const date = new Date(now.getFullYear(), now.getMonth() - (5 - index), 1);
    return {
      monthKey: `${date.getFullYear()}-${date.getMonth()}`,
      month: date.toLocaleDateString('pt-BR', { month: 'short' }),
      revenue: 0,
      proposals: 0,
    };
  });

  const monthMap = new Map(months.map((item) => [item.monthKey, item]));

  proposalStore.proposals.forEach((proposal) => {
    const date = parseDate(proposal.receivedAt);
    if (!date) return;

    const monthKey = `${date.getFullYear()}-${date.getMonth()}`;
    const monthEntry = monthMap.get(monthKey);
    if (!monthEntry) return;

    monthEntry.proposals += 1;

    if (['accepted', 'in_progress', 'delivered', 'completed'].includes(proposal.status)) {
      monthEntry.revenue += getProposalAmount(proposal);
    }
  });

  return months;
});

const maxRevenueValue = computed(() =>
  Math.max(1, ...revenueData.value.map((item) => item.revenue)),
);

const maxProposalValue = computed(() =>
  Math.max(1, ...revenueData.value.map((item) => item.proposals)),
);

const servicesByCategoryData = computed(() => {
  const palette = ['#286AD8', '#435574', '#2B2E33', '#323842'];
  const counts = new Map<string, number>();

  periodProposals.value.forEach((proposal) => {
    const category = proposal.categoryLabel || proposal.category || 'Sem categoria';
    counts.set(category, (counts.get(category) || 0) + 1);
  });

  const total = periodProposals.value.length || 1;

  return Array.from(counts.entries())
    .sort((a, b) => b[1] - a[1])
    .slice(0, 4)
    .map(([name, count], index) => ({
      name,
      value: Math.round((count / total) * 100),
      color: palette[index % palette.length],
    }));
});

const recentProposals = computed(() =>
  proposalStore.proposals
    .filter((proposal) => ['pending', 'negotiating'].includes(proposal.status))
    .sort(
      (left, right) =>
        getApiDateTimestamp(right.updatedAt || right.receivedAt) -
        getApiDateTimestamp(left.updatedAt || left.receivedAt),
    )
    .slice(0, 3)
    .map((proposal) => ({
      id: proposal.id,
      clientName: proposal.clientName,
      clientAvatar: proposal.clientAvatar,
      service: proposal.service,
      description: proposal.description,
      budget: proposal.budget ?? proposal.freelancerResponse?.value ?? 0,
      deadline: proposal.deadline,
      status: 'pending' as const,
      receivedAt: formatTimeAgoFromApiDate(proposal.receivedAt).replace('Atualizado ', ''),
    })),
);

const activeJobs = computed(() =>
  proposalStore.proposals
    .filter((proposal) => ['accepted', 'in_progress', 'delivered'].includes(proposal.status))
    .sort(
      (left, right) =>
        getApiDateTimestamp(right.updatedAt || right.receivedAt) -
        getApiDateTimestamp(left.updatedAt || left.receivedAt),
    )
    .slice(0, 3)
    .map((proposal) => ({
      id: proposal.id,
      clientName: proposal.clientName,
      service: proposal.service,
      progress:
        proposal.status === 'accepted'
          ? 25
          : proposal.status === 'in_progress'
            ? 65
            : 95,
      deadline: proposal.freelancerResponse?.executionDays
        ? `${proposal.freelancerResponse.executionDays} dias`
        : proposal.deadline,
      status: 'in-progress' as const,
    })),
);

const recentActivities = computed(() =>
  proposalStore.proposals
    .slice()
    .sort(
      (left, right) =>
        getApiDateTimestamp(right.updatedAt || right.receivedAt) -
        getApiDateTimestamp(left.updatedAt || left.receivedAt),
    )
    .slice(0, 6)
    .map((proposal) => {
      if (proposal.status === 'completed') {
        return {
          id: `${proposal.id}-completed`,
          type: 'completion',
          icon: CheckCircle,
          title: 'Serviço concluído',
          description: `${proposal.service} foi concluído com sucesso`,
          time: formatTimeAgoFromApiDate(proposal.updatedAt || proposal.receivedAt).replace('Atualizado ', ''),
          color: 'text-green-600',
          bgColor: 'bg-green-100',
        };
      }

      if (proposal.status === 'rejected' || proposal.status === 'archived') {
        return {
          id: `${proposal.id}-archived`,
          type: 'status',
          icon: AlertCircle,
          title: 'Solicitação finalizada',
          description: `${proposal.service} foi encerrada`,
          time: formatTimeAgoFromApiDate(proposal.updatedAt || proposal.receivedAt).replace('Atualizado ', ''),
          color: 'text-red-600',
          bgColor: 'bg-red-100',
        };
      }

      if (proposal.status === 'in_progress' || proposal.status === 'accepted' || proposal.status === 'delivered') {
        return {
          id: `${proposal.id}-progress`,
          type: 'progress',
          icon: MessageSquare,
          title: 'Serviço em andamento',
          description: `${proposal.clientName} - ${proposal.service}`,
          time: formatTimeAgoFromApiDate(proposal.updatedAt || proposal.receivedAt).replace('Atualizado ', ''),
          color: 'text-blue-600',
          bgColor: 'bg-blue-100',
        };
      }

      return {
        id: `${proposal.id}-proposal`,
        type: 'proposal',
        icon: FileText,
        title: 'Nova proposta recebida',
        description: `${proposal.clientName} solicitou ${proposal.service}`,
        time: formatTimeAgoFromApiDate(proposal.receivedAt).replace('Atualizado ', ''),
        color: 'text-purple-600',
        bgColor: 'bg-purple-100',
      };
    }),
);

const isDashboardLoading = computed(() => proposalStore.isLoading || isLoadingReviews.value);

const formatCurrency = (value: number) => {
  return new Intl.NumberFormat('pt-BR', {
    style: 'currency',
    currency: 'BRL',
  }).format(value);
};

const getInitials = (name: string) => {
  return name
    .split(' ')
    .map((n) => n[0])
    .join('')
    .toUpperCase()
    .slice(0, 2);
};

const getUserId = () => {
  const userData = localStorage.getItem('user');
  if (!userData) return null;

  try {
    const user = JSON.parse(userData);
    return user.id ? String(user.id) : null;
  } catch {
    return null;
  }
};

const getUserEmail = () => {
  const token = localStorage.getItem('token');
  if (!token) return null;
  try {
    const payload = JSON.parse(atob(token.split('.')[1]));
    return payload.sub || payload.email;
  } catch {
    return null;
  }
};

const loadReviews = async () => {
  const userId = getUserId();
  if (!userId) {
    reviews.value = [];
    return;
  }

  isLoadingReviews.value = true;
  try {
    reviews.value = await reviewService.getReviewsForUser(userId);
  } catch (error) {
    console.error('Erro ao carregar avaliações do dashboard:', error);
    reviews.value = [];
  } finally {
    isLoadingReviews.value = false;
  }
};

const refreshDashboardData = async () => {
  await Promise.all([proposalStore.loadProposals(), loadReviews()]);
};

const handleNotification = (event: any) => {
  const notification: ServiceRequestNotification = event.detail;
  console.log('📩 Notificação recebida no Dashboard:', notification);

  switch (notification.type) {
    case 'NEW_SERVICE_REQUEST':
      toast.success(`Nova solicitação: ${notification.serviceName} - ${notification.contractorName}`);
      break;
    case 'COUNTER_PROPOSAL':
      toast.info(`${notification.contractorName} fez uma contra-proposta de ${notification.counterProposalAmount}`);
      break;
    case 'PROPOSAL_ACCEPTED':
      toast.success(`Proposta aceita por ${notification.contractorName}!`);
      break;
    case 'SERVICE_REQUEST_STATUS_CHANGED':
      if (notification.status === 'IN_PROGRESS') {
        toast.success('Pagamento confirmado! Serviço em andamento');
      } else {
        toast.info(notification.message || `Status alterado para ${notification.status}`);
      }
      break;
    case 'SERVICE_REQUEST_CANCELLED':
      toast.info(`Solicitação cancelada: ${notification.message}`);
      break;
    case 'NEW_REVIEW':
      toast.success(`${notification.reviewerName} avaliou seu serviço com ${notification.rating} estrelas`);
      break;
    default:
      toast.info(notification.message);
  }

  refreshDashboardData();
};

onMounted(async () => {
  await refreshDashboardData();

  const userEmail = getUserEmail();
  if (userEmail) {
    wsConnection = useServiceRequestWebSocket(userEmail);
    wsConnection.connect().catch((error) => {
      console.error('Erro ao conectar WebSocket no dashboard:', error);
    });
    window.addEventListener('service-request-notification', handleNotification);
  } else {
    console.warn('⚠️ Email do usuário não encontrado para WebSocket');
  }
});

onUnmounted(() => {
  window.removeEventListener('service-request-notification', handleNotification);
  if (wsConnection) {
    wsConnection.disconnect();
  }
});
</script>

<template>
  <div class="max-w-7xl mx-auto space-y-6">
    <!-- Header -->
    <div>
      <h1 class="text-[#263238] mb-2">Dashboard</h1>
      <p class="text-[#435574]">Bem-vindo de volta! Aqui está um resumo do seu desempenho.</p>
    </div>

    <!-- Period Selector -->
    <div class="flex gap-2">
      <Button
        @click="selectedPeriod = 'week'"
        :variant="selectedPeriod === 'week' ? 'default' : 'outline'"
        size="sm"
      >
        Semana
      </Button>
      <Button
        @click="selectedPeriod = 'month'"
        :variant="selectedPeriod === 'month' ? 'default' : 'outline'"
        size="sm"
      >
        Mês
      </Button>
      <Button
        @click="selectedPeriod = 'year'"
        :variant="selectedPeriod === 'year' ? 'default' : 'outline'"
        size="sm"
      >
        Ano
      </Button>
    </div>

    <div v-if="isDashboardLoading" class="flex justify-center py-10">
      <div class="animate-spin h-8 w-8 border-4 border-[#286AD8] border-t-transparent rounded-full"></div>
    </div>

    <!-- Stats Cards -->
    <div v-else class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4">
      <!-- Revenue -->
      <Card class="p-6">
        <div class="flex items-start justify-between mb-4">
          <div class="w-12 h-12 rounded-full bg-green-100 flex items-center justify-center">
            <DollarSign class="h-6 w-6 text-green-600" />
          </div>
          <Badge
            :class="`${
              stats.revenue.change >= 0
                ? 'bg-green-100 text-green-700 hover:bg-green-100'
                : 'bg-red-100 text-red-700 hover:bg-red-100'
            }`"
          >
            <span class="flex items-center gap-1">
              <ArrowUp v-if="stats.revenue.change >= 0" class="h-3 w-3" />
              <ArrowDown v-else class="h-3 w-3" />
              {{ Math.abs(stats.revenue.change) }}%
            </span>
          </Badge>
        </div>
        <div>
          <p class="text-sm text-[#435574] mb-1">Receita Total</p>
          <p class="text-2xl text-[#263238]">{{ formatCurrency(stats.revenue.current) }}</p>
          <p class="text-xs text-[#435574] mt-1">
            vs {{ formatCurrency(stats.revenue.previous) }} {{ comparisonLabel }}
          </p>
        </div>
      </Card>

      <!-- Active Services -->
      <Card class="p-6">
        <div class="flex items-start justify-between mb-4">
          <div class="w-12 h-12 rounded-full bg-blue-100 flex items-center justify-center">
            <Briefcase class="h-6 w-6 text-[#286AD8]" />
          </div>
          <Badge
            :class="`${
              stats.activeServices.change >= 0
                ? 'bg-green-100 text-green-700 hover:bg-green-100'
                : 'bg-red-100 text-red-700 hover:bg-red-100'
            }`"
          >
            <span class="flex items-center gap-1">
              <ArrowUp v-if="stats.activeServices.change >= 0" class="h-3 w-3" />
              <ArrowDown v-else class="h-3 w-3" />
              {{ Math.abs(stats.activeServices.change) }}%
            </span>
          </Badge>
        </div>
        <div>
          <p class="text-sm text-[#435574] mb-1">Serviços Ativos</p>
          <p class="text-2xl text-[#263238]">{{ stats.activeServices.current }}</p>
          <p class="text-xs text-[#435574] mt-1">
            vs {{ stats.activeServices.previous }} {{ comparisonLabel }}
          </p>
        </div>
      </Card>

      <!-- Completed Jobs -->
      <Card class="p-6">
        <div class="flex items-start justify-between mb-4">
          <div class="w-12 h-12 rounded-full bg-purple-100 flex items-center justify-center">
            <CheckCircle class="h-6 w-6 text-purple-600" />
          </div>
          <Badge
            :class="`${
              stats.completedJobs.change >= 0
                ? 'bg-green-100 text-green-700 hover:bg-green-100'
                : 'bg-red-100 text-red-700 hover:bg-red-100'
            }`"
          >
            <span class="flex items-center gap-1">
              <ArrowUp v-if="stats.completedJobs.change >= 0" class="h-3 w-3" />
              <ArrowDown v-else class="h-3 w-3" />
              {{ Math.abs(stats.completedJobs.change) }}%
            </span>
          </Badge>
        </div>
        <div>
          <p class="text-sm text-[#435574] mb-1">Trabalhos Concluídos</p>
          <p class="text-2xl text-[#263238]">{{ stats.completedJobs.current }}</p>
          <p class="text-xs text-[#435574] mt-1">
            Total no período selecionado
          </p>
        </div>
      </Card>

      <!-- Rating -->
      <Card class="p-6">
        <div class="flex items-start justify-between mb-4">
          <div class="w-12 h-12 rounded-full bg-yellow-100 flex items-center justify-center">
            <Star class="h-6 w-6 text-yellow-600" />
          </div>
          <Badge
            :class="`${
              stats.rating.change >= 0
                ? 'bg-green-100 text-green-700 hover:bg-green-100'
                : 'bg-red-100 text-red-700 hover:bg-red-100'
            }`"
          >
            <span class="flex items-center gap-1">
              <ArrowUp v-if="stats.rating.change >= 0" class="h-3 w-3" />
              <ArrowDown v-else class="h-3 w-3" />
              {{ Math.abs(stats.rating.change) }}%
            </span>
          </Badge>
        </div>
        <div>
          <p class="text-sm text-[#435574] mb-1">Avaliação Média</p>
          <p class="text-2xl text-[#263238]">{{ stats.rating.current.toFixed(1) }} ⭐</p>
          <p class="text-xs text-[#435574] mt-1">
            Baseado em {{ reviews.length }} avaliação{{ reviews.length !== 1 ? 'es' : '' }}
          </p>
        </div>
      </Card>
    </div>

    <!-- Charts Row -->
    <div class="grid grid-cols-1 lg:grid-cols-3 gap-6">
      <!-- Revenue Chart (Simplified) -->
      <Card class="p-6 lg:col-span-2">
        <div class="flex items-center justify-between mb-6">
          <div>
            <h3 class="text-[#263238] mb-1">Receita e Propostas</h3>
            <p class="text-sm text-[#435574]">Últimos 6 meses</p>
          </div>
          <TrendingUp class="h-5 w-5 text-green-600" />
        </div>
        <div class="h-[280px] flex flex-col justify-end gap-4">
          <div class="flex items-end justify-between gap-2 h-full">
            <div
              v-for="(item, index) in revenueData"
              :key="index"
              class="flex-1 flex flex-col items-center gap-2"
            >
              <div class="relative w-full h-full flex flex-col justify-end gap-1">
                <div
                  class="w-full bg-[#286AD8] rounded-t"
                  :style="{ height: `${(item.revenue / maxRevenueValue) * 100}%` }"
                  :title="`Receita: ${formatCurrency(item.revenue)}`"
                />
                <div
                  class="w-full bg-[#435574] rounded-t opacity-70"
                  :style="{ height: `${(item.proposals / maxProposalValue) * 100}%` }"
                  :title="`Propostas: ${item.proposals}`"
                />
              </div>
              <span class="text-xs text-[#435574]">{{ item.month }}</span>
            </div>
          </div>
          <div class="flex items-center justify-center gap-4 mt-2">
            <div class="flex items-center gap-2">
              <div class="w-4 h-4 bg-[#286AD8] rounded"></div>
              <span class="text-sm text-[#435574]">Receita</span>
            </div>
            <div class="flex items-center gap-2">
              <div class="w-4 h-4 bg-[#435574] rounded"></div>
              <span class="text-sm text-[#435574]">Propostas</span>
            </div>
          </div>
        </div>
      </Card>

      <!-- Services by Category (Simplified) -->
      <Card class="p-6">
        <div class="mb-6">
          <h3 class="text-[#263238] mb-1">Serviços por Categoria</h3>
          <p class="text-sm text-[#435574]">Distribuição atual</p>
        </div>
        <div class="h-[280px] flex flex-col justify-center gap-4">
          <template v-if="servicesByCategoryData.length > 0">
            <div
              v-for="item in servicesByCategoryData"
              :key="item.name"
              class="space-y-2"
            >
              <div class="flex items-center justify-between text-sm">
                <div class="flex items-center gap-2">
                  <div
                    class="w-3 h-3 rounded-full"
                    :style="{ backgroundColor: item.color }"
                  />
                  <span class="text-[#435574]">{{ item.name }}</span>
                </div>
                <span class="text-[#263238] font-semibold">{{ item.value }}%</span>
              </div>
              <div class="w-full bg-gray-200 rounded-full h-2">
                <div
                  class="h-2 rounded-full transition-all"
                  :style="{ 
                    width: `${item.value}%`,
                    backgroundColor: item.color 
                  }"
                />
              </div>
            </div>
          </template>
          <p v-else class="text-sm text-[#435574] text-center">
            Sem categorias no período selecionado.
          </p>
        </div>
      </Card>
    </div>

    <!-- Recent Proposals & Active Jobs -->
    <div class="grid grid-cols-1 lg:grid-cols-2 gap-6">
      <!-- Recent Proposals -->
      <Card class="p-6">
        <div class="flex items-center justify-between mb-6">
          <div>
            <h3 class="text-[#263238] mb-1">Propostas Recentes</h3>
            <p class="text-sm text-[#435574]">{{ recentProposals.length }} aguardando resposta</p>
          </div>
          <Button
            @click="props.onNavigate('freelancer-proposals')"
            variant="outline"
            size="sm"
          >
            Ver Todas
          </Button>
        </div>
        <div class="space-y-4">
          <p v-if="recentProposals.length === 0" class="text-sm text-[#435574]">
            Nenhuma proposta pendente no momento.
          </p>
          <div
            v-for="proposal in recentProposals"
            :key="proposal.id"
            class="p-4 rounded-lg border border-gray-200 hover:border-[#286AD8] transition-colors cursor-pointer"
            @click="props.onNavigate('freelancer-proposals', { proposalId: proposal.id })"
          >
            <div class="flex items-start gap-3 mb-3">
              <Avatar class="h-10 w-10">
                <AvatarImage :src="proposal.clientAvatar" :alt="proposal.clientName" />
                <AvatarFallback class="bg-[#286AD8] text-white">
                  {{ getInitials(proposal.clientName) }}
                </AvatarFallback>
              </Avatar>
              <div class="flex-1">
                <div class="flex items-start justify-between mb-1">
                  <div>
                    <p class="text-sm text-[#263238]">{{ proposal.clientName }}</p>
                    <p class="text-xs text-[#435574]">{{ proposal.receivedAt }}</p>
                  </div>
                  <Badge variant="secondary" class="text-xs">
                    {{ proposal.service }}
                  </Badge>
                </div>
                <p class="text-sm text-[#435574] line-clamp-2 mb-2">
                  {{ proposal.description }}
                </p>
                <div class="flex items-center gap-4 text-xs text-[#435574]">
                  <span class="flex items-center gap-1">
                    <DollarSign class="h-3 w-3" />
                    {{ formatCurrency(proposal.budget) }}
                  </span>
                  <span class="flex items-center gap-1">
                    <Clock class="h-3 w-3" />
                    {{ proposal.deadline }}
                  </span>
                </div>
              </div>
            </div>
            <div class="flex gap-2">
              <Button
                size="sm"
                variant="default"
                class="flex-1"
                @click.stop="props.onNavigate('freelancer-proposals', { proposalId: proposal.id })"
              >
                Responder
              </Button>
              <Button
                size="sm"
                variant="outline"
                class="flex-1"
                @click.stop="props.onNavigate('freelancer-proposals', { proposalId: proposal.id })"
              >
                Recusar
              </Button>
            </div>
          </div>
        </div>
      </Card>

      <!-- Active Jobs -->
      <Card class="p-6">
        <div class="flex items-center justify-between mb-6">
          <div>
            <h3 class="text-[#263238] mb-1">Serviços em Andamento</h3>
            <p class="text-sm text-[#435574]">{{ activeJobs.length }} projetos ativos</p>
          </div>
          <Button
            @click="props.onNavigate('freelancer-proposals')"
            variant="outline"
            size="sm"
          >
            Ver Todos
          </Button>
        </div>
        <div class="space-y-4">
          <p v-if="activeJobs.length === 0" class="text-sm text-[#435574]">
            Nenhum serviço ativo encontrado.
          </p>
          <div
            v-for="job in activeJobs"
            :key="job.id"
            class="p-4 rounded-lg border border-gray-200 hover:border-[#286AD8] transition-colors cursor-pointer"
          >
            <div class="flex items-start justify-between mb-3">
              <div>
                <p class="text-sm text-[#263238] mb-1">{{ job.service }}</p>
                <p class="text-xs text-[#435574]">Cliente: {{ job.clientName }}</p>
              </div>
              <Badge variant="secondary" class="text-xs">
                {{ job.deadline }}
              </Badge>
            </div>
            <div class="space-y-2">
              <div class="flex items-center justify-between text-xs">
                <span class="text-[#435574]">Progresso</span>
                <span class="text-[#263238]">{{ job.progress }}%</span>
              </div>
              <Progress :value="job.progress" class="h-2" />
            </div>
            <div class="flex gap-2 mt-3">
              <Button size="sm" variant="outline" class="flex-1">
                <MessageSquare class="h-3 w-3 mr-1" />
                Mensagens
              </Button>
              <Button size="sm" variant="outline" class="flex-1">
                <Eye class="h-3 w-3 mr-1" />
                Ver Detalhes
              </Button>
            </div>
          </div>
        </div>
      </Card>
    </div>

    <!-- Recent Activities -->
    <Card class="p-6">
      <div class="mb-6">
        <h3 class="text-[#263238] mb-1">Atividades Recentes</h3>
        <p class="text-sm text-[#435574]">Últimas atualizações</p>
      </div>
      <div class="space-y-4">
        <p v-if="recentActivities.length === 0" class="text-sm text-[#435574]">
          Nenhuma atividade recente para exibir.
        </p>
        <div
          v-for="activity in recentActivities"
          :key="activity.id"
          class="flex items-start gap-4"
        >
          <div :class="`w-10 h-10 rounded-full ${activity.bgColor} flex items-center justify-center flex-shrink-0`">
            <component :is="activity.icon" :class="`h-5 w-5 ${activity.color}`" />
          </div>
          <div class="flex-1 min-w-0">
            <p class="text-sm text-[#263238]">{{ activity.title }}</p>
            <p class="text-sm text-[#435574]">{{ activity.description }}</p>
            <p class="text-xs text-[#435574] mt-1">{{ activity.time }}</p>
          </div>
        </div>
      </div>
    </Card>
  </div>
</template>

