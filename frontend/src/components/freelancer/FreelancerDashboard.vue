<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted } from 'vue';
import { useRouter } from 'vue-router';
import {
  Chart as ChartJS,
  Title,
  Tooltip,
  Legend,
  BarElement,
  LineElement,
  PointElement,
  CategoryScale,
  LinearScale,
  ArcElement,
  Filler,
} from 'chart.js';
import { Bar, Doughnut, Line } from 'vue-chartjs';
import { useServiceRequestWebSocket, type ServiceRequestNotification } from '@/composables/useServiceRequestWebSocket';
import {
  TrendingUp,
  DollarSign,
  Clock,
  Star,
  Target,
  Handshake,
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
import reviewService, { type ReviewResponse, type PendingReview } from '@/api/services/reviewService';
import walletService, { type WalletBalance, type WalletTransaction } from '@/api/services/walletService';
import { useUserStore } from '@/stores/userStore';
import { formatTimeAgoFromApiDate, getApiDateTimestamp } from '@/utils/apiDate';
import Card from '../ui/card.vue';
import Badge from '../ui/badge.vue';
import Button from '../ui/button.vue';
import Input from '../ui/input.vue';
import Select from '../ui/select.vue';
import SelectContent from '../ui/select-content.vue';
import SelectItem from '../ui/select-item.vue';
import SelectTrigger from '../ui/select-trigger.vue';
import SelectValue from '../ui/select-value.vue';
import Avatar from '../ui/avatar.vue';
import AvatarImage from '../ui/avatar-image.vue';
import AvatarFallback from '../ui/avatar-fallback.vue';
import Progress from '../ui/progress.vue';
import { toast } from '@/utils/toast';

ChartJS.register(Title, Tooltip, Legend, BarElement, LineElement, PointElement, CategoryScale, LinearScale, ArcElement, Filler);

interface Props {
  onNavigate: (page: string, params?: any) => void;
}

const props = defineProps<Props>();

let wsConnection: ReturnType<typeof useServiceRequestWebSocket> | null = null;
const router = useRouter();
const userStore = useUserStore();
const proposalStore = useProposalStore();

const openProposalDetail = (id: string) => {
  proposalStore.markAsViewed(id);
  props.onNavigate('freelancer-proposal-detail', { requestId: id });
};

const openServiceDetail = (id: string) => {
  props.onNavigate('freelancer-project-detail', { requestId: id });
};

const openServiceChat = (id: string) => {
  router.push(`/chat/${id}`);
};
const reviews = ref<ReviewResponse[]>([]);
const pendingReviews = ref<PendingReview[]>([]);
const walletBalance = ref<WalletBalance | null>(null);
const walletTransactions = ref<WalletTransaction[]>([]);
const isLoadingReviews = ref(false);
const isLoadingWallet = ref(false);

const timeFilter = ref<
  'Hoje' |
  'Ontem' |
  'Todo o período' |
  'Últimos 7 dias' |
  'Últimos 30 dias' |
  'Mês atual' |
  'Último mês' |
  'Intervalo personalizado'
>('Todo o período');
const customDateFrom = ref('');
const customDateTo = ref('');
const MILLISECONDS_PER_DAY = 1000 * 60 * 60 * 24;

const parseDate = (value: string) => {
  const parsed = new Date(value);
  return Number.isNaN(parsed.getTime()) ? null : parsed;
};

const getProposalAmount = (proposal: Proposal) =>
  proposal.freelancerResponse?.value ?? proposal.budget ?? 0;

const startOfDay = (date: Date) =>
  new Date(date.getFullYear(), date.getMonth(), date.getDate());

const endOfDay = (date: Date) =>
  new Date(date.getFullYear(), date.getMonth(), date.getDate(), 23, 59, 59, 999);

const addDays = (date: Date, days: number) => {
  const result = new Date(date);
  result.setDate(result.getDate() + days);
  return result;
};

const getDateKey = (date: Date) =>
  `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')}`;

const getMonthKey = (date: Date) =>
  `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}`;

const getInclusiveDayCount = (start: Date, end: Date) =>
  Math.max(
    1,
    Math.ceil((endOfDay(end).getTime() - startOfDay(start).getTime() + 1) / MILLISECONDS_PER_DAY),
  );

interface DashboardDateRange {
  start: Date;
  end: Date;
}

const earliestAvailableDate = computed(() => {
  const timestamps = [
    ...proposalStore.proposals.flatMap((proposal) => {
      const receivedAt = parseDate(proposal.receivedAt);
      const updatedAt = parseDate(proposal.updatedAt || proposal.receivedAt);
      return [receivedAt, updatedAt]
        .filter((date): date is Date => date !== null)
        .map((date) => date.getTime());
    }),
    ...reviews.value
      .map((review) => parseDate(review.createdAt))
      .filter((date): date is Date => date !== null)
      .map((date) => date.getTime()),
    ...walletTransactions.value
      .map((transaction) => parseDate(transaction.date))
      .filter((date): date is Date => date !== null)
      .map((date) => date.getTime()),
  ];

  if (timestamps.length === 0) {
    return startOfDay(new Date());
  }

  return startOfDay(new Date(Math.min(...timestamps)));
});

const currentPeriodRange = computed<DashboardDateRange>(() => {
  const now = new Date();
  const todayStart = startOfDay(now);

  switch (timeFilter.value) {
    case 'Hoje':
      return { start: todayStart, end: now };
    case 'Ontem': {
      const yesterdayStart = addDays(todayStart, -1);
      return { start: yesterdayStart, end: new Date(todayStart.getTime() - 1) };
    }
    case 'Todo o período':
      return { start: earliestAvailableDate.value, end: now };
    case 'Últimos 7 dias':
      return { start: addDays(todayStart, -7), end: now };
    case 'Mês atual':
      return { start: new Date(now.getFullYear(), now.getMonth(), 1), end: now };
    case 'Último mês':
      return {
        start: new Date(now.getFullYear(), now.getMonth() - 1, 1),
        end: new Date(now.getFullYear(), now.getMonth(), 0, 23, 59, 59, 999),
      };
    case 'Intervalo personalizado': {
      const from = customDateFrom.value
        ? new Date(`${customDateFrom.value}T00:00:00`)
        : earliestAvailableDate.value;
      const to = customDateTo.value
        ? new Date(`${customDateTo.value}T23:59:59.999`)
        : now;

      if (from <= to) {
        return { start: from, end: to };
      }

      return { start: to, end: from };
    }
    case 'Últimos 30 dias':
    default:
      return { start: addDays(todayStart, -30), end: now };
  }
});

const previousPeriodRange = computed<DashboardDateRange>(() => {
  const now = new Date();
  const todayStart = startOfDay(now);

  switch (timeFilter.value) {
    case 'Hoje': {
      const yesterdayStart = addDays(todayStart, -1);
      return { start: yesterdayStart, end: new Date(todayStart.getTime() - 1) };
    }
    case 'Ontem': {
      const dayBeforeYesterday = addDays(todayStart, -2);
      const yesterdayStart = addDays(todayStart, -1);
      return { start: dayBeforeYesterday, end: new Date(yesterdayStart.getTime() - 1) };
    }
    case 'Mês atual':
      return {
        start: new Date(now.getFullYear(), now.getMonth() - 1, 1),
        end: new Date(now.getFullYear(), now.getMonth(), 0, 23, 59, 59, 999),
      };
    case 'Último mês':
      return {
        start: new Date(now.getFullYear(), now.getMonth() - 2, 1),
        end: new Date(now.getFullYear(), now.getMonth() - 1, 0, 23, 59, 59, 999),
      };
    default: {
      const { start, end } = currentPeriodRange.value;
      const duration = end.getTime() - start.getTime();
      const previousEnd = new Date(start.getTime() - 1);
      const previousStart = new Date(previousEnd.getTime() - duration);
      return { start: previousStart, end: previousEnd };
    }
  }
});

const isDateInRange = (value: string | null | undefined, range: DashboardDateRange) => {
  if (!value) return false;
  const date = parseDate(value);
  return date !== null && date >= range.start && date <= range.end;
};

const handleTimeFilterChange = (value: string) => {
  timeFilter.value = value as typeof timeFilter.value;
};

// Period filtering

const periodProposals = computed(() => {
  const range = currentPeriodRange.value;
  return proposalStore.proposals.filter((proposal) =>
    isDateInRange(proposal.receivedAt, range),
  );
});

const previousPeriodProposals = computed(() => {
  const range = previousPeriodRange.value;
  return proposalStore.proposals.filter((proposal) =>
    isDateInRange(proposal.receivedAt, range),
  );
});

const periodReviews = computed(() => {
  const range = currentPeriodRange.value;
  return reviews.value.filter((review) => isDateInRange(review.createdAt, range));
});

const previousPeriodReviews = computed(() => {
  const range = previousPeriodRange.value;
  return reviews.value.filter((review) => isDateInRange(review.createdAt, range));
});

const periodEarnings = computed(() => {
  const range = currentPeriodRange.value;
  return walletTransactions.value.filter((tx) => {
    if (tx.type !== 'EARNING') return false;
    return isDateInRange(tx.date, range);
  });
});

const previousPeriodEarnings = computed(() => {
  const range = previousPeriodRange.value;
  return walletTransactions.value.filter((tx) => {
    if (tx.type !== 'EARNING') return false;
    return isDateInRange(tx.date, range);
  });
});

// Stats

const averageRating = (list: ReviewResponse[]) => {
  if (list.length === 0) return 0;
  return list.reduce((sum, r) => sum + r.rating, 0) / list.length;
};

const calculateChange = (current: number, previous: number) => {
  if (previous === 0) return current > 0 ? 100 : 0;
  return Number((((current - previous) / previous) * 100).toFixed(1));
};

const calculateRate = (current: number, total: number) => {
  if (total === 0) return 0;
  return Number(((current / total) * 100).toFixed(1));
};

const closedContractStatuses: Proposal['status'][] = ['in_progress', 'delivered', 'completed'];
const revenueProposalStatuses: Proposal['status'][] = ['accepted', 'in_progress', 'delivered', 'completed'];
const hasRecordedWalletEarnings = computed(() =>
  walletTransactions.value.some((transaction) => transaction.type === 'EARNING'),
);

const getRevenueFromProposals = (proposals: Proposal[]) =>
  proposals
    .filter((proposal) => revenueProposalStatuses.includes(proposal.status))
    .reduce((sum, proposal) => sum + getProposalAmount(proposal), 0);

const currentPeriodRevenue = computed(() =>
  hasRecordedWalletEarnings.value
    ? periodEarnings.value.reduce((sum, transaction) => sum + transaction.amount, 0)
    : getRevenueFromProposals(periodProposals.value),
);

const previousPeriodRevenue = computed(() =>
  hasRecordedWalletEarnings.value
    ? previousPeriodEarnings.value.reduce((sum, transaction) => sum + transaction.amount, 0)
    : getRevenueFromProposals(previousPeriodProposals.value),
);

const comparisonLabel = computed(() => {
  switch (timeFilter.value) {
    case 'Hoje': return 'ontem';
    case 'Ontem': return 'dia anterior';
    case 'Todo o período': return 'período anterior';
    case 'Últimos 7 dias': return 'semana anterior';
    case 'Mês atual':
    case 'Último mês':
      return 'mês anterior';
    case 'Últimos 30 dias':
      return '30 dias anteriores';
    case 'Intervalo personalizado':
      return 'período anterior';
    default:
      return 'período anterior';
  }
});

const stats = computed(() => {
  const activeStatuses: Proposal['status'][] = ['accepted', 'in_progress', 'delivered'];
  const completedStatuses: Proposal['status'][] = ['completed'];
  const revenueNow = currentPeriodRevenue.value;
  const revenuePrev = previousPeriodRevenue.value;

  const currentActiveServices = periodProposals.value.filter((p) => activeStatuses.includes(p.status)).length;
  const previousActiveServices = previousPeriodProposals.value.filter((p) => activeStatuses.includes(p.status)).length;

  const currentCompletedJobs = periodProposals.value.filter((p) => completedStatuses.includes(p.status)).length;
  const previousCompletedJobs = previousPeriodProposals.value.filter((p) => completedStatuses.includes(p.status)).length;

  const currentConvertedRequests = periodProposals.value.filter((p) => closedContractStatuses.includes(p.status)).length;
  const previousConvertedRequests = previousPeriodProposals.value.filter((p) => closedContractStatuses.includes(p.status)).length;

  const currentConversionRate = calculateRate(currentConvertedRequests, periodProposals.value.length);
  const previousConversionRate = calculateRate(previousConvertedRequests, previousPeriodProposals.value.length);

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
    conversion: {
      current: currentConversionRate,
      previous: previousConversionRate,
      change: calculateChange(currentConversionRate, previousConversionRate),
      hiredCurrent: currentConvertedRequests,
      totalCurrent: periodProposals.value.length,
    },
    rating: {
      current: currentRating,
      previous: previousRating,
      change: calculateChange(currentRating, previousRating),
    },
  };
});

