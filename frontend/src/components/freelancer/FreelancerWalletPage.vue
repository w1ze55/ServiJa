<script setup lang="ts">
import { computed, onMounted, ref } from 'vue';
import {
  Wallet,
  DollarSign,
  Clock,
  TrendingUp,
  ArrowDownToLine,
  ArrowUpFromLine,
  Plus,
  Trash2,
  Download,
  Star,
  Building2,
  QrCode,
  CheckCircle,
  AlertCircle,
  Loader2,
  X,
} from 'lucide-vue-next';
import Card from '../ui/card.vue';
import Badge from '../ui/badge.vue';
import Button from '../ui/button.vue';
import Input from '../ui/input.vue';
import Label from '../ui/label.vue';
import Separator from '../ui/separator.vue';
import { toast } from '@/utils/toast';
import { formatApiDate } from '@/utils/apiDate';
import walletService, {
  type WalletBalance,
  type WalletTransaction,
  type BankAccount,
  type CreateBankAccountRequest,
} from '@/api/services/walletService';

interface Props {
  onNavigate: (page: string, params?: any) => void;
}

defineProps<Props>();

// State
const loading = ref(true);
const wallet = ref<WalletBalance | null>(null);
const transactions = ref<WalletTransaction[]>([]);
const bankAccounts = ref<BankAccount[]>([]);
const transactionFilter = ref<'ALL' | 'EARNING' | 'WITHDRAWAL'>('ALL');

// Modals
const showWithdrawModal = ref(false);
const showAddAccountModal = ref(false);
const isProcessing = ref(false);

// Withdraw form
const withdrawAmount = ref('');
const withdrawalMethod = ref<'PIX' | 'TED'>('PIX');
const withdrawPixKey = ref('');
const withdrawPixKeyType = ref('CPF');
const withdrawBankAccountId = ref<number | null>(null);

// Add account form
const newAccountType = ref<'PIX_KEY' | 'BANK_ACCOUNT'>('PIX_KEY');
const newPixKey = ref('');
const newPixKeyType = ref('CPF');
const newBankName = ref('');
const newBankCode = ref('');
const newAgency = ref('');
const newAccountNumber = ref('');
const newAccountDigit = ref('');
const newAccountBankType = ref('CONTA_CORRENTE');
const newHolderName = ref('');
const newHolderCpfCnpj = ref('');

// Computed
const filteredTransactions = computed(() => {
  if (transactionFilter.value === 'ALL') return transactions.value;
  return transactions.value.filter((t) => t.type === transactionFilter.value);
});

const totalBalance = computed(() => {
  if (!wallet.value) return 0;
  return wallet.value.availableBalance + wallet.value.pendingBalance;
});

const maxWithdrawAmount = computed(() => wallet.value?.availableBalance ?? 0);

const pixAccounts = computed(() =>
  bankAccounts.value.filter((account) => account.accountType === 'PIX_KEY')
);

const tedAccounts = computed(() =>
  bankAccounts.value.filter((account) => account.accountType === 'BANK_ACCOUNT')
);

const preferredTedAccountId = computed(() => {
  const defaultAccount = tedAccounts.value.find((account) => account.isDefault);
  return defaultAccount?.id ?? tedAccounts.value[0]?.id ?? null;
});

const withdrawSubmitDisabled = computed(() => {
  if (isProcessing.value || !withdrawAmount.value) {
    return true;
  }

  if (withdrawalMethod.value === 'PIX') {
    return !withdrawPixKey.value.trim();
  }

  return withdrawBankAccountId.value === null;
});

// Methods
const formatCurrency = (value: number) => {
  return new Intl.NumberFormat('pt-BR', { style: 'currency', currency: 'BRL' }).format(value);
};

const formatDate = (dateStr: string) => {
  return formatApiDate(dateStr, {
    day: '2-digit',
    month: '2-digit',
    year: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
  });
};

