<script setup lang="ts">
import { ref, computed } from 'vue';
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

const props = defineProps<Props>();

const paymentMethod = ref<string>('');
const isProcessing = ref(false);
const cardData = ref({
  number: '',
  name: '',
  expiry: '',
  cvv: '',
});

const platformFee = computed(() => props.proposal.price * 0.1); // 10% taxa da plataforma
const totalAmount = computed(() => props.proposal.price + platformFee.value);

const handleConfirmPayment = async () => {
  if (!paymentMethod.value) {
    alert('Selecione um método de pagamento');
    return;
  }

  isProcessing.value = true;

  // Simular processamento de pagamento
  setTimeout(() => {
    props.onConfirmPayment(paymentMethod.value, {
      cardData: paymentMethod.value === 'credit-card' ? cardData.value : null,
      requestId: props.requestId,
      amount: totalAmount.value,
    });
    isProcessing.value = false;
  }, 2000);
};

const getInitials = (name: string) => {
  return name
    .split(' ')
    .map((n) => n[0])
    .join('')
    .toUpperCase()
    .slice(0, 2);
};
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
        <!-- Security Badge -->
        <div class="bg-blue-50 border border-blue-200 rounded-lg p-4 flex items-center gap-3">
          <Shield class="h-5 w-5 text-[#286AD8]" />
          <div>
            <p class="text-sm text-[#263238]">Pagamento 100% seguro</p>
            <p class="text-xs text-[#435574]">Conexão criptografada e dados protegidos</p>
          </div>
        </div>

        <!-- Payment Method Selection -->
        <Card class="p-6">
          <h3 class="text-[#263238] mb-4">Método de Pagamento</h3>

          <RadioGroup :model-value="paymentMethod" @update:model-value="(val) => paymentMethod = val as string">
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

          <!-- Credit Card Form -->
          <div v-if="paymentMethod === 'credit-card'" class="mt-6 space-y-4 p-4 bg-gray-50 rounded-lg">
            <div class="space-y-2">
              <Label for="card-number">Número do cartão</Label>
              <Input
                id="card-number"
                placeholder="0000 0000 0000 0000"
                :model-value="cardData.number"
                @update:model-value="(val) => cardData.number = val as string"
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
                  @update:model-value="(val) => cardData.expiry = val as string"
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

          <!-- PIX QR Code -->
          <div v-if="paymentMethod === 'pix'" class="mt-6 p-6 bg-gray-50 rounded-lg text-center">
            <div class="w-48 h-48 bg-white rounded-lg mx-auto mb-4 flex items-center justify-center">
              <QrCode class="h-32 w-32 text-gray-400" />
            </div>
            <p class="text-sm text-[#435574] mb-2">Escaneie o QR Code com seu app de pagamento</p>
            <p class="text-xs text-[#435574]">Ou copie o código PIX abaixo</p>
            <div class="mt-4">
              <Input
                value="00020126580014br.gov.bcb.pix..."
                readonly
                class="text-center text-xs"
              />
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