// Period helpers

const periodLengthInDays = computed(() =>
  getInclusiveDayCount(currentPeriodRange.value.start, currentPeriodRange.value.end),
);

const periodDescription = computed(() => {
  switch (timeFilter.value) {
    case 'Hoje': return 'hoje';
    case 'Ontem': return 'ontem';
    case 'Todo o período': return 'durante todo o período';
    case 'Últimos 7 dias': return 'nos últimos 7 dias';
    case 'Mês atual': return 'no mês atual';
    case 'Último mês': return 'no último mês';
    case 'Últimos 30 dias': return 'nos últimos 30 dias';
    case 'Intervalo personalizado': return 'no período selecionado';
    default: return 'durante todo o período';
  }
});

const barChartLabel = computed(() => {
  switch (timeFilter.value) {
    case 'Hoje': return 'Hoje';
    case 'Ontem': return 'Ontem';
    case 'Todo o período': return 'Todo o período';
    case 'Últimos 7 dias': return 'Últimos 7 dias';
    case 'Mês atual': return 'Mês atual';
    case 'Último mês': return 'Último mês';
    case 'Últimos 30 dias': return 'Últimos 30 dias';
    case 'Intervalo personalizado': return 'Intervalo personalizado';
    default: return 'Todo o período';
  }
});

