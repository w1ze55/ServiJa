<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue';
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
import DropdownMenu from '../ui/dropdown-menu.vue';
import DropdownMenuContent from '../ui/dropdown-menu-content.vue';
import DropdownMenuItem from '../ui/dropdown-menu-item.vue';
import Input from '../ui/input.vue';
import Label from '../ui/label.vue';
import Separator from '../ui/separator.vue';
import Select from '../ui/select.vue';
import SelectContent from '../ui/select-content.vue';
import SelectItem from '../ui/select-item.vue';
import SelectTrigger from '../ui/select-trigger.vue';
import SelectValue from '../ui/select-value.vue';
import { toast } from '@/utils/toast';
import { formatApiDate } from '@/utils/apiDate';
import { formatCPF, formatPhone, validateCPF } from '@/utils/masks';
import walletService, {
  type WalletBalance,
  type WalletTransaction,
  type BankAccount,
  type CreateBankAccountRequest,
  type WalletReportFormat,
} from '@/api/services/walletService';

interface Props {
  onNavigate: (page: string, params?: any) => void;
}

type PixKeyType = 'CPF' | 'CNPJ' | 'EMAIL' | 'PHONE' | 'RANDOM';

const normalizePixKeyType = (value?: string): PixKeyType => {
  const normalizedValue = value?.trim().toUpperCase();

  if (normalizedValue === 'CNPJ') return 'CNPJ';
  if (normalizedValue === 'EMAIL' || normalizedValue === 'E-MAIL') return 'EMAIL';
  if (normalizedValue === 'PHONE' || normalizedValue === 'TELEFONE' || normalizedValue === 'CELLPHONE') return 'PHONE';
  if (normalizedValue === 'RANDOM' || normalizedValue === 'ALEATORIA' || normalizedValue === 'CHAVE_ALEATORIA') return 'RANDOM';
  return 'CPF';
};

defineProps<Props>();

// State
const loading = ref(true);
const wallet = ref<WalletBalance | null>(null);
const bankAccounts = ref<BankAccount[]>([]);
const transactionFilter = ref<'ALL' | 'EARNING' | 'WITHDRAWAL'>('ALL');
const timeFilter = ref('Todo o período');
const customDateFrom = ref('');
const customDateTo = ref('');

// Transações (paginadas server-side)
const pagedTransactions = ref<WalletTransaction[]>([]);
const currentPage = ref(1); // 1-based na UI
const pageSize = ref(20);
const totalPages = ref(0);
const totalElements = ref(0);
const isLoadingTransactions = ref(false);

// Modals
const showWithdrawModal = ref(false);
const showAddAccountModal = ref(false);
const isProcessing = ref(false);
const reportMenuOpen = ref(false);

// Withdraw form
const withdrawAmount = ref('');
const withdrawalMethod = ref<'PIX' | 'TED'>('PIX');
const withdrawPixKey = ref('');
const withdrawPixKeyType = ref<PixKeyType>('CPF');
const withdrawBankAccountId = ref<number | null>(null);

// Add account form
const newAccountType = ref<'PIX_KEY' | 'BANK_ACCOUNT'>('PIX_KEY');
const newPixKey = ref('');
const newPixKeyType = ref<PixKeyType>('CPF');
const newBankName = ref('');
const newBankCode = ref('');
const newAgency = ref('');
const newAccountNumber = ref('');
const newAccountDigit = ref('');
const newAccountBankType = ref('CONTA_CORRENTE');
const newHolderName = ref('');
const newHolderCpfCnpj = ref('');

// Computed
// Formata uma Date local para YYYY-MM-DD (respeitando o fuso local)
const formatDateInput = (date: Date) => date.toLocaleDateString('en-CA');

