<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import {
  Search, X, Download, ChevronLeft, ChevronRight,
  UserPlus, UserCog, Trash2, LogIn, ShieldAlert, KeyRound, ImagePlus,
  Briefcase, FilePlus, FileEdit, FileX2, ToggleLeft,
  ClipboardList, ClipboardCheck, MessageSquare, MapPin,
  Star, CreditCard, Wallet, Landmark, FolderHeart, Heart, HeartOff,
  Folder, User as UserIcon, KeyRound as Auth, Server,
  Receipt, Headset, MessageSquareReply, Check, AlertTriangle,
  ChevronRight as ChevronRightIcon,
} from 'lucide-vue-next'
import AdminLayout from '@/layouts/AdminLayout.vue'
import AuditLogDrawer from './AuditLogDrawer.vue'
import { useAuditLogStore } from '@/stores/auditLogStore'
import type { AuditLogEntry, ActorType } from '@/api/services/auditLogService'
import Select from '@/components/ui/select.vue'
import SelectContent from '@/components/ui/select-content.vue'
import SelectItem from '@/components/ui/select-item.vue'
import SelectTrigger from '@/components/ui/select-trigger.vue'
import SelectValue from '@/components/ui/select-value.vue'

const store = useAuditLogStore()
const drawerOpen = ref(false)
const actorTypeFilter = ref('')
const actionFilter = ref('')
const entityTypeFilter = ref('')
const resultFilter = ref('')
const timeFilter = ref('Ultimos 30 dias')
const customDateFrom = ref('')
const customDateTo = ref('')

const TIME_FILTER_OPTIONS = [
  { value: 'Hoje', label: 'Hoje' },
  { value: 'Ontem', label: 'Ontem' },
  { value: 'Ultimos 7 dias', label: 'Últimos 7 dias' },
  { value: 'Ultimos 30 dias', label: 'Últimos 30 dias' },
  { value: 'Mes atual', label: 'Mês atual' },
  { value: 'Ultimo mes', label: 'Último mês' },
  { value: 'Intervalo personalizado', label: 'Intervalo personalizado' },
] as const

// ---- action metadata ----------------------------------------------------
type ActionMeta = { label: string; cls: string; icon: unknown }

