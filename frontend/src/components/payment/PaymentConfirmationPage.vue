<script setup lang="ts">
import { CheckCircle2, XCircle, Download, ArrowRight, Mail } from 'lucide-vue-next';
import Button from '../ui/button.vue';
import Card from '../ui/card.vue';
import Separator from '../ui/separator.vue';
import Badge from '../ui/badge.vue';

interface Props {
  success: boolean;
  transactionId?: string;
  paymentMethod?: string;
  amount?: number;
  serviceName?: string;
  freelancerName?: string;
  errorMessage?: string;
  onDownloadReceipt?: () => void;
  onGoToService?: () => void;
  onRetry?: () => void;
}

const props = defineProps<Props>();

const formatPaymentMethod = (method?: string) => {
  switch (method) {
    case 'pix':
      return 'PIX';
    case 'credit-card':
      return 'Cartão de Crédito';
    case 'wallet':
      return 'Carteira Digital';
    default:
      return method;
  }
};
</script>

<template>
  <div class="max-w-2xl mx-auto py-12">
    <Card class="p-8">
      <!-- Success State -->
      <div v-if="success" class="text-center space-y-6">
        <!-- Success Icon with Animation -->
        <div class="flex justify-center">
          <div class="relative">
            <div class="absolute inset-0 bg-green-100 rounded-full animate-ping opacity-75" />
            <div class="relative bg-green-100 rounded-full p-6">
              <CheckCircle2 class="h-16 w-16 text-green-600" />
            </div>
          </div>
        </div>

        <!-- Success Message -->
        <div>
          <h1 class="text-[#263238] mb-2">Pagamento Confirmado!</h1>
          <p class="text-[#435574]">Seu pagamento foi processado com sucesso</p>
        </div>

        <!-- Transaction Details -->
        <div class="bg-gray-50 rounded-lg p-6 text-left space-y-4">
          <div class="flex justify-between items-center">
            <span class="text-[#435574]">ID da Transação</span>
            <Badge v-if="transactionId" variant="secondary" class="font-mono text-xs">
              {{ transactionId }}
            </Badge>
          </div>

          <Separator />

          <div class="flex justify-between">
            <span class="text-[#435574]">Serviço</span>
            <span class="text-[#263238]">{{ serviceName }}</span>
          </div>

          <div class="flex justify-between">
            <span class="text-[#435574]">Freelancer</span>
            <span class="text-[#263238]">{{ freelancerName }}</span>
          </div>

          <div class="flex justify-between">
            <span class="text-[#435574]">Método de Pagamento</span>
            <span class="text-[#263238]">{{ formatPaymentMethod(paymentMethod) }}</span>
          </div>

          <Separator />

          <div class="flex justify-between">
            <span class="text-[#263238]">Valor Total</span>
            <span class="text-[#263238]">R$ {{ amount?.toFixed(2) }}</span>
          </div>
        </div>

        <!-- Email Notification -->
        <div class="bg-blue-50 border border-blue-200 rounded-lg p-4 flex items-start gap-3">
          <Mail class="h-5 w-5 text-[#286AD8] mt-0.5 flex-shrink-0" />
          <div class="text-left text-sm">
            <p class="text-[#263238]">Confirmação enviada por e-mail</p>
            <p class="text-[#435574]">Enviamos o recibo da transação para seu e-mail cadastrado</p>
          </div>
        </div>

        <!-- Actions -->
        <div class="space-y-3 pt-4">
          <Button
            v-if="onDownloadReceipt"
            @click="props.onDownloadReceipt"
            variant="outline"
            class="w-full border-[#286AD8] text-[#286AD8] hover:bg-blue-50"
            size="lg"
          >
            <Download class="h-4 w-4 mr-2" />
            Baixar Recibo
          </Button>

          <Button
            v-if="onGoToService"
            @click="props.onGoToService"
            class="w-full bg-[#286AD8] hover:bg-[#435574] transition-colors"
            size="lg"
          >
            Acompanhar Serviço
            <ArrowRight class="h-4 w-4 ml-2" />
          </Button>
        </div>

        <!-- Additional Info -->
        <div class="pt-4 border-t">
          <p class="text-sm text-[#435574]">
            O serviço foi marcado como <strong>"Em execução"</strong>.
            Você pode acompanhar o progresso e se comunicar com o freelancer na área de acompanhamento.
          </p>
        </div>
      </div>

      <!-- Error State -->
      <div v-else class="text-center space-y-6">
        <!-- Error Icon -->
        <div class="flex justify-center">
          <div class="bg-red-100 rounded-full p-6">
            <XCircle class="h-16 w-16 text-red-600" />
          </div>
        </div>

        <!-- Error Message -->
        <div>
          <h1 class="text-[#263238] mb-2">Falha no Pagamento</h1>
          <p class="text-[#435574]">Não foi possível processar seu pagamento</p>
        </div>

        <!-- Error Details -->
        <div class="bg-red-50 border border-red-200 rounded-lg p-4">
          <p class="text-sm text-red-800">
            {{
              errorMessage ||
              'Ocorreu um erro ao processar o pagamento. Por favor, verifique seus dados e tente novamente.'
            }}
          </p>
        </div>

        <!-- Common Issues -->
        <div class="bg-gray-50 rounded-lg p-6 text-left">
          <p class="text-sm text-[#263238] mb-3">Possíveis causas:</p>
          <ul class="text-sm text-[#435574] space-y-2">
            <li class="flex items-start gap-2">
              <span class="text-[#286AD8] mt-1">•</span>
              <span>Dados do cartão incorretos ou cartão vencido</span>
            </li>
            <li class="flex items-start gap-2">
              <span class="text-[#286AD8] mt-1">•</span>
              <span>Saldo insuficiente ou limite excedido</span>
            </li>
            <li class="flex items-start gap-2">
              <span class="text-[#286AD8] mt-1">•</span>
              <span>Problemas de conexão com a operadora</span>
            </li>
            <li class="flex items-start gap-2">
              <span class="text-[#286AD8] mt-1">•</span>
              <span>Transação bloqueada por segurança</span>
            </li>
          </ul>
        </div>

        <!-- Actions -->
        <div class="space-y-3 pt-4">
          <Button
            v-if="onRetry"
            @click="props.onRetry"
            class="w-full bg-[#286AD8] hover:bg-[#435574] transition-colors"
            size="lg"
          >
            Tentar Novamente
          </Button>

          <Button
            @click="() => window.history.back()"
            variant="outline"
            class="w-full"
            size="lg"
          >
            Voltar
          </Button>
        </div>

        <!-- Support -->
        <div class="pt-4 border-t">
          <p class="text-sm text-[#435574]">
            Precisa de ajuda?
            <a href="#" class="text-[#286AD8] hover:underline">Entre em contato com o suporte</a>
          </p>
        </div>
      </div>
    </Card>
  </div>
</template>