// Converte o filtro de tempo selecionado em um intervalo { dateFrom, dateTo }
// (YYYY-MM-DD) que é enviado ao backend. "Todo o período" não envia limites.
const selectedDateRange = computed<{ dateFrom?: string; dateTo?: string }>(() => {
  const now = new Date();
  const startOfToday = new Date(now.getFullYear(), now.getMonth(), now.getDate());

  switch (timeFilter.value) {
    case 'Hoje':
      return { dateFrom: formatDateInput(startOfToday), dateTo: formatDateInput(startOfToday) };
    case 'Ontem': {
      const yesterday = new Date(startOfToday);
      yesterday.setDate(yesterday.getDate() - 1);
      return { dateFrom: formatDateInput(yesterday), dateTo: formatDateInput(yesterday) };
    }
    case 'Últimos 7 dias': {
      const from = new Date(startOfToday);
      from.setDate(from.getDate() - 7);
      return { dateFrom: formatDateInput(from), dateTo: formatDateInput(startOfToday) };
    }
    case 'Últimos 30 dias': {
      const from = new Date(startOfToday);
      from.setDate(from.getDate() - 30);
      return { dateFrom: formatDateInput(from), dateTo: formatDateInput(startOfToday) };
    }
    case 'Mês atual': {
      const from = new Date(now.getFullYear(), now.getMonth(), 1);
      return { dateFrom: formatDateInput(from), dateTo: formatDateInput(startOfToday) };
    }
    case 'Último mês': {
      const from = new Date(now.getFullYear(), now.getMonth() - 1, 1);
      const to = new Date(now.getFullYear(), now.getMonth(), 0);
      return { dateFrom: formatDateInput(from), dateTo: formatDateInput(to) };
    }
    case 'Intervalo personalizado':
      return {
        dateFrom: customDateFrom.value || undefined,
        dateTo: customDateTo.value || undefined,
      };
    case 'Todo o período':
    default:
      return {};
  }
});

const fetchTransactions = async () => {
  isLoadingTransactions.value = true;
  try {
    const { dateFrom, dateTo } = selectedDateRange.value;
    const result = await walletService.getTransactionsPaged({
      type: transactionFilter.value,
      dateFrom,
      dateTo,
      page: currentPage.value - 1,
      size: pageSize.value,
    });
    pagedTransactions.value = result.content;
    totalElements.value = result.totalElements;
    totalPages.value = result.totalPages;
  } catch (error: any) {
    console.error('Erro ao carregar transações:', error);
    toast.error('Erro ao carregar transações');
  } finally {
    isLoadingTransactions.value = false;
  }
};

const goToPage = (page: number) => {
  const target = Math.min(Math.max(page, 1), Math.max(totalPages.value, 1));
  if (target === currentPage.value) return;
  currentPage.value = target;
  fetchTransactions();
};

// Páginas exibidas no controle de paginação (janela de até 5)
const visiblePageNumbers = computed(() => {
  const pages = totalPages.value;
  const current = currentPage.value;
  return Array.from({ length: Math.min(5, pages) }, (_, i) => {
    if (pages <= 5) return i + 1;
    if (current <= 3) return i + 1;
    if (current >= pages - 2) return pages - 4 + i;
    return current - 2 + i;
  });
});

// Ao trocar qualquer filtro, volta para a primeira página e recarrega.
watch([transactionFilter, timeFilter, customDateFrom, customDateTo], () => {
  // Evita disparos com intervalo personalizado pela metade (apenas uma das datas preenchida)
  if (timeFilter.value === 'Intervalo personalizado') {
    const onlyOneFilled = Boolean(customDateFrom.value) !== Boolean(customDateTo.value);
    if (onlyOneFilled) return;
  }
  currentPage.value = 1;
  fetchTransactions();
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
  if (isProcessing.value || getWithdrawAmountValue() <= 0) {
    return true;
  }

  if (withdrawalMethod.value === 'PIX') {
    return !normalizePixKeyValue(withdrawPixKey.value, withdrawPixKeyType.value);
  }

  return withdrawBankAccountId.value === null;
});

