<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { useReviewStore } from '@/stores/reviewStore';
import { useServiceRequestWebSocket } from '@/composables/useServiceRequestWebSocket';
import MainLayout from '@/components/layout/MainLayout.vue';
import RatingForm from '@/components/shared/RatingForm.vue';
import Button from '@/components/ui/button.vue';
import { CheckCircle2 } from 'lucide-vue-next';
import { useNavigation } from '@/utils/navigation';
import { toast } from '@/utils/toast';
import api from '@/api/config/axios';

const route = useRoute();
const router = useRouter();
const reviewStore = useReviewStore();
const { navigate, logout } = useNavigation();

const user = ref<any>(null);
const requestId = computed(() => route.query.requestId as string);
const serviceRequest = ref<any>(null);
const alreadyReviewed = ref(false);
const isLoadingData = ref(true);

let wsConnection: any = null;

const clientTags = [
  { id: 'clear_communication', label: 'Comunicação Clara', icon: '💬' },
  { id: 'timely_payment', label: 'Pagamento Pontual', icon: '💰' },
  { id: 'respectful', label: 'Respeito', icon: '🤝' },
  { id: 'clear_requirements', label: 'Clareza nos Requisitos', icon: '📋' },
  { id: 'constructive_feedback', label: 'Feedback Construtivo', icon: '💡' },
  { id: 'responsive', label: 'Responsivo', icon: '⚡' },
  { id: 'flexible', label: 'Flexível', icon: '🔄' },
  { id: 'professional', label: 'Profissional', icon: '👔' },
];

const clientName = computed(() => {
  return serviceRequest.value?.contractorName || 'Cliente';
});

const clientAvatar = computed(() => {
  return serviceRequest.value?.contractorAvatar;
});

const serviceName = computed(() => {
  return serviceRequest.value?.serviceName || 'Serviço';
});

const handleSubmit = async (review: { rating: number; comment: string; tags: string[] }) => {
  if (!requestId.value) return;

  await reviewStore.submitReview(requestId.value, {
    rating: review.rating,
    comment: review.comment,
    tags: review.tags,
  });
  reviewStore.removePendingReview(parseInt(requestId.value));
  toast.success('Avaliação enviada com sucesso!');
};

const handleBack = () => {
  router.push('/freelancer-deliveries');
};

const handleNavigate = (page: string, params?: any) => {
  navigate(page, 'provider', params);
};

const handleLogout = () => {
  logout();
};

const handleSearch = (query: string) => {
  router.push({ path: '/search', query: { q: query } });
};

const getUserEmail = () => {
  const token = localStorage.getItem('token');
  if (!token) return null;
  try {
    const payload = JSON.parse(atob(token.split('.')[1]));
    return payload.sub || payload.email;
  } catch {
    return null;
  }
};

const handleNotification = (event: any) => {
  const notification = event.detail;
  if (notification.type === 'NEW_REVIEW' && notification.serviceRequestId === parseInt(requestId.value)) {
    toast.info('O cliente também avaliou este serviço!');
  }
};

onMounted(async () => {
  const userData = localStorage.getItem('user');
  if (userData) {
    user.value = JSON.parse(userData);
    if (user.value.type !== 'provider') {
      router.push('/cliente-home');
      return;
    }
  } else {
    router.push('/login');
    return;
  }

  if (!requestId.value) {
    toast.error('ID da solicitação não fornecido');
    router.push('/freelancer-deliveries');
    return;
  }

  try {
    const response = await api.get(`/service-requests/${requestId.value}`);
    serviceRequest.value = response.data;
  } catch {
    toast.error('Erro ao carregar dados do serviço');
  }

  const canReviewResult = await reviewStore.checkCanReview(requestId.value);
  if (!canReviewResult) {
    const existing = await reviewStore.getMyReview(requestId.value);
    if (existing) {
      alreadyReviewed.value = true;
    }
  }

  isLoadingData.value = false;

  const userEmail = getUserEmail();
  if (userEmail) {
    wsConnection = useServiceRequestWebSocket(userEmail);
    wsConnection.connect();
    window.addEventListener('service-request-notification', handleNotification);
  }
});

onUnmounted(() => {
  window.removeEventListener('service-request-notification', handleNotification);
  if (wsConnection) {
    wsConnection.disconnect();
  }
});
</script>

<template>
  <MainLayout
    v-if="user"
    :user="user"
    current-page="freelancer-deliveries"
    :on-navigate="handleNavigate"
    :on-logout="handleLogout"
    :on-search="handleSearch"
  >
    <div v-if="isLoadingData" class="flex justify-center py-12">
      <div class="animate-spin h-8 w-8 border-4 border-[#286AD8] border-t-transparent rounded-full"></div>
    </div>

    <div v-else-if="alreadyReviewed" class="max-w-2xl mx-auto py-12">
      <div class="bg-white rounded-lg border border-gray-200 p-8 text-center space-y-4">
        <div class="bg-green-100 rounded-full p-6 inline-block">
          <CheckCircle2 class="h-16 w-16 text-green-600" />
        </div>
        <h2 class="text-xl font-semibold text-[#263238]">Você já avaliou este cliente</h2>
        <p class="text-[#435574]">Sua avaliação já foi registrada. Obrigado!</p>
        <Button variant="default" @click="handleBack">
          Voltar para Entregas
        </Button>
      </div>
    </div>

    <div v-else class="p-4">
      <RatingForm
        title="Avaliar Cliente"
        :subtitle="serviceName"
        :person-name="clientName"
        :person-avatar="clientAvatar"
        person-label="Como foi sua experiência com este cliente?"
        :tags="clientTags"
        tags-title="Pontos Fortes (opcional)"
        tags-description="Selecione as características que se destacaram neste cliente"
        comment-placeholder="Conte como foi trabalhar com este cliente..."
        comment-description="Compartilhe detalhes sobre sua experiência com este cliente"
        success-title="Avaliação Enviada!"
        success-message="Obrigado por avaliar o cliente"
        back-button-text="Voltar para Entregas"
        :on-back="handleBack"
        :on-submit="handleSubmit"
      />
    </div>
  </MainLayout>
</template>
