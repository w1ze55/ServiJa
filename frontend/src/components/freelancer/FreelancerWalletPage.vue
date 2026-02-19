<script setup lang="ts">
import { ref, computed } from 'vue';
import {
  Wallet,
  DollarSign,
  ArrowUpRight,
  ArrowDownLeft,
  Clock,
  CheckCircle,
  XCircle,
  CreditCard,
  Building,
  Plus,
  Eye,
  EyeOff,
  Download,
  Filter,
} from 'lucide-vue-next';
import Button from '../ui/button.vue';
import Input from '../ui/input.vue';
import Label from '../ui/label.vue';
import Card from '../ui/card.vue';
import Badge from '../ui/badge.vue';
import Dialog from '../ui/dialog.vue';
import DialogContent from '../ui/dialog-content.vue';
import DialogDescription from '../ui/dialog-description.vue';
import DialogFooter from '../ui/dialog-footer.vue';
import DialogHeader from '../ui/dialog-header.vue';
import DialogTitle from '../ui/dialog-title.vue';
import Select from '../ui/select.vue';
import SelectContent from '../ui/select-content.vue';
import SelectItem from '../ui/select-item.vue';
import SelectTrigger from '../ui/select-trigger.vue';
import SelectValue from '../ui/select-value.vue';
import { toast } from '../../utils/toast';

interface BankAccount {
  id: string;
  bankName: string;
  accountType: 'checking' | 'savings';
  accountNumber: string;
  agency: string;
  holderName: string;
  holderDocument: string;
  isDefault: boolean;
}

interface Transaction {
  id: string;
  type: 'withdrawal' | 'earning' | 'refund';
  amount: number;
  status: 'pending' | 'processing' | 'completed' | 'failed';
  description: string;
  date: string;
  bankAccount?: string;
  serviceName?: string;
}

interface Props {
  onNavigate: (page: string, params?: any) => void;
}

const props = defineProps<Props>();

const showWithdrawalDialog = ref(false);
const showBankAccountDialog = ref(false);
const showBalance = ref(true);
const withdrawalAmount = ref('');
const selectedBankAccount = ref('');
const filterStatus = ref('all');
const currentPage = ref(1);
const itemsPerPage = 10;

// Bank account form
const bankForm = ref({
  bankName: '',
  accountType: 'checking' as 'checking' | 'savings',
  accountNumber: '',
  agency: '',
  holderName: '',
  holderDocument: '',
});

// Mock data
const balance = ref({
  available: 3250.5,
  pending: 1500.0,
  total: 4750.5,
});

const bankAccounts = ref<BankAccount[]>([
  {
    id: '1',
    bankName: 'Banco do Brasil',
    accountType: 'checking',
    accountNumber: '****5678',
    agency: '1234',
    holderName: 'João Silva',
    holderDocument: '***.***.***-**',
    isDefault: true,
  },
]);

const transactions = ref<Transaction[]>([
  {
    id: '1',
    type: 'earning',
    amount: 2500,
    status: 'completed',
    description: 'Pagamento recebido',
    date: '2026-11-10 14:30',
    serviceName: 'Instalação Elétrica Residencial',
  },
  {
    id: '2',
    type: 'withdrawal',
    amount: 1500,
    status: 'completed',
    description: 'Saque realizado',
    date: '2026-11-08 10:15',
    bankAccount: 'Banco do Brasil - Ag. 1234',
  },
  {
    id: '3',
    type: 'earning',
    amount: 800,
    status: 'completed',
    description: 'Pagamento recebido',
    date: '2026-11-05 16:45',
    serviceName: 'Manutenção Preventiva',
  },
  {
    id: '4',
    type: 'withdrawal',
    amount: 1000,
    status: 'processing',
    description: 'Saque em processamento',
    date: '2026-11-11 09:00',
    bankAccount: 'Banco do Brasil - Ag. 1234',
  },
  {
    id: '5',
    type: 'earning',
    amount: 1500,
    status: 'pending',
    description: 'Aguardando liberação',
    date: '2026-11-09 11:20',
    serviceName: 'Automação Residencial',
  },
  {
    id: '6',
    type: 'withdrawal',
    amount: 500,
    status: 'failed',
    description: 'Saque falhou - dados bancários incorretos',
    date: '2026-11-03 15:10',
    bankAccount: 'Banco do Brasil - Ag. 1234',
  },
]);

// Filter transactions
const filteredTransactions = computed(() => {
  if (filterStatus.value === 'all') {
    return transactions.value;
  }
  return transactions.value.filter((t) => t.status === filterStatus.value);
});