// Methods
const formatCurrency = (value: number) => {
  return new Intl.NumberFormat('pt-BR', { style: 'currency', currency: 'BRL' }).format(value);
};

const MAX_WITHDRAW_INPUT_DIGITS = 10;
const PIX_KEY_EMAIL_MAX_LENGTH = 256;

const getCurrencyDigits = (value: string) => value.replace(/\D/g, '');

const getLimitedCurrencyDigits = (value: string) => getCurrencyDigits(value).slice(0, MAX_WITHDRAW_INPUT_DIGITS);

const parseCurrencyInput = (value: string) => {
  const digits = getCurrencyDigits(value);
  if (!digits) return 0;
  return Number(digits) / 100;
};

const formatCurrencyInput = (value: string | number) => {
  const numericValue = typeof value === 'number' ? value : parseCurrencyInput(value);
  return numericValue > 0 ? formatCurrency(numericValue) : '';
};

const MAX_WITHDRAW_FORMATTED_LENGTH = formatCurrencyInput('9'.repeat(MAX_WITHDRAW_INPUT_DIGITS)).length;

const getWithdrawAmountValue = () => parseCurrencyInput(withdrawAmount.value);

const handleWithdrawAmountInput = (value: string) => {
  const limitedDigits = getLimitedCurrencyDigits(value);
  withdrawAmount.value = formatCurrencyInput(limitedDigits);
};

const handleWithdrawAmountBeforeInput = (event: InputEvent) => {
  const target = event.target as HTMLInputElement | null;
  const insertedDigits = getCurrencyDigits(event.data ?? '');

  if (!target || !insertedDigits) {
    return;
  }

  const selectionStart = target.selectionStart ?? target.value.length;
  const selectionEnd = target.selectionEnd ?? selectionStart;
  const selectedDigits = getCurrencyDigits(target.value.slice(selectionStart, selectionEnd)).length;
  const nextDigitsCount = getCurrencyDigits(target.value).length - selectedDigits + insertedDigits.length;

  if (nextDigitsCount > MAX_WITHDRAW_INPUT_DIGITS) {
    event.preventDefault();
  }
};

const formatCNPJ = (value: string) => {
  const numbers = value.replace(/\D/g, '').slice(0, 14);

  if (numbers.length <= 2) return numbers;
  if (numbers.length <= 5) return `${numbers.slice(0, 2)}.${numbers.slice(2)}`;
  if (numbers.length <= 8) return `${numbers.slice(0, 2)}.${numbers.slice(2, 5)}.${numbers.slice(5)}`;
  if (numbers.length <= 12) return `${numbers.slice(0, 2)}.${numbers.slice(2, 5)}.${numbers.slice(5, 8)}/${numbers.slice(8)}`;
  return `${numbers.slice(0, 2)}.${numbers.slice(2, 5)}.${numbers.slice(5, 8)}/${numbers.slice(8, 12)}-${numbers.slice(12)}`;
};

const validateEmail = (value: string) => {
  if (!value || value.length > PIX_KEY_EMAIL_MAX_LENGTH) {
    return false;
  }

  return /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(value);
};

const getPixKeyDigits = (value: string) => value.replace(/\D/g, '');

const getPixKeyDigitLimit = (type: PixKeyType) => {
  if (type === 'CPF' || type === 'PHONE') return 11;
  if (type === 'CNPJ') return 14;
  return null;
};

const isNumericPixKeyType = (type: PixKeyType) => type === 'CPF' || type === 'CNPJ' || type === 'PHONE';

const shouldBlockNonNumericPixKeyInput = (type: PixKeyType, event: InputEvent) => {
  if (!isNumericPixKeyType(type)) {
    return false;
  }

  if (!event.data) {
    return false;
  }

  return /\D/.test(event.data);
};

