import api from '@/api/config/axios'

export interface AdminDashboardMoneyMetric {
  current: number
  previous: number
  changePercent: number
}

export interface AdminDashboardCountMetric {
  current: number
  previous: number
  changePercent: number
}

export interface AdminDashboardRateMetric {
  current: number
  previous: number
  changePercent: number
  currentCompletedCount: number
  currentRequestCount: number
  previousCompletedCount: number
  previousRequestCount: number
}

export interface AdminDashboardSummary {
  totalRevenue: AdminDashboardMoneyMetric
  serviJaFee: AdminDashboardMoneyMetric
  newUsers: AdminDashboardCountMetric
  completedServices: AdminDashboardCountMetric
  averageTicket: AdminDashboardMoneyMetric
  activeClients: AdminDashboardCountMetric
  activeProfessionals: AdminDashboardCountMetric
  conversionRate: AdminDashboardRateMetric
}

export interface AdminDashboardPeriod {
  startDate: string
  endDate: string
  previousStartDate: string
  previousEndDate: string
  granularity: 'DAILY' | 'MONTHLY'
  allTime: boolean
}

export interface AdminDashboardPaymentStatus {
  key: 'COMPLETED' | 'PENDING' | 'CANCELLED' | 'REFUNDED' | 'OTHERS'
  label: string
  count: number
}

export interface AdminDashboardRevenuePoint {
  key: string
  label: string
  amount: number
}

export interface AdminDashboardTopContractedService {
  label: string
  contractsCount: number
}

export interface AdminDashboardResponse {
  period: AdminDashboardPeriod
  summary: AdminDashboardSummary
  paymentStatusBreakdown: AdminDashboardPaymentStatus[]
  revenueSeries: AdminDashboardRevenuePoint[]
  topContractedServices: AdminDashboardTopContractedService[]
}

export interface AdminDashboardParams {
  dateFrom?: string
  dateTo?: string
}

export const adminDashboardService = {
  async getDashboard(params: AdminDashboardParams = {}): Promise<AdminDashboardResponse> {
    const cleanParams: Record<string, string> = {}
    if (params.dateFrom) cleanParams.dateFrom = params.dateFrom
    if (params.dateTo) cleanParams.dateTo = params.dateTo

    const response = await api.get<AdminDashboardResponse>('/admin/dashboard', {
      params: cleanParams,
    })

    return response.data
  },
}
