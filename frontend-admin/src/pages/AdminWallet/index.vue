<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import {
  ArrowDownToLine,
  ArrowUpFromLine,
  Building2,
  CreditCard,
  Download,
  Landmark,
  Loader2,
  Plus,
  ReceiptText,
  RefreshCw,
  ShieldAlert,
  Star,
  Trash2,
  Wallet,
  X,
} from 'lucide-vue-next'
import AdminLayout from '@/layouts/AdminLayout.vue'
import Card from '@/components/ui/card.vue'
import DropdownMenu from '@/components/ui/dropdown-menu.vue'
import DropdownMenuContent from '@/components/ui/dropdown-menu-content.vue'
import DropdownMenuItem from '@/components/ui/dropdown-menu-item.vue'
import EmptyState from '@/components/ui/empty-state.vue'
import PageHeader from '@/components/ui/page-header.vue'
import Select from '@/components/ui/select.vue'
import SelectContent from '@/components/ui/select-content.vue'
import SelectItem from '@/components/ui/select-item.vue'
import SelectTrigger from '@/components/ui/select-trigger.vue'
import SelectValue from '@/components/ui/select-value.vue'
import StatCard from '@/components/ui/stat-card.vue'
import { toast } from '@/utils/toast'
import {
  useAdminPlatformWalletStore,
  type AdminWalletTimeFilter,
} from '@/stores/adminPlatformWalletStore'
import type {
  AdminPlatformBankAccount,
  AdminPlatformCreateBankAccountRequest,
} from '@/api/services/adminPlatformWalletService'

type PixKeyType = 'CPF' | 'CNPJ' | 'EMAIL' | 'PHONE' | 'RANDOM'
type AccountType = 'PIX_KEY' | 'BANK_ACCOUNT'

const TIME_FILTER_OPTIONS: AdminWalletTimeFilter[] = [
  'Todo o período',
  'Hoje',
  'Ontem',
  'Últimos 7 dias',
  'Últimos 30 dias',
  'Mês atual',
  'Último mês',
  'Intervalo personalizado',
]

const store = useAdminPlatformWalletStore()

const showWithdrawModal = ref(false)
const showAddAccountModal = ref(false)
const reportMenuOpen = ref(false)

const withdrawAmount = ref('')
const withdrawBankAccountId = ref<number | null>(null)

const newAccountType = ref<AccountType>('PIX_KEY')
const newPixKey = ref('')
const newPixKeyType = ref<PixKeyType>('CPF')
const newBankName = ref('')
const newBankCode = ref('')
const newAgency = ref('')
const newAgencyDigit = ref('')
const newAccountNumber = ref('')
const newAccountDigit = ref('')
const newAccountBankType = ref('CONTA_CORRENTE')
const newHolderName = ref('')
const newHolderCpfCnpj = ref('')
const newIsDefault = ref(false)

const wallet = computed(() => store.wallet)
const transactions = computed(() => store.transactions)
const bankAccounts = computed(() => store.bankAccounts)
const transactionsSummaryLabel = computed(() => {
  const count = transactions.value.length
  return `${count} ${count === 1 ? 'transação' : 'transações'} no recorte selecionado.`
})

const hasAccounts = computed(() => bankAccounts.value.length > 0)
const withdrawableAccounts = computed(() => bankAccounts.value)
const preferredAccountId = computed(
  () => bankAccounts.value.find((account) => account.isDefault)?.id ?? bankAccounts.value[0]?.id ?? null
)

const formatCurrency = (value: number | null | undefined) =>
  new Intl.NumberFormat('pt-BR', {
    style: 'currency',
    currency: 'BRL',
  }).format(value ?? 0)

const formatDateTime = (value?: string) => {
  if (!value) return '-'

  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return '-'

  return new Intl.DateTimeFormat('pt-BR', {
    day: '2-digit',
    month: '2-digit',
    year: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
  }).format(date)
}

const parseAmount = (value: string) => {
  if (!value) return 0
  const normalized = value.replace(/\./g, '').replace(',', '.').trim()
  const parsed = Number(normalized)
  return Number.isFinite(parsed) ? parsed : 0
}

const formatStatusLabel = (status?: string) => {
  switch (status) {
    case 'COMPLETED':
      return 'Concluído'
    case 'PENDING':
      return 'Pendente'
    case 'PROCESSING':
      return 'Processando'
    case 'FAILED':
      return 'Falhou'
    case 'CANCELLED':
      return 'Cancelado'
    default:
      return status || '-'
  }
}

