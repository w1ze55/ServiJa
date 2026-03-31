import axios from '../config/axios';

export interface WalletBalance {
  id: number;
  availableBalance: number;
  pendingBalance: number;
  totalEarned: number;
  totalWithdrawn: number;
  createdAt: string;
  updatedAt: string;
}

export interface WalletTransaction {
  id: number;
  type: 'EARNING' | 'WITHDRAWAL';
  amount: number;
  status: string;
  description: string;
  paymentMethod: string;
  serviceName?: string;
  clientName?: string;
  date: string;
}

export interface BankAccount {
  id: number;
  accountType: 'PIX_KEY' | 'BANK_ACCOUNT';
  bankName?: string;
  bankCode?: string;
  agency?: string;
  accountNumber?: string;
  accountBankType?: string;
  pixKey?: string;
  pixKeyValue?: string;
  pixKeyType?: string;
  holderName: string;
  holderCpfCnpj: string;
  isDefault: boolean;
  createdAt: string;
}

export interface CreateBankAccountRequest {
  accountType: 'PIX_KEY' | 'BANK_ACCOUNT';
  bankName?: string;
  bankCode?: string;
  agency?: string;
  agencyDigit?: string;
  accountNumber?: string;
  accountDigit?: string;
  accountBankType?: string;
  pixKey?: string;
  pixKeyType?: string;
  holderName: string;
  holderCpfCnpj: string;
  isDefault?: boolean;
}

export interface WithdrawalRequest {
  amount: number;
  withdrawalMethod: 'PIX' | 'TED';
  bankAccountId?: number;
  pixKey?: string;
  pixKeyType?: string;
}

export interface WithdrawalResponse {
  id: number;
  amount: number;
  status: string;
  withdrawalMethod: 'PIX' | 'TED';
  bankAccountId?: number;
  pixKey?: string;
  pixKeyType?: string;
  transactionId?: string;
  rejectionReason?: string;
  processedAt?: string;
  createdAt: string;
}

const walletService = {
  // Carteira
  async getWallet(): Promise<WalletBalance> {
    const response = await axios.get('/wallet');
    return response.data;
  },

  // Transações
  async getTransactions(type: string = 'ALL'): Promise<WalletTransaction[]> {
    const response = await axios.get('/wallet/transactions', {
      params: { type },
    });
    return response.data;
  },

  // Contas bancárias
  async getBankAccounts(): Promise<BankAccount[]> {
    const response = await axios.get('/wallet/bank-accounts');
    return response.data;
  },

  async addBankAccount(data: CreateBankAccountRequest): Promise<BankAccount> {
    const response = await axios.post('/wallet/bank-accounts', data);
    return response.data;
  },

  async deleteBankAccount(id: number): Promise<void> {
    await axios.delete(`/wallet/bank-accounts/${id}`);
  },

  async setDefaultAccount(id: number): Promise<BankAccount> {
    const response = await axios.put(`/wallet/bank-accounts/${id}/default`);
    return response.data;
  },

  // Saques
  async requestWithdrawal(data: WithdrawalRequest): Promise<WithdrawalResponse> {
    const response = await axios.post('/wallet/withdraw', data);
    return response.data;
  },

  async getWithdrawals(): Promise<WithdrawalResponse[]> {
    const response = await axios.get('/wallet/withdrawals');
    return response.data;
  },

  async cancelWithdrawal(id: number): Promise<void> {
    await axios.delete(`/wallet/withdrawals/${id}`);
  },

  // Relatório
  async downloadReport(): Promise<void> {
    const response = await axios.get('/wallet/report', { responseType: 'blob' });
    const url = window.URL.createObjectURL(new Blob([response.data]));
    const link = document.createElement('a');
    link.href = url;
    link.setAttribute('download', 'relatorio-carteira.csv');
    document.body.appendChild(link);
    link.click();
    link.remove();
    window.URL.revokeObjectURL(url);
  },
};

export default walletService;
