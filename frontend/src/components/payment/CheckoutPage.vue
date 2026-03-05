<script setup lang="ts">
import { ref, computed, watch, onMounted, onUnmounted } from 'vue';
import { useRouter } from 'vue-router';
import { usePaymentWebSocket } from '../../composables/usePaymentWebSocket';
import {
  CreditCard,
  Smartphone,
  QrCode,
  ArrowLeft,
  Shield,
  Clock,
  MapPin,
  User,
  FileText,
  Loader2,
  CheckCircle,
  Copy,
  Info,
  RefreshCw,
  AlertTriangle,
  Zap,
  Star,
} from 'lucide-vue-next';
import Button from '../ui/button.vue';
import Label from '../ui/label.vue';
import Input from '../ui/input.vue';
import RadioGroup from '../ui/radio-group.vue';
import RadioGroupItem from '../ui/radio-group-item.vue';
import Card from '../ui/card.vue';
import Separator from '../ui/separator.vue';
import Avatar from '../ui/avatar.vue';
import AvatarFallback from '../ui/avatar-fallback.vue';
import AvatarImage from '../ui/avatar-image.vue';
import Badge from '../ui/badge.vue';
import ServiceRequestTimeline from '../service/ServiceRequestTimeline.vue';

interface Proposal {
  freelancerId: string;
  freelancerName: string;
  freelancerAvatar?: string;
  freelancerRating: number;
  service: string;
  description: string;
  price: number;
  deliveryTime: string;
}

interface Props {
  requestId: string;
  proposal: Proposal;
  onBack: () => void;
  onConfirmPayment: (paymentMethod: string, paymentData: any) => void;
}

const BASE_URL = import.meta.env.VITE_API_URL;

const props = defineProps<Props>();

const router = useRouter();

// Obter email do usuário do token JWT
const getUserEmail = (): string => {
  try {
    const token = localStorage.getItem('token');
    if (token) {
      const payload = JSON.parse(atob(token.split('.')[1]));
      return payload.sub || '';
    }
  } catch (e) {
    console.warn('Erro ao obter email do token:', e);
  }
  return '';
};

const userEmail = getUserEmail();

// Inicializar WebSocket
const { connected: wsConnected, connectionError: wsError, connect: connectWS, disconnect: disconnectWS } = usePaymentWebSocket(userEmail);

const paymentMethod = ref<string>('');
const isProcessing = ref(false);
const isLoadingPix = ref(false);
const showPixQrCode = ref(false);
const pixData = ref<{ qrCodeImage?: string; payload?: string; expirationDate?: string } | null>(null);
const pixCpf = ref<string>('');
const currentPaymentId = ref<number | null>(null);
const paymentConfirmed = ref(false);
const copyFeedback = ref<string | null>(null);
const copyFeedbackVisible = ref(false);
const copyFeedbackFading = ref(false);
let copyFeedbackTimeout: ReturnType<typeof setTimeout> | null = null;
let copyFeedbackFadeTimeout: ReturnType<typeof setTimeout> | null = null;
const cardData = ref({
  number: '',
  name: '',
  expiry: '',
  cvv: '',
});

const platformFee = computed(() => props.proposal.price * 0.1);
const totalAmount = computed(() => props.proposal.price + platformFee.value);

const formatCardNumber = (value: string) => {
  const cleaned = value.replace(/\s/g, '');
  const groups = cleaned.match(/.{1,4}/g);
  return groups ? groups.join(' ') : cleaned;
};

const handleCardNumberInput = (e: Event) => {
  const input = e.target as HTMLInputElement;
  const formatted = formatCardNumber(input.value);
  cardData.value.number = formatted;
};

const handleExpiryInput = (e: Event) => {
  const input = e.target as HTMLInputElement;
  let value = input.value.replace(/\D/g, '');
  
  if (value.length >= 2) {
    value = value.slice(0, 2) + '/' + value.slice(2, 4);
  }
  
  cardData.value.expiry = value;
};

const formatCpf = (value: string) => {
  const cleaned = value.replace(/\D/g, '');
  if (cleaned.length <= 11) {
    return cleaned
      .replace(/(\d{3})(\d)/, '$1.$2')
      .replace(/(\d{3})(\d)/, '$1.$2')
      .replace(/(\d{3})(\d{1,2})$/, '$1-$2');
  }
  return cleaned.substring(0, 11);
};

const handleCpfInput = (e: Event) => {
  const input = e.target as HTMLInputElement;
  const formatted = formatCpf(input.value);
  pixCpf.value = formatted;
};

const validateCpf = (cpf: string): boolean => {
  const cleaned = cpf.replace(/\D/g, '');
  return cleaned.length === 11;
};