const getStatusClasses = (status?: string) => {
  switch (status) {
    case 'COMPLETED':
      return 'bg-emerald-100 text-emerald-700'
    case 'PENDING':
      return 'bg-amber-100 text-amber-800'
    case 'PROCESSING':
      return 'bg-blue-100 text-blue-700'
    case 'FAILED':
      return 'bg-red-100 text-red-700'
    case 'CANCELLED':
      return 'bg-slate-200 text-slate-700'
    default:
      return 'bg-slate-100 text-slate-700'
  }
}

const normalizePaymentMethod = (value?: string) => {
  if (!value) return '-'
  const normalized = value.toLowerCase()
  if (normalized === 'credit-card') return 'Cartão'
  if (normalized === 'pix') return 'PIX'
  return value.toUpperCase()
}

const describeAccount = (account: AdminPlatformBankAccount) => {
  if (account.accountType === 'PIX_KEY') {
    const label = account.pixKeyType === 'EMAIL' ? 'E-mail' : account.pixKeyType || 'PIX'
    return `${label}: ${account.pixKeyValue || account.pixKey || '-'}`
  }

  return `${account.bankName || 'Banco'} - Ag ${account.agency || '--'} - Conta ${account.accountNumber || '--'}`
}

const summaryCards = computed(() => [
  {
    key: 'available',
    title: 'Disponível para saque',
    subtitle: 'Saldo liberado para a plataforma',
    value: formatCurrency(wallet.value?.availableBalance),
    icon: Wallet,
    iconWrapper: 'bg-emerald-100',
    iconClass: 'text-emerald-700',
  },
  {
    key: 'reserved',
    title: 'Em processamento',
    subtitle: 'Valores reservados em saques',
    value: formatCurrency(wallet.value?.reservedBalance),
    icon: ArrowUpFromLine,
    iconWrapper: 'bg-amber-100',
    iconClass: 'text-amber-700',
  },
  {
    key: 'earned',
    title: 'Taxas acumuladas',
    subtitle: 'Total de 5% liberado em serviços concluídos',
    value: formatCurrency(wallet.value?.totalEarned),
    icon: ReceiptText,
    iconWrapper: 'bg-blue-100',
    iconClass: 'text-blue-700',
  },
  {
    key: 'withdrawn',
    title: 'Total sacado',
    subtitle: 'Saques concluídos da plataforma',
    value: formatCurrency(wallet.value?.totalWithdrawn),
    icon: Landmark,
    iconWrapper: 'bg-slate-100',
    iconClass: 'text-slate-700',
  },
])

const openWithdrawModal = () => {
  withdrawAmount.value = ''
  withdrawBankAccountId.value = preferredAccountId.value
  showWithdrawModal.value = true
}

const resetAccountForm = () => {
  newAccountType.value = 'PIX_KEY'
  newPixKey.value = ''
  newPixKeyType.value = 'CPF'
  newBankName.value = ''
  newBankCode.value = ''
  newAgency.value = ''
  newAgencyDigit.value = ''
  newAccountNumber.value = ''
  newAccountDigit.value = ''
  newAccountBankType.value = 'CONTA_CORRENTE'
  newHolderName.value = ''
  newHolderCpfCnpj.value = ''
  newIsDefault.value = false
}

const openAddAccountModal = () => {
  resetAccountForm()
  showAddAccountModal.value = true
}

const closeWithdrawModal = () => {
  showWithdrawModal.value = false
}

const closeAddAccountModal = () => {
  showAddAccountModal.value = false
}

const validateWithdraw = () => {
  const amount = parseAmount(withdrawAmount.value)

  if (amount <= 0) {
    return 'Informe um valor de saque válido.'
  }

  if ((wallet.value?.availableBalance ?? 0) < amount) {
    return 'O valor informado excede o saldo disponível.'
  }

  if (!withdrawBankAccountId.value) {
    return 'Selecione uma conta de destino.'
  }

  return ''
}

const buildAccountPayload = (): AdminPlatformCreateBankAccountRequest => ({
  accountType: newAccountType.value,
  bankName: newAccountType.value === 'BANK_ACCOUNT' ? newBankName.value.trim() : undefined,
  bankCode: newAccountType.value === 'BANK_ACCOUNT' ? newBankCode.value.trim() : undefined,
  agency: newAccountType.value === 'BANK_ACCOUNT' ? newAgency.value.trim() : undefined,
  agencyDigit: newAccountType.value === 'BANK_ACCOUNT' ? newAgencyDigit.value.trim() : undefined,
  accountNumber: newAccountType.value === 'BANK_ACCOUNT' ? newAccountNumber.value.trim() : undefined,
  accountDigit: newAccountType.value === 'BANK_ACCOUNT' ? newAccountDigit.value.trim() : undefined,
  accountBankType:
    newAccountType.value === 'BANK_ACCOUNT' ? newAccountBankType.value : undefined,
  pixKey: newAccountType.value === 'PIX_KEY' ? newPixKey.value.trim() : undefined,
  pixKeyType: newAccountType.value === 'PIX_KEY' ? newPixKeyType.value : undefined,
  holderName: newHolderName.value.trim(),
  holderCpfCnpj: newHolderCpfCnpj.value.trim(),
  isDefault: newIsDefault.value,
})