const formatPixKeyValue = (value: string, type: PixKeyType) => {
  if (type === 'CPF') return formatCPF(value);
  if (type === 'CNPJ') return formatCNPJ(value);
  if (type === 'PHONE') return formatPhone(value);
  if (type === 'EMAIL') return value.trim().slice(0, PIX_KEY_EMAIL_MAX_LENGTH);
  return value;
};

const normalizePixKeyValue = (value: string, type: PixKeyType) => {
  if (type === 'CPF' || type === 'CNPJ' || type === 'PHONE') {
    return getPixKeyDigits(value);
  }

  return value.trim();
};

const getPixKeyMaxLength = (type: PixKeyType) => {
  if (type === 'CPF') return formatCPF('9'.repeat(11)).length;
  if (type === 'CNPJ') return formatCNPJ('9'.repeat(14)).length;
  if (type === 'PHONE') return formatPhone('9'.repeat(11)).length;
  if (type === 'EMAIL') return PIX_KEY_EMAIL_MAX_LENGTH;
  return undefined;
};

const getPixKeyInputMode = (type: PixKeyType) => {
  if (type === 'EMAIL') return 'email';
  if (type === 'RANDOM') return 'text';
  return 'numeric';
};

const getPixKeyTypeLabel = (type: PixKeyType) => {
  if (type === 'EMAIL') return 'E-mail';
  if (type === 'PHONE') return 'Telefone';
  if (type === 'RANDOM') return 'Chave aleatória';
  return type;
};

const getPixKeyPlaceholder = (type: PixKeyType) => {
  if (type === 'CPF') return '000.000.000-00';
  if (type === 'CNPJ') return '00.000.000/0000-00';
  if (type === 'EMAIL') return 'email@exemplo.com';
  if (type === 'PHONE') return '(11) 99999-9999';
  return 'Informe sua chave PIX';
};

const getPixKeyError = (value: string, type: PixKeyType) => {
  const normalizedValue = normalizePixKeyValue(value, type);

  if (!normalizedValue) {
    return 'Informe a chave PIX';
  }

  if (type === 'CPF' && !validateCPF(value)) {
    return 'Informe um CPF válido';
  }

  if (type === 'CNPJ' && normalizedValue.length !== 14) {
    return 'Informe um CNPJ com 14 dígitos';
  }

  if (type === 'EMAIL' && !validateEmail(normalizedValue)) {
    return 'Informe um e-mail válido';
  }

  if (type === 'PHONE' && normalizedValue.length !== 11) {
    return 'Informe um telefone com 11 dígitos';
  }

  return '';
};

const withdrawPixKeyPlaceholder = computed(() => getPixKeyPlaceholder(withdrawPixKeyType.value));

const withdrawPixKeyMaxLength = computed(() => getPixKeyMaxLength(withdrawPixKeyType.value));
const withdrawPixKeyInputMode = computed(() => getPixKeyInputMode(withdrawPixKeyType.value));

const handleWithdrawPixKeyInput = (value: string) => {
  withdrawPixKey.value = formatPixKeyValue(value, withdrawPixKeyType.value);
};

const handleWithdrawPixKeyTypeChange = (value: PixKeyType) => {
  withdrawPixKeyType.value = value;
  withdrawPixKey.value = formatPixKeyValue(withdrawPixKey.value, value);
};

const handleWithdrawPixKeyTypeSelect = (event: Event) => {
  const value = (event.target as HTMLSelectElement).value as PixKeyType;
  handleWithdrawPixKeyTypeChange(value);
};