const copyPixCode = async () => {
  if (pixData.value?.payload) {
    try {
      if (copyFeedbackTimeout) {
        clearTimeout(copyFeedbackTimeout);
      }
      if (copyFeedbackFadeTimeout) {
        clearTimeout(copyFeedbackFadeTimeout);
      }
      await navigator.clipboard.writeText(pixData.value.payload);
      copyFeedback.value = 'Copiado';
      copyFeedbackVisible.value = true;
      copyFeedbackFading.value = false;

      copyFeedbackFadeTimeout = setTimeout(() => {
        copyFeedbackFading.value = true;
        copyFeedbackFadeTimeout = null;
      }, 1500);

      copyFeedbackTimeout = setTimeout(() => {
        copyFeedback.value = null;
        copyFeedbackVisible.value = false;
        copyFeedbackFading.value = false;
        copyFeedbackTimeout = null;
      }, 2000);
    } catch (err) {
      console.error('Erro ao copiar o código PIX:', err);
    }
  }
};

// Observar mudanças no método de pagamento
watch(paymentMethod, async (newMethod, oldMethod) => {
  // Se já existe um pagamento em andamento e estamos voltando para PIX, não resetar
  if (newMethod === 'pix' && currentPaymentId.value) {
    // Se temos um pagamento PIX existente, tentar carregar o QR Code
    if (!pixData.value) {
      await loadExistingPixQrCode(currentPaymentId.value);
    }
    return;
  }
  
  if (newMethod === 'pix') {
    // Novo pagamento PIX - aguardar CPF
    showPixQrCode.value = false;
    pixData.value = null;
    pixCpf.value = '';
  } else if (oldMethod === 'pix') {
    // Trocando de PIX para outro método - limpar dados do PIX
    // Mas manter currentPaymentId se existir um pagamento
    showPixQrCode.value = false;
    pixData.value = null;
    pixCpf.value = '';
  }
});

const generatePixQrCode = async () => {
  // Validar CPF antes de gerar
  if (!validateCpf(pixCpf.value)) {
    alert('Por favor, digite um CPF válido (11 dígitos)');
    return;
  }

  // Se já existe um pagamento, apenas recarregar o QR Code
  if (currentPaymentId.value) {
    isLoadingPix.value = true;
    try {
      const apiUrl = import.meta.env.VITE_API_URL || 'http://localhost:8080';
      const qrCodeResponse = await fetch(
        `${apiUrl}/api/payments/${currentPaymentId.value}/pix-qrcode`,
        {
          headers: {
            'Authorization': `Bearer ${localStorage.getItem('token')}`,
          },
        }
      );

      if (qrCodeResponse.ok) {
        const qrCodeData = await qrCodeResponse.json();
        pixData.value = {
          qrCodeImage: qrCodeData.encodedImage,
          payload: qrCodeData.payload,
          expirationDate: qrCodeData.expirationDate,
        };
        showPixQrCode.value = true;
        console.log('QR Code PIX recarregado com sucesso');
      }
    } catch (error) {
      console.error('Erro ao recarregar QR Code:', error);
      alert('Erro ao carregar QR Code. Tente novamente.');
    } finally {
      isLoadingPix.value = false;
    }
    return;
  }

  isLoadingPix.value = true;
  showPixQrCode.value = false;
  pixData.value = null;

  try {
    const apiUrl = import.meta.env.VITE_API_URL || 'http://localhost:8080';

    // Criar um pagamento temporário para gerar o QR Code
    const response = await fetch(`${apiUrl}/api/payments`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'Authorization': `Bearer ${localStorage.getItem('token')}`,
      },
      body: JSON.stringify({
        serviceRequestId: parseInt(props.requestId),
        paymentMethod: 'PIX',
        amount: totalAmount.value,
        cpf: pixCpf.value.replace(/\D/g, ''), // Enviar CPF sem formatação
      }),
    });

    if (!response.ok) {
      const errorData = await response.json().catch(() => ({}));
      
      // Se já existe um pagamento, buscar ele ao invés de criar um novo
      if (errorData.message && errorData.message.includes('Já existe um pagamento')) {
        console.log('Pagamento já existe, buscando QR Code...');
        await checkExistingPayment();
        if (currentPaymentId.value) {
          // Tentar gerar novamente (agora vai usar o pagamento existente)
          await generatePixQrCode();
          return;
        }
      }
      
      throw new Error(errorData.message || 'Erro ao gerar QR Code PIX');
    }

    const payment = await response.json();
    currentPaymentId.value = payment.id;

    // Buscar os dados do QR Code
    if (payment.asaasPaymentId) {
      const qrCodeResponse = await fetch(
        `${apiUrl}/api/payments/${payment.id}/pix-qrcode`,
        {
          headers: {
            'Authorization': `Bearer ${localStorage.getItem('token')}`,
          },
        }
      );

      if (qrCodeResponse.ok) {
        const qrCodeData = await qrCodeResponse.json();
        pixData.value = {
          qrCodeImage: qrCodeData.encodedImage,
          payload: qrCodeData.payload,
          expirationDate: qrCodeData.expirationDate,
        };
        showPixQrCode.value = true;
      }
    }
  } catch (error: any) {
    console.error('Erro ao gerar QR Code PIX:', error);
    alert(error.message || 'Erro ao gerar QR Code PIX. Tente novamente.');
  } finally {
    isLoadingPix.value = false;
  }
};

