<script setup lang="ts">
import { ref, onMounted } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import CheckoutPage from '@/components/payment/CheckoutPage.vue';
import { paymentService } from '@/api/services/paymentService';
import { ArrowLeft } from 'lucide-vue-next';
import Button from '@/components/ui/button.vue';
import api from '@/api/config/axios';

const route = useRoute();
const router = useRouter();

const requestId = ref<string>('');
const proposalData = ref<any>(null);
const isLoading = ref(true);
const error = ref<string | null>(null);

onMounted(async () => {
  requestId.value = route.query.requestId as string;

  if (!requestId.value) {
    error.value = 'ID da solicitação não encontrado';
    isLoading.value = false;
    return;
  }

  try {
    // Buscar dados da proposta aceita
    const response = await api.get(`/service-requests/${requestId.value}`);
    const data = response.data;

    // Verificar se a proposta foi aceita
    if (data.status !== 'ACCEPTED') {
      error.value = 'Esta proposta ainda não foi aceita ou já foi processada';
      isLoading.value = false;
      return;
    }

    // Montar dados da proposta para o checkout
    proposalData.value = {
      freelancerId: data.freelancerId,
      freelancerName: data.freelancerName,
      freelancerAvatar: data.freelancerAvatar,
      freelancerRating: 4.8,
      service: data.serviceName,
      description: data.description,
      price: parseFloat(data.responseValue || data.counterValue || data.budget),
      deliveryTime: `${data.responseDays || data.counterDays || 7} dias`,
    };

    isLoading.value = false;
  } catch (err) {
    console.error('Erro ao carregar proposta:', err);
    error.value = 'Erro ao carregar dados da proposta';
    isLoading.value = false;
  }
});

const handleBack = () => {
  router.push('/my-requests');
};

const handleConfirmPayment = async (paymentMethod: string, paymentData: any) => {
  try {
    const payment = await paymentService.createPayment({
      serviceRequestId: requestId.value,
      paymentMethod: paymentMethod as 'pix' | 'credit-card' | 'wallet',
      amount: paymentData.amount,
      cardData: paymentData.cardData,
    });

    // Redirecionar para página de sucesso
    router.push({
      name: 'payment-success',
      query: { paymentId: payment.id },
    });
  } catch (err) {
    console.error('Erro ao processar pagamento:', err);
    alert('Erro ao processar pagamento. Tente novamente.');
  }
};
</script>

<template>
  <div class="min-h-screen bg-gray-50 py-8 px-4">
    <!-- Loading State -->
    <div v-if="isLoading" class="max-w-6xl mx-auto">
      <div class="flex items-center justify-center py-20">
        <div class="text-center">
          <div class="animate-spin h-12 w-12 border-4 border-[#286AD8] border-t-transparent rounded-full mx-auto mb-4" />
          <p class="text-[#435574]">Carregando dados do checkout...</p>
        </div>
      </div>
    </div>

    <!-- Error State -->
    <div v-else-if="error" class="max-w-6xl mx-auto">
      <div class="bg-white rounded-lg shadow-sm p-8 text-center">
        <div class="w-16 h-16 bg-red-100 rounded-full flex items-center justify-center mx-auto mb-4">
          <ArrowLeft class="h-8 w-8 text-red-600" />
        </div>
        <h2 class="text-2xl font-bold text-[#263238] mb-2">Ops!</h2>
        <p class="text-[#435574] mb-6">{{ error }}</p>
        <Button @click="handleBack" class="bg-[#286AD8] hover:bg-[#435574]">
          Voltar para Minhas Solicitações
        </Button>
      </div>
    </div>

    <!-- Checkout Page -->
    <CheckoutPage
      v-else-if="proposalData"
      :request-id="requestId"
      :proposal="proposalData"
      :on-back="handleBack"
      :on-confirm-payment="handleConfirmPayment"
    />
  </div>
</template>
