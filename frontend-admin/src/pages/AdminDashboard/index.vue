<script setup lang="ts">
import { computed, onMounted, watch } from 'vue'
import {
  ArcElement,
  BarElement,
  CategoryScale,
  Chart as ChartJS,
  Filler,
  Legend,
  LineElement,
  LinearScale,
  PointElement,
  Title,
  Tooltip,
} from 'chart.js'
import { Doughnut, Line } from 'vue-chartjs'
import {
  BadgeDollarSign,
  CalendarRange,
  CheckCircle2,
  CircleDollarSign,
  LayoutDashboard,
  RefreshCw,
  TrendingUp,
  TriangleAlert,
  UserCheck,
  Users,
} from 'lucide-vue-next'
import AdminLayout from '@/layouts/AdminLayout.vue'
import Card from '@/components/ui/card.vue'
import EmptyState from '@/components/ui/empty-state.vue'
import PageHeader from '@/components/ui/page-header.vue'
import SectionHeader from '@/components/ui/section-header.vue'
import Select from '@/components/ui/select.vue'
import SelectContent from '@/components/ui/select-content.vue'
import SelectItem from '@/components/ui/select-item.vue'
import SelectTrigger from '@/components/ui/select-trigger.vue'
import SelectValue from '@/components/ui/select-value.vue'
import StatCard from '@/components/ui/stat-card.vue'
import TrendBadge from '@/components/ui/trend-badge.vue'
import {
  useAdminDashboardStore,
  type DashboardTimeFilter,
} from '@/stores/adminDashboardStore'

ChartJS.register(
  Title,
  Tooltip,
  Legend,
  BarElement,
  LineElement,
  PointElement,
  CategoryScale,
  LinearScale,
  ArcElement,
  Filler
)

const TIME_FILTER_OPTIONS: DashboardTimeFilter[] = [
  'Todo o período',
  'Hoje',
  'Ontem',
  'Últimos 7 dias',
  'Últimos 30 dias',
  'Mês atual',
  'Último mês',
  'Intervalo personalizado',
]

const store = useAdminDashboardStore()

const dashboard = computed(() => store.dashboard)
const summary = computed(() => dashboard.value?.summary ?? null)
const paymentStatusItems = computed(() => dashboard.value?.paymentStatusBreakdown ?? [])
const topServices = computed(() => dashboard.value?.topContractedServices ?? [])

const selectedTimeFilterLabel = computed(() => store.timeFilter)

const comparisonLabel = computed(() => {
  switch (store.timeFilter) {
    case 'Hoje':
      return 'ontem'
    case 'Ontem':
      return 'dia anterior'
    case 'Últimos 7 dias':
      return 'semana anterior'
    case 'Últimos 30 dias':
      return '30 dias anteriores'
    case 'Mês atual':
    case 'Último mês':
      return 'mês anterior'
    case 'Intervalo personalizado':
    case 'Todo o período':
    default:
      return 'período anterior'
  }
})

const formatCurrency = (value: number) =>
  new Intl.NumberFormat('pt-BR', {
    style: 'currency',
    currency: 'BRL',
  }).format(value || 0)

const formatCompactCurrency = (value: number) => {
  const absoluteValue = Math.abs(value)

  if (absoluteValue >= 1000000) {
    return `R$${(value / 1000000).toFixed(absoluteValue >= 10000000 ? 0 : 1)} mi`
  }

  if (absoluteValue >= 1000) {
    return `R$${(value / 1000).toFixed(absoluteValue >= 10000 ? 0 : 1)} mil`
  }

  return `R$${Math.round(value)}`
}

const formatNumber = (value: number) =>
  new Intl.NumberFormat('pt-BR').format(value || 0)