const handleConfirmPayment = async () => {
  if (!paymentMethod.value) {
    alert('Selecione um método de pagamento');
    return;
  }

  if (paymentMethod.value === 'credit-card') {
    if (!cardData.value.number || !cardData.value.name || !cardData.value.expiry || !cardData.value.cvv) {
      alert('Preencha todos os dados do cartão');
      return;
    }
  }

  isProcessing.value = true;

  try {
    await props.onConfirmPayment(paymentMethod.value, {
      cardData: paymentMethod.value === 'credit-card' ? cardData.value : null,
      requestId: props.requestId,
      amount: totalAmount.value,
    });
  } catch (error) {
    console.error('Erro ao processar pagamento:', error);
    alert('Erro ao processar pagamento. Tente novamente.');
  } finally {
    isProcessing.value = false;
  }
};

const getInitials = (name: string) => {
  return name
    .split(' ')
    .map((n) => n[0])
    .join('')
    .toUpperCase()
    .slice(0, 2);
};

// =============================================
// BOTÃO DE TESTE - REMOVER EM PRODUÇÃO
// =============================================
const isSimulatingPayment = ref(false);

const simulatePaymentConfirmation = async () => {
  if (!currentPaymentId.value) {
    alert('Nenhum pagamento para simular');
    return;
  }

  isSimulatingPayment.value = true;

  try {
    const apiUrl = import.meta.env.VITE_API_URL || 'http://localhost:8080';
    const token = localStorage.getItem('token');

    const response = await fetch(`${apiUrl}/api/payments/${currentPaymentId.value}/simulate-confirmation`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        Authorization: `Bearer ${token}`,
      },
    });

    if (!response.ok) {
      throw new Error('Erro ao simular confirmação');
    }

    console.log('Simulação de confirmação enviada');
    alert('Simulação enviada! Aguarde a confirmação via WebSocket...');
  } catch (error) {
    console.error('Erro ao simular pagamento:', error);
    alert('Erro ao simular pagamento. Verifique se o endpoint existe no backend.');
  } finally {
    isSimulatingPayment.value = false;
  }
};
// =============================================

// Estado para mensagens de erro/status
const paymentError = ref<string | null>(null);

// Handler para notificações de pagamento via WebSocket
const handlePaymentNotification = (event: Event) => {
  const customEvent = event as CustomEvent;
  const notification = customEvent.detail;

  console.log('Notificação de pagamento recebida:', notification);

  // Verificar se a notificação é para o pagamento atual
  if (notification.paymentId && notification.paymentId !== currentPaymentId.value) {
    console.log('Notificação ignorada - não é para o pagamento atual');
    return;
  }

  switch (notification.type) {
    case 'PIX_CONFIRMED':
      console.log('PIX confirmado');
      paymentConfirmed.value = true;
      paymentError.value = null;
      // Redirecionar para página de sucesso após 2 segundos
      setTimeout(() => {
        router.push(`/payment-success?paymentId=${notification.paymentId}`);
      }, 2000);
      break;

    case 'PAYMENT_FAILED':
      console.error('Pagamento falhou:', notification.reason);
      paymentError.value = notification.message || 'O pagamento falhou. Tente novamente.';
      break;

    case 'PAYMENT_CANCELLED':
      console.warn('Pagamento cancelado');
      paymentError.value = 'O pagamento foi cancelado.';
      break;

    case 'PAYMENT_STATUS_CHANGED':
      console.log('Status do pagamento alterado:', notification.status);
      if (notification.status === 'COMPLETED') {
        paymentConfirmed.value = true;
        setTimeout(() => {
          router.push(`/payment-success?paymentId=${notification.paymentId}`);
        }, 2000);
      } else if (notification.status === 'FAILED' || notification.status === 'CANCELLED') {
        paymentError.value = `Pagamento ${notification.status === 'FAILED' ? 'falhou' : 'foi cancelado'}.`;
      }
      break;

    default:
      console.log('Tipo de notificação não tratado:', notification.type);
  }
};

