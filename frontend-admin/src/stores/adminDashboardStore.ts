import { computed, ref } from 'vue'
import { defineStore } from 'pinia'
import {
  adminDashboardService,
  type AdminDashboardParams,
  type AdminDashboardResponse,
} from '@/api/services/adminDashboardService'

export type DashboardTimeFilter =
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

export const useAdminDashboardStore = defineStore('adminDashboard', () => {
  const dashboard = ref<AdminDashboardResponse | null>(null)
  const isLoading = ref(false)
  const errorMessage = ref('')
  const timeFilter = ref<DashboardTimeFilter>('Todo o período')
  const customDateFrom = ref('')
  const customDateTo = ref('')
  let currentRequestId = 0

  const resolvedParams = computed<AdminDashboardParams>(() => {
    const now = new Date()
    const startOfToday = new Date(now.getFullYear(), now.getMonth(), now.getDate())

    switch (timeFilter.value) {
      case 'Hoje':
        return {
          dateFrom: formatDateInput(startOfToday),
          dateTo: formatDateInput(startOfToday),
        }
      case 'Ontem': {
        const yesterday = new Date(startOfToday)
        yesterday.setDate(yesterday.getDate() - 1)
        return {
          dateFrom: formatDateInput(yesterday),
          dateTo: formatDateInput(yesterday),
        }
      }
      case 'Últimos 7 dias': {
        const sevenDaysAgo = new Date(startOfToday)
        sevenDaysAgo.setDate(sevenDaysAgo.getDate() - 7)
        return {
          dateFrom: formatDateInput(sevenDaysAgo),
          dateTo: formatDateInput(startOfToday),
        }
      }
      case 'Últimos 30 dias': {
        const thirtyDaysAgo = new Date(startOfToday)
        thirtyDaysAgo.setDate(thirtyDaysAgo.getDate() - 30)
        return {
          dateFrom: formatDateInput(thirtyDaysAgo),
          dateTo: formatDateInput(startOfToday),
        }
      }
      case 'Mês atual': {
        const startOfMonth = new Date(now.getFullYear(), now.getMonth(), 1)
        return {
          dateFrom: formatDateInput(startOfMonth),
          dateTo: formatDateInput(startOfToday),
        }
      }
      case 'Último mês': {
        const startOfLastMonth = new Date(now.getFullYear(), now.getMonth() - 1, 1)
        const endOfLastMonth = new Date(now.getFullYear(), now.getMonth(), 0)
        return {
          dateFrom: formatDateInput(startOfLastMonth),
          dateTo: formatDateInput(endOfLastMonth),
        }
      }
      case 'Intervalo personalizado':
        return {
          dateFrom: customDateFrom.value || undefined,
          dateTo: customDateTo.value || undefined,
        }
      case 'Todo o período':
      default:
        return {}
    }
  })

  const loadDashboard = async () => {
    const requestId = ++currentRequestId
    isLoading.value = true
    errorMessage.value = ''

    try {
      const response = await adminDashboardService.getDashboard(resolvedParams.value)

      if (requestId === currentRequestId) {
        dashboard.value = response
      }
    } catch (error: unknown) {
      if (requestId !== currentRequestId) return

      const apiError = error as { response?: { data?: { message?: string } } }
      dashboard.value = null
      errorMessage.value =
        apiError.response?.data?.message ||
        'Não foi possível carregar o dashboard agora. Tente novamente.'
    } finally {
      if (requestId === currentRequestId) {
        isLoading.value = false
      }
    }
  }

  return {
    dashboard,
    isLoading,
    errorMessage,
    timeFilter,
    customDateFrom,
    customDateTo,
    resolvedParams,
    loadDashboard,
  }
})