// Pagination
const totalPages = computed(() => Math.ceil(filteredTransactions.value.length / itemsPerPage));
const startIndex = computed(() => (currentPage.value - 1) * itemsPerPage);
const currentTransactions = computed(() =>
  filteredTransactions.value.slice(startIndex.value, startIndex.value + itemsPerPage)
);

const formatCurrency = (value: number) => {
  return new Intl.NumberFormat('pt-BR', {
    style: 'currency',
    currency: 'BRL',
  }).format(value);
};

const maskDocument = (doc: string) => {
  return doc.replace(/(\d{3})(\d{3})(\d{3})(\d{2})/, '***.$2.***-**');
};

const maskAccount = (account: string) => {
  return account.replace(/\d(?=\d{4})/g, '*');
};

const handleWithdrawal = () => {
  const amount = parseFloat(withdrawalAmount.value);

  if (!amount || amount <= 0) {
    toast.error('Digite um valor válido');
    return;
  }

  if (amount > balance.value.available) {
    toast.error('Saldo insuficiente para saque');
    return;
  }

  if (!selectedBankAccount.value) {
    toast.error('Selecione uma conta bancária');
    return;
  }

  toast.success('Solicitação de saque enviada! O valor será transferido em até 2 dias úteis.');
  showWithdrawalDialog.value = false;
  withdrawalAmount.value = '';
};

const handleAddBankAccount = () => {
  if (
    !bankForm.value.bankName ||
    !bankForm.value.accountNumber ||
    !bankForm.value.agency ||
    !bankForm.value.holderName ||
    !bankForm.value.holderDocument
  ) {
    toast.error('Preencha todos os campos obrigatórios');
    return;
  }

  // Validate CPF/CNPJ (simplified)
  if (
    bankForm.value.holderDocument.replace(/\D/g, '').length !== 11 &&
    bankForm.value.holderDocument.replace(/\D/g, '').length !== 14
  ) {
    toast.error('CPF/CNPJ inválido');
    return;
  }

  const newAccount: BankAccount = {
    id: Date.now().toString(),
    ...bankForm.value,
    accountNumber: maskAccount(bankForm.value.accountNumber),
    holderDocument: maskDocument(bankForm.value.holderDocument),
    isDefault: bankAccounts.value.length === 0,
  };

  bankAccounts.value = [...bankAccounts.value, newAccount];
  toast.success('Conta bancária cadastrada com sucesso!');
  showBankAccountDialog.value = false;
  bankForm.value = {
    bankName: '',
    accountType: 'checking',
    accountNumber: '',
    agency: '',
    holderName: '',
    holderDocument: '',
  };
};

const getTransactionIcon = (type: Transaction['type']) => {
  switch (type) {
    case 'earning':
      return { component: ArrowDownLeft, class: 'h-5 w-5 text-green-600' };
    case 'withdrawal':
      return { component: ArrowUpRight, class: 'h-5 w-5 text-blue-600' };
    case 'refund':
      return { component: ArrowDownLeft, class: 'h-5 w-5 text-yellow-600' };
  }
};

const handleRequestWithdrawal = () => {
  if (bankAccounts.value.length === 0) {
    toast.error('Cadastre uma conta bancária primeiro');
    showBankAccountDialog.value = true;
  } else {
    showWithdrawalDialog.value = true;
  }
};
</script>