const getStatusBadge = (status: string) => {
  const map: Record<string, { label: string; class: string }> = {
    COMPLETED: { label: 'Concluído', class: 'bg-green-100 text-green-700' },
    PENDING: { label: 'Pendente', class: 'bg-yellow-100 text-yellow-700' },
    PROCESSING: { label: 'Processando', class: 'bg-blue-100 text-blue-700' },
    FAILED: { label: 'Falhou', class: 'bg-red-100 text-red-700' },
    CANCELLED: { label: 'Cancelado', class: 'bg-gray-100 text-gray-700' },
  };
  return map[status] || { label: status, class: 'bg-gray-100 text-gray-700' };
};

const describeAccount = (account: BankAccount) => {
  if (account.accountType === 'PIX_KEY') {
    return `${account.pixKeyType || 'PIX'}: ${account.pixKey || ''}`;
  }

  return `${account.bankName || 'Banco'} · Ag ${account.agency || '--'} · CC ${account.accountNumber || '--'}`;
};

const resetWithdrawForm = () => {
  withdrawAmount.value = '';
  withdrawalMethod.value = 'PIX';
  withdrawPixKey.value = '';
  withdrawPixKeyType.value = 'CPF';
  withdrawBankAccountId.value = preferredTedAccountId.value;
};

const closeWithdrawModal = (force = false) => {
  if (isProcessing.value && !force) {
    return;
  }

  showWithdrawModal.value = false;
  resetWithdrawForm();
};

const applyPixAccountToWithdraw = (account?: BankAccount) => {
  if (!account || account.accountType !== 'PIX_KEY') {
    return;
  }

  withdrawPixKey.value = account.pixKeyValue || '';
  withdrawPixKeyType.value = account.pixKeyType || 'CPF';
};

const setWithdrawalMethod = (method: 'PIX' | 'TED') => {
  withdrawalMethod.value = method;

  if (method === 'TED') {
    withdrawBankAccountId.value = withdrawBankAccountId.value ?? preferredTedAccountId.value;
    return;
  }

  if (!withdrawPixKey.value.trim()) {
    applyPixAccountToWithdraw(pixAccounts.value.find((account) => account.isDefault) || pixAccounts.value[0]);
  }
};

const openWithdrawModal = (method: 'PIX' | 'TED' = 'PIX', account?: BankAccount) => {
  resetWithdrawForm();
  showWithdrawModal.value = true;
  setWithdrawalMethod(method);

  if (account?.accountType === 'PIX_KEY' && method === 'PIX') {
    applyPixAccountToWithdraw(account);
  }

  if (account?.accountType === 'BANK_ACCOUNT' && method === 'TED') {
    withdrawBankAccountId.value = account.id;
  }
};

const loadData = async () => {
  loading.value = true;
  try {
    const [walletData, txData, accountsData] = await Promise.all([
      walletService.getWallet(),
      walletService.getTransactions('ALL'),
      walletService.getBankAccounts(),
    ]);

    wallet.value = walletData;
    transactions.value = txData;
    bankAccounts.value = accountsData;

    if (!showWithdrawModal.value) {
      resetWithdrawForm();
    } else if (withdrawalMethod.value === 'TED' && withdrawBankAccountId.value === null) {
      withdrawBankAccountId.value = preferredTedAccountId.value;
    }
  } catch (error: any) {
    console.error('Erro ao carregar dados da carteira:', error);
    toast.error('Erro ao carregar dados da carteira');
  } finally {
    loading.value = false;
  }
};