const handleWithdrawPixKeyBeforeInput = (event: InputEvent) => {
  if (shouldBlockNonNumericPixKeyInput(withdrawPixKeyType.value, event)) {
    event.preventDefault();
    return;
  }

  const digitLimit = getPixKeyDigitLimit(withdrawPixKeyType.value);
  const target = event.target as HTMLInputElement | null;
  const insertedDigits = getPixKeyDigits(event.data ?? '');

  if (!digitLimit || !target || !insertedDigits) {
    return;
  }

  const selectionStart = target.selectionStart ?? target.value.length;
  const selectionEnd = target.selectionEnd ?? selectionStart;
  const selectedDigits = getPixKeyDigits(target.value.slice(selectionStart, selectionEnd)).length;
  const nextDigitsCount = getPixKeyDigits(target.value).length - selectedDigits + insertedDigits.length;

  if (nextDigitsCount > digitLimit) {
    event.preventDefault();
  }
};

const getWithdrawPixKeyError = () => {
  const normalizedValue = normalizePixKeyValue(withdrawPixKey.value, withdrawPixKeyType.value);

  if (!normalizedValue) {
    return 'Informe a chave PIX';
  }

  if (withdrawPixKeyType.value === 'CPF' && !validateCPF(withdrawPixKey.value)) {
    return 'Informe um CPF válido';
  }

  if (withdrawPixKeyType.value === 'CNPJ' && normalizedValue.length !== 14) {
    return 'Informe um CNPJ com 14 dígitos';
  }

  if (withdrawPixKeyType.value === 'EMAIL' && !validateEmail(normalizedValue)) {
    return 'Informe um e-mail válido';
  }

  if (withdrawPixKeyType.value === 'PHONE' && normalizedValue.length !== 11) {
    return 'Informe um telefone com 11 dígitos';
  }

  return '';
};

const newPixKeyPlaceholder = computed(() => getPixKeyPlaceholder(newPixKeyType.value));
const newPixKeyMaxLength = computed(() => getPixKeyMaxLength(newPixKeyType.value));
const newPixKeyInputMode = computed(() => getPixKeyInputMode(newPixKeyType.value));

const handleNewPixKeyInput = (value: string) => {
  newPixKey.value = formatPixKeyValue(value, newPixKeyType.value);
};

const handleNewPixKeyTypeChange = (value: PixKeyType) => {
  newPixKeyType.value = value;
  newPixKey.value = formatPixKeyValue(newPixKey.value, value);
};

const handleNewPixKeyTypeSelect = (event: Event) => {
  const value = (event.target as HTMLSelectElement).value as PixKeyType;
  handleNewPixKeyTypeChange(value);
};

const handleNewPixKeyBeforeInput = (event: InputEvent) => {
  if (shouldBlockNonNumericPixKeyInput(newPixKeyType.value, event)) {
    event.preventDefault();
    return;
  }

  const digitLimit = getPixKeyDigitLimit(newPixKeyType.value);
  const target = event.target as HTMLInputElement | null;
  const insertedDigits = getPixKeyDigits(event.data ?? '');

  if (!digitLimit || !target || !insertedDigits) {
    return;
  }

  const selectionStart = target.selectionStart ?? target.value.length;
  const selectionEnd = target.selectionEnd ?? selectionStart;
  const selectedDigits = getPixKeyDigits(target.value.slice(selectionStart, selectionEnd)).length;
  const nextDigitsCount = getPixKeyDigits(target.value).length - selectedDigits + insertedDigits.length;

  if (nextDigitsCount > digitLimit) {
    event.preventDefault();
  }
};

const getNewPixKeyError = () => getPixKeyError(newPixKey.value, newPixKeyType.value);

const validateCNPJ = (cnpj: string) => {
  const cleanCNPJ = cnpj.replace(/\D/g, '');

  if (cleanCNPJ.length !== 14) return false;
  if (/^(\d)\1{13}$/.test(cleanCNPJ)) return false;

  const calculateDigit = (base: string, factors: number[]) => {
    const total = base
      .split('')
      .reduce((sum, digit, index) => sum + Number(digit) * factors[index], 0);

    const remainder = total % 11;
    return remainder < 2 ? 0 : 11 - remainder;
  };

  const firstDigit = calculateDigit(cleanCNPJ.slice(0, 12), [5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2]);
  const secondDigit = calculateDigit(cleanCNPJ.slice(0, 13), [6, 5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2]);

  return firstDigit === Number(cleanCNPJ[12]) && secondDigit === Number(cleanCNPJ[13]);
};