// Lifecycle: Conectar WebSocket quando montar
onMounted(async () => {
  console.log('CheckoutPage montado');
  console.log('userEmail:', userEmail);
  console.log('VITE_API_URL:', import.meta.env.VITE_API_URL);
  
  if (userEmail) {
    console.log('Conectando WebSocket para:', userEmail);
    connectWS();
  } else {
    console.warn('userEmail vazio, WebSocket não será conectado');
  }

  // Verificar se já existe um pagamento para esta solicitação
  await checkExistingPayment();

  // Escutar notificações de pagamento
  window.addEventListener('payment-notification', handlePaymentNotification);
});

// Verificar se já existe pagamento em aberto
const checkExistingPayment = async () => {
  try {
    const token = localStorage.getItem('token');
    const apiUrl = import.meta.env.VITE_API_URL || 'http://localhost:8080';
    
    const response = await fetch(`${apiUrl}/api/payments/service-request/${props.requestId}`, {
      headers: {
        'Authorization': `Bearer ${token}`,
        'Content-Type': 'application/json'
      }
    });

    if (response.ok) {
      const payment = await response.json();
      console.log('Pagamento existente encontrado:', payment);
      
      // Se o pagamento já foi completado, marcar como confirmado
      if (payment.status === 'COMPLETED') {
        console.log('Pagamento já foi completado anteriormente');
        paymentConfirmed.value = true;
        currentPaymentId.value = payment.id;
        paymentMethod.value = payment.paymentMethod?.toLowerCase() || 'pix';
        return;
      }
      
      // Se o pagamento ainda não foi completado, carregar os dados
      if (payment.status === 'PENDING' || payment.status === 'PROCESSING') {
        currentPaymentId.value = payment.id;
        paymentMethod.value = payment.paymentMethod?.toLowerCase() || 'pix';
        
        // Se for PIX, buscar QR Code atualizado da API
        if (payment.paymentMethod?.toLowerCase() === 'pix') {
          await loadExistingPixQrCode(payment.id);
        }
      }
    }
  } catch (error) {
    // Se não encontrar pagamento, continuar normalmente
    console.log('Nenhum pagamento existente encontrado');
  }
};

// Carregar QR Code de um pagamento PIX existente
const loadExistingPixQrCode = async (paymentId: number) => {
  try {
    const apiUrl = import.meta.env.VITE_API_URL || 'http://localhost:8080';
    const qrCodeResponse = await fetch(
      `${apiUrl}/api/payments/${paymentId}/pix-qrcode`,
      {
        headers: {
          'Authorization': `Bearer ${localStorage.getItem('token')}`,
        },
      }
    );

    if (qrCodeResponse.ok) {
      const qrCodeData = await qrCodeResponse.json();
      
      // Verificar se o pagamento ainda está pendente (não expirado)
      if (qrCodeData.status === 'COMPLETED') {
        paymentConfirmed.value = true;
        console.log('Pagamento já foi confirmado');
        return;
      }
      
      pixData.value = {
        qrCodeImage: qrCodeData.encodedImage,
        payload: qrCodeData.payload,
        expirationDate: qrCodeData.expirationDate,
      };
      showPixQrCode.value = true;
      console.log('QR Code PIX carregado do pagamento existente');
    } else {
      console.warn('Não foi possível carregar QR Code do pagamento existente');
    }
  } catch (error) {
    console.error('Erro ao carregar QR Code existente:', error);
  }
};

// Recarregar QR Code do pagamento existente (usado pelo botão na UI)
const reloadPixQrCode = async () => {
  if (!currentPaymentId.value) {
    // Se não tem pagamento, voltar para tela de CPF
    showPixQrCode.value = false;
    pixData.value = null;
    return;
  }
  
  isLoadingPix.value = true;
  try {
    await loadExistingPixQrCode(currentPaymentId.value);
  } finally {
    isLoadingPix.value = false;
  }
};

// Lifecycle: Desconectar quando desmontar
onUnmounted(() => {
  console.log('Desconectando WebSocket');
  disconnectWS();
  window.removeEventListener('payment-notification', handlePaymentNotification);
  if (copyFeedbackTimeout) clearTimeout(copyFeedbackTimeout);
  if (copyFeedbackFadeTimeout) clearTimeout(copyFeedbackFadeTimeout);
});
</script>