const primaryCards = computed(() => {
  if (!summary.value) return []

  return [
    {
      key: 'revenue',
      title: 'Faturamento total',
      value: formatCurrency(summary.value.totalRevenue.current),
      supportingText: `vs ${formatCurrency(summary.value.totalRevenue.previous)} ${comparisonLabel.value}`,
      subtitle: 'Valores pagos em serviços realizados',
      changePercent: summary.value.totalRevenue.changePercent,
      icon: CircleDollarSign,
      iconWrapper: 'bg-green-100',
      iconClass: 'text-green-600',
    },
    {
      key: 'servija-fee',
      title: 'Parte da Servi Já',
      value: formatCurrency(summary.value.serviJaFee.current),
      supportingText: `vs ${formatCurrency(summary.value.serviJaFee.previous)} ${comparisonLabel.value}`,
      subtitle: 'Taxa fixa de 5% sobre o faturamento',
      changePercent: summary.value.serviJaFee.changePercent,
      icon: BadgeDollarSign,
      iconWrapper: 'bg-blue-100',
      iconClass: 'text-blue-600',
    },
    {
      key: 'new-users',
      title: 'Usuários novos',
      value: formatNumber(summary.value.newUsers.current),
      supportingText: `vs ${formatNumber(summary.value.newUsers.previous)} ${comparisonLabel.value}`,
      subtitle: 'Novos clientes e profissionais',
      changePercent: summary.value.newUsers.changePercent,
      icon: Users,
      iconWrapper: 'bg-purple-100',
      iconClass: 'text-purple-600',
    },
    {
      key: 'completed-services',
      title: 'Serviços realizados',
      value: formatNumber(summary.value.completedServices.current),
      supportingText: `vs ${formatNumber(summary.value.completedServices.previous)} ${comparisonLabel.value}`,
      subtitle: 'Serviços concluídos no período',
      changePercent: summary.value.completedServices.changePercent,
      icon: CheckCircle2,
      iconWrapper: 'bg-amber-100',
      iconClass: 'text-amber-600',
    },
  ]
})

const secondaryCards = computed(() => {
  if (!summary.value) return []

  return [
    {
      key: 'average-ticket',
      title: 'Ticket médio',
      value: formatCurrency(summary.value.averageTicket.current),
      subtitle: `Anterior: ${formatCurrency(summary.value.averageTicket.previous)}`,
      changePercent: summary.value.averageTicket.changePercent,
      icon: TrendingUp,
      iconWrapper: 'bg-emerald-100',
      iconClass: 'text-emerald-600',
    },
    {
      key: 'active-clients',
      title: 'Clientes ativos',
      value: formatNumber(summary.value.activeClients.current),
      subtitle: `Anterior: ${formatNumber(summary.value.activeClients.previous)}`,
      changePercent: summary.value.activeClients.changePercent,
      icon: UserCheck,
      iconWrapper: 'bg-cyan-100',
      iconClass: 'text-cyan-700',
    },
    {
      key: 'active-professionals',
      title: 'Profissionais ativos',
      value: formatNumber(summary.value.activeProfessionals.current),
      subtitle: `Anterior: ${formatNumber(summary.value.activeProfessionals.previous)}`,
      changePercent: summary.value.activeProfessionals.changePercent,
      icon: Users,
      iconWrapper: 'bg-orange-100',
      iconClass: 'text-orange-700',
    },
    {
      key: 'conversion-rate',
      title: 'Taxa de conversão',
      value: `${summary.value.conversionRate.current.toLocaleString('pt-BR', {
        minimumFractionDigits: 1,
        maximumFractionDigits: 1,
      })}%`,
      subtitle: `${formatNumber(summary.value.conversionRate.currentCompletedCount)} concluídos de ${formatNumber(summary.value.conversionRate.currentRequestCount)} solicitações`,
      changePercent: summary.value.conversionRate.changePercent,
      icon: CalendarRange,
      iconWrapper: 'bg-rose-100',
      iconClass: 'text-rose-600',
    },
  ]
})

const revenueHasData = computed(() =>
  dashboard.value?.revenueSeries.some((point) => point.amount > 0) ?? false
)

const paymentStatusHasData = computed(() =>
  paymentStatusItems.value.some((item) => item.count > 0)
)

const topServicesHasData = computed(() => topServices.value.length > 0)

const isDashboardEmpty = computed(() => {
  if (!summary.value) return false

  const hasSummaryData = [
    summary.value.totalRevenue.current,
    summary.value.serviJaFee.current,
    summary.value.newUsers.current,
    summary.value.completedServices.current,
    summary.value.averageTicket.current,
    summary.value.activeClients.current,
    summary.value.activeProfessionals.current,
    summary.value.conversionRate.current,
  ].some((value) => Number(value) > 0)

  return !hasSummaryData && !revenueHasData.value && !paymentStatusHasData.value && !topServicesHasData.value
})