const ACTION_META: Record<string, ActionMeta> = {
  // Auth
  USER_REGISTERED:              { label: 'Cadastro',                cls: 'bg-emerald-50 text-emerald-700', icon: UserPlus },
  LOGIN:                        { label: 'Login',                   cls: 'bg-violet-50 text-violet-700',   icon: LogIn },
  LOGIN_FAILED:                 { label: 'Login falhou',            cls: 'bg-rose-50 text-rose-700',       icon: ShieldAlert },
  // User profile
  PROFILE_UPDATED:              { label: 'Perfil atualizado',       cls: 'bg-blue-50 text-blue-700',       icon: UserCog },
  PASSWORD_CHANGED:             { label: 'Senha alterada',          cls: 'bg-amber-50 text-amber-700',     icon: KeyRound },
  AVATAR_UPDATED:               { label: 'Avatar atualizado',       cls: 'bg-blue-50 text-blue-700',       icon: ImagePlus },
  ACCOUNT_DELETED:              { label: 'Conta excluída',          cls: 'bg-red-50 text-red-700',         icon: Trash2 },
  FREELANCER_PROFILE_UPDATED:   { label: 'Perfil profissional',     cls: 'bg-blue-50 text-blue-700',       icon: Briefcase },
  FREELANCER_LOCATION_UPDATED:  { label: 'Localização atualizada',  cls: 'bg-blue-50 text-blue-700',       icon: MapPin },
  // Services
  SERVICE_CREATED:              { label: 'Serviço criado',          cls: 'bg-emerald-50 text-emerald-700', icon: FilePlus },
  SERVICE_UPDATED:              { label: 'Serviço atualizado',      cls: 'bg-blue-50 text-blue-700',       icon: FileEdit },
  SERVICE_DELETED:              { label: 'Serviço excluído',        cls: 'bg-red-50 text-red-700',         icon: FileX2 },
  SERVICE_TOGGLED:              { label: 'Serviço ativado/pausado', cls: 'bg-amber-50 text-amber-700',     icon: ToggleLeft },
  // Service requests
  SERVICE_REQUEST_CREATED:      { label: 'Solicitação criada',      cls: 'bg-emerald-50 text-emerald-700', icon: ClipboardList },
  SERVICE_REQUEST_STATUS:       { label: 'Status alterado',         cls: 'bg-blue-50 text-blue-700',       icon: ClipboardList },
  SERVICE_REQUEST_RESPONDED:    { label: 'Proposta enviada',        cls: 'bg-blue-50 text-blue-700',       icon: MessageSquareReply },
  SERVICE_REQUEST_COMPLETED:    { label: 'Serviço concluído',       cls: 'bg-emerald-50 text-emerald-700', icon: ClipboardCheck },
  SERVICE_REQUEST_DELETED:      { label: 'Solicitação excluída',    cls: 'bg-red-50 text-red-700',         icon: Trash2 },
  MILESTONE_UPDATED:            { label: 'Marco atualizado',        cls: 'bg-blue-50 text-blue-700',       icon: ClipboardList },
  COUNTER_PROPOSAL_SENT:        { label: 'Contraproposta enviada',  cls: 'bg-blue-50 text-blue-700',       icon: MessageSquare },
  COUNTER_PROPOSAL_ACCEPTED:    { label: 'Contraproposta aceita',   cls: 'bg-emerald-50 text-emerald-700', icon: Check },
  COUNTER_PROPOSAL_REJECTED:    { label: 'Contraproposta recusada', cls: 'bg-rose-50 text-rose-700',       icon: X },
  FREELANCER_ATTACHMENTS:       { label: 'Entrega anexada',         cls: 'bg-blue-50 text-blue-700',       icon: ClipboardList },
  CONTRACTOR_ATTACHMENTS:       { label: 'Anexos do contratante',   cls: 'bg-blue-50 text-blue-700',       icon: ClipboardList },
  // Reviews
  REVIEW_CREATED:               { label: 'Avaliação criada',        cls: 'bg-emerald-50 text-emerald-700', icon: Star },
  // Payments
  PAYMENT_CREATED:              { label: 'Pagamento iniciado',      cls: 'bg-blue-50 text-blue-700',       icon: CreditCard },
  PAYMENT_SIMULATED:            { label: 'Pagamento simulado',      cls: 'bg-amber-50 text-amber-700',     icon: CreditCard },
  // Wallet
  WITHDRAWAL_REQUESTED:         { label: 'Saque solicitado',        cls: 'bg-amber-50 text-amber-700',     icon: Wallet },
  WITHDRAWAL_CANCELLED:         { label: 'Saque cancelado',         cls: 'bg-rose-50 text-rose-700',       icon: Wallet },
  BANK_ACCOUNT_ADDED:           { label: 'Conta bancária criada',   cls: 'bg-emerald-50 text-emerald-700', icon: Landmark },
  BANK_ACCOUNT_DELETED:         { label: 'Conta bancária excluída', cls: 'bg-red-50 text-red-700',         icon: Landmark },
  BANK_ACCOUNT_DEFAULT:         { label: 'Conta padrão definida',   cls: 'bg-blue-50 text-blue-700',       icon: Landmark },
  // Portfolio
  PORTFOLIO_CREATED:            { label: 'Projeto criado',          cls: 'bg-emerald-50 text-emerald-700', icon: FolderHeart },
  PORTFOLIO_UPDATED:            { label: 'Projeto atualizado',      cls: 'bg-blue-50 text-blue-700',       icon: FolderHeart },
  PORTFOLIO_DELETED:            { label: 'Projeto excluído',        cls: 'bg-red-50 text-red-700',         icon: FolderHeart },
  // Favorites
  FAVORITE_ADDED:               { label: 'Favoritado',              cls: 'bg-rose-50 text-rose-700',       icon: Heart },
  FAVORITE_REMOVED:             { label: 'Desfavoritado',           cls: 'bg-slate-100 text-slate-700',    icon: HeartOff },
  // Support
  SUPPORT_REQUEST_CREATED:      { label: 'Ticket criado',           cls: 'bg-amber-50 text-amber-700',     icon: Headset },
  // Adjustments
  ADJUSTMENT_REQUESTED:         { label: 'Ajuste solicitado',       cls: 'bg-amber-50 text-amber-700',     icon: ClipboardList },
  ADJUSTMENT_STATUS_CHANGED:    { label: 'Ajuste atualizado',       cls: 'bg-blue-50 text-blue-700',       icon: ClipboardList },
  ADJUSTMENT_DELETED:           { label: 'Ajuste excluído',         cls: 'bg-red-50 text-red-700',         icon: Trash2 },
}

