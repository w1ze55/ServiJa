<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted } from 'vue';
import {
  Chart as ChartJS,
  Title,
  Tooltip,
  Legend,
  BarElement,
  CategoryScale,
  LinearScale,
  ArcElement,
} from 'chart.js';
import { Bar, Doughnut } from 'vue-chartjs';
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
  Wallet,
} from 'lucide-vue-next';
import { useProposalStore, type Proposal } from '@/stores/proposalStore';
import reviewService, { type ReviewResponse } from '@/api/services/reviewService';
import walletService, { type WalletBalance, type WalletTransaction } from '@/api/services/walletService';
import { formatTimeAgoFromApiDate, getApiDateTimestamp } from '@/utils/apiDate';
import Card from '../ui/card.vue';
import Badge from '../ui/badge.vue';
import Button from '../ui/button.vue';
import Avatar from '../ui/avatar.vue';
import AvatarImage from '../ui/avatar-image.vue';
import AvatarFallback from '../ui/avatar-fallback.vue';
import Progress from '../ui/progress.vue';
import { toast } from '@/utils/toast';

ChartJS.register(Title, Tooltip, Legend, BarElement, CategoryScale, LinearScale, ArcElement);

interface Props {
  onNavigate: (page: string, params?: any) => void;
}

const props = defineProps<Props>();

let wsConnection: ReturnType<typeof useServiceRequestWebSocket> | null = null;
const proposalStore = useProposalStore();
const reviews = ref<ReviewResponse[]>([]);
const walletBalance = ref<WalletBalance | null>(null);
const walletTransactions = ref<WalletTransaction[]>([]);
const isLoadingReviews = ref(false);
const isLoadingWallet = ref(false);

const selectedPeriod = ref<'week' | 'month' | 'year'>('month');

const getWindowDays = () => {
  switch (selectedPeriod.value) {
    case 'week': return 7;
    case 'year': return 365;
    default: return 30;
  }
};

const parseDate = (value: string) => {
  const parsed = new Date(value);
  return Number.isNaN(parsed.getTime()) ? null : parsed;
};

const getProposalAmount = (proposal: Proposal) =>
  proposal.freelancerResponse?.value ?? proposal.budget ?? 0;

// ── Period filtering ──────────────────────────────────────────────

