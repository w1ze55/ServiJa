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
import PaymentTimeline from './PaymentTimeline.vue';

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
const { connected: wsConnected, connect: connectWS, disconnect: disconnectWS } = usePaymentWebSocket(userEmail);

const paymentMethod = ref<string>('');
const isProcessing = ref(false);
const isLoadingPix = ref(false);
const showPixQrCode = ref(false);
const pixData = ref<{ qrCodeImage?: string; payload?: string; expirationDate?: string } | null>(null);
const pixCpf = ref<string>('');
const currentPaymentId = ref<number | null>(null);
const paymentConfirmed = ref(false);
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

const copyPixCode = () => {
  if (pixData.value?.payload) {
    navigator.clipboard.writeText(pixData.value.payload);
    alert('Código PIX copiado!');
  }
};

// Gerar QR Code PIX automaticamente quando o método PIX for selecionado
watch(paymentMethod, async (newMethod) => {
  if (newMethod === 'pix') {
    // Não gerar automaticamente, aguardar o CPF
    showPixQrCode.value = false;
    pixData.value = null;
    pixCpf.value = '';
  } else {
    // Limpar dados do PIX quando trocar de método
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
        console.log('✅ QR Code PIX recarregado com sucesso');
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
        console.log('ℹ️ Pagamento já existe, buscando...');
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

// Handler para notificações de pagamento via WebSocket
const handlePaymentNotification = (event: Event) => {
  const customEvent = event as CustomEvent;
  const notification = customEvent.detail;

  console.log('🔔 Notificação de pagamento recebida:', notification);

  if (notification.type === 'PIX_CONFIRMED' && notification.paymentId === currentPaymentId.value) {
    paymentConfirmed.value = true;

    // Redirecionar para página de sucesso após 2 segundos
    setTimeout(() => {
      router.push(`/payment-success?paymentId=${notification.paymentId}`);
    }, 2000);
  }
};

// Lifecycle: Conectar WebSocket quando montar
onMounted(async () => {
  if (userEmail) {
    console.log('🔌 Conectando WebSocket para:', userEmail);
    connectWS();
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
      console.log('💳 Pagamento existente encontrado:', payment);
      
      // Se o pagamento já foi completado, marcar como confirmado e não redirecionar
      if (payment.status === 'COMPLETED') {
        console.log('✅ Pagamento já foi completado anteriormente');
        paymentConfirmed.value = true;
        currentPaymentId.value = payment.id;
        paymentMethod.value = payment.paymentMethod;
        return;
      }
      
      // Se o pagamento ainda não foi completado, carregar os dados
      if (payment.status === 'PENDING' || payment.status === 'PROCESSING') {
        currentPaymentId.value = payment.id;
        paymentMethod.value = payment.paymentMethod.toLowerCase();
        
        // Se for PIX, carregar QR Code
        if (paymentMethod.value === 'pix' && payment.pixQrCode) {
          pixData.value = {
            qrCodeImage: payment.pixQrCode,
            payload: payment.pixCopyPaste
          };
          showPixQrCode.value = true;
          console.log('✅ QR Code PIX carregado do pagamento existente');
        }
      }
    }
  } catch (error) {
    // Se não encontrar pagamento, continuar normalmente
    console.log('ℹ️ Nenhum pagamento existente encontrado');
  }
};

// Lifecycle: Desconectar quando desmontar
onUnmounted(() => {
  console.log('🔌 Desconectando WebSocket');
  disconnectWS();
  window.removeEventListener('payment-notification', handlePaymentNotification);
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
      <!-- WebSocket Status -->
      <div v-if="paymentMethod === 'pix' && showPixQrCode" class="lg:col-span-2">
        <div class="mb-4 p-3 bg-gray-50 rounded-lg border flex items-center gap-2">
          <div :class="wsConnected ? 'bg-green-500' : 'bg-gray-400'" class="w-2 h-2 rounded-full animate-pulse"></div>
          <p class="text-xs text-[#435574]">
            {{ wsConnected ? '🟢 Conectado - Aguardando confirmação automática' : '⚪ Desconectado - Reconectando...' }}
          </p>
        </div>
      </div>

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
              <h3 class="text-[#263238] text-xl font-bold mb-2">✅ Pagamento Já Realizado!</h3>
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

          <!-- PIX Form -->
          <div v-if="paymentMethod === 'pix'" class="mt-6 space-y-4 p-4 bg-blue-50 rounded-lg border border-blue-200">
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
          <h3 class="text-[#263238] mb-4">✅ QR Code PIX Gerado</h3>
          
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
              <p class="text-sm text-[#263238] font-semibold">
                📱 Escaneie o QR Code com seu app de pagamento
              </p>
              <p class="text-xs text-[#435574]">Ou copie o código PIX abaixo</p>

              <!-- PIX Copy/Paste Code -->
              <div class="flex gap-2 max-w-md mx-auto">
                <Input
                  :value="pixData.payload"
                  readonly
                  class="text-xs font-mono flex-1 bg-gray-100"
                />
                <Button
                  @click="copyPixCode"
                  variant="outline"
                  size="sm"
                  class="shrink-0 bg-white"
                >
                  📋 Copiar
                </Button>
              </div>

              <!-- Expiration Info -->
              <div class="mt-4 p-3 bg-blue-50 rounded-lg border border-blue-200">
                <p class="text-xs text-[#435574] font-medium">
                  ⏱️ Válido até: {{ pixData.expirationDate ? new Date(pixData.expirationDate).toLocaleString('pt-BR') : 'Hoje às 23:59' }}
                </p>
                <p class="text-xs text-[#435574] mt-1">
                  💡 O pagamento será confirmado automaticamente após a aprovação
                </p>
              </div>

              <!-- Payment Value -->
              <div class="mt-4 p-4 bg-green-50 rounded-lg border-2 border-green-300">
                <p class="text-base text-green-800 font-bold">
                  💰 Valor a pagar: R$ {{ totalAmount.toFixed(2).replace('.', ',') }}
                </p>
              </div>

              <!-- Waiting for Payment -->
              <div v-if="!paymentConfirmed" class="mt-4 p-4 bg-yellow-50 rounded-lg border-2 border-yellow-300">
                <div class="flex items-center gap-2">
                  <Loader2 class="h-4 w-4 text-yellow-700 animate-spin" />
                  <p class="text-sm text-yellow-800 font-semibold">
                    Aguardando confirmação do pagamento...
                  </p>
                </div>
                <p class="text-xs text-yellow-700 mt-2">
                  Assim que o pagamento for confirmado, você será notificado automaticamente!
                </p>
              </div>

              <!-- Payment Confirmed -->
              <div v-if="paymentConfirmed" class="mt-4 p-4 bg-green-100 rounded-lg border-2 border-green-400 animate-pulse">
                <div class="flex items-center gap-2">
                  <CheckCircle class="h-5 w-5 text-green-700" />
                  <p class="text-sm text-green-800 font-bold">
                    ✅ Pagamento Confirmado!
                  </p>
                </div>
                <p class="text-xs text-green-700 mt-2">
                  Redirecionando para confirmação...
                </p>
              </div>

              <!-- Generate New QR Code Button -->
              <Button
                v-if="!paymentConfirmed"
                @click="() => { showPixQrCode = false; pixData = null; pixCpf = ''; currentPaymentId = null; }"
                variant="ghost"
                class="mt-4 text-[#435574]"
                size="sm"
              >
                🔄 Gerar Novo QR Code
              </Button>
            </div>

            <!-- Payment Timeline -->
            <div v-if="currentPaymentId" class="mt-8 pt-6 border-t border-gray-200">
              <h4 class="text-lg font-semibold text-[#263238] mb-4">📋 Status do Pagamento</h4>
              <PaymentTimeline :payment-id="currentPaymentId" />
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
                <span>⭐</span>
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
          <div class="flex justify-between mb-6">
            <span class="text-[#263238]">Total</span>
            <span class="text-[#263238]">R$ {{ totalAmount.toFixed(2) }}</span>
          </div>

          <!-- Confirm Button -->
          <Button
            @click="handleConfirmPayment"
            :disabled="!paymentMethod || isProcessing"
            class="w-full bg-[#286AD8] hover:bg-[#435574] transition-colors"
            size="lg"
          >
            <span v-if="isProcessing" class="flex items-center gap-2">
              <span class="animate-spin h-4 w-4 border-2 border-white border-t-transparent rounded-full" />
              Processando...
            </span>
            <span v-else>Confirmar Pagamento</span>
          </Button>

          <p class="text-xs text-center text-[#435574] mt-4">
            Ao confirmar, você concorda com os
            <a href="#" class="text-[#286AD8] hover:underline">Termos de Serviço</a>
            e
            <a href="#" class="text-[#286AD8] hover:underline">Política de Privacidade</a>
          </p>
        </Card>
      </div>
    </div>
  </div>
</template>




