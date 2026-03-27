<script setup lang="ts">
import { ref, onMounted } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { CheckCircle, Download, Loader2 } from 'lucide-vue-next';
import Button from '@/components/ui/button.vue';
import Card from '@/components/ui/card.vue';
import Separator from '@/components/ui/separator.vue';
import { paymentService } from '@/api/services/paymentService';

const route = useRoute();
const router = useRouter();

const paymentId = ref<string>('');
const paymentData = ref<any>(null);
const isLoading = ref(true);
const isDownloadingReceipt = ref(false);

onMounted(async () => {
  paymentId.value = route.query.paymentId as string;

  if (!paymentId.value) {
    router.push('/my-requests');
    return;
  }

  try {
    const payment = await paymentService.getPayment(paymentId.value);
    paymentData.value = payment;
  } catch (err) {
    console.error('Erro ao carregar dados do pagamento:', err);
  } finally {
    isLoading.value = false;
  }
});

const handleBackToDashboard = () => {
  router.push('/cliente-dashboard');
};

const handleDownloadReceipt = async () => {
  isDownloadingReceipt.value = true;
  try {
    const receiptData = await paymentService.getReceiptUrl(paymentId.value);
    const url = receiptData.transactionReceiptUrl || receiptData.invoiceUrl;

    if (url) {
      window.open(url, '_blank');
    } else {
      alert('O recibo ainda não está disponível para este pagamento. Tente novamente em alguns instantes.');
    }
  } catch (err) {
    console.error('Erro ao obter recibo:', err);
    alert('Erro ao obter recibo. Tente novamente.');
  } finally {
    isDownloadingReceipt.value = false;
  }
};
</script>

<template>
  <div class="min-h-screen bg-gray-50 py-12 px-4">
    <div class="max-w-2xl mx-auto">
      <!-- Loading -->
      <div v-if="isLoading" class="text-center py-20">
        <div class="animate-spin h-12 w-12 border-4 border-[#286AD8] border-t-transparent rounded-full mx-auto mb-4" />
        <p class="text-[#435574]">Carregando...</p>
      </div>

      <!-- Success Content -->
      <Card v-else class="p-8 text-center">
        <!-- Success Icon -->
        <div class="w-20 h-20 bg-green-100 rounded-full flex items-center justify-center mx-auto mb-6">
          <CheckCircle class="h-12 w-12 text-green-600" />
        </div>

        <!-- Title -->
        <h1 class="text-3xl font-bold text-[#263238] mb-3">Pagamento Confirmado!</h1>
        <p class="text-[#435574] mb-8">
          Seu pagamento foi processado com sucesso. O prestador foi notificado e em breve entrará em contato.
        </p>

        <Separator class="my-8" />

        <!-- Payment Details -->
        <div v-if="paymentData" class="text-left space-y-3 mb-8">
          <h3 class="font-semibold text-[#263238] mb-4">Detalhes do Pagamento</h3>
          
          <div class="flex justify-between text-sm">
            <span class="text-[#435574]">ID da Transação:</span>
            <span class="text-[#263238] font-mono">{{ paymentData.id }}</span>
          </div>
          
          <div class="flex justify-between text-sm">
            <span class="text-[#435574]">Método de Pagamento:</span>
            <span class="text-[#263238]">{{ paymentData.paymentMethod || 'PIX' }}</span>
          </div>
          
          <div class="flex justify-between text-sm">
            <span class="text-[#435574]">Valor Total:</span>
            <span class="text-[#263238] font-semibold">R$ {{ paymentData.amount?.toFixed(2) || '0.00' }}</span>
          </div>
          
          <div class="flex justify-between text-sm">
            <span class="text-[#435574]">Data:</span>
            <span class="text-[#263238]">{{ new Date().toLocaleDateString('pt-BR') }}</span>
          </div>
        </div>

        <Separator class="my-8" />

        <!-- Next Steps -->
        <div class="bg-blue-50 rounded-lg p-6 mb-8 text-left">
          <h3 class="font-semibold text-[#263238] mb-3">Próximos Passos</h3>
          <ul class="space-y-2 text-sm text-[#435574]">
            <li class="flex items-start gap-2">
              <span class="text-[#286AD8] mt-0.5">•</span>
              <span>O prestador foi notificado e receberá os detalhes do serviço</span>
            </li>
            <li class="flex items-start gap-2">
              <span class="text-[#286AD8] mt-0.5">•</span>
              <span>Você pode acompanhar o progresso na área de acompanhamento do serviço</span>
            </li>
            <li class="flex items-start gap-2">
              <span class="text-[#286AD8] mt-0.5">•</span>
              <span>Entre em contato com o prestador através do chat se necessário</span>
            </li>
          </ul>
        </div>

        <!-- Actions -->
        <div class="space-y-3">
          <Button
            @click="handleDownloadReceipt"
            :disabled="isDownloadingReceipt"
            class="w-full bg-[#286AD8] hover:bg-[#435574]"
            size="lg"
          >
            <Loader2 v-if="isDownloadingReceipt" class="h-5 w-5 mr-2 animate-spin" />
            <Download v-else class="h-5 w-5 mr-2" />
            {{ isDownloadingReceipt ? 'Obtendo recibo...' : 'Baixar Recibo' }}
          </Button>

          <Button
            @click="handleBackToDashboard"
            variant="outline"
            class="w-full"
            size="lg"
          >
            Voltar ao Dashboard
          </Button>
        </div>
      </Card>
    </div>
  </div>
</template>