const periodProposals = computed(() => {
  const windowDays = getWindowDays();
  const now = new Date();
  const start = new Date(now);
  start.setDate(now.getDate() - windowDays);
  return proposalStore.proposals.filter((p) => {
    const date = parseDate(p.receivedAt);
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
  return proposalStore.proposals.filter((p) => {
    const date = parseDate(p.receivedAt);
    return date !== null && date >= start && date < end;
  });
});

const periodReviews = computed(() => {
  const windowDays = getWindowDays();
  const now = new Date();
  const start = new Date(now);
  start.setDate(now.getDate() - windowDays);
  return reviews.value.filter((r) => {
    const date = parseDate(r.createdAt);
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
  return reviews.value.filter((r) => {
    const date = parseDate(r.createdAt);
    return date !== null && date >= start && date < end;
  });
});

const periodEarnings = computed(() => {
  const windowDays = getWindowDays();
  const now = new Date();
  const start = new Date(now);
  start.setDate(now.getDate() - windowDays);
  return walletTransactions.value.filter((tx) => {
    if (tx.type !== 'EARNING') return false;
    const date = parseDate(tx.date);
    return date !== null && date >= start && date <= now;
  });
});

const previousPeriodEarnings = computed(() => {
  const windowDays = getWindowDays();
  const now = new Date();
  const end = new Date(now);
  end.setDate(now.getDate() - windowDays);
  const start = new Date(end);
  start.setDate(end.getDate() - windowDays);
  return walletTransactions.value.filter((tx) => {
    if (tx.type !== 'EARNING') return false;
    const date = parseDate(tx.date);
    return date !== null && date >= start && date < end;
  });
});

// ── Stats ─────────────────────────────────────────────────────────

const averageRating = (list: ReviewResponse[]) => {
  if (list.length === 0) return 0;
  return list.reduce((sum, r) => sum + r.rating, 0) / list.length;
};

const calculateChange = (current: number, previous: number) => {
  if (previous === 0) return current > 0 ? 100 : 0;
  return Number((((current - previous) / previous) * 100).toFixed(1));
};

const comparisonLabel = computed(() => {
  switch (selectedPeriod.value) {
    case 'week': return 'semana anterior';
    case 'year': return 'ano anterior';
    default: return 'mês anterior';
  }
});

const stats = computed(() => {
  const activeStatuses: Proposal['status'][] = ['accepted', 'in_progress', 'delivered'];
  const completedStatuses: Proposal['status'][] = ['completed'];

  const currentRevenue = periodEarnings.value.reduce((sum, tx) => sum + tx.amount, 0);
  const previousRevenue = previousPeriodEarnings.value.reduce((sum, tx) => sum + tx.amount, 0);

  // Fallback to proposal budgets when no wallet transactions exist yet
  const currentRevenueFallback = periodProposals.value
    .filter((p) => ['accepted', 'in_progress', 'delivered', 'completed'].includes(p.status))
    .reduce((sum, p) => sum + getProposalAmount(p), 0);
  const previousRevenueFallback = previousPeriodProposals.value
    .filter((p) => ['accepted', 'in_progress', 'delivered', 'completed'].includes(p.status))
    .reduce((sum, p) => sum + getProposalAmount(p), 0);

  const revenueNow = currentRevenue > 0 ? currentRevenue : currentRevenueFallback;
  const revenuePrev = previousRevenue > 0 ? previousRevenue : previousRevenueFallback;

  const currentActiveServices = periodProposals.value.filter((p) => activeStatuses.includes(p.status)).length;
  const previousActiveServices = previousPeriodProposals.value.filter((p) => activeStatuses.includes(p.status)).length;

  const currentCompletedJobs = periodProposals.value.filter((p) => completedStatuses.includes(p.status)).length;
  const previousCompletedJobs = previousPeriodProposals.value.filter((p) => completedStatuses.includes(p.status)).length;

  const currentRating = averageRating(periodReviews.value);
  const previousRating = averageRating(previousPeriodReviews.value);

  return {
    revenue: {
      current: revenueNow,
      previous: revenuePrev,
      change: calculateChange(revenueNow, revenuePrev),
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

// ── Chart: Bar (earnings + proposals per month) ───────────────────

const barChartData = computed(() => {
  const now = new Date();
  const months = Array.from({ length: 6 }, (_, i) => {
    const date = new Date(now.getFullYear(), now.getMonth() - (5 - i), 1);
    return {
      key: `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}`,
      label: date.toLocaleDateString('pt-BR', { month: 'short' }),
      earnings: 0,
      proposals: 0,
    };
  });

  const monthMap = new Map(months.map((m) => [m.key, m]));

  walletTransactions.value.forEach((tx) => {
    if (tx.type !== 'EARNING') return;
    const date = parseDate(tx.date);
    if (!date) return;
    const key = `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}`;
    const entry = monthMap.get(key);
    if (entry) entry.earnings += tx.amount;
  });

  // Fallback: use proposal budgets when wallet has no transactions
  if (walletTransactions.value.filter((t) => t.type === 'EARNING').length === 0) {
    proposalStore.proposals.forEach((p) => {
      const date = parseDate(p.receivedAt);
      if (!date) return;
      if (!['accepted', 'in_progress', 'delivered', 'completed'].includes(p.status)) return;
      const key = `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}`;
      const entry = monthMap.get(key);
      if (entry) entry.earnings += getProposalAmount(p);
    });
  }

  proposalStore.proposals.forEach((p) => {
    const date = parseDate(p.receivedAt);
    if (!date) return;
    const key = `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}`;
    const entry = monthMap.get(key);
    if (entry) entry.proposals += 1;
  });

  return {
    labels: months.map((m) => m.label),
    datasets: [
      {
        label: 'Receita (R$)',
        data: months.map((m) => m.earnings),
        backgroundColor: '#286AD8',
        borderRadius: 6,
        borderSkipped: false,
      },
      {
        label: 'Propostas',
        data: months.map((m) => m.proposals),
        backgroundColor: '#43557466',
        borderRadius: 6,
        borderSkipped: false,
      },
    ],
  };
});

const barChartOptions = {
  responsive: true,
  maintainAspectRatio: false,
  plugins: {
    legend: {
      position: 'bottom' as const,
      labels: { color: '#435574', font: { size: 12 }, padding: 16 },
    },
    tooltip: {
      callbacks: {
        label: (ctx: any) =>
          ctx.dataset.label === 'Receita (R$)'
            ? ` ${formatCurrency(ctx.parsed.y)}`
            : ` ${ctx.parsed.y} proposta${ctx.parsed.y !== 1 ? 's' : ''}`,
      },
    },
  },
  scales: {
    x: { grid: { display: false }, ticks: { color: '#435574' } },
    y: {
      grid: { color: '#f1f5f9' },
      ticks: {
        color: '#435574',
        callback: (value: number | string) =>
          typeof value === 'number' && value >= 1000
            ? `R$${(value / 1000).toFixed(0)}k`
            : `R$${value}`,
      },
    },
  },
};

// ── Chart: Doughnut (revenue per service) ─────────────────────────

const doughnutChartData = computed(() => {
  const palette = ['#286AD8', '#435574', '#2B2E33', '#323842'];
  const revenue = new Map<string, number>();

  // Priority: wallet transactions with serviceName (real payments)
  const earningTxs = walletTransactions.value.filter((tx) => tx.type === 'EARNING' && tx.serviceName);
  if (earningTxs.length > 0) {
    earningTxs.forEach((tx) => {
      const name = tx.serviceName!;
      revenue.set(name, (revenue.get(name) || 0) + tx.amount);
    });
  } else {
    // Fallback: proposal amounts grouped by service name
    proposalStore.proposals
      .filter((p) => ['accepted', 'in_progress', 'delivered', 'completed'].includes(p.status))
      .forEach((p) => {
        const name = p.service || 'Sem serviço';
        revenue.set(name, (revenue.get(name) || 0) + getProposalAmount(p));
      });
  }

  const entries = Array.from(revenue.entries())
    .sort((a, b) => b[1] - a[1])
    .slice(0, 5);

  if (entries.length === 0) {
    return {
      labels: ['Sem dados'],
      datasets: [{ data: [1], backgroundColor: ['#e5e7eb'], hoverOffset: 4 }],
    };
  }

  return {
    labels: entries.map(([name]) => name),
    datasets: [{
      data: entries.map(([, amount]) => amount),
      backgroundColor: palette.slice(0, entries.length),
      hoverOffset: 6,
      borderWidth: 2,
      borderColor: '#fff',
    }],
  };
});

const doughnutOptions = {
  responsive: true,
  maintainAspectRatio: false,
  plugins: {
    legend: {
      position: 'bottom' as const,
      labels: { color: '#435574', font: { size: 12 }, padding: 12 },
    },
    tooltip: {
      callbacks: {
        label: (ctx: any) => ` ${ctx.label}: ${formatCurrency(ctx.parsed)}`,
      },
    },
  },
};

// ── Lists ──────────────────────────────────────────────────────────

const recentProposals = computed(() =>
  proposalStore.proposals
    .filter((p) => ['pending', 'negotiating'].includes(p.status))
    .sort((a, b) =>
      getApiDateTimestamp(b.updatedAt || b.receivedAt) -
      getApiDateTimestamp(a.updatedAt || a.receivedAt),
    )
    .slice(0, 3)
    .map((p) => ({
      id: p.id,
      clientName: p.clientName,
      clientAvatar: p.clientAvatar,
      service: p.service,
      description: p.description,
      budget: p.budget ?? p.freelancerResponse?.value ?? 0,
      deadline: p.deadline,
      receivedAt: formatTimeAgoFromApiDate(p.receivedAt).replace('Atualizado ', ''),
    })),
);

const activeJobs = computed(() =>
  proposalStore.proposals
    .filter((p) => ['accepted', 'in_progress', 'delivered'].includes(p.status))
    .sort((a, b) =>
      getApiDateTimestamp(b.updatedAt || b.receivedAt) -
      getApiDateTimestamp(a.updatedAt || a.receivedAt),
    )
    .slice(0, 3)
    .map((p) => ({
      id: p.id,
      clientName: p.clientName,
      service: p.service,
      progress: p.status === 'accepted' ? 25 : p.status === 'in_progress' ? 65 : 95,
      deadline: p.freelancerResponse?.executionDays
        ? `${p.freelancerResponse.executionDays} dias`
        : p.deadline,
    })),
);

const recentActivities = computed(() =>
  proposalStore.proposals
    .slice()
    .sort((a, b) =>
      getApiDateTimestamp(b.updatedAt || b.receivedAt) -
      getApiDateTimestamp(a.updatedAt || a.receivedAt),
    )
    .slice(0, 6)
    .map((p) => {
      const time = formatTimeAgoFromApiDate(p.updatedAt || p.receivedAt).replace('Atualizado ', '');
      if (p.status === 'completed') {
        return { id: `${p.id}-c`, icon: CheckCircle, title: 'Serviço concluído', description: `${p.service} foi concluído com sucesso`, time, color: 'text-green-600', bgColor: 'bg-green-100' };
      }
      if (p.status === 'rejected' || p.status === 'archived') {
        return { id: `${p.id}-a`, icon: AlertCircle, title: 'Solicitação finalizada', description: `${p.service} foi encerrada`, time, color: 'text-red-600', bgColor: 'bg-red-100' };
      }
      if (['in_progress', 'accepted', 'delivered'].includes(p.status)) {
        return { id: `${p.id}-p`, icon: MessageSquare, title: 'Serviço em andamento', description: `${p.clientName} - ${p.service}`, time, color: 'text-blue-600', bgColor: 'bg-blue-100' };
      }
      return { id: `${p.id}-n`, icon: FileText, title: 'Nova proposta recebida', description: `${p.clientName} solicitou ${p.service}`, time, color: 'text-purple-600', bgColor: 'bg-purple-100' };
    }),
);

// ── Recent wallet transactions ────────────────────────────────────

const recentTransactions = computed(() =>
  walletTransactions.value
    .slice()
    .sort((a, b) => new Date(b.date).getTime() - new Date(a.date).getTime())
    .slice(0, 5),
);

// ── Helpers ───────────────────────────────────────────────────────

const isDashboardLoading = computed(() =>
  proposalStore.isLoading || isLoadingReviews.value || isLoadingWallet.value,
);

const formatCurrency = (value: number) =>
  new Intl.NumberFormat('pt-BR', { style: 'currency', currency: 'BRL' }).format(value);

const getInitials = (name: string) =>
  name.split(' ').map((n) => n[0]).join('').toUpperCase().slice(0, 2);

const getUserId = () => {
  try {
    const user = JSON.parse(localStorage.getItem('user') || '');
    return user.id ? String(user.id) : null;
  } catch {
    return null;
  }
};

const getUserEmail = () => {
  try {
    const payload = JSON.parse(atob(localStorage.getItem('token')!.split('.')[1]));
    return payload.sub || payload.email;
  } catch {
    return null;
  }
};

// ── Data loading ──────────────────────────────────────────────────

const loadReviews = async () => {
  const userId = getUserId();
  if (!userId) { reviews.value = []; return; }
  isLoadingReviews.value = true;
  try {
    reviews.value = await reviewService.getReviewsForUser(userId);
  } catch {
    reviews.value = [];
  } finally {
    isLoadingReviews.value = false;
  }
};

const loadWalletData = async () => {
  isLoadingWallet.value = true;
  try {
    const [balance, transactions] = await Promise.all([
      walletService.getWallet(),
      walletService.getTransactions('ALL'),
    ]);
    walletBalance.value = balance;
    walletTransactions.value = transactions;
  } catch {
    // Non-critical – charts fall back to proposal data
  } finally {
    isLoadingWallet.value = false;
  }
};

const refreshDashboardData = async () => {
  await Promise.all([proposalStore.loadProposals(), loadReviews(), loadWalletData()]);
};

const handleNotification = (event: any) => {
  const n: ServiceRequestNotification = event.detail;
  switch (n.type) {
    case 'NEW_SERVICE_REQUEST':
      toast.success(`Nova solicitação: ${n.serviceName} - ${n.contractorName}`);
      break;
    case 'COUNTER_PROPOSAL':
      toast.info(`${n.contractorName} fez uma contra-proposta de ${n.counterProposalAmount}`);
      break;
    case 'PROPOSAL_ACCEPTED':
      toast.success(`Proposta aceita por ${n.contractorName}!`);
      break;
    case 'SERVICE_REQUEST_STATUS_CHANGED':
      if (n.status === 'IN_PROGRESS') toast.success('Pagamento confirmado! Serviço em andamento');
      else toast.info(n.message || `Status alterado para ${n.status}`);
      break;
    case 'SERVICE_REQUEST_CANCELLED':
      toast.info(`Solicitação cancelada: ${n.message}`);
      break;
    case 'NEW_REVIEW':
      toast.success(`${n.reviewerName} avaliou seu serviço com ${n.rating} estrelas`);
      break;
    default:
      toast.info(n.message);
  }
  refreshDashboardData();
};

onMounted(async () => {
  await refreshDashboardData();

  const userEmail = getUserEmail();
  if (userEmail) {
    wsConnection = useServiceRequestWebSocket(userEmail);
    wsConnection.connect().catch(() => {});
    window.addEventListener('service-request-notification', handleNotification);
  }
});

onUnmounted(() => {
  window.removeEventListener('service-request-notification', handleNotification);
  wsConnection?.disconnect();
});
</script>

<template>
  <div class="max-w-7xl mx-auto space-y-6">
    <!-- Header -->
    <div>
      <h1 class="text-2xl font-bold text-[#263238] mb-2">Dashboard</h1>
      <p class="text-[#435574]">Bem-vindo de volta! Aqui está um resumo do seu desempenho.</p>
    </div>

    <!-- Period Selector -->
    <div class="flex gap-2">
      <Button @click="selectedPeriod = 'week'" :variant="selectedPeriod === 'week' ? 'default' : 'outline'" size="sm">Semana</Button>
      <Button @click="selectedPeriod = 'month'" :variant="selectedPeriod === 'month' ? 'default' : 'outline'" size="sm">Mês</Button>
      <Button @click="selectedPeriod = 'year'" :variant="selectedPeriod === 'year' ? 'default' : 'outline'" size="sm">Ano</Button>
    </div>

    <!-- Loading -->
    <div v-if="isDashboardLoading" class="flex justify-center py-10">
      <div class="animate-spin h-8 w-8 border-4 border-[#286AD8] border-t-transparent rounded-full"></div>
    </div>

    <template v-else>
      <!-- Stats Cards -->
      <div class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4">
        <!-- Revenue -->
        <Card class="p-6">
          <div class="flex items-start justify-between mb-4">
            <div class="w-12 h-12 rounded-full bg-green-100 flex items-center justify-center">
              <DollarSign class="h-6 w-6 text-green-600" />
            </div>
            <Badge :class="stats.revenue.change >= 0 ? 'bg-green-100 text-green-700 hover:bg-green-100' : 'bg-red-100 text-red-700 hover:bg-red-100'">
              <span class="flex items-center gap-1">
                <ArrowUp v-if="stats.revenue.change >= 0" class="h-3 w-3" />
                <ArrowDown v-else class="h-3 w-3" />
                {{ Math.abs(stats.revenue.change) }}%
              </span>
            </Badge>
          </div>
          <p class="text-sm text-[#435574] mb-1">Receita no Período</p>
          <p class="text-2xl text-[#263238]">{{ formatCurrency(stats.revenue.current) }}</p>
          <p class="text-xs text-[#435574] mt-1">vs {{ formatCurrency(stats.revenue.previous) }} {{ comparisonLabel }}</p>
        </Card>

        <!-- Wallet Balance -->
        <Card class="p-6">
          <div class="flex items-start justify-between mb-4">
            <div class="w-12 h-12 rounded-full bg-blue-100 flex items-center justify-center">
              <Wallet class="h-6 w-6 text-[#286AD8]" />
            </div>
            <span v-if="walletBalance?.pendingBalance" class="text-xs text-yellow-600 bg-yellow-50 px-2 py-1 rounded-full">
              + {{ formatCurrency(walletBalance.pendingBalance) }} pendente
            </span>
          </div>
          <p class="text-sm text-[#435574] mb-1">Saldo Disponível</p>
          <p class="text-2xl text-[#263238]">{{ formatCurrency(walletBalance?.availableBalance ?? 0) }}</p>
          <p class="text-xs text-[#435574] mt-1">Total ganho: {{ formatCurrency(walletBalance?.totalEarned ?? 0) }}</p>
        </Card>

        <!-- Completed Jobs -->
        <Card class="p-6">
          <div class="flex items-start justify-between mb-4">
            <div class="w-12 h-12 rounded-full bg-purple-100 flex items-center justify-center">
              <CheckCircle class="h-6 w-6 text-purple-600" />
            </div>
            <Badge :class="stats.completedJobs.change >= 0 ? 'bg-green-100 text-green-700 hover:bg-green-100' : 'bg-red-100 text-red-700 hover:bg-red-100'">
              <span class="flex items-center gap-1">
                <ArrowUp v-if="stats.completedJobs.change >= 0" class="h-3 w-3" />
                <ArrowDown v-else class="h-3 w-3" />
                {{ Math.abs(stats.completedJobs.change) }}%
              </span>
            </Badge>
          </div>
          <p class="text-sm text-[#435574] mb-1">Trabalhos Concluídos</p>
          <p class="text-2xl text-[#263238]">{{ stats.completedJobs.current }}</p>
          <p class="text-xs text-[#435574] mt-1">vs {{ stats.completedJobs.previous }} {{ comparisonLabel }}</p>
        </Card>

        <!-- Rating -->
        <Card class="p-6">
          <div class="flex items-start justify-between mb-4">
            <div class="w-12 h-12 rounded-full bg-yellow-100 flex items-center justify-center">
              <Star class="h-6 w-6 text-yellow-600" />
            </div>
            <Badge :class="stats.rating.change >= 0 ? 'bg-green-100 text-green-700 hover:bg-green-100' : 'bg-red-100 text-red-700 hover:bg-red-100'">
              <span class="flex items-center gap-1">
                <ArrowUp v-if="stats.rating.change >= 0" class="h-3 w-3" />
                <ArrowDown v-else class="h-3 w-3" />
                {{ Math.abs(stats.rating.change) }}%
              </span>
            </Badge>
          </div>
          <p class="text-sm text-[#435574] mb-1">Avaliação Média</p>
          <p class="text-2xl text-[#263238]">{{ stats.rating.current.toFixed(1) }} ⭐</p>
          <p class="text-xs text-[#435574] mt-1">{{ reviews.length }} avaliação{{ reviews.length !== 1 ? 'ões' : '' }} no total</p>
        </Card>
      </div>

      <!-- Charts Row -->
      <div class="grid grid-cols-1 lg:grid-cols-3 gap-6">
        <!-- Bar Chart: Earnings + Proposals -->
        <Card class="p-6 lg:col-span-2">
          <div class="flex items-center justify-between mb-6">
            <div>
              <h3 class="text-[#263238] mb-1">Receita e Propostas</h3>
              <p class="text-sm text-[#435574]">Últimos 6 meses</p>
            </div>
            <TrendingUp class="h-5 w-5 text-green-600" />
          </div>
          <div class="h-[260px]">
            <Bar :data="barChartData" :options="barChartOptions" />
          </div>
        </Card>

        <!-- Doughnut Chart: Services by Category -->
        <Card class="p-6">
          <div class="mb-6">
            <h3 class="text-[#263238] mb-1">Faturamento por Serviço</h3>
            <p class="text-sm text-[#435574]">Receita gerada por cada serviço</p>
          </div>
          <div class="h-[260px] flex items-center justify-center">
            <Doughnut :data="doughnutChartData" :options="doughnutOptions" />
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
            <Button @click="props.onNavigate('freelancer-proposals')" variant="outline" size="sm">Ver Todas</Button>
          </div>
          <div class="space-y-4">
            <p v-if="recentProposals.length === 0" class="text-sm text-[#435574]">Nenhuma proposta pendente no momento.</p>
            <div
              v-for="proposal in recentProposals"
              :key="proposal.id"
              class="p-4 rounded-lg border border-gray-200 hover:border-[#286AD8] transition-colors cursor-pointer"
              @click="props.onNavigate('freelancer-proposals', { proposalId: proposal.id })"
            >
              <div class="flex items-start gap-3 mb-3">
                <Avatar class="h-10 w-10">
                  <AvatarImage :src="proposal.clientAvatar" :alt="proposal.clientName" />
                  <AvatarFallback class="bg-[#286AD8] text-white">{{ getInitials(proposal.clientName) }}</AvatarFallback>
                </Avatar>
                <div class="flex-1">
                  <div class="flex items-start justify-between mb-1">
                    <div>
                      <p class="text-sm text-[#263238]">{{ proposal.clientName }}</p>
                      <p class="text-xs text-[#435574]">{{ proposal.receivedAt }}</p>
                    </div>
                    <Badge variant="secondary" class="text-xs">{{ proposal.service }}</Badge>
                  </div>
                  <p class="text-sm text-[#435574] line-clamp-2 mb-2">{{ proposal.description }}</p>
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
                <Button size="sm" variant="default" class="flex-1" @click.stop="props.onNavigate('freelancer-proposals', { proposalId: proposal.id })">Responder</Button>
                <Button size="sm" variant="outline" class="flex-1" @click.stop="props.onNavigate('freelancer-proposals', { proposalId: proposal.id })">Recusar</Button>
              </div>
            </div>
          </div>
        </Card>

        <!-- Active Jobs -->
        <Card class="p-6">
          <div class="flex items-center justify-between mb-6">
            <div>
              <h3 class="text-[#263238] mb-1">Serviços em Andamento</h3>
              <p class="text-sm text-[#435574]">{{ activeJobs.length }} projeto{{ activeJobs.length !== 1 ? 's' : '' }} ativo{{ activeJobs.length !== 1 ? 's' : '' }}</p>
            </div>
            <Button @click="props.onNavigate('freelancer-proposals')" variant="outline" size="sm">Ver Todos</Button>
          </div>
          <div class="space-y-4">
            <p v-if="activeJobs.length === 0" class="text-sm text-[#435574]">Nenhum serviço ativo encontrado.</p>
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
                <Badge variant="secondary" class="text-xs">{{ job.deadline }}</Badge>
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

      <!-- Wallet Transactions + Recent Activities -->
      <div class="grid grid-cols-1 lg:grid-cols-2 gap-6">
        <!-- Recent Wallet Transactions -->
        <Card class="p-6">
          <div class="flex items-center justify-between mb-6">
            <div>
              <h3 class="text-[#263238] mb-1">Últimas Transações</h3>
              <p class="text-sm text-[#435574]">Histórico da carteira</p>
            </div>
            <Button @click="props.onNavigate('freelancer-wallet')" variant="outline" size="sm">Ver Carteira</Button>
          </div>
          <div class="space-y-3">
            <p v-if="recentTransactions.length === 0" class="text-sm text-[#435574]">Nenhuma transação registrada ainda.</p>
            <div
              v-for="tx in recentTransactions"
              :key="tx.id"
              class="flex items-center justify-between p-3 rounded-lg bg-gray-50"
            >
              <div class="flex items-center gap-3">
                <div
                  :class="tx.type === 'EARNING' ? 'bg-green-100' : 'bg-red-100'"
                  class="w-8 h-8 rounded-full flex items-center justify-center flex-shrink-0"
                >
                  <ArrowUp v-if="tx.type === 'EARNING'" class="h-4 w-4 text-green-600" />
                  <ArrowDown v-else class="h-4 w-4 text-red-600" />
                </div>
                <div>
                  <p class="text-sm text-[#263238]">{{ tx.serviceName || tx.description }}</p>
                  <p class="text-xs text-[#435574]">{{ tx.clientName || tx.paymentMethod }}</p>
                </div>
              </div>
              <div class="text-right">
                <p :class="tx.type === 'EARNING' ? 'text-green-600' : 'text-red-600'" class="text-sm font-semibold">
                  {{ tx.type === 'EARNING' ? '+' : '-' }}{{ formatCurrency(tx.amount) }}
                </p>
                <p class="text-xs text-[#435574]">{{ new Date(tx.date).toLocaleDateString('pt-BR') }}</p>
              </div>
            </div>
          </div>
        </Card>

        <!-- Recent Activities -->
        <Card class="p-6">
          <div class="mb-6">
            <h3 class="text-[#263238] mb-1">Atividades Recentes</h3>
            <p class="text-sm text-[#435574]">Últimas atualizações das propostas</p>
          </div>
          <div class="space-y-4">
            <p v-if="recentActivities.length === 0" class="text-sm text-[#435574]">Nenhuma atividade recente para exibir.</p>
            <div v-for="activity in recentActivities" :key="activity.id" class="flex items-start gap-4">
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
  </div>
</template>