const getHolderCpfCnpjDigits = (value = newHolderCpfCnpj.value) => value.replace(/\D/g, '').slice(0, 14);

const formatHolderCpfCnpj = (value: string) => {
  const digits = getHolderCpfCnpjDigits(value);
  return digits.length <= 11 ? formatCPF(digits) : formatCNPJ(digits);
};

const holderCpfCnpjPlaceholder = computed(() =>
  getHolderCpfCnpjDigits().length > 11 ? '00.000.000/0000-00' : '000.000.000-00'
);

const handleHolderCpfCnpjInput = (value: string) => {
  newHolderCpfCnpj.value = formatHolderCpfCnpj(value);
};

const handleHolderCpfCnpjBeforeInput = (event: InputEvent) => {
  if (event.data && /\D/.test(event.data)) {
    event.preventDefault();
    return;
  }

  const target = event.target as HTMLInputElement | null;
  const insertedDigits = (event.data ?? '').replace(/\D/g, '');

  if (!target || !insertedDigits) {
    return;
  }

  const selectionStart = target.selectionStart ?? target.value.length;
  const selectionEnd = target.selectionEnd ?? selectionStart;
  const selectedDigits = target.value.slice(selectionStart, selectionEnd).replace(/\D/g, '').length;
  const nextDigitsCount = target.value.replace(/\D/g, '').length - selectedDigits + insertedDigits.length;

  if (nextDigitsCount > 14) {
    event.preventDefault();
  }
};