const fallbackActionMeta: ActionMeta = { label: '', cls: 'bg-slate-100 text-slate-700', icon: ClipboardList }

const actionMeta = (action: string): ActionMeta => {
  return ACTION_META[action] ?? { ...fallbackActionMeta, label: action }
}

// ---- entity icons -------------------------------------------------------
const ENTITY_ICONS: Record<string, unknown> = {
  USER: UserIcon,
  SERVICE: Briefcase,
  SERVICE_REQUEST: ClipboardList,
  REVIEW: Star,
  PAYMENT: CreditCard,
  WITHDRAWAL: Wallet,
  BANK_ACCOUNT: Landmark,
  PORTFOLIO: FolderHeart,
  FAVORITE: Heart,
  SUPPORT_TICKET: Headset,
  AUTH: Auth,
  ADJUSTMENT: Receipt,
  SYSTEM: Server,
  CATEGORY: Folder,
}

const ENTITY_LABEL: Record<string, string> = {
  USER: 'Usuário',
  SERVICE: 'Serviço',
  SERVICE_REQUEST: 'Solicitação',
  REVIEW: 'Avaliação',
  PAYMENT: 'Pagamento',
  WITHDRAWAL: 'Saque',
  BANK_ACCOUNT: 'Conta bancária',
  PORTFOLIO: 'Portfólio',
  FAVORITE: 'Favorito',
  SUPPORT_TICKET: 'Suporte',
  AUTH: 'Autenticação',
  ADJUSTMENT: 'Ajuste',
  SYSTEM: 'Sistema',
  CATEGORY: 'Categoria',
}

// ---- actor type metadata ------------------------------------------------
const ACTOR_TYPE_META: Record<ActorType, { label: string; cls: string }> = {
  CONTRACTOR: { label: 'Contratante', cls: 'bg-blue-50 text-blue-700' },
  FREELANCER: { label: 'Freelancer',  cls: 'bg-emerald-50 text-emerald-700' },
  ADMIN:      { label: 'Admin',       cls: 'bg-violet-50 text-violet-700' },
  GUEST:      { label: 'Anônimo',     cls: 'bg-slate-100 text-slate-700' },
}

// ---- avatar colors -------------------------------------------------------
const AVATAR_COLORS = [
  'bg-purple-500', 'bg-blue-500', 'bg-emerald-500', 'bg-orange-500',
  'bg-rose-500', 'bg-cyan-500', 'bg-indigo-500', 'bg-amber-500',
]
const actorColor = (id: number | null) => AVATAR_COLORS[(id ?? 0) % AVATAR_COLORS.length]
const actorInitials = (name: string | null) => {
  if (!name) return '?'
  return name.split(' ').slice(0, 2).map((p) => p[0]).join('').toUpperCase()
}

// ---- date/time formatting ------------------------------------------------
const fmtDate = (iso: string) => new Date(iso).toLocaleDateString('pt-BR')
const fmtTime = (iso: string) => new Date(iso).toLocaleTimeString('pt-BR')

// ---- filters -------------------------------------------------------------
const searchInput = ref('')
let searchTimer: ReturnType<typeof setTimeout>
const onSearch = () => {
  clearTimeout(searchTimer)
  searchTimer = setTimeout(() => {
    onFilterChange()
  }, 300)
}

const onFilterChange = () => {
  const normalizedSearch = searchInput.value.trim()
  const { dateFrom, dateTo } = resolveDateRange()
  const shouldPrioritizeSearch = normalizedSearch.length > 0

  store.filters.search = normalizedSearch
  store.filters.actorType = shouldPrioritizeSearch ? '' : actorTypeFilter.value
  store.filters.action = shouldPrioritizeSearch ? '' : actionFilter.value
  store.filters.entityType = shouldPrioritizeSearch ? '' : entityTypeFilter.value
  store.filters.result = shouldPrioritizeSearch ? '' : resultFilter.value
  store.filters.dateFrom = shouldPrioritizeSearch ? '' : dateFrom
  store.filters.dateTo = shouldPrioritizeSearch ? '' : dateTo
  store.filters.page = 0
  store.loadLogs()
}

const clearFilters = () => {
  searchInput.value = ''
  actorTypeFilter.value = ''
  actionFilter.value = ''
  entityTypeFilter.value = ''
  resultFilter.value = ''
  timeFilter.value = 'Ultimos 30 dias'
  customDateFrom.value = ''
  customDateTo.value = ''
  store.filters = { search: '', action: '', entityType: '', actorType: '', result: '', dateFrom: '', dateTo: '', page: 0, size: 20 }
  onFilterChange()
}

