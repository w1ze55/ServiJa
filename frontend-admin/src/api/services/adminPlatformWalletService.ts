import api from '@/api/config/axios'

export interface AdminPlatformWalletResponse {
  id: number
  availableBalance: number
  reservedBalance: number
  totalEarned: number
  totalWithdrawn: number
  createdAt: string
  updatedAt: string
}

export type AdminPlatformTransactionType = 'ALL' | 'FEE' | 'WITHDRAWAL'

export interface AdminPlatformWalletTransaction {
  id: number
  type: 'FEE' | 'WITHDRAWAL'
  amount: number
  status: string
  description: string
  paymentMethod?: string
  serviceName?: string
  referenceLabel?: string
  date: string
}

export interface AdminPlatformBankAccount {
  id: number
  accountType: 'PIX_KEY' | 'BANK_ACCOUNT'
  bankName?: string
  bankCode?: string
  agency?: string
  accountNumber?: string
  accountBankType?: string
  pixKey?: string
  pixKeyValue?: string
  pixKeyType?: string
  holderName: string
  holderCpfCnpj: string
  isDefault: boolean
  createdAt: string
}

export interface AdminPlatformCreateBankAccountRequest {
  accountType: 'PIX_KEY' | 'BANK_ACCOUNT'
  bankName?: string
  bankCode?: string
  agency?: string
  agencyDigit?: string
  accountNumber?: string
  accountDigit?: string
  accountBankType?: string
  pixKey?: string
  pixKeyType?: string
  holderName: string
  holderCpfCnpj: string
  isDefault?: boolean
}

export interface AdminPlatformWithdrawalRequest {
  amount: number
  bankAccountId: number
}

export interface AdminPlatformWithdrawalResponse {
  id: number
  amount: number
  status: string
  withdrawalMethod: 'PIX' | 'TED'
  bankAccountId: number
  destinationLabel: string
  transactionId: string
  rejectionReason?: string
  processedAt?: string
  createdAt: string
}

export type AdminPlatformReportFormat = 'PDF' | 'EXCEL'

export interface AdminPlatformTransactionsParams {
  type?: AdminPlatformTransactionType
  dateFrom?: string
  dateTo?: string
}

const cleanParams = (params: AdminPlatformTransactionsParams = {}) => {
  const next: Record<string, string> = {}
  if (params.type) next.type = params.type
  if (params.dateFrom) next.dateFrom = params.dateFrom
  if (params.dateTo) next.dateTo = params.dateTo
  return next
}

export const adminPlatformWalletService = {
  async getWallet(): Promise<AdminPlatformWalletResponse> {
    const response = await api.get<AdminPlatformWalletResponse>('/admin/platform-wallet')
    return response.data
  },

  async getTransactions(
    params: AdminPlatformTransactionsParams = {}
  ): Promise<AdminPlatformWalletTransaction[]> {
    const response = await api.get<AdminPlatformWalletTransaction[]>(
      '/admin/platform-wallet/transactions',
      {
        params: cleanParams(params),
      }
    )

    return response.data
  },

  async getBankAccounts(): Promise<AdminPlatformBankAccount[]> {
    const response = await api.get<AdminPlatformBankAccount[]>(
      '/admin/platform-wallet/bank-accounts'
    )
    return response.data
  },

  async addBankAccount(
    data: AdminPlatformCreateBankAccountRequest
  ): Promise<AdminPlatformBankAccount> {
    const response = await api.post<AdminPlatformBankAccount>(
      '/admin/platform-wallet/bank-accounts',
      data
    )
    return response.data
  },

  async deleteBankAccount(id: number): Promise<void> {
    await api.delete(`/admin/platform-wallet/bank-accounts/${id}`)
  },

  async setDefaultAccount(id: number): Promise<AdminPlatformBankAccount> {
    const response = await api.put<AdminPlatformBankAccount>(
      `/admin/platform-wallet/bank-accounts/${id}/default`
    )
    return response.data
  },

  async requestWithdrawal(
    data: AdminPlatformWithdrawalRequest
  ): Promise<AdminPlatformWithdrawalResponse> {
    const response = await api.post<AdminPlatformWithdrawalResponse>(
      '/admin/platform-wallet/withdrawals',
      data
    )
    return response.data
  },

  async getWithdrawals(): Promise<AdminPlatformWithdrawalResponse[]> {
    const response = await api.get<AdminPlatformWithdrawalResponse[]>(
      '/admin/platform-wallet/withdrawals'
    )
    return response.data
  },

  async downloadReport(
    format: AdminPlatformReportFormat,
    params: AdminPlatformTransactionsParams = {}
  ): Promise<void> {
    const response = await api.get('/admin/platform-wallet/report', {
      params: {
        format,
        ...cleanParams(params),
      },
      responseType: 'blob',
    })

    const url = window.URL.createObjectURL(new Blob([response.data]))
    const link = document.createElement('a')
    link.href = url
    const contentDisposition = response.headers['content-disposition']
    const filenameMatch = contentDisposition?.match(/filename[^;=\n]*=((['"]).*?\2|[^;\n]*)/i)
    const filename =
      filenameMatch?.[1]?.replace(/['"]/g, '') ||
      `relatorio-carteira-plataforma.${format === 'PDF' ? 'pdf' : 'xlsx'}`

    link.setAttribute('download', filename)
    document.body.appendChild(link)
    link.click()
    link.remove()
    window.URL.revokeObjectURL(url)
  },
}