const getHolderCpfCnpjError = () => {
  const digits = getHolderCpfCnpjDigits();

  if (!digits) {
    return 'Informe o CPF/CNPJ do titular';
  }

  if (digits.length <= 11) {
    return validateCPF(newHolderCpfCnpj.value) ? '' : 'Informe um CPF válido';
  }

  if (digits.length !== 14) {
    return 'Informe um CNPJ com 14 dígitos';
  }

  return validateCNPJ(newHolderCpfCnpj.value) ? '' : 'Informe um CNPJ válido';
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
    const pixKeyType = normalizePixKeyType(account.pixKeyType);
    const pixKeyValue = account.pixKeyValue || account.pixKey || '';
    return `${getPixKeyTypeLabel(pixKeyType)}: ${formatPixKeyValue(pixKeyValue, pixKeyType)}`;
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

  const pixKeyType = normalizePixKeyType(account.pixKeyType);
  withdrawPixKeyType.value = pixKeyType;
  withdrawPixKey.value = formatPixKeyValue(account.pixKeyValue || account.pixKey || '', pixKeyType);
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
    const [walletData, accountsData] = await Promise.all([
      walletService.getWallet(),
      walletService.getBankAccounts(),
    ]);

    wallet.value = walletData;
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
  const amount = getWithdrawAmountValue();
  if (Number.isNaN(amount) || amount <= 0) {
    toast.error('Informe um valor válido');
    return;
  }
  if (amount > maxWithdrawAmount.value) {
    toast.error('Valor excede o saldo disponível');
    return;
  }
  if (withdrawalMethod.value === 'PIX') {
    const pixKeyError = getWithdrawPixKeyError();
    if (pixKeyError) {
      toast.error(pixKeyError);
      return;
    }
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
            pixKey: normalizePixKeyValue(withdrawPixKey.value, withdrawPixKeyType.value),
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
    currentPage.value = 1;
    await Promise.all([loadData(), fetchTransactions()]);
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

  const holderCpfCnpjError = getHolderCpfCnpjError();
  if (holderCpfCnpjError) {
    toast.error(holderCpfCnpjError);
    return;
  }

  const data: CreateBankAccountRequest = {
    accountType: newAccountType.value,
    holderName: newHolderName.value,
    holderCpfCnpj: getHolderCpfCnpjDigits(),
    isDefault: bankAccounts.value.length === 0,
  };

  if (newAccountType.value === 'PIX_KEY') {
    const pixKeyError = getNewPixKeyError();
    if (pixKeyError) {
      toast.error(pixKeyError);
      return;
    }
    data.pixKey = normalizePixKeyValue(newPixKey.value, newPixKeyType.value);
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

const handleDownloadReport = async (format: WalletReportFormat) => {
  try {
    await walletService.downloadReport(format);
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

onMounted(() => {
  loadData();
  fetchTransactions();
});
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
          <h1 class="text-2xl font-bold text-[#263238] mb-2">
            Carteira &amp; Saques
          </h1>
          <p class="text-[#435574]">Gerencie seus ganhos, contas e saques</p>
        </div>
        <div class="flex gap-3">
          <DropdownMenu :open="reportMenuOpen" @update:open="reportMenuOpen = $event">
            <Button
              variant="outline"
              size="sm"
              type="button"
              @click.stop="reportMenuOpen = !reportMenuOpen"
            >
              <Download class="h-4 w-4 mr-2" />
              Exportar Relatório
            </Button>
            <DropdownMenuContent align="end" class="z-[9999] min-w-[12rem] bg-white text-[#263238]">
              <DropdownMenuItem @click="handleDownloadReport('PDF')">
                Exportar em PDF
              </DropdownMenuItem>
              <DropdownMenuItem @click="handleDownloadReport('EXCEL')">
                Exportar em Excel
              </DropdownMenuItem>
            </DropdownMenuContent>
          </DropdownMenu>
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
                  'bg-blue-100'
                }`">
                  <QrCode v-if="account.accountType === 'PIX_KEY'" class="h-4 w-4 text-[#286AD8]" />
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
            <p class="text-sm text-[#435574]">{{ totalElements }} transações encontradas</p>
          </div>
          <div class="flex flex-col sm:flex-row gap-2 sm:items-center">
            <div class="flex gap-2 flex-wrap">
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

            <div class="w-full sm:w-[220px]">
              <Select :model-value="timeFilter" @update:model-value="(val: string) => timeFilter = val">
                <SelectTrigger>
                  <SelectValue placeholder="Todo o período" />
                </SelectTrigger>
                <SelectContent>
                  <SelectItem value="Todo o período">Todo o período</SelectItem>
                  <SelectItem value="Hoje">Hoje</SelectItem>
                  <SelectItem value="Ontem">Ontem</SelectItem>
                  <SelectItem value="Últimos 7 dias">Últimos 7 dias</SelectItem>
                  <SelectItem value="Últimos 30 dias">Últimos 30 dias</SelectItem>
                  <SelectItem value="Mês atual">Mês atual</SelectItem>
                  <SelectItem value="Último mês">Último mês</SelectItem>
                  <SelectItem value="Intervalo personalizado">Intervalo personalizado</SelectItem>
                </SelectContent>
              </Select>
            </div>
          </div>
        </div>

        <div v-if="timeFilter === 'Intervalo personalizado'" class="grid grid-cols-1 md:grid-cols-2 gap-4 mb-4 mt-1 pt-4 border-t">
          <div class="space-y-1">
            <label class="text-sm font-medium text-[#435574]">De</label>
            <Input
              type="date"
              :model-value="customDateFrom"
              @update:model-value="(val: string) => customDateFrom = val"
            />
          </div>
          <div class="space-y-1">
            <label class="text-sm font-medium text-[#435574]">Até</label>
            <Input
              type="date"
              :model-value="customDateTo"
              @update:model-value="(val: string) => customDateTo = val"
            />
          </div>
        </div>

        <div v-if="isLoadingTransactions" class="flex justify-center py-12">
          <Loader2 class="h-6 w-6 animate-spin text-[#286AD8]" />
        </div>

        <div v-else-if="pagedTransactions.length === 0" class="text-center py-12 text-[#435574]">
          <DollarSign class="h-10 w-10 mx-auto mb-3 opacity-40" />
          <p class="text-sm">Nenhuma transação encontrada</p>
        </div>

        <div v-else class="space-y-3">
          <div
            v-for="tx in pagedTransactions"
            :key="`${tx.type}-${tx.id}`"
            class="flex items-center gap-4 p-4 rounded-lg border border-gray-200 hover:border-gray-300 transition-colors"
          >
            <div :class="`w-10 h-10 rounded-full flex items-center justify-center flex-shrink-0 ${
              tx.type === 'EARNING'
                ? 'bg-blue-100'
                : 'bg-green-100'
            }`">
              <ArrowDownToLine v-if="tx.type === 'EARNING'" class="h-5 w-5 text-blue-600" />
              <ArrowUpFromLine v-else class="h-5 w-5 text-green-600" />
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

            <p :class="`text-sm font-semibold whitespace-nowrap ${tx.type === 'EARNING' ? 'text-blue-600' : 'text-green-600'}`">
              {{ tx.amount >= 0 ? '+' : '' }}{{ formatCurrency(Math.abs(tx.amount)) }}
            </p>
          </div>
        </div>

        <!-- Paginação -->
        <div v-if="totalPages > 1" class="flex items-center justify-center gap-2 mt-6">
          <Button
            variant="outline"
            size="sm"
            :disabled="currentPage === 1 || isLoadingTransactions"
            @click="goToPage(currentPage - 1)"
          >
            Anterior
          </Button>

          <Button
            v-for="pageNum in visiblePageNumbers"
            :key="pageNum"
            :variant="currentPage === pageNum ? 'default' : 'outline'"
            size="sm"
            :disabled="isLoadingTransactions"
            @click="goToPage(pageNum)"
          >
            {{ pageNum }}
          </Button>

          <Button
            variant="outline"
            size="sm"
            :disabled="currentPage === totalPages || isLoadingTransactions"
            @click="goToPage(currentPage + 1)"
          >
            Próxima
          </Button>
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
                  :model-value="withdrawAmount"
                  type="text"
                  inputmode="decimal"
                  :maxlength="MAX_WITHDRAW_FORMATTED_LENGTH"
                  placeholder="R$ 0,00"
                  @beforeinput="handleWithdrawAmountBeforeInput"
                  @update:model-value="handleWithdrawAmountInput"
                />
                <Button
                  @click="withdrawAmount = formatCurrencyInput(maxWithdrawAmount)"
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
                    :value="withdrawPixKeyType"
                    @change="handleWithdrawPixKeyTypeSelect"
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
                    :model-value="withdrawPixKey"
                    :inputmode="withdrawPixKeyInputMode"
                    :maxlength="withdrawPixKeyMaxLength"
                    :placeholder="withdrawPixKeyPlaceholder"
                    @beforeinput="handleWithdrawPixKeyBeforeInput"
                    @update:model-value="handleWithdrawPixKeyInput"
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
                    :value="newPixKeyType"
                    @change="handleNewPixKeyTypeSelect"
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
                    :model-value="newPixKey"
                    :inputmode="newPixKeyInputMode"
                    :maxlength="newPixKeyMaxLength"
                    :placeholder="newPixKeyPlaceholder"
                    @beforeinput="handleNewPixKeyBeforeInput"
                    @update:model-value="handleNewPixKeyInput"
                  />
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
                <Input
                  :model-value="newHolderCpfCnpj"
                  inputmode="numeric"
                  :maxlength="18"
                  :placeholder="holderCpfCnpjPlaceholder"
                  @beforeinput="handleHolderCpfCnpjBeforeInput"
                  @update:model-value="handleHolderCpfCnpjInput"
                />
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