// Distinct actions seen in current page — used to populate dropdown
const knownActions = computed(() => {
  return Object.keys(ACTION_META)
})

const knownEntityTypes = computed(() => Object.keys(ENTITY_LABEL))

const selectedActorTypeLabel = computed(() => {
  switch (store.filters.actorType) {
    case 'CONTRACTOR':
      return 'Contratante'
    case 'FREELANCER':
      return 'Freelancer'
    case 'GUEST':
      return 'Anônimo'
    default:
      return 'Tipo: Todos'
  }
})

const selectedActionLabel = computed(() =>
  store.filters.action ? ACTION_META[store.filters.action]?.label ?? store.filters.action : 'Todas as ações'
)

const selectedEntityTypeLabel = computed(() =>
  store.filters.entityType ? ENTITY_LABEL[store.filters.entityType] ?? store.filters.entityType : 'Todas as entidades'
)

const selectedResultLabel = computed(() => {
  switch (store.filters.result) {
    case 'SUCCESS':
      return 'Sucesso'
    case 'FAILURE':
      return 'Falha'
    default:
      return 'Resultado: Todos'
  }
})

const handleTimeFilterChange = (value: string) => {
  timeFilter.value = value
  onFilterChange()
}

const selectedTimeFilterLabel = computed(() =>
  TIME_FILTER_OPTIONS.find((option) => option.value === timeFilter.value)?.label || 'Últimos 30 dias'
)

const formatDateInput = (date: Date) => {
  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  return `${year}-${month}-${day}`
}

const resolveDateRange = () => {
  const now = new Date()
  const startOfToday = new Date(now.getFullYear(), now.getMonth(), now.getDate())

  switch (timeFilter.value) {
    case 'Hoje':
      return { dateFrom: formatDateInput(startOfToday), dateTo: formatDateInput(startOfToday) }
    case 'Ontem': {
      const yesterday = new Date(startOfToday)
      yesterday.setDate(yesterday.getDate() - 1)
      return { dateFrom: formatDateInput(yesterday), dateTo: formatDateInput(yesterday) }
    }
    case 'Ultimos 7 dias': {
      const sevenDaysAgo = new Date(startOfToday)
      sevenDaysAgo.setDate(sevenDaysAgo.getDate() - 7)
      return { dateFrom: formatDateInput(sevenDaysAgo), dateTo: formatDateInput(startOfToday) }
    }
    case 'Ultimos 30 dias': {
      const thirtyDaysAgo = new Date(startOfToday)
      thirtyDaysAgo.setDate(thirtyDaysAgo.getDate() - 30)
      return { dateFrom: formatDateInput(thirtyDaysAgo), dateTo: formatDateInput(startOfToday) }
    }
    case 'Mes atual': {
      const startOfMonth = new Date(now.getFullYear(), now.getMonth(), 1)
      return { dateFrom: formatDateInput(startOfMonth), dateTo: formatDateInput(startOfToday) }
    }
    case 'Ultimo mes': {
      const startOfLastMonth = new Date(now.getFullYear(), now.getMonth() - 1, 1)
      const endOfLastMonth = new Date(now.getFullYear(), now.getMonth(), 0)
      return { dateFrom: formatDateInput(startOfLastMonth), dateTo: formatDateInput(endOfLastMonth) }
    }
    case 'Intervalo personalizado':
      return { dateFrom: customDateFrom.value, dateTo: customDateTo.value }
    default:
      return { dateFrom: '', dateTo: '' }
  }
}

// ---- pagination ----------------------------------------------------------
const goToPage = (p: number) => {
  store.filters.page = p
  const normalizedSearch = searchInput.value.trim()
  const { dateFrom, dateTo } = resolveDateRange()
  const shouldPrioritizeSearch = normalizedSearch.length > 0

  store.loadLogs({
    ...store.filters,
    search: normalizedSearch,
    actorType: shouldPrioritizeSearch ? '' : store.filters.actorType,
    action: shouldPrioritizeSearch ? '' : store.filters.action,
    entityType: shouldPrioritizeSearch ? '' : store.filters.entityType,
    result: shouldPrioritizeSearch ? '' : store.filters.result,
    dateFrom: shouldPrioritizeSearch ? '' : dateFrom,
    dateTo: shouldPrioritizeSearch ? '' : dateTo,
    page: p,
  })
}