const validateAccountForm = () => {
  if (!newHolderName.value.trim()) {
    return 'Informe o nome do titular.'
  }

  if (!newHolderCpfCnpj.value.trim()) {
    return 'Informe o CPF/CNPJ do titular.'
  }

  if (newAccountType.value === 'PIX_KEY') {
    if (!newPixKey.value.trim()) {
      return 'Informe a chave PIX.'
    }
    return ''
  }

  if (!newBankCode.value.trim() || !newAgency.value.trim() || !newAccountNumber.value.trim()) {
    return 'Preencha o código do banco, a agência e a conta.'
  }

  return ''
}

const handleRefresh = async () => {
  try {
    await store.loadAll()
  } catch {
    toast.error(store.errorMessage || 'Não foi possível atualizar a carteira.')
  }
}

const handleSetDefault = async (id: number) => {
  try {
    await store.setDefaultBankAccount(id)
    toast.success('Conta padrão atualizada.')
  } catch (error: unknown) {
    const apiError = error as { response?: { data?: { message?: string } } }
    toast.error(apiError.response?.data?.message || 'Não foi possível definir a conta padrão.')
  }
}

const handleDeleteAccount = async (id: number) => {
  try {
    await store.deleteBankAccount(id)
    toast.success('Conta removida com sucesso.')
  } catch (error: unknown) {
    const apiError = error as { response?: { data?: { message?: string } } }
    toast.error(apiError.response?.data?.message || 'Não foi possível remover a conta.')
  }
}

const handleAddAccount = async () => {
  const validationMessage = validateAccountForm()
  if (validationMessage) {
    toast.warning(validationMessage)
    return
  }

  try {
    await store.addBankAccount(buildAccountPayload())
    closeAddAccountModal()
    toast.success('Conta cadastrada com sucesso.')
  } catch (error: unknown) {
    const apiError = error as { response?: { data?: { message?: string } } }
    toast.error(apiError.response?.data?.message || 'Não foi possível cadastrar a conta.')
  }
}

const handleWithdraw = async () => {
  const validationMessage = validateWithdraw()
  if (validationMessage) {
    toast.warning(validationMessage)
    return
  }

  try {
    const response = await store.requestWithdrawal({
      amount: parseAmount(withdrawAmount.value),
      bankAccountId: withdrawBankAccountId.value as number,
    })

    closeWithdrawModal()

    if (response.status === 'FAILED') {
      toast.error(response.rejectionReason || 'O saque falhou e o saldo foi devolvido.')
      return
    }

    toast.success('Saque solicitado com sucesso.')
  } catch (error: unknown) {
    const apiError = error as { response?: { data?: { message?: string } } }
    toast.error(apiError.response?.data?.message || 'Não foi possível solicitar o saque.')
  }
}

const handleDownloadReport = async (format: 'PDF' | 'EXCEL') => {
  try {
    await store.downloadReport(format)
    toast.success(`Relatório em ${format} exportado com sucesso.`)
  } catch (error: unknown) {
    const apiError = error as { response?: { data?: { message?: string } } }
    toast.error(apiError.response?.data?.message || 'Não foi possível exportar o relatório.')
  }
}

watch(
  [() => store.transactionType, () => store.timeFilter, () => store.customDateFrom, () => store.customDateTo],
  () => {
    store.loadTransactions().catch(() => {
      toast.error(store.transactionsErrorMessage || 'Não foi possível carregar o histórico.')
    })
  }
)

onMounted(() => {
  store.loadAll().catch(() => {
    toast.error(store.errorMessage || 'Não foi possível carregar a carteira da plataforma.')
  })
})
</script>