const revenueSeries = computed(() => dashboard.value?.revenueSeries ?? [])

const revenueChartData = computed(() => ({
  labels: revenueSeries.value.map((point) => point.label),
  datasets: [
    {
      label: 'Faturamento',
      data: revenueSeries.value.map((point) => point.amount),
      borderColor: '#286AD8',
      backgroundColor: (context: any) => {
        const { ctx, chartArea } = context.chart
        if (!chartArea) {
          return 'rgba(40, 106, 216, 0.14)'
        }

        const gradient = ctx.createLinearGradient(0, chartArea.top, 0, chartArea.bottom)
        gradient.addColorStop(0, 'rgba(40, 106, 216, 0.32)')
        gradient.addColorStop(1, 'rgba(40, 106, 216, 0.02)')
        return gradient
      },
      fill: true,
      tension: 0.35,
      borderWidth: 3,
      pointRadius: 0,
      pointHoverRadius: 5,
    },
  ],
}))

const revenueChartOptions = computed(() => ({
  responsive: true,
  maintainAspectRatio: false,
  plugins: {
    legend: {
      display: false,
    },
    tooltip: {
      callbacks: {
        label: (context: any) => formatCurrency(Number(context.parsed.y ?? 0)),
      },
    },
  },
  scales: {
    x: {
      grid: {
        display: false,
      },
      ticks: {
        color: '#6B7A90',
      },
    },
    y: {
      beginAtZero: true,
      grid: {
        color: '#E6EEF8',
      },
      ticks: {
        color: '#6B7A90',
        callback: (value: string | number) => formatCompactCurrency(Number(value)),
      },
    },
  },
}))

const revenueSummary = computed(() => {
  const total = revenueSeries.value.reduce((sum, point) => sum + point.amount, 0)
  const peak = revenueSeries.value.reduce<{ label: string; amount: number } | null>(
    (currentPeak, point) => {
      if (point.amount <= 0) return currentPeak
      if (!currentPeak || point.amount > currentPeak.amount) {
        return { label: point.label, amount: point.amount }
      }
      return currentPeak
    },
    null
  )

  return {
    total,
    peakLabel: peak?.label ?? 'Sem dados',
    peakAmount: peak?.amount ?? 0,
    average: revenueSeries.value.length > 0 ? total / revenueSeries.value.length : 0,
  }
})

const paymentStatusColors: Record<string, string> = {
  COMPLETED: '#16A34A',
  PENDING: '#F59E0B',
  CANCELLED: '#EF4444',
  REFUNDED: '#8B5CF6',
  OTHERS: '#64748B',
}

const paymentChartData = computed(() => ({
  labels: paymentStatusItems.value.map((item) => item.label),
  datasets: [
    {
      data: paymentStatusItems.value.map((item) => item.count),
      backgroundColor: paymentStatusItems.value.map(
        (item) => paymentStatusColors[item.key] || '#64748B'
      ),
      borderWidth: 0,
      hoverOffset: 8,
    },
  ],
}))

const paymentChartOptions = computed(() => ({
  responsive: true,
  maintainAspectRatio: false,
  cutout: '68%',
  plugins: {
    legend: {
      display: false,
    },
    tooltip: {
      callbacks: {
        label: (context: any) =>
          `${context.label}: ${formatNumber(Number(context.parsed ?? 0))}`,
      },
    },
  },
}))

const topServicesTotal = computed(() =>
  topServices.value.reduce((sum, item) => sum + item.contractsCount, 0)
)

const handleTimeFilterChange = (value: string) => {
  store.timeFilter = value as DashboardTimeFilter
}

const retryLoad = () => {
  store.loadDashboard()
}

watch(
  [() => store.timeFilter, () => store.customDateFrom, () => store.customDateTo],
  () => {
    store.loadDashboard()
  }
)

onMounted(() => {
  store.loadDashboard()
})
</script>