// Proposals filtered by activity date (updatedAt) for active jobs and activities
const periodProposalsByActivity = computed(() => {
  const range = currentPeriodRange.value;
  return proposalStore.proposals.filter((proposal) =>
    isDateInRange(proposal.updatedAt || proposal.receivedAt, range),
  );
});

// Wallet transactions filtered by current period
const periodTransactions = computed(() => {
  const range = currentPeriodRange.value;
  return walletTransactions.value.filter((transaction) =>
    isDateInRange(transaction.date, range),
  );
});

// Chart: Revenue line

const revenueTimeline = computed(() => {
  const range = currentPeriodRange.value;
  const useDailyBuckets = periodLengthInDays.value <= 45;
  const spansMultipleYears = range.start.getFullYear() !== range.end.getFullYear();

  const buckets = useDailyBuckets
    ? Array.from({ length: periodLengthInDays.value }, (_, index) => {
      const date = addDays(startOfDay(range.start), index);
      return {
        key: getDateKey(date),
        label: periodLengthInDays.value <= 7
          ? date.toLocaleDateString('pt-BR', { weekday: 'short' })
          : date.toLocaleDateString('pt-BR', { day: '2-digit', month: '2-digit' }),
        value: 0,
      };
    })
    : (() => {
      const items: Array<{ key: string; label: string; value: number }> = [];
      let cursor = new Date(range.start.getFullYear(), range.start.getMonth(), 1);
      const lastMonth = new Date(range.end.getFullYear(), range.end.getMonth(), 1);

      while (cursor <= lastMonth) {
        items.push({
          key: getMonthKey(cursor),
          label: cursor.toLocaleDateString(
            'pt-BR',
            spansMultipleYears ? { month: 'short', year: '2-digit' } : { month: 'short' },
          ),
          value: 0,
        });
        cursor = new Date(cursor.getFullYear(), cursor.getMonth() + 1, 1);
      }

      return items;
    })();

  const bucketMap = new Map(buckets.map((bucket) => [bucket.key, bucket]));
  const getBucketKey = (date: Date) => (useDailyBuckets ? getDateKey(date) : getMonthKey(date));

  periodEarnings.value.forEach((transaction) => {
    if (!hasRecordedWalletEarnings.value) return;
    const date = parseDate(transaction.date);
    if (!date) return;
    const bucket = bucketMap.get(getBucketKey(date));
    if (bucket) bucket.value += transaction.amount;
  });

  if (!hasRecordedWalletEarnings.value) {
    periodProposals.value
      .filter((proposal) => revenueProposalStatuses.includes(proposal.status))
      .forEach((proposal) => {
        const date = parseDate(proposal.receivedAt);
        if (!date) return;
        const bucket = bucketMap.get(getBucketKey(date));
        if (bucket) bucket.value += getProposalAmount(proposal);
      });
  }

  return buckets;
});

const revenueChartHasData = computed(() =>
  revenueTimeline.value.some((point) => point.value > 0),
);

const revenueChartSummary = computed(() => {
  const points = revenueTimeline.value;
  const total = points.reduce((sum, point) => sum + point.value, 0);
  const peakPoint = points.reduce<{ label: string; value: number } | null>((currentPeak, point) => {
    if (point.value <= 0) {
      return currentPeak;
    }

    if (currentPeak === null || point.value > currentPeak.value) {
      return { label: point.label, value: point.value };
    }
    return currentPeak;
  }, null);

  return {
    total,
    average: points.length > 0 ? total / points.length : 0,
    peakLabel: peakPoint?.label ?? 'Sem dados',
    peakValue: peakPoint?.value ?? 0,
    averageLabel: periodLengthInDays.value <= 45 ? 'Média por dia' : 'Média por mês',
  };
});

const revenueLineChartData = computed(() => ({
  labels: revenueTimeline.value.map((point) => point.label),
  datasets: [
    {
      label: 'Faturamento',
      data: revenueTimeline.value.map((point) => Number(point.value.toFixed(2))),
      borderColor: '#286AD8',
      backgroundColor: (context: any) => {
        const { ctx, chartArea } = context.chart;
        if (!chartArea) {
          return 'rgba(40, 106, 216, 0.16)';
        }

        const gradient = ctx.createLinearGradient(0, chartArea.top, 0, chartArea.bottom);
        gradient.addColorStop(0, 'rgba(40, 106, 216, 0.30)');
        gradient.addColorStop(1, 'rgba(40, 106, 216, 0.02)');
        return gradient;
      },
      fill: true,
      tension: 0.42,
      cubicInterpolationMode: 'monotone' as const,
      borderWidth: 3,
      pointRadius: (context: any) => {
        const data = (context.dataset.data as number[]).map((value) => Number(value));
        const value = Number(data[context.dataIndex] ?? 0);
        const maxValue = Math.max(...data, 0);
        const isLastPoint = context.dataIndex === data.length - 1 && value > 0;
        const isPeakPoint = value > 0 && value === maxValue;
        return isLastPoint || isPeakPoint ? 4 : 0;
      },
      pointHoverRadius: 6,
      pointHitRadius: 18,
      pointBackgroundColor: '#FFFFFF',
      pointBorderColor: '#286AD8',
      pointBorderWidth: 2,
    },
  ],
}));

