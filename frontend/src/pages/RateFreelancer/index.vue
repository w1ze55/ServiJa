<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { useClientRequestStore } from '@/stores/clientRequestStore';
import { useReviewStore } from '@/stores/reviewStore';
import { useServiceRequestWebSocket } from '@/composables/useServiceRequestWebSocket';
import RateFreelancerPage from '@/components/service/RateFreelancerPage.vue';
import MainLayout from '@/components/layout/MainLayout.vue';
import Button from '@/components/ui/button.vue';
import { useNavigation } from '@/utils/navigation';
import { toast } from '@/utils/toast';

const route = useRoute();
const router = useRouter();
const store = useClientRequestStore();
const reviewStore = useReviewStore();
const { navigate, logout } = useNavigation();

const user = ref<any>(null);
const requestId = computed(() => route.query.requestId as string);
const alreadyReviewed = ref(false);
const canReview = ref(true);

let wsConnection: any = null;

const request = computed(() => {
  if (!requestId.value) return null;
  return store.getRequestById(requestId.value);
});

const handleBack = () => {
  router.push('/cliente-projects');
};

const handleSubmitReview = async (review: { rating: number; comment: string; tags: string[] }) => {
  if (!requestId.value) return;

  try {
    await reviewStore.submitReview(requestId.value, {
      rating: review.rating,
      comment: review.comment,
      tags: review.tags,
    });
    reviewStore.removePendingReview(parseInt(requestId.value));
    toast.success('Avaliação enviada com sucesso!');
  } catch (err: any) {
    const message = err.response?.data?.message || 'Erro ao enviar avaliação.';
    toast.error(message);
  }
};

const handleNavigate = (page: string, params?: any) => {
  navigate(page, 'contractor', params);
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
    toast.info('O freelancer também avaliou este serviço!');
  }
};

onMounted(async () => {
  const userData = localStorage.getItem('user');
  if (userData) {
    user.value = JSON.parse(userData);
    if (user.value.type !== 'contractor') {
      router.push('/freelancer-dashboard');
      return;
    }
  } else {
    router.push('/login');
    return;
  }

  if (!requestId.value) {
    toast.error('ID da solicitação não fornecido');
    router.push('/cliente-projects');
    return;
  }

  if (store.requests.length === 0) {
    await store.loadRequests();
  }

  const canReviewResult = await reviewStore.checkCanReview(requestId.value);
  canReview.value = canReviewResult;

  if (!canReviewResult) {
    const existing = await reviewStore.getMyReview(requestId.value);
    if (existing) {
      alreadyReviewed.value = true;
    }
  }

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
    current-page="cliente-projects"
    :on-navigate="handleNavigate"
    :on-logout="handleLogout"
    :on-search="handleSearch"
  >
    <div v-if="store.isLoading" class="flex justify-center py-12">
      <div class="animate-spin h-8 w-8 border-4 border-[#286AD8] border-t-transparent rounded-full"></div>
    </div>

    <div v-else-if="alreadyReviewed" class="max-w-2xl mx-auto py-12">
      <div class="bg-white rounded-lg border border-gray-200 p-8 text-center space-y-4">
        <div class="bg-green-100 rounded-full p-6 inline-block">
          <svg class="h-16 w-16 text-green-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M5 13l4 4L19 7" />
          </svg>
        </div>
        <h2 class="text-xl font-semibold text-[#263238]">Você já avaliou este serviço</h2>
        <p class="text-[#435574]">Sua avaliação já foi registrada. Obrigado!</p>
        <Button variant="default" @click="handleBack">
          Voltar para Meus Projetos
        </Button>
      </div>
    </div>

    <div v-else-if="!request" class="max-w-4xl mx-auto p-6">
      <div class="text-center py-12">
        <h2 class="text-xl font-semibold text-[#263238] mb-2">Solicitação não encontrada</h2>
        <p class="text-[#435574] mb-4">A solicitação que você está procurando não existe ou foi removida.</p>
        <Button variant="default" @click="handleBack">
          Voltar para Meus Projetos
        </Button>
      </div>
    </div>

    <RateFreelancerPage
      v-else
      :service-id="request.id"
      :service-name="request.service"
      :freelancer-name="request.freelancerName || 'Freelancer'"
      :freelancer-avatar="request.freelancerAvatar"
      :on-back="handleBack"
      :on-submit-review="handleSubmitReview"
    />
  </MainLayout>
</template>