<template>
  <div class="max-w-6xl mx-auto space-y-6">
    <!-- Header -->
    <div class="flex items-start justify-between">
      <div>
        <h1 class="text-[#263238] mb-2">Carteira & Saques</h1>
        <p class="text-[#435574]">Gerencie seus ganhos e solicite saques</p>
      </div>
      <Button @click="handleRequestWithdrawal" class="bg-[#286AD8] hover:bg-[#435574]">
        <ArrowUpRight class="h-4 w-4 mr-2" />
        Solicitar Saque
      </Button>
    </div>

    <!-- Balance Cards -->
    <div class="grid grid-cols-1 md:grid-cols-3 gap-4">
      <Card class="p-6 bg-gradient-to-br from-[#286AD8] to-[#435574] text-white">
        <div class="flex items-start justify-between mb-4">
          <div>
            <p class="text-white/80 text-sm mb-1">Saldo Total</p>
            <div class="flex items-center gap-2">
              <h2 v-if="showBalance" class="text-white">{{ formatCurrency(balance.total) }}</h2>
              <h2 v-else class="text-white">••••••</h2>
              <button
                @click="showBalance = !showBalance"
                class="text-white/80 hover:text-white"
              >
                <EyeOff v-if="showBalance" class="h-5 w-5" />
                <Eye v-else class="h-5 w-5" />
              </button>
            </div>
          </div>
          <div class="w-12 h-12 rounded-full bg-white/20 flex items-center justify-center">
            <Wallet class="h-6 w-6 text-white" />
          </div>
        </div>
      </Card>

      <Card class="p-6">
        <div class="flex items-start justify-between">
          <div>
            <p class="text-[#435574] text-sm mb-1">Disponível para Saque</p>
            <h3 class="text-[#263238]">
              {{ showBalance ? formatCurrency(balance.available) : '••••••' }}
            </h3>
          </div>
          <div class="w-10 h-10 rounded-full bg-green-100 flex items-center justify-center">
            <DollarSign class="h-5 w-5 text-green-600" />
          </div>
        </div>
      </Card>

      <Card class="p-6">
        <div class="flex items-start justify-between">
          <div>
            <p class="text-[#435574] text-sm mb-1">Pendente de Liberação</p>
            <h3 class="text-[#263238]">
              {{ showBalance ? formatCurrency(balance.pending) : '••••••' }}
            </h3>
          </div>
          <div class="w-10 h-10 rounded-full bg-yellow-100 flex items-center justify-center">
            <Clock class="h-5 w-5 text-yellow-600" />
          </div>
        </div>
      </Card>
    </div>

    <!-- Bank Accounts -->
    <Card class="p-6">
      <div class="flex items-center justify-between mb-4">
        <h3 class="text-[#263238]">Contas Bancárias</h3>
        <Button
          @click="showBankAccountDialog = true"
          variant="outline"
          size="sm"
          class="border-[#286AD8] text-[#286AD8] hover:bg-blue-50"
        >
          <Plus class="h-4 w-4 mr-2" />
          Adicionar Conta
        </Button>
      </div>

      <div v-if="bankAccounts.length === 0" class="text-center py-8">
        <Building class="h-12 w-12 text-gray-400 mx-auto mb-4" />
        <p class="text-[#435574] mb-4">Nenhuma conta bancária cadastrada</p>
        <Button
          @click="showBankAccountDialog = true"
          class="bg-[#286AD8] hover:bg-[#435574]"
        >
          Cadastrar Conta Bancária
        </Button>
      </div>
      <div v-else class="space-y-3">
        <div
          v-for="account in bankAccounts"
          :key="account.id"
          class="flex items-center justify-between p-4 border rounded-lg"
        >
          <div class="flex items-center gap-4">
            <div class="w-12 h-12 rounded-full bg-blue-100 flex items-center justify-center">
              <Building class="h-6 w-6 text-[#286AD8]" />
            </div>
            <div>
              <div class="flex items-center gap-2">
                <h4 class="text-[#263238]">{{ account.bankName }}</h4>
                <Badge v-if="account.isDefault" variant="secondary" class="text-xs">
                  Padrão
                </Badge>
              </div>
              <p class="text-sm text-[#435574]">
                {{ account.accountType === 'checking' ? 'Conta Corrente' : 'Conta Poupança' }} •
                Ag. {{ account.agency }} • C/C {{ account.accountNumber }}
              </p>
              <p class="text-xs text-[#435574]">
                {{ account.holderName }} • CPF {{ account.holderDocument }}
              </p>
            </div>
          </div>
        </div>
      </div>
    </Card>

    <!-- Transactions History -->
    <Card class="p-6">
      <div class="flex items-center justify-between mb-4">
        <h3 class="text-[#263238]">Histórico de Transações</h3>
        <div class="flex gap-2">
          <Select :model-value="filterStatus" @update:model-value="(val) => filterStatus = val as string">
            <SelectTrigger class="w-[180px]">
              <SelectValue placeholder="Filtrar por status" />
            </SelectTrigger>
            <SelectContent>
              <SelectItem value="all">Todos</SelectItem>
              <SelectItem value="completed">Concluídos</SelectItem>
              <SelectItem value="processing">Processando</SelectItem>
              <SelectItem value="pending">Pendentes</SelectItem>
              <SelectItem value="failed">Falhos</SelectItem>
            </SelectContent>
          </Select>
          <Button variant="outline" size="icon">
            <Download class="h-4 w-4" />
          </Button>
        </div>
      </div>

      <div class="space-y-3">
        <div
          v-for="transaction in currentTransactions"
          :key="transaction.id"
          class="flex items-center justify-between p-4 border rounded-lg hover:bg-gray-50 transition-colors"
        >
          <div class="flex items-center gap-4">
            <div
              :class="`w-10 h-10 rounded-full flex items-center justify-center ${
                transaction.type === 'earning'
                  ? 'bg-green-100'
                  : transaction.type === 'withdrawal'
                  ? 'bg-blue-100'
                  : 'bg-yellow-100'
              }`"
            >
              <component 
                :is="getTransactionIcon(transaction.type).component" 
                :class="getTransactionIcon(transaction.type).class"
              />
            </div>
            <div>
              <div class="flex items-center gap-2 mb-1">
                <p class="text-[#263238]">{{ transaction.description }}</p>
                <template v-if="transaction.status === 'pending'">
                  <Badge class="bg-yellow-100 text-yellow-700 hover:bg-yellow-100">
                    <Clock class="h-3 w-3 mr-1" />
                    Pendente
                  </Badge>
                </template>
                <template v-else-if="transaction.status === 'processing'">
                  <Badge class="bg-blue-100 text-blue-700 hover:bg-blue-100">
                    <Clock class="h-3 w-3 mr-1" />
                    Processando
                  </Badge>
                </template>
                <template v-else-if="transaction.status === 'completed'">
                  <Badge class="bg-green-100 text-green-700 hover:bg-green-100">
                    <CheckCircle class="h-3 w-3 mr-1" />
                    Concluído
                  </Badge>
                </template>
                <template v-else-if="transaction.status === 'failed'">
                  <Badge class="bg-red-100 text-red-700 hover:bg-red-100">
                    <XCircle class="h-3 w-3 mr-1" />
                    Falhou
                  </Badge>
                </template>
              </div>
              <p class="text-sm text-[#435574]">
                {{ transaction.date }}
                <span v-if="transaction.serviceName"> • {{ transaction.serviceName }}</span>
                <span v-if="transaction.bankAccount"> • {{ transaction.bankAccount }}</span>
              </p>
            </div>
          </div>
          <div class="text-right">
            <p
              :class="`text-lg ${
                transaction.type === 'earning' ? 'text-green-600' : 'text-[#263238]'
              }`"
            >
              {{ transaction.type === 'earning' ? '+' : '-' }}
              {{ formatCurrency(transaction.amount) }}
            </p>
          </div>
        </div>
      </div>

      <!-- Pagination -->
      <div v-if="totalPages > 1" class="flex items-center justify-center gap-2 mt-6">
        <Button
          @click="currentPage = Math.max(1, currentPage - 1)"
          :disabled="currentPage === 1"
          variant="outline"
          size="sm"
        >
          Anterior
        </Button>
        <div class="flex items-center gap-1">
          <Button
            v-for="pageNum in Array.from({ length: Math.min(5, totalPages) }, (_, i) => i + 1)"
            :key="pageNum"
            @click="currentPage = pageNum"
            :variant="currentPage === pageNum ? 'default' : 'outline'"
            size="sm"
            :class="currentPage === pageNum ? 'bg-[#286AD8] hover:bg-[#435574]' : ''"
          >
            {{ pageNum }}
          </Button>
        </div>
        <Button
          @click="currentPage = Math.min(totalPages, currentPage + 1)"
          :disabled="currentPage === totalPages"
          variant="outline"
          size="sm"
        >
          Próxima
        </Button>
      </div>
    </Card>

    <!-- Withdrawal Dialog -->
    <Dialog :open="showWithdrawalDialog" @update:open="(val) => showWithdrawalDialog = val">
      <DialogContent class="sm:max-w-md">
        <DialogHeader>
          <DialogTitle>Solicitar Saque</DialogTitle>
          <DialogDescription>
            Transfira seus ganhos para sua conta bancária
          </DialogDescription>
        </DialogHeader>

        <div class="space-y-4">
          <div class="p-4 bg-blue-50 border border-blue-200 rounded-lg">
            <p class="text-sm text-[#263238] mb-1">Saldo Disponível</p>
            <p class="text-2xl text-[#286AD8]">{{ formatCurrency(balance.available) }}</p>
          </div>

          <div class="space-y-2">
            <Label for="amount">Valor do Saque *</Label>
            <Input
              id="amount"
              type="number"
              :model-value="withdrawalAmount"
              @update:model-value="(val) => withdrawalAmount = val as string"
              placeholder="0,00"
              min="0"
              :max="balance.available"
              step="0.01"
            />
            <div class="flex gap-2">
              <Button
                @click="withdrawalAmount = (balance.available * 0.25).toFixed(2)"
                variant="outline"
                size="sm"
              >
                25%
              </Button>
              <Button
                @click="withdrawalAmount = (balance.available * 0.5).toFixed(2)"
                variant="outline"
                size="sm"
              >
                50%
              </Button>
              <Button
                @click="withdrawalAmount = (balance.available * 0.75).toFixed(2)"
                variant="outline"
                size="sm"
              >
                75%
              </Button>
              <Button
                @click="withdrawalAmount = balance.available.toFixed(2)"
                variant="outline"
                size="sm"
              >
                100%
              </Button>
            </div>
          </div>

          <div class="space-y-2">
            <Label for="bank-account">Conta Bancária *</Label>
            <Select
              :model-value="selectedBankAccount"
              @update:model-value="(val) => selectedBankAccount = val as string"
            >
              <SelectTrigger>
                <SelectValue placeholder="Selecione a conta" />
              </SelectTrigger>
              <SelectContent>
                <SelectItem
                  v-for="account in bankAccounts"
                  :key="account.id"
                  :value="account.id"
                >
                  {{ account.bankName }} - Ag. {{ account.agency }} - C/C
                  {{ account.accountNumber }}
                </SelectItem>
              </SelectContent>
            </Select>
          </div>

          <div class="p-3 bg-yellow-50 border border-yellow-200 rounded-lg">
            <p class="text-sm text-yellow-800">
              ⏱ O valor será transferido em até 2 dias úteis
            </p>
          </div>
        </div>

        <DialogFooter>
          <Button @click="showWithdrawalDialog = false" variant="outline">
            Cancelar
          </Button>
          <Button @click="handleWithdrawal" class="bg-[#286AD8] hover:bg-[#435574]">
            Confirmar Saque
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>

    <!-- Add Bank Account Dialog -->
    <Dialog :open="showBankAccountDialog" @update:open="(val) => showBankAccountDialog = val">
      <DialogContent class="sm:max-w-lg">
        <DialogHeader>
          <DialogTitle>Cadastrar Conta Bancária</DialogTitle>
          <DialogDescription>
            Adicione seus dados bancários para receber pagamentos
          </DialogDescription>
        </DialogHeader>

        <div class="space-y-4">
          <div class="space-y-2">
            <Label for="bank-name">Banco *</Label>
            <Input
              id="bank-name"
              :model-value="bankForm.bankName"
              @update:model-value="(val) => bankForm.bankName = val as string"
              placeholder="Ex: Banco do Brasil"
            />
          </div>

          <div class="space-y-2">
            <Label for="account-type">Tipo de Conta *</Label>
            <Select
              :model-value="bankForm.accountType"
              @update:model-value="(val) => bankForm.accountType = val as 'checking' | 'savings'"
            >
              <SelectTrigger>
                <SelectValue />
              </SelectTrigger>
              <SelectContent>
                <SelectItem value="checking">Conta Corrente</SelectItem>
                <SelectItem value="savings">Conta Poupança</SelectItem>
              </SelectContent>
            </Select>
          </div>

          <div class="grid grid-cols-2 gap-4">
            <div class="space-y-2">
              <Label for="agency">Agência *</Label>
              <Input
                id="agency"
                :model-value="bankForm.agency"
                @update:model-value="(val) => bankForm.agency = val as string"
                placeholder="1234"
              />
            </div>
            <div class="space-y-2">
              <Label for="account-number">Conta *</Label>
              <Input
                id="account-number"
                :model-value="bankForm.accountNumber"
                @update:model-value="(val) => bankForm.accountNumber = val as string"
                placeholder="12345-6"
              />
            </div>
          </div>

          <div class="space-y-2">
            <Label for="holder-name">Nome do Titular *</Label>
            <Input
              id="holder-name"
              :model-value="bankForm.holderName"
              @update:model-value="(val) => bankForm.holderName = val as string"
              placeholder="Nome completo"
            />
          </div>

          <div class="space-y-2">
            <Label for="holder-document">CPF/CNPJ *</Label>
            <Input
              id="holder-document"
              :model-value="bankForm.holderDocument"
              @update:model-value="(val) => bankForm.holderDocument = val as string"
              placeholder="000.000.000-00"
            />
          </div>

          <div class="p-3 bg-blue-50 border border-blue-200 rounded-lg">
            <p class="text-sm text-blue-800">
              🔒 Seus dados bancários são criptografados e protegidos
            </p>
          </div>
        </div>

        <DialogFooter>
          <Button @click="showBankAccountDialog = false" variant="outline">
            Cancelar
          </Button>
          <Button @click="handleAddBankAccount" class="bg-[#286AD8] hover:bg-[#435574]">
            Cadastrar Conta
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  </div>
</template>