<template>
  <div class="max-w-6xl mx-auto space-y-6">
    <!-- Header -->
    <div class="flex items-center gap-4">
      <Button
        @click="props.onBack"
        variant="ghost"
        size="icon"
        class="hover:bg-gray-100"
      >
        <ArrowLeft class="h-5 w-5" />
      </Button>
      <div>
        <h1 class="text-[#263238]">Checkout</h1>
        <p class="text-[#435574]">Finalize a contratação do serviço</p>
      </div>
    </div>

    <div class="grid grid-cols-1 lg:grid-cols-3 gap-6">
      <!-- Payment Methods -->
      <div class="lg:col-span-2 space-y-6">
        <!-- Pagamento Já Realizado -->
        <Card v-if="paymentConfirmed" class="p-6 bg-green-50 border-2 border-green-400">
          <div class="flex items-start gap-4">
              <div class="flex-shrink-0">
                <div class="w-12 h-12 bg-green-500 rounded-full flex items-center justify-center">
                  <CheckCircle class="h-7 w-7 text-white" />
                </div>
              </div>
              <div class="flex-1">
              <h3 class="text-[#263238] text-xl font-bold mb-2">Pagamento Já Realizado!</h3>
              <p class="text-[#435574] mb-4">
                Este serviço já foi pago com sucesso. Não é necessário realizar um novo pagamento.
              </p>
              
              <div class="bg-white p-4 rounded-lg border border-green-300 mb-4">
                <div class="grid grid-cols-2 gap-3 text-sm">
                  <div>
                    <p class="text-[#435574] font-medium">ID do Pagamento:</p>
                    <p class="text-[#263238] font-mono">#{{ currentPaymentId }}</p>
                  </div>
                  <div>
                    <p class="text-[#435574] font-medium">Status:</p>
                    <Badge class="bg-green-100 text-green-700">CONFIRMADO</Badge>
                  </div>
                </div>
              </div>

              <div class="flex gap-3">
                <Button
                  @click="router.push(`/payment-success?paymentId=${currentPaymentId}`)"
                  class="bg-[#286AD8] hover:bg-[#1e5bb8] text-white"
                >
                  Ver Detalhes do Pagamento
                </Button>
                <Button
                  @click="props.onBack"
                  variant="outline"
                  class="border-[#286AD8] text-[#286AD8] hover:bg-blue-50"
                >
                  Voltar
                </Button>
              </div>
            </div>
          </div>
        </Card>

        <!-- Security Badge -->
        <div v-if="!paymentConfirmed" class="bg-blue-50 border border-blue-200 rounded-lg p-4 flex items-center gap-3">
          <Shield class="h-5 w-5 text-[#286AD8]" />
          <div>
            <p class="text-sm text-[#263238]">Pagamento 100% seguro</p>
            <p class="text-xs text-[#435574]">Conexão criptografada e dados protegidos</p>
          </div>
        </div>

        <!-- Payment Method Selection -->
        <Card v-if="!paymentConfirmed" class="p-6">
          <h3 class="text-[#263238] mb-4">Método de Pagamento</h3>

          <RadioGroup :value="paymentMethod" @update:value="(val: string) => paymentMethod = val">
            <div class="space-y-3">
              <!-- PIX -->
              <label
                :class="`flex items-center gap-3 p-4 border-2 rounded-lg cursor-pointer transition-all ${
                  paymentMethod === 'pix'
                    ? 'border-[#286AD8] bg-blue-50'
                    : 'border-gray-200 hover:border-gray-300'
                }`"
              >
                <RadioGroupItem value="pix" id="pix" />
                <QrCode class="h-5 w-5 text-[#286AD8]" />
                <div class="flex-1">
                  <p class="text-[#263238]">PIX</p>
                  <p class="text-sm text-[#435574]">Aprovação imediata</p>
                </div>
                <Badge class="bg-green-100 text-green-700 hover:bg-green-100">
                  Recomendado
                </Badge>
              </label>

              <!-- Credit Card -->
              <label
                :class="`flex items-center gap-3 p-4 border-2 rounded-lg cursor-pointer transition-all ${
                  paymentMethod === 'credit-card'
                    ? 'border-[#286AD8] bg-blue-50'
                    : 'border-gray-200 hover:border-gray-300'
                }`"
              >
                <RadioGroupItem value="credit-card" id="credit-card" />
                <CreditCard class="h-5 w-5 text-[#286AD8]" />
                <div class="flex-1">
                  <p class="text-[#263238]">Cartão de Crédito</p>
                  <p class="text-sm text-[#435574]">Parcelamento disponível</p>
                </div>
              </label>

              <!-- Digital Wallet -->
              <label
                :class="`flex items-center gap-3 p-4 border-2 rounded-lg cursor-pointer transition-all ${
                  paymentMethod === 'wallet'
                    ? 'border-[#286AD8] bg-blue-50'
                    : 'border-gray-200 hover:border-gray-300'
                }`"
              >
                <RadioGroupItem value="wallet" id="wallet" />
                <Smartphone class="h-5 w-5 text-[#286AD8]" />
                <div class="flex-1">
                  <p class="text-[#263238]">Carteira Digital</p>
                  <p class="text-sm text-[#435574]">PicPay, Mercado Pago</p>
                </div>
              </label>
            </div>
          </RadioGroup>

          <!-- PIX - Pagamento em andamento (sem QR Code visível) -->
          <div v-if="paymentMethod === 'pix' && currentPaymentId && !showPixQrCode && !paymentConfirmed" class="mt-6 space-y-4 p-4 bg-yellow-50 rounded-lg border border-yellow-300">
            <div class="flex items-start gap-3">
              <Clock class="h-5 w-5 text-yellow-600 mt-0.5" />
              <div class="flex-1">
                <p class="text-sm text-[#263238] font-medium">Pagamento PIX em andamento</p>
                <p class="text-xs text-[#435574] mt-1">
                  Já existe um pagamento PIX aguardando confirmação para este serviço.
                </p>
                <p class="text-xs text-[#435574] mt-1">
                  ID do Pagamento: <span class="font-mono font-medium">#{{ currentPaymentId }}</span>
                </p>
              </div>
            </div>

            <Button
              @click="reloadPixQrCode"
              :disabled="isLoadingPix"
              class="w-full bg-[#286AD8] hover:bg-[#1e5bb8] text-white"
            >
              <Loader2 v-if="isLoadingPix" class="h-4 w-4 mr-2 animate-spin" />
              <QrCode v-else class="h-4 w-4 mr-2" />
              {{ isLoadingPix ? 'Carregando...' : 'Exibir QR Code PIX' }}
            </Button>
          </div>

          <!-- PIX Form (apenas se não tem pagamento em andamento) -->
          <div v-if="paymentMethod === 'pix' && !currentPaymentId && !showPixQrCode" class="mt-6 space-y-4 p-4 bg-blue-50 rounded-lg border border-blue-200">
            <div class="flex items-start gap-3 mb-4">
              <Shield class="h-5 w-5 text-[#286AD8] mt-0.5" />
              <div>
                <p class="text-sm text-[#263238] font-medium">Informe seu CPF para gerar o QR Code</p>
                <p class="text-xs text-[#435574] mt-1">Obrigatório para criar a cobrança PIX</p>
              </div>
            </div>
            
            <div class="space-y-2">
              <Label for="pix-cpf" class="text-[#263238]">CPF do pagador *</Label>
              <Input
                id="pix-cpf"
                placeholder="000.000.000-00"
                :model-value="pixCpf"
                @input="handleCpfInput"
                maxlength="14"
                class="text-center text-lg font-mono"
              />
              <p class="text-xs text-[#435574]">Digite apenas os números do seu CPF</p>
            </div>

            <Button
              @click="generatePixQrCode"
              :disabled="!pixCpf || pixCpf.replace(/\D/g, '').length !== 11 || isLoadingPix"
              class="w-full bg-[#286AD8] hover:bg-[#1e5bb8] text-white"
            >
              <Loader2 v-if="isLoadingPix" class="h-4 w-4 mr-2 animate-spin" />
              <QrCode v-else class="h-4 w-4 mr-2" />
              {{ isLoadingPix ? 'Gerando QR Code...' : 'Gerar QR Code PIX' }}
            </Button>
          </div>

          <!-- Credit Card Form -->
          <div v-if="paymentMethod === 'credit-card'" class="mt-6 space-y-4 p-4 bg-gray-50 rounded-lg">
            <div class="space-y-2">
              <Label for="card-number">Número do cartão</Label>
              <Input
                id="card-number"
                placeholder="0000 0000 0000 0000"
                :model-value="cardData.number"
                @input="handleCardNumberInput"
                maxlength="19"
              />
            </div>

            <div class="space-y-2">
              <Label for="card-name">Nome no cartão</Label>
              <Input
                id="card-name"
                placeholder="Nome como está no cartão"
                :model-value="cardData.name"
                @update:model-value="(val) => cardData.name = val as string"
              />
            </div>

            <div class="grid grid-cols-2 gap-4">
              <div class="space-y-2">
                <Label for="card-expiry">Validade</Label>
                <Input
                  id="card-expiry"
                  placeholder="MM/AA"
                  :model-value="cardData.expiry"
                  @input="handleExpiryInput"
                  maxlength="5"
                />
              </div>
              <div class="space-y-2">
                <Label for="card-cvv">CVV</Label>
                <Input
                  id="card-cvv"
                  placeholder="123"
                  type="password"
                  :model-value="cardData.cvv"
                  @update:model-value="(val) => cardData.cvv = val as string"
                  maxlength="4"
                />
              </div>
            </div>

            <div class="pt-2">
              <Button
                @click="handleConfirmPayment"
                :disabled="isProcessing"
                class="w-full bg-[#286AD8] hover:bg-[#435574] transition-colors"
                size="lg"
              >
                <span v-if="isProcessing" class="flex items-center gap-2">
                  <span class="animate-spin h-4 w-4 border-2 border-white border-t-transparent rounded-full" />
                  Processando...
                </span>
                <span v-else>Confirmar Pagamento</span>
              </Button>
              <p class="text-xs text-center text-[#435574] mt-3">
                Ao confirmar, você concorda com os
                <a href="#" class="text-[#286AD8] hover:underline">Termos de Serviço</a>
                e
                <a href="#" class="text-[#286AD8] hover:underline">Política de Privacidade</a>
              </p>
            </div>
          </div>

          <!-- Digital Wallet -->
          <div v-if="paymentMethod === 'wallet'" class="mt-6 p-6 bg-gray-50 rounded-lg">
            <p class="text-sm text-[#435574] mb-4 text-center">
              Você será redirecionado para completar o pagamento
            </p>
            <div class="grid grid-cols-2 gap-3">
              <Button variant="outline" class="w-full">PicPay</Button>
              <Button variant="outline" class="w-full">Mercado Pago</Button>
            </div>
          </div>
        </Card>

        <!-- PIX QR Code Display (Após Geração) -->
        <Card v-if="paymentMethod === 'pix' && showPixQrCode && pixData" class="p-6">
          <div class="flex items-center gap-2 mb-4">
            <CheckCircle class="h-5 w-5 text-green-600" />
            <h3 class="text-[#263238]">QR Code PIX Gerado</h3>
          </div>
          
          <div class="text-center">
            <!-- QR Code Image -->
            <div class="bg-white rounded-lg p-4 mb-4 inline-block border-2 border-[#286AD8] shadow-lg">
              <img
                :src="`data:image/png;base64,${pixData.qrCodeImage}`"
                alt="QR Code PIX"
                class="w-64 h-64 mx-auto"
              />
            </div>

            <!-- Instructions -->
            <div class="space-y-3">
              <div class="flex items-center justify-center gap-2 text-sm text-[#263238] font-semibold">
                <QrCode class="h-4 w-4 text-[#286AD8]" />
                <p>Escaneie o QR Code com seu app de pagamento</p>
              </div>
              <p class="text-xs text-[#435574]">Ou copie o código PIX abaixo</p>

              <!-- PIX Copy/Paste Code -->
              <div class="flex gap-2 max-w-md mx-auto">
                <Input
                  :value="pixData.payload"
                  readonly
                  class="text-xs font-mono flex-1 bg-gray-100"
                />
                <div class="relative">
                  <Button
                    @click="copyPixCode"
                    variant="outline"
                    size="sm"
                    class="shrink-0 bg-white"
                  >
                    <Copy class="h-4 w-4 mr-2" />
                    Copiar
                  </Button>
                  <transition name="fade">
                    <div
                      v-if="copyFeedbackVisible"
                      class="absolute left-1/2 -translate-x-1/2 -top-12 pointer-events-none transition-opacity duration-300"
                      :class="copyFeedbackFading ? 'opacity-0' : 'opacity-100'"
                    >
                      <div class="relative bg-white/95 text-green-700 border border-green-500/90 text-xs px-3 py-2 rounded-md shadow-lg whitespace-nowrap opacity-95">
                        {{ copyFeedback }}
                        <div class="absolute left-1/2 -bottom-1.5 -translate-x-1/2 w-3 h-3 bg-white/95 border-b border-r border-green-500/90 rotate-45 shadow-sm"></div>
                      </div>
                    </div>
                  </transition>
                </div>
                <Button
                  v-if="!paymentConfirmed"
                  @click="reloadPixQrCode"
                  variant="outline"
                  size="sm"
                  class="shrink-0 bg-white"
                  :disabled="isLoadingPix"
                >
                  <Loader2 v-if="isLoadingPix" class="h-4 w-4 mr-2 animate-spin" />
                  <RefreshCw v-else class="h-4 w-4 mr-2" />
                  Recarregar
                </Button>
              </div>

              <!-- Expiration Info -->
              <div class="mt-4 p-3 bg-blue-50 rounded-lg border border-blue-200">
                <div class="flex items-center gap-2 text-xs text-[#435574] font-medium">
                  <Clock class="h-4 w-4 text-[#286AD8]" />
                  <p>Válido até: {{ pixData.expirationDate ? new Date(pixData.expirationDate).toLocaleString('pt-BR') : 'Hoje às 23:59' }}</p>
                </div>
                <div class="flex items-start gap-2 text-xs text-[#435574] mt-2">
                  <Info class="h-4 w-4 text-[#286AD8]" />
                  <p>O pagamento será confirmado automaticamente após a aprovação</p>
                </div>
              </div>

              <!-- Payment Error -->
              <div v-if="paymentError" class="mt-4 p-4 bg-red-50 rounded-lg border-2 border-red-300">
                <div class="flex items-center gap-2">
                  <svg class="h-5 w-5 text-red-600" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 8v4m0 4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
                  </svg>
                  <p class="text-sm text-red-800 font-semibold">
                    {{ paymentError }}
                  </p>
                </div>
                <Button
                  @click="() => { paymentError = null; reloadPixQrCode(); }"
                  class="mt-3 bg-red-600 hover:bg-red-700 text-white"
                  size="sm"
                >
                  Tentar Novamente
                </Button>
              </div>

              <!-- Payment Confirmed -->
              <div v-if="paymentConfirmed" class="mt-4 p-4 bg-green-100 rounded-lg border-2 border-green-400 animate-pulse">
                <div class="flex items-center gap-2">
                  <CheckCircle class="h-5 w-5 text-green-700" />
                  <p class="text-sm text-green-800 font-bold">
                    Pagamento Confirmado!
                  </p>
                </div>
                <p class="text-xs text-green-700 mt-2">
                  Redirecionando para confirmação...
                </p>
              </div>
              <!-- BOTÃO DE TESTE - REMOVER EM PRODUÇÃO -->
              <div v-if="!paymentConfirmed" class="mt-4 p-3 bg-yellow-100 border-2 border-yellow-400 border-dashed rounded-lg">
                <p class="text-xs text-yellow-800 font-bold mb-2 flex items-center gap-2">
                  <AlertTriangle class="h-4 w-4 text-yellow-700" />
                  MODO DE TESTE (REMOVER EM PRODUÇÃO)
                </p>
                <Button
                  @click="simulatePaymentConfirmation"
                  :disabled="isSimulatingPayment"
                  class="w-full bg-yellow-500 hover:bg-yellow-600 text-yellow-900"
                  size="sm"
                >
                  <Loader2 v-if="isSimulatingPayment" class="h-4 w-4 mr-2 animate-spin" />
                  <template v-else>
                    <Zap class="h-4 w-4 mr-2" />
                  </template>
                  {{ isSimulatingPayment ? 'Simulando...' : 'Simular Confirmação do Pagamento' }}
                </Button>
              </div>
              <!-- FIM DO BOTÃO DE TESTE -->
            </div>

            <!-- Histórico da Solicitação -->
            <div class="mt-8 pt-6 border-t border-gray-200">
              <ServiceRequestTimeline
                :request-id="props.requestId"
                title="Histórico da Solicitação"
              />
            </div>
          </div>
        </Card>
      </div>

      <!-- Order Summary -->
      <div class="lg:col-span-1">
        <Card class="p-6 sticky top-6">
          <h3 class="text-[#263238] mb-4">Resumo do Pedido</h3>

          <!-- Freelancer Info -->
          <div class="flex items-center gap-3 mb-4">
            <Avatar class="h-12 w-12">
              <AvatarImage :src="proposal.freelancerAvatar" :alt="proposal.freelancerName" />
              <AvatarFallback class="bg-[#286AD8] text-white">
                {{ getInitials(proposal.freelancerName) }}
              </AvatarFallback>
            </Avatar>
            <div class="flex-1 min-w-0">
              <p class="text-[#263238] truncate">{{ proposal.freelancerName }}</p>
              <div class="flex items-center gap-1 text-sm text-[#435574]">
                <Star class="h-4 w-4 text-yellow-500" />
                <span>{{ proposal.freelancerRating.toFixed(1) }}</span>
              </div>
            </div>
          </div>

          <Separator class="my-4" />

          <!-- Service Details -->
          <div class="space-y-3 mb-4">
            <div class="flex items-start gap-2">
              <FileText class="h-4 w-4 text-[#435574] mt-0.5 flex-shrink-0" />
              <div class="flex-1 min-w-0">
                <p class="text-sm text-[#435574]">Serviço</p>
                <p class="text-[#263238]">{{ proposal.service }}</p>
              </div>
            </div>

            <div class="flex items-start gap-2">
              <Clock class="h-4 w-4 text-[#435574] mt-0.5 flex-shrink-0" />
              <div class="flex-1 min-w-0">
                <p class="text-sm text-[#435574]">Prazo de entrega</p>
                <p class="text-[#263238]">{{ proposal.deliveryTime }}</p>
              </div>
            </div>
          </div>

          <Separator class="my-4" />

          <!-- Price Breakdown -->
          <div class="space-y-2 mb-4">
            <div class="flex justify-between text-[#435574]">
              <span>Valor do serviço</span>
              <span>R$ {{ proposal.price.toFixed(2) }}</span>
            </div>
            <div class="flex justify-between text-[#435574]">
              <span>Taxa da plataforma (10%)</span>
              <span>R$ {{ platformFee.toFixed(2) }}</span>
            </div>
          </div>

          <Separator class="my-4" />

          <!-- Total -->
          <div class="flex justify-between">
            <span class="text-[#263238]">Total</span>
            <span class="text-[#263238]">R$ {{ totalAmount.toFixed(2) }}</span>
          </div>
        </Card>
      </div>
    </div>
  </div>
</template>