const revenueLineChartOptions = {
  responsive: true,
  maintainAspectRatio: false,
  interaction: {
    intersect: false,
    mode: 'index' as const,
  },
  plugins: {
    legend: {
      display: false,
    },
    tooltip: {
      backgroundColor: '#1E293B',
      titleColor: '#FFFFFF',
      bodyColor: '#FFFFFF',
      displayColors: false,
      padding: 12,
      cornerRadius: 12,
      callbacks: {
        title: (items: any[]) => items[0]?.label ?? '',
        label: (ctx: any) => `Faturamento: ${formatCurrency(Number(ctx.parsed.y ?? 0))}`,
      },
    },
  },
  scales: {
    x: {
      grid: {
        display: false,
        drawBorder: false,
      },
      ticks: {
        color: '#435574',
        maxRotation: 0,
        autoSkip: true,
        font: {
          size: 11,
        },
      },
    },
    y: {
      beginAtZero: true,
      grid: {
        color: 'rgba(67, 85, 116, 0.10)',
        drawBorder: false,
      },
      ticks: {
        color: '#435574',
        padding: 10,
        callback: (value: number | string) => formatCurrencyAxis(Number(value)),
      },
    },
  },
};

const serviceRevenueBreakdown = computed(() => {
  const palette = ['#286AD8', '#4B7FD6', '#739EEB', '#9ABAF7', '#435574'];
  const revenue = new Map<string, number>();

  if (hasRecordedWalletEarnings.value) {
    periodEarnings.value.forEach((transaction) => {
      const serviceName = transaction.serviceName?.trim() || 'Sem serviço';
      revenue.set(serviceName, (revenue.get(serviceName) || 0) + transaction.amount);
    });
  } else {
    periodProposals.value
      .filter((proposal) => revenueProposalStatuses.includes(proposal.status))
      .forEach((proposal) => {
        const serviceName = proposal.service?.trim() || 'Sem serviço';
        revenue.set(serviceName, (revenue.get(serviceName) || 0) + getProposalAmount(proposal));
      });
  }

  const sortedEntries = Array.from(revenue.entries())
    .filter(([, amount]) => amount > 0)
    .sort((a, b) => b[1] - a[1]);

  if (sortedEntries.length > 5) {
    const topEntries = sortedEntries.slice(0, 4);
    const otherServicesTotal = sortedEntries
      .slice(4)
      .reduce((sum, [, amount]) => sum + amount, 0);

    sortedEntries.splice(0, sortedEntries.length, ...topEntries, ['Outros', otherServicesTotal]);
  }

  const total = sortedEntries.reduce((sum, [, amount]) => sum + amount, 0);

  return sortedEntries.map(([serviceName, amount], index) => ({
    serviceName,
    amount,
    color: palette[index] || palette[palette.length - 1],
    share: total > 0 ? Number(((amount / total) * 100).toFixed(1)) : 0,
  }));
});

const serviceRevenueChartHasData = computed(() =>
  serviceRevenueBreakdown.value.length > 0,
);

const serviceRevenueSummary = computed(() => ({
  total: serviceRevenueBreakdown.value.reduce((sum, item) => sum + item.amount, 0),
  topService: serviceRevenueBreakdown.value[0] ?? null,
}));

const doughnutChartData = computed(() => ({
  labels: serviceRevenueBreakdown.value.map((item) => item.serviceName),
  datasets: [
    {
      data: serviceRevenueBreakdown.value.map((item) => Number(item.amount.toFixed(2))),
      backgroundColor: serviceRevenueBreakdown.value.map((item) => item.color),
      hoverBackgroundColor: serviceRevenueBreakdown.value.map((item) => item.color),
      borderColor: '#FFFFFF',
      borderWidth: 4,
      hoverOffset: 10,
      spacing: 2,
    },
  ],
}));

const doughnutOptions = {
  responsive: true,
  maintainAspectRatio: false,
  cutout: '72%',
  plugins: {
    legend: {
      display: false,
    },
    tooltip: {
      backgroundColor: '#1E293B',
      titleColor: '#FFFFFF',
      bodyColor: '#FFFFFF',
      displayColors: false,
      padding: 12,
      cornerRadius: 12,
      callbacks: {
        title: (items: any[]) => items[0]?.label ?? '',
        label: (ctx: any) => `Faturamento: ${formatCurrency(Number(ctx.parsed ?? 0))}`,
        afterLabel: (ctx: any) => {
          const item = serviceRevenueBreakdown.value[ctx.dataIndex];
          return item ? `Participação: ${item.share}%` : '';
        },
      },
    },
  },
};

const mostContractedServices = computed(() => {
  const counts = new Map<string, number>();

  periodProposals.value
    .filter((proposal) => closedContractStatuses.includes(proposal.status))
    .forEach((proposal) => {
      const serviceName = proposal.service?.trim() || 'Sem serviço';
      counts.set(serviceName, (counts.get(serviceName) || 0) + 1);
    });

  return Array.from(counts.entries())
    .sort((a, b) => b[1] - a[1])
    .slice(0, 5);
});

const mostContractedServicesChartHasData = computed(() =>
  mostContractedServices.value.length > 0,
);

const mostContractedServicesChartData = computed(() => ({
  labels: mostContractedServices.value.map(([serviceName]) => serviceName),
  datasets: [
    {
      label: 'Contratações',
      data: mostContractedServices.value.map(([, total]) => total),
      backgroundColor: '#286AD8',
      borderRadius: 6,
      borderSkipped: false,
      maxBarThickness: 28,
    },
  ],
}));

const mostContractedServicesChartOptions = {
  responsive: true,
  maintainAspectRatio: false,
  indexAxis: 'y' as const,
  plugins: {
    legend: {
      display: false,
    },
    tooltip: {
      callbacks: {
        label: (ctx: any) => {
          const total = Number(ctx.parsed.x ?? 0);
          return ` ${total} contratação${total !== 1 ? 'ões' : ''}`;
        },
      },
    },
  },
  scales: {
    x: {
      beginAtZero: true,
      grid: { color: '#f1f5f9' },
      ticks: {
        color: '#435574',
        precision: 0,
        stepSize: 1,
      },
    },
    y: {
      grid: { display: false },
      ticks: { color: '#435574' },
    },
  },
};

// Lists