const pageNumbers = () => {
  const total = store.totalPages
  if (total <= 7) return Array.from({ length: total }, (_, i) => i)
  const cur = store.currentPage
  const pages: (number | '…')[] = [0]
  if (cur > 2) pages.push('…')
  for (let i = Math.max(1, cur - 1); i <= Math.min(total - 2, cur + 1); i++) pages.push(i)
  if (cur < total - 3) pages.push('…')
  pages.push(total - 1)
  return pages
}

// ---- drawer --------------------------------------------------------------
const openDrawer = (log: AuditLogEntry) => {
  store.selectedLog = log
  drawerOpen.value = true
}
const closeDrawer = () => {
  drawerOpen.value = false
  store.clearSelectedLog()
}

// ---- export CSV ----------------------------------------------------------
const exportCsv = () => {
  const rows = [
    ['ID', 'Data/Hora', 'Usuário', 'Email', 'Tipo', 'Ação', 'Entidade', 'ID Entidade', 'Resultado', 'IP', 'User Agent'],
    ...store.logs.map((l) => [
      l.id,
      new Date(l.timestamp).toLocaleString('pt-BR'),
      l.actorName ?? '',
      l.actorEmail ?? '',
      l.actorType ?? '',
      l.action,
      l.entityType ?? '',
      l.entityId ?? '',
      l.result,
      l.ipAddress ?? '',
      l.userAgent ?? '',
    ]),
  ]
  const csv = rows.map((r) => r.map((v) => `"${String(v).replace(/"/g, '""')}"`).join(',')).join('\n')
  const blob = new Blob([csv], { type: 'text/csv' })
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = 'audit-logs.csv'
  a.click()
  URL.revokeObjectURL(url)
}

onMounted(() => onFilterChange())
</script>

