import { computed, ref } from 'vue'
import { defineStore } from 'pinia'
import {
  adminPlatformWalletService,
  type AdminPlatformBankAccount,
  type AdminPlatformCreateBankAccountRequest,
  type AdminPlatformReportFormat,
  type AdminPlatformTransactionType,
  type AdminPlatformTransactionsParams,
  type AdminPlatformWalletResponse,
  type AdminPlatformWalletTransaction,
  type AdminPlatformWithdrawalRequest,
  type AdminPlatformWithdrawalResponse,
} from '@/api/services/adminPlatformWalletService'

export type AdminWalletTimeFilter =
  | 'Todo o período'
  | 'Hoje'
  | 'Ontem'
  | 'Últimos 7 dias'
  | 'Últimos 30 dias'
  | 'Mês atual'
  | 'Último mês'
  | 'Intervalo personalizado'

const formatDateInput = (date: Date) => {
  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  return `${year}-${month}-${day}`
}

const extractErrorMessage = (error: unknown, fallback: string) => {
  const apiError = error as { response?: { data?: { message?: string } } }
  return apiError.response?.data?.message || fallback
}

export const useAdminPlatformWalletStore = defineStore('adminPlatformWallet', () => {
  const wallet = ref<AdminPlatformWalletResponse | null>(null)
  const transactions = ref<AdminPlatformWalletTransaction[]>([])
  const bankAccounts = ref<AdminPlatformBankAccount[]>([])
  const withdrawals = ref<AdminPlatformWithdrawalResponse[]>([])

  const isLoading = ref(false)
  const isTransactionsLoading = ref(false)
  const isAccountsLoading = ref(false)
  const isMutating = ref(false)
  const errorMessage = ref('')
  const transactionsErrorMessage = ref('')
  const accountsErrorMessage = ref('')

  const transactionType = ref<AdminPlatformTransactionType>('ALL')
  const timeFilter = ref<AdminWalletTimeFilter>('Todo o período')
  const customDateFrom = ref('')
  const customDateTo = ref('')

  let currentTransactionsRequestId = 0

  const resolvedTransactionParams = computed<AdminPlatformTransactionsParams>(() => {
    const now = new Date()
    const startOfToday = new Date(now.getFullYear(), now.getMonth(), now.getDate())

    switch (timeFilter.value) {
      case 'Hoje':
        return {
          type: transactionType.value,
          dateFrom: formatDateInput(startOfToday),
          dateTo: formatDateInput(startOfToday),
        }
      case 'Ontem': {
        const yesterday = new Date(startOfToday)
        yesterday.setDate(yesterday.getDate() - 1)
        return {
          type: transactionType.value,
          dateFrom: formatDateInput(yesterday),
          dateTo: formatDateInput(yesterday),
        }
      }
      case 'Últimos 7 dias': {
        const sevenDaysAgo = new Date(startOfToday)
        sevenDaysAgo.setDate(sevenDaysAgo.getDate() - 7)
        return {
          type: transactionType.value,
          dateFrom: formatDateInput(sevenDaysAgo),
          dateTo: formatDateInput(startOfToday),
        }
      }
      case 'Últimos 30 dias': {
        const thirtyDaysAgo = new Date(startOfToday)
        thirtyDaysAgo.setDate(thirtyDaysAgo.getDate() - 30)
        return {
          type: transactionType.value,
          dateFrom: formatDateInput(thirtyDaysAgo),
          dateTo: formatDateInput(startOfToday),
        }
      }
      case 'Mês atual': {
        const startOfMonth = new Date(now.getFullYear(), now.getMonth(), 1)
        return {
          type: transactionType.value,
          dateFrom: formatDateInput(startOfMonth),
          dateTo: formatDateInput(startOfToday),
        }
      }
      case 'Último mês': {
        const startOfLastMonth = new Date(now.getFullYear(), now.getMonth() - 1, 1)
        const endOfLastMonth = new Date(now.getFullYear(), now.getMonth(), 0)
        return {
          type: transactionType.value,
          dateFrom: formatDateInput(startOfLastMonth),
          dateTo: formatDateInput(endOfLastMonth),
        }
      }
      case 'Intervalo personalizado':
        return {
          type: transactionType.value,
          dateFrom: customDateFrom.value || undefined,
          dateTo: customDateTo.value || undefined,
        }
      case 'Todo o período':
      default:
        return {
          type: transactionType.value,
        }
    }
  })

  const loadWallet = async () => {
    try {
      wallet.value = await adminPlatformWalletService.getWallet()
      errorMessage.value = ''
    } catch (error: unknown) {
      errorMessage.value = extractErrorMessage(
        error,
        'Não foi possível carregar a carteira da plataforma.'
      )
      throw error
    }
  }

  const loadTransactions = async () => {
    const requestId = ++currentTransactionsRequestId
    isTransactionsLoading.value = true
    transactionsErrorMessage.value = ''

    try {
      const response = await adminPlatformWalletService.getTransactions(
        resolvedTransactionParams.value
      )

      if (requestId === currentTransactionsRequestId) {
        transactions.value = response
      }
    } catch (error: unknown) {
      if (requestId !== currentTransactionsRequestId) {
        return
      }

      transactionsErrorMessage.value = extractErrorMessage(
        error,
        'Não foi possível carregar o histórico da carteira.'
      )
      transactions.value = []
      throw error
    } finally {
      if (requestId === currentTransactionsRequestId) {
        isTransactionsLoading.value = false
      }
    }
  }

  const loadBankAccounts = async () => {
    isAccountsLoading.value = true
    accountsErrorMessage.value = ''

    try {
      bankAccounts.value = await adminPlatformWalletService.getBankAccounts()
    } catch (error: unknown) {
      accountsErrorMessage.value = extractErrorMessage(
        error,
        'Não foi possível carregar as contas da plataforma.'
      )
      bankAccounts.value = []
      throw error
    } finally {
      isAccountsLoading.value = false
    }
  }

  const loadWithdrawals = async () => {
    withdrawals.value = await adminPlatformWalletService.getWithdrawals()
  }

  const loadAll = async () => {
    isLoading.value = true
    errorMessage.value = ''

    try {
      await Promise.all([
        loadWallet(),
        loadTransactions(),
        loadBankAccounts(),
        loadWithdrawals(),
      ])
    } finally {
      isLoading.value = false
    }
  }

  const refreshAfterMutation = async () => {
    await Promise.all([loadWallet(), loadTransactions(), loadBankAccounts(), loadWithdrawals()])
  }

  const addBankAccount = async (data: AdminPlatformCreateBankAccountRequest) => {
    isMutating.value = true
    try {
      const account = await adminPlatformWalletService.addBankAccount(data)
      await refreshAfterMutation()
      return account
    } finally {
      isMutating.value = false
    }
  }

  const deleteBankAccount = async (id: number) => {
    isMutating.value = true
    try {
      await adminPlatformWalletService.deleteBankAccount(id)
      await refreshAfterMutation()
    } finally {
      isMutating.value = false
    }
  }

  const setDefaultBankAccount = async (id: number) => {
    isMutating.value = true
    try {
      const account = await adminPlatformWalletService.setDefaultAccount(id)
      await refreshAfterMutation()
      return account
    } finally {
      isMutating.value = false
    }
  }

  const requestWithdrawal = async (data: AdminPlatformWithdrawalRequest) => {
    isMutating.value = true
    try {
      const withdrawal = await adminPlatformWalletService.requestWithdrawal(data)
      await refreshAfterMutation()
      return withdrawal
    } finally {
      isMutating.value = false
    }
  }

  const downloadReport = async (format: AdminPlatformReportFormat) => {
    await adminPlatformWalletService.downloadReport(format, resolvedTransactionParams.value)
  }

  return {
    wallet,
    transactions,
    bankAccounts,
    withdrawals,
    isLoading,
    isTransactionsLoading,
    isAccountsLoading,
    isMutating,
    errorMessage,
    transactionsErrorMessage,
    accountsErrorMessage,
    transactionType,
    timeFilter,
    customDateFrom,
    customDateTo,
    resolvedTransactionParams,
    loadWallet,
    loadTransactions,
    loadBankAccounts,
    loadWithdrawals,
    loadAll,
    addBankAccount,
    deleteBankAccount,
    setDefaultBankAccount,
    requestWithdrawal,
    downloadReport,
  }
})