const handleWithdraw = async () => {
  const amount = parseFloat(withdrawAmount.value);
  if (Number.isNaN(amount) || amount <= 0) {
    toast.error('Informe um valor válido');
    return;
  }
  if (amount > maxWithdrawAmount.value) {
    toast.error('Valor excede o saldo disponível');
    return;
  }
  if (withdrawalMethod.value === 'PIX' && !withdrawPixKey.value.trim()) {
    toast.error('Informe a chave PIX');
    return;
  }

  if (withdrawalMethod.value === 'TED' && withdrawBankAccountId.value === null) {
    toast.error('Selecione uma conta bancária para TED');
    return;
  }

  isProcessing.value = true;
  try {
    await walletService.requestWithdrawal(
      withdrawalMethod.value === 'PIX'
        ? {
            amount,
            withdrawalMethod: 'PIX',
            pixKey: withdrawPixKey.value.trim(),
            pixKeyType: withdrawPixKeyType.value,
          }
        : {
            amount,
            withdrawalMethod: 'TED',
            bankAccountId: withdrawBankAccountId.value!,
          }
    );
    toast.success('Saque solicitado com sucesso!');
    closeWithdrawModal(true);
    await loadData();
  } catch (error: any) {
    toast.error(error.response?.data?.message || 'Erro ao solicitar saque');
  } finally {
    isProcessing.value = false;
  }
};

const handleAddAccount = async () => {
  if (!newHolderName.value.trim() || !newHolderCpfCnpj.value.trim()) {
    toast.error('Preencha nome e CPF/CNPJ do titular');
    return;
  }

  const data: CreateBankAccountRequest = {
    accountType: newAccountType.value,
    holderName: newHolderName.value,
    holderCpfCnpj: newHolderCpfCnpj.value.replace(/\D/g, ''),
    isDefault: bankAccounts.value.length === 0,
  };

  if (newAccountType.value === 'PIX_KEY') {
    if (!newPixKey.value.trim()) {
      toast.error('Informe a chave PIX');
      return;
    }
    data.pixKey = newPixKey.value;
    data.pixKeyType = newPixKeyType.value;
  } else {
    if (!newBankCode.value || !newAgency.value || !newAccountNumber.value) {
      toast.error('Preencha todos os dados bancários');
      return;
    }
    data.bankName = newBankName.value;
    data.bankCode = newBankCode.value;
    data.agency = newAgency.value;
    data.accountNumber = newAccountNumber.value;
    data.accountDigit = newAccountDigit.value;
    data.accountBankType = newAccountBankType.value;
  }

  isProcessing.value = true;
  try {
    await walletService.addBankAccount(data);
    toast.success('Conta cadastrada com sucesso!');
    showAddAccountModal.value = false;
    resetAccountForm();
    await loadData();
  } catch (error: any) {
    toast.error(error.response?.data?.message || 'Erro ao cadastrar conta');
  } finally {
    isProcessing.value = false;
  }
};

const handleDeleteAccount = async (id: number) => {
  try {
    await walletService.deleteBankAccount(id);
    toast.success('Conta removida');
    await loadData();
  } catch (error: any) {
    toast.error('Erro ao remover conta');
  }
};

const handleSetDefault = async (id: number) => {
  try {
    await walletService.setDefaultAccount(id);
    toast.success('Conta definida como padrão');
    await loadData();
  } catch (error: any) {
    toast.error('Erro ao definir padrão');
  }
};

const handleDownloadReport = async () => {
  try {
    await walletService.downloadReport();
    toast.success('Relatório baixado com sucesso');
  } catch (error: any) {
    toast.error('Erro ao gerar relatório');
  }
};

const resetAccountForm = () => {
  newAccountType.value = 'PIX_KEY';
  newPixKey.value = '';
  newPixKeyType.value = 'CPF';
  newBankName.value = '';
  newBankCode.value = '';
  newAgency.value = '';
  newAccountNumber.value = '';
  newAccountDigit.value = '';
  newAccountBankType.value = 'CONTA_CORRENTE';
  newHolderName.value = '';
  newHolderCpfCnpj.value = '';
};

const useAccountForWithdraw = (account: BankAccount) => {
  if (account.accountType === 'PIX_KEY') {
    openWithdrawModal('PIX', account);
    return;
  }

  openWithdrawModal('TED', account);
};