<template>
  <AdminLayout>
    <div class="space-y-6">
      <!-- Header -->
      <div class="flex flex-wrap items-end justify-between gap-3">
        <div>
          <h1 class="text-2xl font-bold text-slate-900">Logs de Auditoria</h1>
          <p class="mt-1 text-sm text-slate-500">
            Rastreie todas as ações realizadas por contratantes e freelancers na plataforma
          </p>
        </div>
        <button
          type="button"
          class="flex items-center gap-2 rounded-lg border border-slate-200 bg-white px-3 py-2 text-sm font-medium text-slate-700 shadow-sm transition hover:bg-slate-50"
          @click="exportCsv"
        >
          <Download class="h-4 w-4" />
          Exportar CSV
        </button>
      </div>

      <!-- Filters -->
      <div class="rounded-xl border border-slate-200 bg-white p-4 shadow-sm">
        <div class="flex flex-wrap gap-3">
          <div class="relative min-w-[240px] flex-1">
            <Search class="absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-slate-400" />
            <input
              v-model="searchInput"
              type="text"
              placeholder="Buscar por usuário, ação ou ID da entidade..."
              class="w-full rounded-lg border border-slate-200 py-2 pl-9 pr-3 text-sm outline-none transition focus:border-blue-400 focus:ring-2 focus:ring-blue-100"
              @input="onSearch"
            />
          </div>

          <select v-if="false"
            v-model="store.filters.actorType"
            class="rounded-lg border border-slate-200 px-3 py-2 text-sm outline-none focus:border-blue-400 focus:ring-2 focus:ring-blue-100"
            @change="onFilterChange"
          >
            <option value="">Tipo: Todos</option>
            <option value="CONTRACTOR">Contratante</option>
            <option value="FREELANCER">Freelancer</option>
            <option value="GUEST">Anônimo</option>
          </select>

          <Select
            :model-value="store.filters.actorType"
            @update:model-value="
              (value) => {
                store.filters.actorType = value as typeof store.filters.actorType
                onFilterChange()
              }
            "
          >
            <SelectTrigger class="min-w-[180px]">
              <SelectValue :label="selectedActorTypeLabel" placeholder="Tipo: Todos" />
            </SelectTrigger>
            <SelectContent>
              <SelectItem value="">Tipo: Todos</SelectItem>
              <SelectItem value="CONTRACTOR">Contratante</SelectItem>
              <SelectItem value="FREELANCER">Freelancer</SelectItem>
              <SelectItem value="GUEST">Anônimo</SelectItem>
            </SelectContent>
          </Select>

          <select v-if="false"
            v-model="store.filters.action"
            class="rounded-lg border border-slate-200 px-3 py-2 text-sm outline-none focus:border-blue-400 focus:ring-2 focus:ring-blue-100"
            @change="onFilterChange"
          >
            <option value="">Todas as ações</option>
            <option v-for="a in knownActions" :key="a" :value="a">{{ ACTION_META[a].label }}</option>
          </select>

          <Select
            :model-value="store.filters.action"
            @update:model-value="
              (value) => {
                store.filters.action = value
                onFilterChange()
              }
            "
          >
            <SelectTrigger class="min-w-[220px]">
              <SelectValue :label="selectedActionLabel" placeholder="Todas as ações" />
            </SelectTrigger>
            <SelectContent>
              <SelectItem value="">Todas as ações</SelectItem>
              <SelectItem v-for="a in knownActions" :key="a" :value="a">{{ ACTION_META[a].label }}</SelectItem>
            </SelectContent>
          </Select>

          <select v-if="false"
            v-model="store.filters.entityType"
            class="rounded-lg border border-slate-200 px-3 py-2 text-sm outline-none focus:border-blue-400 focus:ring-2 focus:ring-blue-100"
            @change="onFilterChange"
          >
            <option value="">Todas as entidades</option>
            <option v-for="e in knownEntityTypes" :key="e" :value="e">{{ ENTITY_LABEL[e] }}</option>
          </select>

          <Select
            :model-value="store.filters.entityType"
            @update:model-value="
              (value) => {
                store.filters.entityType = value
                onFilterChange()
              }
            "
          >
            <SelectTrigger class="min-w-[220px]">
              <SelectValue :label="selectedEntityTypeLabel" placeholder="Todas as entidades" />
            </SelectTrigger>
            <SelectContent>
              <SelectItem value="">Todas as entidades</SelectItem>
              <SelectItem v-for="e in knownEntityTypes" :key="e" :value="e">{{ ENTITY_LABEL[e] }}</SelectItem>
            </SelectContent>
          </Select>

          <select v-if="false"
            v-model="store.filters.result"
            class="rounded-lg border border-slate-200 px-3 py-2 text-sm outline-none focus:border-blue-400 focus:ring-2 focus:ring-blue-100"
            @change="onFilterChange"
          >
            <option value="">Resultado: Todos</option>
            <option value="SUCCESS">Sucesso</option>
            <option value="FAILURE">Falha</option>
          </select>

          <Select
            :model-value="store.filters.result"
            @update:model-value="
              (value) => {
                store.filters.result = value as typeof store.filters.result
                onFilterChange()
              }
            "
          >
            <SelectTrigger class="min-w-[180px]">
              <SelectValue :label="selectedResultLabel" placeholder="Resultado: Todos" />
            </SelectTrigger>
            <SelectContent>
              <SelectItem value="">Resultado: Todos</SelectItem>
              <SelectItem value="SUCCESS">Sucesso</SelectItem>
              <SelectItem value="FAILURE">Falha</SelectItem>
            </SelectContent>
          </Select>

          <Select
            :model-value="timeFilter"
            @update:model-value="handleTimeFilterChange"
          >
            <SelectTrigger class="min-w-[220px]">
              <SelectValue :label="selectedTimeFilterLabel" placeholder="Últimos 30 dias" />
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

          <input
            v-if="false"
            v-model="store.filters.dateFrom"
            type="date"
            title="De"
            class="rounded-lg border border-slate-200 px-3 py-2 text-sm text-slate-700 outline-none focus:border-blue-400 focus:ring-2 focus:ring-blue-100"
            @change="onFilterChange"
          />
          <input
            v-if="false"
            v-model="store.filters.dateTo"
            type="date"
            title="Até"
            class="rounded-lg border border-slate-200 px-3 py-2 text-sm text-slate-700 outline-none focus:border-blue-400 focus:ring-2 focus:ring-blue-100"
            @change="onFilterChange"
          />

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
              @change="onFilterChange"
            />
          </div>

          <div class="space-y-1">
            <label class="text-sm font-medium text-slate-700">Até</label>
            <input
              v-model="customDateTo"
              type="date"
              class="w-full rounded-lg border border-slate-200 px-3 py-2 text-sm outline-none transition focus:border-blue-400 focus:ring-2 focus:ring-blue-100"
              @change="onFilterChange"
            />
          </div>
        </div>
      </div>

      <!-- Table -->
      <div class="overflow-hidden rounded-xl border border-slate-200 bg-white shadow-sm">
        <table class="min-w-full divide-y divide-slate-100">
          <thead class="bg-slate-50">
            <tr>
              <th class="px-6 py-3 text-left text-xs font-semibold uppercase tracking-wide text-slate-500">Data / Hora</th>
              <th class="px-4 py-3 text-left text-xs font-semibold uppercase tracking-wide text-slate-500">Usuário</th>
              <th class="px-4 py-3 text-left text-xs font-semibold uppercase tracking-wide text-slate-500">Ação</th>
              <th class="px-4 py-3 text-left text-xs font-semibold uppercase tracking-wide text-slate-500">Entidade</th>
              <th class="px-4 py-3 text-left text-xs font-semibold uppercase tracking-wide text-slate-500">Resultado</th>
              <th class="px-4 py-3 text-left text-xs font-semibold uppercase tracking-wide text-slate-500">IP / Origem</th>
              <th class="px-4 py-3 text-right text-xs font-semibold uppercase tracking-wide text-slate-500"></th>
            </tr>
          </thead>
          <tbody class="divide-y divide-slate-100">
            <template v-if="store.isLoading">
              <tr v-for="i in 8" :key="i" class="animate-pulse">
                <td class="px-6 py-4">
                  <div class="h-3 w-20 rounded bg-slate-200"></div>
                  <div class="mt-1 h-2.5 w-14 rounded bg-slate-100"></div>
                </td>
                <td class="px-4 py-4">
                  <div class="flex items-center gap-3">
                    <div class="h-9 w-9 rounded-full bg-slate-200"></div>
                    <div>
                      <div class="h-3 w-24 rounded bg-slate-200"></div>
                      <div class="mt-1 h-2.5 w-32 rounded bg-slate-100"></div>
                    </div>
                  </div>
                </td>
                <td class="px-4 py-4"><div class="h-5 w-28 rounded-full bg-slate-200"></div></td>
                <td class="px-4 py-4">
                  <div class="flex items-center gap-2">
                    <div class="h-7 w-7 rounded-lg bg-slate-200"></div>
                    <div>
                      <div class="h-3 w-24 rounded bg-slate-200"></div>
                      <div class="mt-1 h-2.5 w-20 rounded bg-slate-100"></div>
                    </div>
                  </div>
                </td>
                <td class="px-4 py-4"><div class="h-5 w-16 rounded-full bg-slate-200"></div></td>
                <td class="px-4 py-4">
                  <div class="h-3 w-24 rounded bg-slate-200"></div>
                  <div class="mt-1 h-2.5 w-28 rounded bg-slate-100"></div>
                </td>
                <td class="px-4 py-4 text-right"><div class="ml-auto h-4 w-4 rounded bg-slate-200"></div></td>
              </tr>
            </template>

            <tr v-else-if="!store.logs.length">
              <td colspan="7" class="px-6 py-16 text-center text-sm text-slate-500">
                Nenhum log encontrado para os filtros aplicados.
              </td>
            </tr>

            <template v-else>
              <tr
                v-for="log in store.logs"
                :key="log.id"
                class="cursor-pointer transition hover:bg-slate-50"
                @click="openDrawer(log)"
              >
                <td class="px-6 py-4 align-middle">
                  <p class="text-sm font-medium text-slate-900">{{ fmtDate(log.timestamp) }}</p>
                  <p class="font-mono text-xs text-slate-500">{{ fmtTime(log.timestamp) }}</p>
                </td>
                <td class="px-4 py-4 align-middle">
                  <div class="flex items-center gap-3">
                    <div
                      :class="['flex h-9 w-9 items-center justify-center rounded-full text-xs font-semibold text-white', actorColor(log.actorId)]"
                    >
                      {{ actorInitials(log.actorName) }}
                    </div>
                    <div class="min-w-0">
                      <p class="truncate text-sm font-medium text-slate-900">{{ log.actorName ?? '—' }}</p>
                      <div class="flex items-center gap-1.5">
                        <p class="truncate text-xs text-slate-500">{{ log.actorEmail ?? '—' }}</p>
                        <span
                          v-if="log.actorType"
                          :class="['inline-flex items-center rounded-full px-1.5 py-0.5 text-[10px] font-medium', ACTOR_TYPE_META[log.actorType].cls]"
                        >
                          {{ ACTOR_TYPE_META[log.actorType].label }}
                        </span>
                      </div>
                    </div>
                  </div>
                </td>
                <td class="px-4 py-4 align-middle">
                  <span
                    :class="['inline-flex items-center gap-1.5 rounded-full px-2.5 py-0.5 text-xs font-medium', actionMeta(log.action).cls]"
                  >
                    <component :is="actionMeta(log.action).icon" class="h-3 w-3" />
                    {{ actionMeta(log.action).label }}
                  </span>
                </td>
                <td class="px-4 py-4 align-middle">
                  <div v-if="log.entityType" class="flex items-center gap-2">
                    <span class="flex h-7 w-7 items-center justify-center rounded-lg bg-slate-100 text-slate-600">
                      <component :is="ENTITY_ICONS[log.entityType] ?? Server" class="h-3.5 w-3.5" />
                    </span>
                    <div class="min-w-0">
                      <p class="truncate text-sm font-medium text-slate-900">{{ log.entityLabel ?? (ENTITY_LABEL[log.entityType] ?? log.entityType) }}</p>
                      <p class="text-xs text-slate-500">
                        {{ ENTITY_LABEL[log.entityType] ?? log.entityType }}
                        <span v-if="log.entityId" class="font-mono"> · {{ log.entityId }}</span>
                      </p>
                    </div>
                  </div>
                  <span v-else class="text-xs text-slate-400">—</span>
                </td>
                <td class="px-4 py-4 align-middle">
                  <span
                    v-if="log.result === 'SUCCESS'"
                    class="inline-flex items-center gap-1.5 rounded-full bg-emerald-50 px-2.5 py-0.5 text-xs font-medium text-emerald-700"
                  >
                    <Check class="h-3 w-3" /> Sucesso
                  </span>
                  <span
                    v-else
                    class="inline-flex items-center gap-1.5 rounded-full bg-red-50 px-2.5 py-0.5 text-xs font-medium text-red-700"
                  >
                    <AlertTriangle class="h-3 w-3" /> Falha
                  </span>
                </td>
                <td class="px-4 py-4 align-middle">
                  <p class="font-mono text-sm text-slate-700">{{ log.ipAddress ?? '—' }}</p>
                  <p class="text-xs text-slate-500">{{ log.userAgent ?? '—' }}</p>
                </td>
                <td class="px-4 py-4 text-right align-middle">
                  <span class="inline-flex h-8 w-8 items-center justify-center rounded-lg text-slate-400">
                    <ChevronRightIcon class="h-4 w-4" />
                  </span>
                </td>
              </tr>
            </template>
          </tbody>
        </table>
      </div>

      <!-- Pagination -->
      <div v-if="store.totalPages > 0" class="flex items-center justify-between">
        <p class="text-sm text-slate-500">
          <span class="font-medium text-slate-900">{{ store.totalElements }}</span> registros no total
        </p>
        <div class="flex items-center gap-1">
          <button
            :disabled="store.currentPage === 0"
            class="flex h-8 w-8 items-center justify-center rounded-lg border border-slate-200 text-slate-600 transition hover:bg-slate-50 disabled:opacity-40"
            @click="goToPage(store.currentPage - 1)"
          >
            <ChevronLeft class="h-4 w-4" />
          </button>
          <template v-for="p in pageNumbers()" :key="p">
            <span v-if="p === '…'" class="flex h-8 w-8 items-center justify-center text-sm text-slate-400">…</span>
            <button
              v-else
              :class="[
                'flex h-8 w-8 items-center justify-center rounded-lg border text-sm transition',
                p === store.currentPage
                  ? 'border-blue-500 bg-blue-500 text-white'
                  : 'border-slate-200 text-slate-700 hover:bg-slate-50',
              ]"
              @click="goToPage(p as number)"
            >
              {{ (p as number) + 1 }}
            </button>
          </template>
          <button
            :disabled="store.currentPage >= store.totalPages - 1"
            class="flex h-8 w-8 items-center justify-center rounded-lg border border-slate-200 text-slate-600 transition hover:bg-slate-50 disabled:opacity-40"
            @click="goToPage(store.currentPage + 1)"
          >
            <ChevronRight class="h-4 w-4" />
          </button>
        </div>
      </div>
    </div>
  </AdminLayout>

  <AuditLogDrawer :log="store.selectedLog" :open="drawerOpen" @close="closeDrawer" />
</template>