<template>
  <AdminLayout>
    <div class="space-y-6">
      <PageHeader
        title="Dashboard"
        subtitle="Acompanhe o desempenho financeiro e operacional da plataforma em tempo real."
      >
        <template #actions>
          <div class="w-full xl:min-w-[260px]">
            <Select :model-value="store.timeFilter" @update:model-value="handleTimeFilterChange">
              <SelectTrigger class="w-full bg-white">
                <SelectValue :label="selectedTimeFilterLabel" placeholder="Todo o período" />
              </SelectTrigger>
              <SelectContent>
                <SelectItem
                  v-for="option in TIME_FILTER_OPTIONS"
                  :key="option"
                  :value="option"
                >
                  {{ option }}
                </SelectItem>
              </SelectContent>
            </Select>
          </div>
        </template>
      </PageHeader>

      <Card
        v-if="store.timeFilter === 'Intervalo personalizado'"
        radius="2xl"
        padding="sm"
      >
        <div class="grid grid-cols-1 gap-4 md:grid-cols-2">
          <div class="space-y-1">
            <label class="text-sm font-medium text-slate-700">De</label>
            <input
              v-model="store.customDateFrom"
              type="date"
              class="w-full rounded-lg border border-slate-200 px-3 py-2 text-sm outline-none transition focus:border-blue-400 focus:ring-2 focus:ring-blue-100"
            />
          </div>

          <div class="space-y-1">
            <label class="text-sm font-medium text-slate-700">Até</label>
            <input
              v-model="store.customDateTo"
              type="date"
              class="w-full rounded-lg border border-slate-200 px-3 py-2 text-sm outline-none transition focus:border-blue-400 focus:ring-2 focus:ring-blue-100"
            />
          </div>
        </div>
      </Card>

      <template v-if="store.isLoading">
        <div class="grid grid-cols-1 gap-4 md:grid-cols-2 xl:grid-cols-4">
          <Card
            v-for="index in 4"
            :key="`primary-skeleton-${index}`"
            padding="none"
            radius="2xl"
            class="h-44 animate-pulse"
          />
        </div>

        <div class="grid grid-cols-1 gap-4 md:grid-cols-2 xl:grid-cols-4">
          <Card
            v-for="index in 4"
            :key="`secondary-skeleton-${index}`"
            padding="none"
            radius="2xl"
            class="h-36 animate-pulse"
          />
        </div>

        <div class="grid grid-cols-1 gap-6 xl:grid-cols-3">
          <Card padding="none" radius="3xl" class="h-[440px] animate-pulse xl:col-span-2" />
          <Card padding="none" radius="3xl" class="h-[440px] animate-pulse" />
        </div>

        <Card padding="none" radius="3xl" class="h-[320px] animate-pulse" />
      </template>

      <EmptyState
        v-else-if="store.errorMessage"
        title="Não foi possível carregar o dashboard"
        :description="store.errorMessage"
        :icon="TriangleAlert"
        tone="danger"
      >
        <template #actions>
          <button
            type="button"
            class="inline-flex items-center justify-center gap-2 rounded-xl bg-red-600 px-4 py-2.5 text-sm font-medium text-white transition hover:bg-red-700"
            @click="retryLoad"
          >
            <RefreshCw class="h-4 w-4" />
            Tentar novamente
          </button>
        </template>
      </EmptyState>

      <EmptyState
        v-else-if="isDashboardEmpty"
        title="Nenhum dado disponível para este período"
        description="Ajuste o filtro de data para explorar outro intervalo. Quando houver movimentação, os cards, gráficos e rankings serão exibidos aqui."
        :icon="LayoutDashboard"
      />

      <template v-else-if="summary && dashboard">
        <div class="grid grid-cols-1 gap-4 md:grid-cols-2 xl:grid-cols-4">
          <StatCard
            v-for="card in primaryCards"
            :key="card.key"
            :title="card.title"
            :subtitle="card.subtitle"
            :value="card.value"
            :supporting-text="card.supportingText"
            :change-percent="card.changePercent"
            :icon="card.icon"
            :icon-wrapper-class="card.iconWrapper"
            :icon-class="card.iconClass"
          />
        </div>

        <div class="grid grid-cols-1 gap-4 md:grid-cols-2 xl:grid-cols-4">
          <StatCard
            v-for="card in secondaryCards"
            :key="card.key"
            :title="card.title"
            :subtitle="card.subtitle"
            :value="card.value"
            :change-percent="card.changePercent"
            :icon="card.icon"
            :icon-wrapper-class="card.iconWrapper"
            :icon-class="card.iconClass"
            compact
            badge-position="inline"
          />
        </div>

        <div class="grid grid-cols-1 gap-6 xl:grid-cols-3">
          <Card
            variant="gradient"
            radius="3xl"
            class="overflow-hidden xl:col-span-2"
          >
            <div class="flex flex-col gap-6">
              <SectionHeader
                title="Evolução do faturamento"
                subtitle="Visualize como o faturamento se distribui ao longo do período selecionado."
              >
                <template #icon>
                  <span class="flex h-10 w-10 items-center justify-center rounded-full bg-[#286AD8] text-white shadow-[0_10px_30px_rgba(40,106,216,0.22)]">
                    <TrendingUp class="h-4 w-4" />
                  </span>
                </template>

                <template #actions>
                  <div class="grid grid-cols-1 gap-3 sm:grid-cols-3">
                    <div class="rounded-2xl border border-white/70 bg-white/85 px-4 py-3 shadow-sm">
                      <p class="text-[11px] font-medium uppercase tracking-[0.14em] text-[#6B7A90]">Total</p>
                      <p class="mt-1 text-lg font-semibold text-[#263238]">
                        {{ formatCurrency(revenueSummary.total) }}
                      </p>
                    </div>
                    <div class="rounded-2xl border border-white/70 bg-white/85 px-4 py-3 shadow-sm">
                      <p class="text-[11px] font-medium uppercase tracking-[0.14em] text-[#6B7A90]">Maior ponto</p>
                      <p class="mt-1 text-lg font-semibold text-[#263238]">
                        {{ formatCurrency(revenueSummary.peakAmount) }}
                      </p>
                      <p class="text-xs text-[#6B7A90]">{{ revenueSummary.peakLabel }}</p>
                    </div>
                    <div class="rounded-2xl border border-white/70 bg-white/85 px-4 py-3 shadow-sm">
                      <p class="text-[11px] font-medium uppercase tracking-[0.14em] text-[#6B7A90]">
                        Média por {{ dashboard.period.granularity === 'DAILY' ? 'dia' : 'mês' }}
                      </p>
                      <p class="mt-1 text-lg font-semibold text-[#263238]">
                        {{ formatCurrency(revenueSummary.average) }}
                      </p>
                      <p class="text-xs text-[#6B7A90]">Comparativo: {{ comparisonLabel }}</p>
                    </div>
                  </div>
                </template>
              </SectionHeader>

              <div class="rounded-[28px] border border-white/70 bg-white/80 p-4 shadow-[0_18px_45px_rgba(40,106,216,0.08)] backdrop-blur">
                <div class="mb-4 flex justify-end">
                  <TrendBadge :change-percent="summary.totalRevenue.changePercent" />
                </div>

                <div v-if="revenueHasData" class="h-[320px]">
                  <Line :data="revenueChartData" :options="revenueChartOptions" />
                </div>

                <EmptyState
                  v-else
                  title="Nenhum faturamento encontrado"
                  description="Quando houver serviços concluídos no período, a evolução aparecerá aqui."
                  compact
                  :surface="false"
                  class="flex h-[320px] items-center justify-center rounded-[22px] border border-dashed border-[#D9E7FF] bg-[#F8FBFF] px-6"
                />
              </div>
            </div>
          </Card>

          <Card radius="3xl" class="border-[#D9E7FF]">
            <div class="flex h-full flex-col gap-6">
              <SectionHeader
                title="Pagamentos por status"
                subtitle="Distribuição dos pagamentos gerados no período atual."
              >
                <template #icon>
                  <span class="flex h-10 w-10 items-center justify-center rounded-full bg-[#286AD8] text-white shadow-[0_10px_30px_rgba(40,106,216,0.22)]">
                    <BadgeDollarSign class="h-4 w-4" />
                  </span>
                </template>
              </SectionHeader>

              <div v-if="paymentStatusHasData" class="flex h-full flex-col gap-5">
                <div class="relative mx-auto h-[260px] w-full max-w-[280px]">
                  <Doughnut :data="paymentChartData" :options="paymentChartOptions" />
                  <div class="pointer-events-none absolute inset-0 flex items-center justify-center">
                    <div class="text-center">
                      <p class="text-[11px] font-medium uppercase tracking-[0.14em] text-[#6B7A90]">Pagamentos</p>
                      <p class="mt-1 text-lg font-semibold text-[#263238]">
                        {{ formatNumber(paymentStatusItems.reduce((sum, item) => sum + item.count, 0)) }}
                      </p>
                    </div>
                  </div>
                </div>

                <div class="space-y-3">
                  <div
                    v-for="item in paymentStatusItems"
                    :key="item.key"
                    class="flex items-center justify-between gap-3 rounded-2xl border border-[#E8EEF7] bg-[#FAFCFF] px-4 py-3"
                  >
                    <div class="min-w-0 flex items-center gap-3">
                      <span
                        class="h-3 w-3 shrink-0 rounded-full"
                        :style="{ backgroundColor: paymentStatusColors[item.key] || '#64748B' }"
                      />
                      <p class="truncate text-sm font-medium text-[#263238]">{{ item.label }}</p>
                    </div>
                    <p class="text-sm font-semibold text-[#263238]">{{ formatNumber(item.count) }}</p>
                  </div>
                </div>
              </div>

              <EmptyState
                v-else
                title="Sem pagamentos no período"
                description="Quando houver pagamentos registrados, a distribuição por status aparecerá aqui."
                compact
                :surface="false"
                class="flex min-h-[360px] items-center justify-center rounded-[22px] border border-dashed border-[#D9E7FF] bg-[#F8FBFF] px-6"
              />
            </div>
          </Card>
        </div>

        <Card radius="3xl">
          <SectionHeader
            title="Serviços mais contratados"
            subtitle="Ranking das categorias e subcategorias com mais contratações fechadas no período."
            class="mb-6"
          >
            <template #icon>
              <span class="flex h-10 w-10 items-center justify-center rounded-full bg-[#286AD8] text-white shadow-[0_10px_30px_rgba(40,106,216,0.22)]">
                <LayoutDashboard class="h-4 w-4" />
              </span>
            </template>
          </SectionHeader>

          <div v-if="topServicesHasData" class="overflow-hidden rounded-2xl border border-slate-200">
            <div class="grid grid-cols-[minmax(0,1.5fr)_120px_140px] gap-4 bg-slate-50 px-5 py-3 text-xs font-semibold uppercase tracking-wide text-slate-500">
              <span>Serviço</span>
              <span class="text-right">Contratações</span>
              <span class="text-right">Participação</span>
            </div>

            <div
              v-for="service in topServices"
              :key="service.label"
              class="grid grid-cols-[minmax(0,1.5fr)_120px_140px] items-center gap-4 border-t border-slate-100 px-5 py-4"
            >
              <div class="min-w-0">
                <p class="truncate text-sm font-medium text-slate-900">{{ service.label }}</p>
                <div class="mt-2 h-2 overflow-hidden rounded-full bg-slate-100">
                  <div
                    class="h-full rounded-full bg-[#286AD8]"
                    :style="{
                      width: `${topServicesTotal > 0 ? (service.contractsCount / topServicesTotal) * 100 : 0}%`,
                    }"
                  />
                </div>
              </div>

              <p class="text-right text-sm font-semibold text-slate-900">
                {{ formatNumber(service.contractsCount) }}
              </p>

              <p class="text-right text-sm text-slate-600">
                {{
                  topServicesTotal > 0
                    ? `${((service.contractsCount / topServicesTotal) * 100).toLocaleString('pt-BR', {
                        minimumFractionDigits: 1,
                        maximumFractionDigits: 1,
                      })}%`
                    : '0,0%'
                }}
              </p>
            </div>
          </div>

          <EmptyState
            v-else
            title="Nenhuma contratação fechada neste período"
            description="O ranking aparecerá automaticamente assim que houver serviços contratados."
            compact
            :surface="false"
            class="flex min-h-[220px] items-center justify-center rounded-2xl border border-dashed border-slate-200 bg-slate-50 px-6"
          />
        </Card>
      </template>
    </div>
  </AdminLayout>
</template>