const recentProposals = computed(() =>
  periodProposals.value
    .filter((p) => ['pending'].includes(p.status))
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
  periodProposalsByActivity.value
    .filter((p) => ['in_progress', 'delivered'].includes(p.status))
    .sort((a, b) =>
      getApiDateTimestamp(b.updatedAt || b.receivedAt) -
      getApiDateTimestamp(a.updatedAt || a.receivedAt),
    )
    .slice(0, 3)
    .map((p) => ({
      id: p.id,
      clientName: p.clientName,
      service: p.service,
      progress: p.status === 'in_progress' ? 65 : 95,
      deadline: p.freelancerResponse?.executionDays
        ? `${p.freelancerResponse.executionDays} dias`
        : p.deadline,
    })),
);

const recentActivities = computed(() => {
  const proposalItems = periodProposalsByActivity.value
    .slice()
    .sort((a, b) =>
      getApiDateTimestamp(b.updatedAt || b.receivedAt) -
      getApiDateTimestamp(a.updatedAt || a.receivedAt),
    )
    .slice(0, 5)
    .map((p) => {
      const time = formatTimeAgoFromApiDate(p.updatedAt || p.receivedAt).replace('Atualizado ', '');
      const sortKey = getApiDateTimestamp(p.updatedAt || p.receivedAt);
      if (p.status === 'completed') {
        return { id: `${p.id}-c`, icon: CheckCircle, title: 'Serviço concluído', description: `${p.service} foi concluído com sucesso`, time, color: 'text-green-600', bgColor: 'bg-green-100', sortKey };
      }
      if (p.status === 'rejected' || p.status === 'archived') {
        return { id: `${p.id}-a`, icon: AlertCircle, title: 'Solicitação finalizada', description: `${p.service} foi encerrada`, time, color: 'text-red-600', bgColor: 'bg-red-100', sortKey };
      }
      if (['in_progress', 'accepted', 'delivered'].includes(p.status)) {
        return { id: `${p.id}-p`, icon: MessageSquare, title: 'Serviço em andamento', description: `${p.clientName} - ${p.service}`, time, color: 'text-blue-600', bgColor: 'bg-blue-100', sortKey };
      }
      return { id: `${p.id}-n`, icon: FileText, title: 'Nova proposta recebida', description: `${p.clientName} solicitou ${p.service}`, time, color: 'text-purple-600', bgColor: 'bg-purple-100', sortKey };
    });

  const range = currentPeriodRange.value;
  const reviewItems = reviews.value
    .filter((r) => isDateInRange(r.createdAt, range))
    .sort((a, b) => new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime())
    .slice(0, 3)
    .map((r) => ({
      id: `review-${r.id}`,
      icon: Star,
      title: 'Nova avaliação recebida',
      description: `${r.reviewerName} avaliou "${r.serviceName}" com ${r.rating} estrela${r.rating !== 1 ? 's' : ''}`,
      time: formatTimeAgoFromApiDate(r.createdAt).replace('Atualizado ', ''),
      color: 'text-yellow-600',
      bgColor: 'bg-yellow-100',
      sortKey: new Date(r.createdAt).getTime(),
    }));

  return [...proposalItems, ...reviewItems]
    .sort((a, b) => b.sortKey - a.sortKey)
    .slice(0, 6);
});

// Recent wallet transactions

const recentTransactions = computed(() =>
  periodTransactions.value
    .slice()
    .sort((a, b) => new Date(b.date).getTime() - new Date(a.date).getTime())
    .slice(0, 5),
);

// Helpers

const isDashboardLoading = computed(() =>
  proposalStore.isLoading || isLoadingReviews.value || isLoadingWallet.value,
);

const formatCurrency = (value: number) =>
  new Intl.NumberFormat('pt-BR', { style: 'currency', currency: 'BRL' }).format(value);

const formatCurrencyAxis = (value: number) => {
  const absoluteValue = Math.abs(value);

  if (absoluteValue >= 1000000) {
    return `R$${(value / 1000000).toFixed(absoluteValue >= 10000000 ? 0 : 1)} mi`;
  }

  if (absoluteValue >= 1000) {
    return `R$${(value / 1000).toFixed(absoluteValue >= 10000 ? 0 : 1)} mil`;
  }

  return `R$${Math.round(value)}`;
};

const getInitials = (name: string) =>
  name.split(' ').map((n) => n[0]).join('').toUpperCase().slice(0, 2);

const getUserId = () => userStore.user?.id ?? null;

const getUserEmail = () => {
  if (userStore.user?.email) return userStore.user.email;
  try {
    const payload = JSON.parse(atob(localStorage.getItem('token')!.split('.')[1]));
    return payload.sub || payload.email;
  } catch {
    return null;
  }
};

const userFirstName = computed(() => {
  const name = userStore.user?.name || '';
  return name.split(' ')[0] || 'Freelancer';
});

// Data loading

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

const loadPendingReviews = async () => {
  try {
    pendingReviews.value = await reviewService.getPendingReviews();
  } catch {
    pendingReviews.value = [];
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
    // Non-critical - charts fall back to proposal data
  } finally {
    isLoadingWallet.value = false;
  }
};