<template>
  <AdminLayout>
    <div class="space-y-6">
      <PageHeader
        title="Carteira da Plataforma"
        subtitle="Gerencie o saldo institucional da Servi Já, as contas globais de saque e o histórico das taxas de 5%."
      >
        <template #actions>
          <div class="flex flex-col gap-2 sm:flex-row">
            <DropdownMenu :open="reportMenuOpen" @update:open="reportMenuOpen = $event">
              <button
                type="button"
                class="inline-flex items-center justify-center gap-2 rounded-xl border border-slate-200 bg-white px-4 py-2.5 text-sm font-medium text-slate-700 transition hover:bg-slate-50"
                @click.stop="reportMenuOpen = !reportMenuOpen"
              >
                <Download class="h-4 w-4" />
                Exportar relatório
              </button>
              <DropdownMenuContent align="end" class="min-w-[12rem]">
                <DropdownMenuItem @click="handleDownloadReport('PDF')">
                  Exportar em PDF
                </DropdownMenuItem>
                <DropdownMenuItem @click="handleDownloadReport('EXCEL')">
                  Exportar em Excel
                </DropdownMenuItem>
              </DropdownMenuContent>
            </DropdownMenu>
            <button
              type="button"
              class="inline-flex items-center justify-center gap-2 rounded-xl bg-slate-900 px-4 py-2.5 text-sm font-medium text-white transition hover:bg-slate-800 disabled:cursor-not-allowed disabled:opacity-60"
              :disabled="!hasAccounts || (wallet?.availableBalance ?? 0) <= 0"
              @click="openWithdrawModal"
            >
              <ArrowUpFromLine class="h-4 w-4" />
              Solicitar saque
            </button>
          </div>
        </template>
      </PageHeader>

      <template v-if="store.isLoading && !wallet">
        <div class="grid grid-cols-1 gap-4 md:grid-cols-2 xl:grid-cols-4">
          <Card
            v-for="index in 4"
            :key="`wallet-skeleton-${index}`"
            padding="none"
            radius="2xl"
            class="h-40 animate-pulse"
          />
        </div>
      </template>

      <EmptyState
        v-else-if="store.errorMessage && !wallet"
        title="Não foi possível carregar a carteira"
        :description="store.errorMessage"
        :icon="ShieldAlert"
        tone="danger"
      />

      <template v-else>
        <div class="grid grid-cols-1 gap-4 md:grid-cols-2 xl:grid-cols-4">
          <StatCard
            v-for="card in summaryCards"
            :key="card.key"
            :title="card.title"
            :subtitle="card.subtitle"
            :value="card.value"
            :icon="card.icon"
            :icon-wrapper-class="card.iconWrapper"
            :icon-class="card.iconClass"
            compact
            badge-position="inline"
          />
        </div>

        <div class="grid grid-cols-1 gap-6 xl:grid-cols-[1.15fr_0.85fr]">
          <Card radius="3xl">
            <div class="flex flex-col gap-4 sm:flex-row sm:items-start sm:justify-between">
              <div>
                <h2 class="text-xl font-semibold text-slate-900">Contas de saque da plataforma</h2>
                <p class="mt-1 text-sm text-slate-500">
                  Todas as contas cadastradas aqui ficam disponíveis para qualquer administrador autorizado.
                </p>
              </div>
              <div class="flex flex-col gap-2 sm:items-end">
                <button
                  type="button"
                  class="inline-flex items-center justify-center gap-2 rounded-xl border border-slate-200 bg-white px-4 py-2.5 text-sm font-medium text-slate-700 transition hover:bg-slate-50"
                  @click="openAddAccountModal"
                >
                  <Plus class="h-4 w-4" />
                  Adicionar
                </button>
              </div>
            </div>

            <div v-if="store.isAccountsLoading" class="mt-5 space-y-3">
              <div
                v-for="index in 3"
                :key="`account-skeleton-${index}`"
                class="h-24 animate-pulse rounded-2xl border border-slate-200 bg-slate-50"
              />
            </div>

            <EmptyState
              v-else-if="store.accountsErrorMessage"
              class="mt-5"
              title="Falha ao carregar contas"
              :description="store.accountsErrorMessage"
              compact
              :icon="ShieldAlert"
              tone="danger"
            />

            <EmptyState
              v-else-if="bankAccounts.length === 0"
              class="mt-5"
              title="Nenhuma conta cadastrada"
              description="Cadastre uma conta PIX ou TED para liberar os saques da carteira da plataforma."
              compact
              :icon="CreditCard"
            />

            <div v-else class="mt-5 space-y-3">
              <div
                v-for="account in bankAccounts"
                :key="account.id"
                class="rounded-2xl border border-slate-200 bg-slate-50/70 p-4"
              >
                <div class="flex flex-col gap-3 sm:flex-row sm:items-start sm:justify-between">
                  <div class="min-w-0">
                    <div class="flex flex-wrap items-center gap-2">
                      <p class="text-sm font-semibold text-slate-900">
                        {{ account.accountType === 'PIX_KEY' ? 'Chave PIX' : 'Conta bancária' }}
                      </p>
                      <span
                        v-if="account.isDefault"
                        class="rounded-full bg-[#286AD8] px-2.5 py-1 text-[11px] font-semibold text-white"
                      >
                        Padrão
                      </span>
                    </div>
                    <p class="mt-1 text-sm text-slate-600">{{ describeAccount(account) }}</p>
                    <p class="mt-1 text-xs text-slate-500">
                      Titular: {{ account.holderName }} • Criada em {{ formatDateTime(account.createdAt) }}
                    </p>
                  </div>

                  <div class="flex items-center gap-2">
                    <button
                      v-if="!account.isDefault"
                      type="button"
                      class="inline-flex items-center justify-center rounded-lg border border-slate-200 bg-white px-3 py-2 text-sm text-slate-700 transition hover:bg-slate-50"
                      @click="handleSetDefault(account.id)"
                    >
                      <Star class="h-4 w-4" />
                    </button>
                    <button
                      type="button"
                      class="inline-flex items-center justify-center rounded-lg border border-red-200 bg-white px-3 py-2 text-sm text-red-600 transition hover:bg-red-50"
                      @click="handleDeleteAccount(account.id)"
                    >
                      <Trash2 class="h-4 w-4" />
                    </button>
                  </div>
                </div>
              </div>
            </div>
          </Card>
        </div>

        <Card radius="3xl">
          <div class="flex flex-col gap-4 lg:flex-row lg:items-start lg:justify-between">
            <div>
              <h2 class="text-xl font-semibold text-slate-900">Histórico de transações</h2>
              <p class="mt-1 text-sm text-slate-500">
                {{ transactionsSummaryLabel }}
              </p>
            </div>

            <div class="flex flex-col gap-3 lg:items-end">
              <div class="flex flex-wrap gap-2">
                <button
                  type="button"
                  class="rounded-xl px-4 py-2 text-sm font-medium transition"
                  :class="store.transactionType === 'ALL' ? 'bg-slate-900 text-white' : 'border border-slate-200 bg-white text-slate-700 hover:bg-slate-50'"
                  @click="store.transactionType = 'ALL'"
                >
                  Todos
                </button>
                <button
                  type="button"
                  class="rounded-xl px-4 py-2 text-sm font-medium transition"
                  :class="store.transactionType === 'FEE' ? 'bg-slate-900 text-white' : 'border border-slate-200 bg-white text-slate-700 hover:bg-slate-50'"
                  @click="store.transactionType = 'FEE'"
                >
                  Taxas
                </button>
                <button
                  type="button"
                  class="rounded-xl px-4 py-2 text-sm font-medium transition"
                  :class="store.transactionType === 'WITHDRAWAL' ? 'bg-slate-900 text-white' : 'border border-slate-200 bg-white text-slate-700 hover:bg-slate-50'"
                  @click="store.transactionType = 'WITHDRAWAL'"
                >
                  Saques
                </button>
                <Select :model-value="store.timeFilter" @update:model-value="(value: string) => (store.timeFilter = value as AdminWalletTimeFilter)">
                  <SelectTrigger class="w-full bg-white">
                    <SelectValue :label="store.timeFilter" placeholder="Todo o período" />
                  </SelectTrigger>
                  <SelectContent>
                    <SelectItem v-for="option in TIME_FILTER_OPTIONS" :key="option" :value="option">
                      {{ option }}
                    </SelectItem>
                  </SelectContent>
                </Select>
              </div>
            </div>
          </div>

          <div
            v-if="store.timeFilter === 'Intervalo personalizado'"
            class="mt-4 grid grid-cols-1 gap-4 border-t border-slate-200 pt-4 md:grid-cols-2"
          >
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

          <div v-if="store.isTransactionsLoading" class="mt-5 space-y-3">
            <div
              v-for="index in 4"
              :key="`transaction-skeleton-${index}`"
              class="h-24 animate-pulse rounded-2xl border border-slate-200 bg-slate-50"
            />
          </div>

          <EmptyState
            v-else-if="store.transactionsErrorMessage"
            class="mt-5"
            title="Falha ao carregar transações"
            :description="store.transactionsErrorMessage"
            compact
            tone="danger"
            :icon="ShieldAlert"
          />

          <EmptyState
            v-else-if="transactions.length === 0"
            class="mt-5"
            title="Nenhuma transação encontrada"
            description="Ajuste os filtros para explorar outro período ou aguarde novas movimentações da carteira."
            compact
            :icon="ReceiptText"
          />

          <div v-else class="mt-5 space-y-3">
            <div
              v-for="transaction in transactions"
              :key="`${transaction.type}-${transaction.id}`"
              class="flex flex-col gap-3 rounded-2xl border border-slate-200 bg-white p-4 transition hover:border-slate-300 sm:flex-row sm:items-center"
            >
              <div
                class="flex h-11 w-11 shrink-0 items-center justify-center rounded-2xl"
                :class="transaction.type === 'FEE' ? 'bg-blue-100 text-blue-700' : 'bg-emerald-100 text-emerald-700'"
              >
                <ArrowDownToLine v-if="transaction.type === 'FEE'" class="h-5 w-5" />
                <ArrowUpFromLine v-else class="h-5 w-5" />
              </div>

              <div class="min-w-0 flex-1">
                <div class="flex flex-wrap items-center gap-2">
                  <p class="truncate text-sm font-semibold text-slate-900">
                    {{ transaction.description }}
                  </p>
                  <span
                    class="rounded-full px-2.5 py-1 text-[11px] font-semibold"
                    :class="getStatusClasses(transaction.status)"
                  >
                    {{ formatStatusLabel(transaction.status) }}
                  </span>
                </div>

                <div class="mt-1 flex flex-wrap gap-x-3 gap-y-1 text-xs text-slate-500">
                  <span v-if="transaction.referenceLabel">{{ transaction.referenceLabel }}</span>
                  <span v-if="transaction.serviceName">{{ transaction.serviceName }}</span>
                  <span>{{ normalizePaymentMethod(transaction.paymentMethod) }}</span>
                  <span>{{ formatDateTime(transaction.date) }}</span>
                </div>
              </div>

              <p
                class="text-right text-sm font-semibold"
                :class="transaction.type === 'FEE' ? 'text-blue-700' : 'text-emerald-700'"
              >
                {{ transaction.amount > 0 ? '+' : '' }}{{ formatCurrency(Math.abs(transaction.amount)) }}
              </p>
            </div>
          </div>
        </Card>
      </template>
    </div>

    <Teleport to="body">
      <Transition name="fade">
        <div
          v-if="showWithdrawModal"
          class="fixed inset-0 z-[70] flex items-center justify-center bg-black/50 p-4"
          @click.self="closeWithdrawModal"
        >
          <div class="relative w-full max-w-xl rounded-3xl bg-white p-6 shadow-2xl">
            <button
              type="button"
              class="absolute right-4 top-4 rounded-lg p-2 text-slate-400 transition hover:bg-slate-100 hover:text-slate-600"
              @click="closeWithdrawModal"
            >
              <X class="h-5 w-5" />
            </button>

            <h3 class="text-xl font-semibold text-slate-900">Solicitar saque da plataforma</h3>
            <p class="mt-1 text-sm text-slate-500">
              Disponível agora:
              <span class="font-semibold text-emerald-700">{{ formatCurrency(wallet?.availableBalance) }}</span>
            </p>

            <div class="mt-5 space-y-4">
              <div class="space-y-1">
                <label class="text-sm font-medium text-slate-700">Valor do saque</label>
                <input
                  v-model="withdrawAmount"
                  type="text"
                  inputmode="decimal"
                  placeholder="0,00"
                  class="w-full rounded-lg border border-slate-200 px-3 py-2.5 text-sm outline-none transition focus:border-blue-400 focus:ring-2 focus:ring-blue-100"
                />
                <button
                  type="button"
                  class="text-xs font-medium text-[#286AD8] transition hover:text-blue-700"
                  @click="withdrawAmount = String(wallet?.availableBalance ?? 0).replace('.', ',')"
                >
                  Usar saldo total
                </button>
              </div>

              <div class="space-y-2">
                <label class="text-sm font-medium text-slate-700">Conta de destino</label>

                <EmptyState
                  v-if="withdrawableAccounts.length === 0"
                  title="Nenhuma conta disponível"
                  description="Cadastre uma conta antes de solicitar um saque."
                  compact
                  :surface="false"
                  :icon="CreditCard"
                  class="rounded-2xl border border-dashed border-slate-200 bg-slate-50 px-4 py-8"
                />

                <div v-else class="max-h-64 space-y-2 overflow-y-auto pr-1">
                  <label
                    v-for="account in withdrawableAccounts"
                    :key="account.id"
                    class="flex cursor-pointer items-start gap-3 rounded-2xl border p-3 transition"
                    :class="withdrawBankAccountId === account.id ? 'border-[#286AD8] bg-blue-50' : 'border-slate-200 hover:border-slate-300'"
                  >
                    <input
                      v-model="withdrawBankAccountId"
                      type="radio"
                      name="platform-withdraw-account"
                      :value="account.id"
                      class="mt-1"
                    />
                    <div class="min-w-0">
                      <div class="flex flex-wrap items-center gap-2">
                        <p class="text-sm font-semibold text-slate-900">
                          {{ account.accountType === 'PIX_KEY' ? 'Destino PIX' : 'Destino TED' }}
                        </p>
                        <span
                          v-if="account.isDefault"
                          class="rounded-full bg-[#286AD8] px-2 py-0.5 text-[10px] font-semibold text-white"
                        >
                          Padrão
                        </span>
                      </div>
                      <p class="mt-1 text-xs text-slate-600">{{ describeAccount(account) }}</p>
                      <p class="mt-1 text-xs text-slate-500">Titular: {{ account.holderName }}</p>
                    </div>
                  </label>
                </div>
              </div>

              <button
                type="button"
                class="inline-flex w-full items-center justify-center gap-2 rounded-xl bg-slate-900 px-4 py-3 text-sm font-medium text-white transition hover:bg-slate-800 disabled:cursor-not-allowed disabled:opacity-60"
                :disabled="store.isMutating"
                @click="handleWithdraw"
              >
                <Loader2 v-if="store.isMutating" class="h-4 w-4 animate-spin" />
                Confirmar saque
              </button>
            </div>
          </div>
        </div>
      </Transition>
    </Teleport>

    <Teleport to="body">
      <Transition name="fade">
        <div
          v-if="showAddAccountModal"
          class="fixed inset-0 z-[70] flex items-center justify-center bg-black/50 p-4"
          @click.self="closeAddAccountModal"
        >
          <div class="relative w-full max-w-2xl rounded-3xl bg-white p-6 shadow-2xl">
            <button
              type="button"
              class="absolute right-4 top-4 rounded-lg p-2 text-slate-400 transition hover:bg-slate-100 hover:text-slate-600"
              @click="closeAddAccountModal"
            >
              <X class="h-5 w-5" />
            </button>

            <h3 class="text-xl font-semibold text-slate-900">Cadastrar conta da plataforma</h3>
            <p class="mt-1 text-sm text-slate-500">
              Salve um destino global para os saques institucionais da Servi Já.
            </p>

            <div class="mt-5 space-y-5">
              <div class="flex flex-wrap gap-2">
                <button
                  type="button"
                  class="inline-flex items-center gap-2 rounded-xl px-4 py-2.5 text-sm font-medium transition"
                  :class="newAccountType === 'PIX_KEY' ? 'bg-slate-900 text-white' : 'border border-slate-200 bg-white text-slate-700 hover:bg-slate-50'"
                  @click="newAccountType = 'PIX_KEY'"
                >
                  <Wallet class="h-4 w-4" />
                  Chave PIX
                </button>
                <button
                  type="button"
                  class="inline-flex items-center gap-2 rounded-xl px-4 py-2.5 text-sm font-medium transition"
                  :class="newAccountType === 'BANK_ACCOUNT' ? 'bg-slate-900 text-white' : 'border border-slate-200 bg-white text-slate-700 hover:bg-slate-50'"
                  @click="newAccountType = 'BANK_ACCOUNT'"
                >
                  <Building2 class="h-4 w-4" />
                  Conta bancária
                </button>
              </div>

              <template v-if="newAccountType === 'PIX_KEY'">
                <div class="grid grid-cols-1 gap-4 md:grid-cols-2">
                  <div class="space-y-1">
                    <label class="text-sm font-medium text-slate-700">Tipo da chave PIX</label>
                    <select
                      v-model="newPixKeyType"
                      class="w-full rounded-lg border border-slate-200 px-3 py-2.5 text-sm outline-none transition focus:border-blue-400 focus:ring-2 focus:ring-blue-100"
                    >
                      <option value="CPF">CPF</option>
                      <option value="CNPJ">CNPJ</option>
                      <option value="EMAIL">E-mail</option>
                      <option value="PHONE">Telefone</option>
                      <option value="RANDOM">Chave aleatória</option>
                    </select>
                  </div>
                  <div class="space-y-1">
                    <label class="text-sm font-medium text-slate-700">Chave PIX</label>
                    <input
                      v-model="newPixKey"
                      type="text"
                      class="w-full rounded-lg border border-slate-200 px-3 py-2.5 text-sm outline-none transition focus:border-blue-400 focus:ring-2 focus:ring-blue-100"
                      placeholder="Informe a chave PIX"
                    />
                  </div>
                </div>
              </template>

              <template v-else>
                <div class="grid grid-cols-1 gap-4 md:grid-cols-2">
                  <div class="space-y-1">
                    <label class="text-sm font-medium text-slate-700">Nome do banco</label>
                    <input
                      v-model="newBankName"
                      type="text"
                      class="w-full rounded-lg border border-slate-200 px-3 py-2.5 text-sm outline-none transition focus:border-blue-400 focus:ring-2 focus:ring-blue-100"
                      placeholder="Ex.: Banco do Brasil"
                    />
                  </div>
                  <div class="space-y-1">
                    <label class="text-sm font-medium text-slate-700">Código do banco</label>
                    <input
                      v-model="newBankCode"
                      type="text"
                      class="w-full rounded-lg border border-slate-200 px-3 py-2.5 text-sm outline-none transition focus:border-blue-400 focus:ring-2 focus:ring-blue-100"
                      placeholder="001"
                    />
                  </div>
                  <div class="space-y-1">
                    <label class="text-sm font-medium text-slate-700">Agência</label>
                    <input
                      v-model="newAgency"
                      type="text"
                      class="w-full rounded-lg border border-slate-200 px-3 py-2.5 text-sm outline-none transition focus:border-blue-400 focus:ring-2 focus:ring-blue-100"
                      placeholder="1234"
                    />
                  </div>
                  <div class="space-y-1">
                    <label class="text-sm font-medium text-slate-700">Dígito da agência</label>
                    <input
                      v-model="newAgencyDigit"
                      type="text"
                      class="w-full rounded-lg border border-slate-200 px-3 py-2.5 text-sm outline-none transition focus:border-blue-400 focus:ring-2 focus:ring-blue-100"
                      placeholder="0"
                    />
                  </div>
                  <div class="space-y-1">
                    <label class="text-sm font-medium text-slate-700">Conta</label>
                    <input
                      v-model="newAccountNumber"
                      type="text"
                      class="w-full rounded-lg border border-slate-200 px-3 py-2.5 text-sm outline-none transition focus:border-blue-400 focus:ring-2 focus:ring-blue-100"
                      placeholder="123456"
                    />
                  </div>
                  <div class="space-y-1">
                    <label class="text-sm font-medium text-slate-700">Dígito da conta</label>
                    <input
                      v-model="newAccountDigit"
                      type="text"
                      class="w-full rounded-lg border border-slate-200 px-3 py-2.5 text-sm outline-none transition focus:border-blue-400 focus:ring-2 focus:ring-blue-100"
                      placeholder="7"
                    />
                  </div>
                  <div class="space-y-1 md:col-span-2">
                    <label class="text-sm font-medium text-slate-700">Tipo de conta</label>
                    <select
                      v-model="newAccountBankType"
                      class="w-full rounded-lg border border-slate-200 px-3 py-2.5 text-sm outline-none transition focus:border-blue-400 focus:ring-2 focus:ring-blue-100"
                    >
                      <option value="CONTA_CORRENTE">Conta corrente</option>
                      <option value="CONTA_POUPANCA">Conta poupança</option>
                    </select>
                  </div>
                </div>
              </template>

              <div class="grid grid-cols-1 gap-4 md:grid-cols-2">
                <div class="space-y-1">
                  <label class="text-sm font-medium text-slate-700">Nome do titular</label>
                  <input
                    v-model="newHolderName"
                    type="text"
                    class="w-full rounded-lg border border-slate-200 px-3 py-2.5 text-sm outline-none transition focus:border-blue-400 focus:ring-2 focus:ring-blue-100"
                    placeholder="Nome completo"
                  />
                </div>
                <div class="space-y-1">
                  <label class="text-sm font-medium text-slate-700">CPF/CNPJ do titular</label>
                  <input
                    v-model="newHolderCpfCnpj"
                    type="text"
                    class="w-full rounded-lg border border-slate-200 px-3 py-2.5 text-sm outline-none transition focus:border-blue-400 focus:ring-2 focus:ring-blue-100"
                    placeholder="Somente números ou valor formatado"
                  />
                </div>
              </div>

              <label class="flex items-center gap-3 rounded-2xl border border-slate-200 bg-slate-50 px-4 py-3 text-sm text-slate-700">
                <input v-model="newIsDefault" type="checkbox" class="rounded border-slate-300" />
                Definir esta conta como padrão para os próximos saques
              </label>

              <button
                type="button"
                class="inline-flex w-full items-center justify-center gap-2 rounded-xl bg-slate-900 px-4 py-3 text-sm font-medium text-white transition disabled:cursor-not-allowed disabled:opacity-60"
                :disabled="store.isMutating"
                @click="handleAddAccount"
              >
                <Loader2 v-if="store.isMutating" class="h-4 w-4 animate-spin" />
                Salvar conta
              </button>
            </div>
          </div>
        </div>
      </Transition>
    </Teleport>
  </AdminLayout>
</template>

<style scoped>
.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.2s ease;
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}
</style>