onMounted(loadData);
</script>

<template>
  <div class="max-w-7xl mx-auto space-y-6">
    <!-- Loading State -->
    <div v-if="loading" class="flex items-center justify-center py-20">
      <Loader2 class="h-8 w-8 animate-spin text-[#286AD8]" />
    </div>

    <template v-else>
      <!-- Header -->
      <div class="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
        <div>
          <h1 class="text-[#263238] flex items-center gap-3">
            <div class="w-10 h-10 rounded-full bg-gradient-to-br from-[#286AD8] to-[#1a4fa0] flex items-center justify-center">
              <Wallet class="h-5 w-5 text-white" />
            </div>
            Carteira &amp; Saques
          </h1>
          <p class="text-[#435574] mt-1">Gerencie seus ganhos, contas e saques</p>
        </div>
        <div class="flex gap-3">
          <Button @click="handleDownloadReport" variant="outline" size="sm">
            <Download class="h-4 w-4 mr-2" />
            Exportar Relatório
          </Button>
          <Button @click="openWithdrawModal()" size="sm">
            <ArrowDownToLine class="h-4 w-4 mr-2" />
            Solicitar Saque
          </Button>
        </div>
      </div>

      <!-- Balance Cards -->
      <div class="grid grid-cols-1 md:grid-cols-3 gap-4">
        <!-- Total Balance -->
        <Card class="p-6 bg-gradient-to-br from-[#286AD8] to-[#1a4fa0] text-white border-0">
          <div class="flex items-start justify-between mb-3">
            <div class="w-10 h-10 rounded-full bg-white/20 flex items-center justify-center">
              <DollarSign class="h-5 w-5 text-white" />
            </div>
            <TrendingUp class="h-5 w-5 text-white/60" />
          </div>
          <p class="text-white/80 text-sm mb-1">Saldo Total</p>
          <p class="text-3xl font-bold">{{ formatCurrency(totalBalance) }}</p>
          <p class="text-white/60 text-xs mt-2">
            Total ganho: {{ formatCurrency(wallet?.totalEarned ?? 0) }}
          </p>
        </Card>

        <!-- Available -->
        <Card class="p-6">
          <div class="flex items-start justify-between mb-3">
            <div class="w-10 h-10 rounded-full bg-green-100 flex items-center justify-center">
              <CheckCircle class="h-5 w-5 text-green-600" />
            </div>
          </div>
          <p class="text-[#435574] text-sm mb-1">Disponível para Saque</p>
          <p class="text-2xl text-[#263238] font-bold">
            {{ formatCurrency(wallet?.availableBalance ?? 0) }}
          </p>
          <p class="text-[#435574] text-xs mt-2">
            Total sacado: {{ formatCurrency(wallet?.totalWithdrawn ?? 0) }}
          </p>
        </Card>

        <!-- Pending -->
        <Card class="p-6">
          <div class="flex items-start justify-between mb-3">
            <div class="w-10 h-10 rounded-full bg-yellow-100 flex items-center justify-center">
              <Clock class="h-5 w-5 text-yellow-600" />
            </div>
          </div>
          <p class="text-[#435574] text-sm mb-1">Pendente de Liberação</p>
          <p class="text-2xl text-[#263238] font-bold">
            {{ formatCurrency(wallet?.pendingBalance ?? 0) }}
          </p>
          <p class="text-[#435574] text-xs mt-2">Serviços aguardando aprovação do cliente</p>
        </Card>
      </div>

      <!-- Bank Accounts Section -->
      <Card class="p-6">
        <div class="flex items-center justify-between mb-4">
          <div>
            <h3 class="text-[#263238]">Contas &amp; Chaves PIX</h3>
            <p class="text-sm text-[#435574]">Gerencie suas contas para recebimento</p>
          </div>
          <Button @click="showAddAccountModal = true" variant="outline" size="sm">
            <Plus class="h-4 w-4 mr-2" />
            Adicionar
          </Button>
        </div>

        <div v-if="bankAccounts.length === 0" class="text-center py-8 text-[#435574]">
          <Building2 class="h-10 w-10 mx-auto mb-3 opacity-40" />
          <p class="text-sm">Nenhuma conta cadastrada</p>
          <p class="text-xs mt-1">Cadastre uma conta para solicitar saques</p>
        </div>

        <div v-else class="grid grid-cols-1 md:grid-cols-2 gap-3">
          <div
            v-for="account in bankAccounts"
            :key="account.id"
            :class="`p-4 rounded-lg border-2 transition-colors ${
              account.isDefault
                ? 'border-[#286AD8] bg-blue-50/50'
                : 'border-gray-200 hover:border-gray-300'
            }`"
          >
            <div class="flex items-start justify-between mb-2">
              <div class="flex items-center gap-2">
                <div :class="`w-8 h-8 rounded-full flex items-center justify-center ${
                  account.accountType === 'PIX_KEY' ? 'bg-green-100' : 'bg-blue-100'
                }`">
                  <QrCode v-if="account.accountType === 'PIX_KEY'" class="h-4 w-4 text-green-600" />
                  <Building2 v-else class="h-4 w-4 text-[#286AD8]" />
                </div>
                <div>
                  <p class="text-sm text-[#263238] font-medium">
                    {{ account.accountType === 'PIX_KEY' ? 'Chave PIX' : 'Conta Bancária' }}
                  </p>
                  <p class="text-xs text-[#435574]">{{ describeAccount(account) }}</p>
                </div>
              </div>
              <div class="flex items-center gap-1">
                <Badge v-if="account.isDefault" class="bg-[#286AD8] text-white text-xs hover:bg-[#286AD8]">
                  Padrão
                </Badge>
                <Button
                  v-if="!account.isDefault"
                  @click="handleSetDefault(account.id)"
                  variant="ghost"
                  size="sm"
                  class="text-xs h-7 px-2"
                >
                  <Star class="h-3 w-3" />
                </Button>
                <Button
                  @click="handleDeleteAccount(account.id)"
                  variant="ghost"
                  size="sm"
                  class="text-xs h-7 px-2 text-red-500 hover:text-red-700"
                >
                  <Trash2 class="h-3 w-3" />
                </Button>
              </div>
            </div>
            <div class="flex items-center justify-between mt-2">
              <p class="text-xs text-[#435574]">{{ account.holderName }}</p>
              <Button
                @click="useAccountForWithdraw(account)"
                variant="outline"
                size="sm"
                class="text-xs h-7"
              >
                Usar para saque
              </Button>
            </div>
          </div>
        </div>
      </Card>

      <!-- Transaction History -->
      <Card class="p-6">
        <div class="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-3 mb-4">
          <div>
            <h3 class="text-[#263238]">Histórico de Transações</h3>
            <p class="text-sm text-[#435574]">{{ transactions.length }} transações registradas</p>
          </div>
          <div class="flex gap-2">
            <Button
              @click="transactionFilter = 'ALL'"
              :variant="transactionFilter === 'ALL' ? 'default' : 'outline'"
              size="sm"
            >
              Todos
            </Button>
            <Button
              @click="transactionFilter = 'EARNING'"
              :variant="transactionFilter === 'EARNING' ? 'default' : 'outline'"
              size="sm"
            >
              Recebimentos
            </Button>
            <Button
              @click="transactionFilter = 'WITHDRAWAL'"
              :variant="transactionFilter === 'WITHDRAWAL' ? 'default' : 'outline'"
              size="sm"
            >
              Saques
            </Button>
          </div>
        </div>

        <div v-if="filteredTransactions.length === 0" class="text-center py-12 text-[#435574]">
          <DollarSign class="h-10 w-10 mx-auto mb-3 opacity-40" />
          <p class="text-sm">Nenhuma transação encontrada</p>
        </div>

        <div v-else class="space-y-3">
          <div
            v-for="tx in filteredTransactions"
            :key="`${tx.type}-${tx.id}`"
            class="flex items-center gap-4 p-4 rounded-lg border border-gray-200 hover:border-gray-300 transition-colors"
          >
            <div :class="`w-10 h-10 rounded-full flex items-center justify-center flex-shrink-0 ${
              tx.type === 'EARNING'
                ? 'bg-green-100'
                : 'bg-red-100'
            }`">
              <ArrowDownToLine v-if="tx.type === 'EARNING'" class="h-5 w-5 text-green-600" />
              <ArrowUpFromLine v-else class="h-5 w-5 text-red-600" />
            </div>

            <div class="flex-1 min-w-0">
              <div class="flex items-center gap-2 mb-1">
                <p class="text-sm text-[#263238] font-medium truncate">
                  {{ tx.description }}
                </p>
                <Badge :class="getStatusBadge(tx.status).class + ' text-xs'">
                  {{ getStatusBadge(tx.status).label }}
                </Badge>
              </div>
              <div class="flex items-center gap-3 text-xs text-[#435574]">
                <span v-if="tx.clientName">{{ tx.clientName }}</span>
                <span v-if="tx.paymentMethod">{{ tx.paymentMethod }}</span>
                <span>{{ formatDate(tx.date) }}</span>
              </div>
            </div>

            <p :class="`text-sm font-semibold whitespace-nowrap ${tx.amount >= 0 ? 'text-green-600' : 'text-red-600'}`">
              {{ tx.amount >= 0 ? '+' : '' }}{{ formatCurrency(Math.abs(tx.amount)) }}
            </p>
          </div>
        </div>
      </Card>
    </template>

    <!-- MODAL: SOLICITAR SAQUE -->
    <Teleport to="body">
      <Transition name="fade">
        <div
          v-if="showWithdrawModal"
          class="fixed inset-0 z-50 flex items-center justify-center bg-black/50 p-4"
          @click.self="closeWithdrawModal()"
        >
          <div class="bg-white rounded-xl shadow-2xl w-full max-w-lg p-6 relative">
            <button @click="closeWithdrawModal()"
              class="absolute top-4 right-4 text-gray-400 hover:text-gray-600">
              <X class="h-5 w-5" />
            </button>

            <h3 class="text-[#263238] text-lg mb-1">Solicitar Saque</h3>
            <p class="text-sm text-[#435574] mb-4">
              Disponível: <span class="font-semibold text-green-600">{{ formatCurrency(maxWithdrawAmount) }}</span>
            </p>

            <div class="space-y-4">
              <div class="space-y-2">
                <Label>Valor do saque (R$)</Label>
                <Input
                  v-model="withdrawAmount"
                  type="number"
                  :max="maxWithdrawAmount"
                  min="1"
                  step="0.01"
                  placeholder="0,00"
                />
                <Button
                  @click="withdrawAmount = String(maxWithdrawAmount)"
                  variant="ghost"
                  size="sm"
                  class="text-xs text-[#286AD8]"
                >
                  Sacar tudo
                </Button>
              </div>

              <div class="space-y-2">
                <Label>Método de saque</Label>
                <div class="grid grid-cols-2 gap-2">
                  <Button
                    @click="setWithdrawalMethod('PIX')"
                    :variant="withdrawalMethod === 'PIX' ? 'default' : 'outline'"
                    size="sm"
                    class="w-full"
                  >
                    PIX
                  </Button>
                  <Button
                    @click="setWithdrawalMethod('TED')"
                    :variant="withdrawalMethod === 'TED' ? 'default' : 'outline'"
                    size="sm"
                    class="w-full"
                  >
                    TED
                  </Button>
                </div>
              </div>

              <template v-if="withdrawalMethod === 'PIX'">
                <div class="space-y-2">
                  <Label>Tipo de chave PIX</Label>
                  <select
                    v-model="withdrawPixKeyType"
                    class="w-full h-10 px-3 rounded-md border border-gray-300 text-sm"
                  >
                    <option value="CPF">CPF</option>
                    <option value="CNPJ">CNPJ</option>
                    <option value="EMAIL">E-mail</option>
                    <option value="PHONE">Telefone</option>
                    <option value="RANDOM">Chave aleatória</option>
                  </select>
                </div>

                <div class="space-y-2">
                  <Label>Chave PIX</Label>
                  <Input
                    v-model="withdrawPixKey"
                    placeholder="Informe sua chave PIX"
                  />
                </div>
              </template>

              <template v-else>
                <div v-if="tedAccounts.length === 0" class="rounded-lg border border-amber-200 bg-amber-50 p-4 text-amber-900">
                  <div class="flex items-start gap-3">
                    <AlertCircle class="h-5 w-5 mt-0.5 flex-shrink-0" />
                    <div class="space-y-2">
                      <p class="text-sm font-medium">Nenhuma conta bancária disponível para TED.</p>
                      <p class="text-xs">
                        Cadastre uma conta bancária primeiro para solicitar saque via TED.
                      </p>
                      <Button
                        @click="closeWithdrawModal(); showAddAccountModal = true"
                        variant="outline"
                        size="sm"
                        class="border-amber-300 bg-white"
                      >
                        Cadastrar conta bancária
                      </Button>
                    </div>
                  </div>
                </div>

                <div v-else class="space-y-2">
                  <Label>Conta bancária para TED</Label>
                  <div class="space-y-2 max-h-56 overflow-y-auto pr-1">
                    <label
                      v-for="account in tedAccounts"
                      :key="account.id"
                      :class="`flex items-start gap-3 rounded-lg border p-3 cursor-pointer transition-colors ${
                        withdrawBankAccountId === account.id
                          ? 'border-[#286AD8] bg-blue-50'
                          : 'border-gray-200 hover:border-gray-300'
                      }`"
                    >
                      <input
                        v-model="withdrawBankAccountId"
                        type="radio"
                        name="withdraw-bank-account"
                        :value="account.id"
                        class="mt-1"
                      />
                      <div class="min-w-0">
                        <div class="flex items-center gap-2">
                          <p class="text-sm text-[#263238] font-medium">
                            {{ account.bankName || 'Conta bancária' }}
                          </p>
                          <Badge v-if="account.isDefault" class="bg-[#286AD8] text-white text-[10px] hover:bg-[#286AD8]">
                            Padrão
                          </Badge>
                        </div>
                        <p class="text-xs text-[#435574] mt-1">
                          {{ describeAccount(account) }}
                        </p>
                        <p class="text-xs text-[#435574] mt-1">
                          Titular: {{ account.holderName }}
                        </p>
                      </div>
                    </label>
                  </div>
                </div>
              </template>

              <Separator />

              <Button
                @click="handleWithdraw"
                :disabled="withdrawSubmitDisabled"
                class="w-full"
              >
                <Loader2 v-if="isProcessing" class="h-4 w-4 mr-2 animate-spin" />
                {{ isProcessing ? 'Processando...' : `Confirmar Saque via ${withdrawalMethod}` }}
              </Button>
            </div>
          </div>
        </div>
      </Transition>
    </Teleport>

    <!-- MODAL: ADICIONAR CONTA -->
    <Teleport to="body">
      <Transition name="fade">
        <div
          v-if="showAddAccountModal"
          class="fixed inset-0 z-50 flex items-center justify-center bg-black/50 p-4"
          @click.self="showAddAccountModal = false"
        >
          <div class="bg-white rounded-xl shadow-2xl w-full max-w-md p-6 relative max-h-[80vh] overflow-y-auto">
            <button @click="showAddAccountModal = false"
              class="absolute top-4 right-4 text-gray-400 hover:text-gray-600">
              <X class="h-5 w-5" />
            </button>

            <h3 class="text-[#263238] text-lg mb-4">Cadastrar Conta</h3>

            <div class="space-y-4">
              <!-- Type selector -->
              <div class="flex gap-2">
                <Button
                  @click="newAccountType = 'PIX_KEY'"
                  :variant="newAccountType === 'PIX_KEY' ? 'default' : 'outline'"
                  size="sm"
                  class="flex-1"
                >
                  <QrCode class="h-4 w-4 mr-2" />
                  Chave PIX
                </Button>
                <Button
                  @click="newAccountType = 'BANK_ACCOUNT'"
                  :variant="newAccountType === 'BANK_ACCOUNT' ? 'default' : 'outline'"
                  size="sm"
                  class="flex-1"
                >
                  <Building2 class="h-4 w-4 mr-2" />
                  Conta Bancária
                </Button>
              </div>

              <!-- PIX fields -->
              <template v-if="newAccountType === 'PIX_KEY'">
                <div class="space-y-2">
                  <Label>Tipo de chave</Label>
                  <select
                    v-model="newPixKeyType"
                    class="w-full h-10 px-3 rounded-md border border-gray-300 text-sm"
                  >
                    <option value="CPF">CPF</option>
                    <option value="CNPJ">CNPJ</option>
                    <option value="EMAIL">E-mail</option>
                    <option value="PHONE">Telefone</option>
                    <option value="RANDOM">Chave aleatória</option>
                  </select>
                </div>
                <div class="space-y-2">
                  <Label>Chave PIX</Label>
                  <Input v-model="newPixKey" placeholder="Informe sua chave PIX" />
                </div>
              </template>

              <!-- Bank Account fields -->
              <template v-else>
                <div class="space-y-2">
                  <Label>Nome do banco</Label>
                  <Input v-model="newBankName" placeholder="Ex.: Banco do Brasil" />
                </div>
                <div class="grid grid-cols-2 gap-3">
                  <div class="space-y-2">
                    <Label>Código do banco</Label>
                    <Input v-model="newBankCode" placeholder="001" maxlength="4" />
                  </div>
                  <div class="space-y-2">
                    <Label>Agência</Label>
                    <Input v-model="newAgency" placeholder="1234" maxlength="6" />
                  </div>
                </div>
                <div class="grid grid-cols-3 gap-3">
                  <div class="col-span-2 space-y-2">
                    <Label>Conta</Label>
                    <Input v-model="newAccountNumber" placeholder="12345" />
                  </div>
                  <div class="space-y-2">
                    <Label>Dígito</Label>
                    <Input v-model="newAccountDigit" placeholder="0" maxlength="2" />
                  </div>
                </div>
                <div class="space-y-2">
                  <Label>Tipo de conta</Label>
                  <select
                    v-model="newAccountBankType"
                    class="w-full h-10 px-3 rounded-md border border-gray-300 text-sm"
                  >
                    <option value="CONTA_CORRENTE">Conta Corrente</option>
                    <option value="CONTA_POUPANCA">Conta Poupança</option>
                  </select>
                </div>
              </template>

              <Separator />

              <!-- Holder info -->
              <div class="space-y-2">
                <Label>Nome do titular</Label>
                <Input v-model="newHolderName" placeholder="Nome completo" />
              </div>
              <div class="space-y-2">
                <Label>CPF/CNPJ do titular</Label>
                <Input v-model="newHolderCpfCnpj" placeholder="000.000.000-00" />
              </div>

              <Button
                @click="handleAddAccount"
                :disabled="isProcessing"
                class="w-full"
              >
                <Loader2 v-if="isProcessing" class="h-4 w-4 mr-2 animate-spin" />
                {{ isProcessing ? 'Salvando...' : 'Salvar Conta' }}
              </Button>
            </div>
          </div>
        </div>
      </Transition>
    </Teleport>
  </div>
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