const refreshDashboardData = async () => {
  await Promise.all([proposalStore.loadProposals(), loadReviews(), loadWalletData(), loadPendingReviews()]);
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
    <div class="flex flex-col gap-4 md:flex-row md:items-end md:justify-between">
      <div>
        <h1 class="text-2xl font-bold text-[#263238] mb-2">Dashboard</h1>
        <p class="text-[#435574]">Olá, {{ userFirstName }}! Aqui está um resumo do seu desempenho.</p>
      </div>

      <div class="w-full md:w-auto md:min-w-[240px]">
        <Select :model-value="timeFilter" @update:model-value="handleTimeFilterChange">
          <SelectTrigger class="w-full bg-white">
            <SelectValue placeholder="Todo o período" />
          </SelectTrigger>
          <SelectContent>
            <SelectItem value="Todo o período">Todo o período</SelectItem>
            <SelectItem value="Hoje">Hoje</SelectItem>
            <SelectItem value="Ontem">Ontem</SelectItem>
            <SelectItem value="Últimos 7 dias">Últimos 7 dias</SelectItem>
            <SelectItem value="Últimos 30 dias">Últimos 30 dias</SelectItem>
            <SelectItem value="Mês atual">Mês atual</SelectItem>
            <SelectItem value="Último mês">Último mês</SelectItem>
            <SelectItem value="Intervalo personalizado">Intervalo personalizado</SelectItem>
          </SelectContent>
        </Select>
      </div>
    </div>

    <!-- Custom Date Range -->
    <Card
      v-if="timeFilter === 'Intervalo personalizado'"
      class="p-4"
    >
      <div
        class="grid grid-cols-1 md:grid-cols-2 gap-4"
      >
        <div class="space-y-1">
          <label class="text-sm font-medium text-[#435574]">De</label>
          <Input
            type="date"
            :model-value="customDateFrom"
            @update:model-value="(value: string) => customDateFrom = value"
          />
        </div>
        <div class="space-y-1">
          <label class="text-sm font-medium text-[#435574]">Até</label>
          <Input
            type="date"
            :model-value="customDateTo"
            @update:model-value="(value: string) => customDateTo = value"
          />
        </div>
      </div>
    </Card>

    <!-- Loading -->
    <div v-if="isDashboardLoading" class="flex justify-center py-10">
      <div class="animate-spin h-8 w-8 border-4 border-[#286AD8] border-t-transparent rounded-full"></div>
    </div>

    <template v-else>
      <!-- Stats Cards -->
      <div class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-5 gap-4">
        <!-- Revenue -->
        <Card class="relative p-6">
          <div class="mb-4 flex items-center gap-3">
            <div class="w-12 h-12 rounded-full bg-green-100 flex items-center justify-center">
              <DollarSign class="h-6 w-6 text-green-600" />
            </div>
            <p class="text-sm text-[#435574]">Receita no Período</p>
          </div>
          <p class="text-2xl text-[#263238]">{{ formatCurrency(stats.revenue.current) }}</p>
          <p class="text-xs text-[#435574] mt-1 pr-20">vs {{ formatCurrency(stats.revenue.previous) }} {{ comparisonLabel }}</p>
          <Badge
            :class="[
              'absolute bottom-6 right-6',
              stats.revenue.change >= 0
                ? 'bg-green-100 text-green-700 hover:bg-green-100'
                : 'bg-red-100 text-red-700 hover:bg-red-100',
            ]"
          >
            <span class="flex items-center gap-1">
              <ArrowUp v-if="stats.revenue.change >= 0" class="h-3 w-3" />
              <ArrowDown v-else class="h-3 w-3" />
              {{ Math.abs(stats.revenue.change) }}%
            </span>
          </Badge>
        </Card>

        <!-- Wallet Balance -->
        <Card class="relative p-6">
          <div class="mb-4 flex items-center gap-3">
            <div class="w-12 h-12 rounded-full bg-blue-100 flex items-center justify-center">
              <Wallet class="h-6 w-6 text-[#286AD8]" />
            </div>
            <p class="text-sm text-[#435574]">Saldo Disponível</p>
          </div>
          <p class="text-2xl text-[#263238]">{{ formatCurrency(walletBalance?.availableBalance ?? 0) }}</p>
          <p class="text-xs text-[#435574] mt-1 pr-24">Total ganho: {{ formatCurrency(walletBalance?.totalEarned ?? 0) }}</p>
          <span
            v-if="walletBalance?.pendingBalance"
            class="absolute bottom-6 right-6 rounded-full bg-yellow-50 px-2 py-1 text-xs text-yellow-600"
          >
            + {{ formatCurrency(walletBalance.pendingBalance) }} pendente
          </span>
        </Card>

        <!-- Completed Jobs -->
        <Card class="relative p-6">
          <div class="mb-4 flex items-center gap-3">
            <div class="w-12 h-12 rounded-full bg-purple-100 flex items-center justify-center">
              <CheckCircle class="h-6 w-6 text-purple-600" />
            </div>
            <p class="text-sm text-[#435574]">Trabalhos Concluídos</p>
          </div>
          <p class="text-2xl text-[#263238]">{{ stats.completedJobs.current }}</p>
          <p class="text-xs text-[#435574] mt-1 pr-20">vs {{ stats.completedJobs.previous }} {{ comparisonLabel }}</p>
          <Badge
            :class="[
              'absolute bottom-6 right-6',
              stats.completedJobs.change >= 0
                ? 'bg-green-100 text-green-700 hover:bg-green-100'
                : 'bg-red-100 text-red-700 hover:bg-red-100',
            ]"
          >
            <span class="flex items-center gap-1">
              <ArrowUp v-if="stats.completedJobs.change >= 0" class="h-3 w-3" />
              <ArrowDown v-else class="h-3 w-3" />
              {{ Math.abs(stats.completedJobs.change) }}%
            </span>
          </Badge>
        </Card>

        <!-- Rating -->
        <Card class="relative p-6">
          <div class="mb-4 flex items-center gap-3">
            <div class="w-12 h-12 rounded-full bg-yellow-100 flex items-center justify-center">
              <Star class="h-6 w-6 text-yellow-600" />
            </div>
            <p class="text-sm text-[#435574]">Avaliação Média</p>
          </div>
          <p class="text-2xl text-[#263238]">{{ stats.rating.current.toFixed(1) }}</p>
          <p class="text-xs text-[#435574] mt-1 pr-20">{{ reviews.length }} {{ reviews.length === 1 ? 'avaliação' : 'avaliações' }} no total</p>
          <Badge
            :class="[
              'absolute bottom-6 right-6',
              stats.rating.change >= 0
                ? 'bg-green-100 text-green-700 hover:bg-green-100'
                : 'bg-red-100 text-red-700 hover:bg-red-100',
            ]"
          >
            <span class="flex items-center gap-1">
              <ArrowUp v-if="stats.rating.change >= 0" class="h-3 w-3" />
              <ArrowDown v-else class="h-3 w-3" />
              {{ Math.abs(stats.rating.change) }}%
            </span>
          </Badge>
        </Card>

        <!-- Conversion Rate -->
        <Card class="relative p-6">
          <div class="mb-4 flex items-center gap-3">
            <div class="w-12 h-12 rounded-full bg-teal-100 flex items-center justify-center">
              <Target class="h-6 w-6 text-teal-600" />
            </div>
            <p class="text-sm text-[#435574]">Taxa de Conversão</p>
          </div>
          <p class="text-2xl text-[#263238]">{{ stats.conversion.current.toFixed(1) }}%</p>
          <p class="text-xs text-[#435574] mt-1 pr-20">
            Contratações fechadas: {{ stats.conversion.hiredCurrent }} de {{ stats.conversion.totalCurrent }}
          </p>
          <Badge
            :class="[
              'absolute bottom-6 right-6',
              stats.conversion.change >= 0
                ? 'bg-green-100 text-green-700 hover:bg-green-100'
                : 'bg-red-100 text-red-700 hover:bg-red-100',
            ]"
          >
            <span class="flex items-center gap-1">
              <ArrowUp v-if="stats.conversion.change >= 0" class="h-3 w-3" />
              <ArrowDown v-else class="h-3 w-3" />
              {{ Math.abs(stats.conversion.change) }}%
            </span>
          </Badge>
        </Card>
      </div>

      <div class="grid grid-cols-1 xl:grid-cols-3 gap-6">
        <!-- Revenue Chart -->
        <Card class="overflow-hidden border border-[#D9E7FF] bg-gradient-to-br from-white via-[#F8FBFF] to-[#EEF4FF] p-6 shadow-sm xl:col-span-2">
          <div class="flex flex-col gap-6">
            <div class="flex flex-col gap-5 xl:flex-row xl:items-start xl:justify-between">
              <div class="space-y-3">
                <div>
                  <div class="mb-1 flex items-center gap-3">
                    <span class="flex h-9 w-9 items-center justify-center rounded-full bg-[#286AD8] text-white shadow-[0_10px_30px_rgba(40,106,216,0.22)]">
                      <TrendingUp class="h-4 w-4" />
                    </span>
                    <h3 class="text-xl font-semibold text-[#263238]">Evolução do faturamento</h3>
                  </div>
                  <p class="text-sm text-[#435574]">Uma leitura simples da sua receita {{ periodDescription }}.</p>
                </div>
              </div>

              <div class="grid grid-cols-1 gap-3 sm:grid-cols-3">
                <div class="rounded-2xl border border-white/70 bg-white/85 px-4 py-3 shadow-sm">
                  <p class="text-[11px] font-medium uppercase tracking-[0.14em] text-[#6B7A90]">Total</p>
                  <p class="mt-1 text-lg font-semibold text-[#263238]">{{ formatCurrency(revenueChartSummary.total) }}</p>
                </div>

                <div class="rounded-2xl border border-white/70 bg-white/85 px-4 py-3 shadow-sm">
                  <p class="text-[11px] font-medium uppercase tracking-[0.14em] text-[#6B7A90]">Maior ponto</p>
                  <p class="mt-1 text-lg font-semibold text-[#263238]">{{ formatCurrency(revenueChartSummary.peakValue) }}</p>
                  <p class="text-xs text-[#6B7A90]">{{ revenueChartSummary.peakLabel }}</p>
                </div>

                <div class="rounded-2xl border border-white/70 bg-white/85 px-4 py-3 shadow-sm">
                  <p class="text-[11px] font-medium uppercase tracking-[0.14em] text-[#6B7A90]">{{ revenueChartSummary.averageLabel }}</p>
                  <p class="mt-1 text-lg font-semibold text-[#263238]">{{ formatCurrency(revenueChartSummary.average) }}</p>
                  <p class="text-xs text-[#6B7A90]">Comparativo: {{ comparisonLabel }}</p>
                </div>
              </div>
            </div>

            <div class="rounded-[28px] border border-white/70 bg-white/80 p-4 shadow-[0_18px_45px_rgba(40,106,216,0.08)] backdrop-blur">
              <div class="mb-4 flex justify-end">
                <Badge :class="stats.revenue.change >= 0 ? 'bg-green-100 text-green-700 hover:bg-green-100' : 'bg-red-100 text-red-700 hover:bg-red-100'">
                  <span class="flex items-center gap-1">
                    <ArrowUp v-if="stats.revenue.change >= 0" class="h-3 w-3" />
                    <ArrowDown v-else class="h-3 w-3" />
                    {{ Math.abs(stats.revenue.change) }}% vs {{ comparisonLabel }}
                  </span>
                </Badge>
              </div>

              <div v-if="revenueChartHasData" class="h-[320px]">
                <Line :data="revenueLineChartData" :options="revenueLineChartOptions" />
              </div>

              <div
                v-else
                class="flex h-[320px] items-center justify-center rounded-[22px] border border-dashed border-[#D9E7FF] bg-[#F8FBFF] text-center"
              >
                <div class="max-w-sm space-y-2 px-6">
                  <p class="text-sm font-medium text-[#263238]">Nenhum faturamento encontrado</p>
                  <p class="text-sm text-[#435574]">Quando houver ganhos registrados, a evolução aparecerá aqui {{ periodDescription }}.</p>
                </div>
              </div>
            </div>
          </div>
        </Card>

        <!-- Revenue by Service -->
        <Card class="border border-[#D9E7FF] bg-white p-6 shadow-sm">
          <div class="flex h-full flex-col gap-6">
            <div>
              <div>
                <div class="mb-1 flex items-center gap-3">
                  <span class="flex h-9 w-9 items-center justify-center rounded-full bg-[#286AD8] text-white shadow-[0_10px_30px_rgba(40,106,216,0.22)]">
                    <DollarSign class="h-4 w-4" />
                  </span>
                  <h3 class="text-xl font-semibold text-[#263238]">Faturamento por Serviço</h3>
                </div>
                <p class="text-sm text-[#435574]">Distribuição da receita {{ periodDescription }}</p>
              </div>
            </div>

            <div
              v-if="serviceRevenueChartHasData"
              class="flex h-full flex-col gap-5"
            >
              <div class="relative mx-auto h-[260px] w-full max-w-[280px]">
                <Doughnut :data="doughnutChartData" :options="doughnutOptions" />
                <div class="pointer-events-none absolute inset-0 flex items-center justify-center">
                  <div class="text-center">
                    <p class="text-[11px] font-medium uppercase tracking-[0.14em] text-[#6B7A90]">Total</p>
                    <p class="mt-1 text-lg font-semibold text-[#263238]">{{ formatCurrency(serviceRevenueSummary.total) }}</p>
                  </div>
                </div>
              </div>

              <div class="space-y-3">
                <div
                  v-for="item in serviceRevenueBreakdown"
                  :key="item.serviceName"
                  class="flex items-center justify-between gap-3 rounded-2xl border border-[#E8EEF7] bg-[#FAFCFF] px-4 py-3"
                >
                  <div class="min-w-0 flex items-center gap-3">
                    <span
                      class="h-3 w-3 shrink-0 rounded-full"
                      :style="{ backgroundColor: item.color }"
                    />
                    <div class="min-w-0">
                      <p class="truncate text-sm font-medium text-[#263238]">{{ item.serviceName }}</p>
                      <p class="text-xs text-[#6B7A90]">{{ item.share }}% do total</p>
                    </div>
                  </div>
                  <p class="text-sm font-semibold text-[#263238]">{{ formatCurrency(item.amount) }}</p>
                </div>
              </div>
            </div>

            <div
              v-else
              class="flex min-h-[360px] items-center justify-center rounded-[22px] border border-dashed border-[#D9E7FF] bg-[#F8FBFF] text-center"
            >
              <div class="max-w-xs space-y-2 px-6">
                <p class="text-sm font-medium text-[#263238]">Sem dados por serviço</p>
                <p class="text-sm text-[#435574]">Quando houver faturamento distribuído entre serviços, a rosquinha aparecerá aqui {{ periodDescription }}.</p>
              </div>
            </div>
          </div>
        </Card>
      </div>

      <!-- Most Contracted Services -->
      <Card class="p-6">
        <div class="mb-6">
          <div class="mb-1 flex items-center gap-3">
            <span class="flex h-9 w-9 items-center justify-center rounded-full bg-[#286AD8] text-white shadow-[0_10px_30px_rgba(40,106,216,0.22)]">
              <Handshake class="h-4 w-4" />
            </span>
            <h3 class="text-xl font-semibold text-[#263238]">Serviços Mais Contratados</h3>
          </div>
          <p class="text-sm text-[#435574]">Contratações fechadas por serviço {{ periodDescription }}</p>
        </div>
        <div v-if="mostContractedServicesChartHasData" class="h-[300px]">
          <Bar :data="mostContractedServicesChartData" :options="mostContractedServicesChartOptions" />
        </div>
        <p v-else class="text-sm text-[#435574]">Nenhuma contratação fechada {{ periodDescription }}.</p>
      </Card>

      <!-- Recent Proposals & Active Jobs -->
      <div class="grid grid-cols-1 lg:grid-cols-2 gap-6">
        <!-- Recent Proposals -->
        <Card class="p-6">
          <div class="flex items-center justify-between mb-6">
            <div>
              <h3 class="text-[#263238] mb-1">Propostas Recentes</h3>
              <p class="text-sm text-[#435574]">{{ recentProposals.length }} aguardando {{ periodDescription }}</p>
            </div>
            <Button @click="props.onNavigate('freelancer-proposals')" variant="outline" size="sm">Ver Todas</Button>
          </div>
          <div class="space-y-4">
            <p v-if="recentProposals.length === 0" class="text-sm text-[#435574]">Nenhuma proposta pendente {{ periodDescription }}.</p>
            <div
              v-for="proposal in recentProposals"
              :key="proposal.id"
              class="p-4 rounded-lg border border-gray-200 hover:border-[#286AD8] transition-colors cursor-pointer"
              @click="openProposalDetail(proposal.id)"
            >
              <div class="flex items-start gap-3 mb-3">
                <Avatar class="h-10 w-10 shrink-0">
                  <AvatarImage :src="proposal.clientAvatar" :alt="proposal.clientName" />
                  <AvatarFallback class="bg-[#286AD8] text-white">{{ getInitials(proposal.clientName) }}</AvatarFallback>
                </Avatar>
                <div class="flex-1 min-w-0">
                  <div class="flex items-start justify-between gap-2 mb-1">
                    <div class="min-w-0">
                      <p class="text-sm text-[#263238] break-words">{{ proposal.clientName }}</p>
                      <p class="text-xs text-[#435574]">{{ proposal.receivedAt }}</p>
                    </div>
                    <Badge variant="secondary" class="text-xs shrink-0">{{ proposal.service }}</Badge>
                  </div>
                  <p class="text-sm text-[#435574] line-clamp-2 mb-2 break-words">{{ proposal.description }}</p>
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
                <Button size="sm" variant="default" class="w-full" @click.stop="openProposalDetail(proposal.id)">Visualizar</Button>
              </div>
            </div>
          </div>
        </Card>

        <!-- Active Jobs -->
        <Card class="p-6">
          <div class="flex items-center justify-between mb-6">
            <div>
              <h3 class="text-[#263238] mb-1">Serviços em Andamento</h3>
              <p class="text-sm text-[#435574]">{{ activeJobs.length }} ativo{{ activeJobs.length !== 1 ? 's' : '' }} {{ periodDescription }}</p>
            </div>
            <Button @click="props.onNavigate('freelancer-proposals')" variant="outline" size="sm">Ver Todos</Button>
          </div>
          <div class="space-y-4">
            <p v-if="activeJobs.length === 0" class="text-sm text-[#435574]">Nenhum serviço ativo {{ periodDescription }}.</p>
            <div
              v-for="job in activeJobs"
              :key="job.id"
              class="p-4 rounded-lg border border-gray-200 hover:border-[#286AD8] transition-colors cursor-pointer"
              @click="openServiceDetail(job.id)"
            >
              <div class="flex items-start justify-between gap-2 mb-3">
                <div class="min-w-0">
                  <p class="text-sm text-[#263238] mb-1 break-words">{{ job.service }}</p>
                  <p class="text-xs text-[#435574] break-words">Cliente: {{ job.clientName }}</p>
                </div>
                <Badge variant="secondary" class="text-xs shrink-0">{{ job.deadline }}</Badge>
              </div>
              <div class="space-y-2">
                <div class="flex items-center justify-between text-xs">
                  <span class="text-[#435574]">Progresso</span>
                  <span class="text-[#263238]">{{ job.progress }}%</span>
                </div>
                <Progress :value="job.progress" class="h-2" />
              </div>
              <div class="flex gap-2 mt-3">
                <Button size="sm" variant="outline" class="flex-1" @click.stop="openServiceChat(job.id)">
                  <MessageSquare class="h-3 w-3 mr-1" />
                  Mensagens
                </Button>
                <Button size="sm" variant="outline" class="flex-1" @click.stop="openServiceDetail(job.id)">
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
              <p class="text-sm text-[#435574]">{{ periodDescription }}</p>
            </div>
            <Button @click="props.onNavigate('freelancer-wallet')" variant="outline" size="sm">Ver Carteira</Button>
          </div>
          <div class="space-y-3">
            <p v-if="recentTransactions.length === 0" class="text-sm text-[#435574]">Nenhuma transação {{ periodDescription }}.</p>
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
            <p class="text-sm text-[#435574]">{{ periodDescription }}</p>
          </div>
          <div class="space-y-4">
            <p v-if="recentActivities.length === 0" class="text-sm text-[#435574]">Nenhuma atividade {{ periodDescription }}.</p>
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
