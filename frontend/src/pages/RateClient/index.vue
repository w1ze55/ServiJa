<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { useReviewStore } from '@/stores/reviewStore';
import { useServiceRequestWebSocket } from '@/composables/useServiceRequestWebSocket';
import MainLayout from '@/components/layout/MainLayout.vue';
import Button from '@/components/ui/button.vue';
import Card from '@/components/ui/card.vue';
import Textarea from '@/components/ui/textarea.vue';
import Badge from '@/components/ui/badge.vue';
import Avatar from '@/components/ui/avatar.vue';
import AvatarFallback from '@/components/ui/avatar-fallback.vue';
import AvatarImage from '@/components/ui/avatar-image.vue';
import Separator from '@/components/ui/separator.vue';
import { ArrowLeft, Star, Send, AlertCircle, Sparkles, CheckCircle2 } from 'lucide-vue-next';
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

const availableTags = [
  { id: 'clear_communication', label: 'Comunicação Clara', icon: '💬' },
  { id: 'timely_payment', label: 'Pagamento Pontual', icon: '💰' },
  { id: 'respectful', label: 'Respeito', icon: '🤝' },
  { id: 'clear_requirements', label: 'Clareza nos Requisitos', icon: '📋' },
  { id: 'constructive_feedback', label: 'Feedback Construtivo', icon: '💡' },
  { id: 'responsive', label: 'Responsivo', icon: '⚡' },
  { id: 'flexible', label: 'Flexível', icon: '🔄' },
  { id: 'professional', label: 'Profissional', icon: '👔' },
];

const rating = ref(0);
const hoverRating = ref(0);
const comment = ref('');
const selectedTags = ref<string[]>([]);
const showPolicyAlert = ref(true);
const isSubmitting = ref(false);
const submitted = ref(false);

const getInitials = (name: string) => {
  return name
    .split(' ')
    .map((n) => n[0])
    .join('')
    .toUpperCase()
    .slice(0, 2);
};

const toggleTag = (tagId: string) => {
  if (selectedTags.value.includes(tagId)) {
    selectedTags.value = selectedTags.value.filter((t) => t !== tagId);
  } else {
    selectedTags.value = [...selectedTags.value, tagId];
  }
};

const getRatingLabel = (stars: number) => {
  switch (stars) {
    case 1: return 'Muito insatisfeito';
    case 2: return 'Insatisfeito';
    case 3: return 'Regular';
    case 4: return 'Satisfeito';
    case 5: return 'Muito satisfeito';
    default: return 'Selecione uma nota';
  }
};

const handleSubmit = async () => {
  if (rating.value === 0) {
    toast.error('Por favor, selecione uma nota de 1 a 5 estrelas');
    return;
  }

  if (!requestId.value) return;

  isSubmitting.value = true;
  try {
    await reviewStore.submitReview(requestId.value, {
      rating: rating.value,
      comment: comment.value,
      tags: selectedTags.value,
    });
    submitted.value = true;
    toast.success('Avaliação enviada com sucesso!');
  } catch (err: any) {
    const message = err.response?.data?.message || 'Erro ao enviar avaliação.';
    toast.error(message);
  } finally {
    isSubmitting.value = false;
  }
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

const clientName = computed(() => {
  return serviceRequest.value?.contractorName || 'Cliente';
});

const clientAvatar = computed(() => {
  return serviceRequest.value?.contractorAvatar;
});

const serviceName = computed(() => {
  return serviceRequest.value?.serviceName || 'Serviço';
});

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
      router.push('/cliente-dashboard');
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
        <Button @click="handleBack" class="bg-[#286AD8] hover:bg-[#435574]">
          Voltar para Entregas
        </Button>
      </div>
    </div>

    <div v-else-if="submitted" class="max-w-2xl mx-auto py-12">
      <Card class="p-8 text-center space-y-6">
        <div class="flex justify-center">
          <div class="relative">
            <div class="absolute inset-0 bg-green-100 rounded-full animate-ping opacity-75" />
            <div class="relative bg-green-100 rounded-full p-6">
              <CheckCircle2 class="h-16 w-16 text-green-600" />
            </div>
          </div>
        </div>

        <div>
          <h1 class="text-[#263238] mb-2">Avaliação Enviada!</h1>
          <p class="text-[#435574]">Obrigado por avaliar o cliente</p>
        </div>

        <div class="bg-gray-50 rounded-lg p-4 text-left">
          <p class="text-sm text-[#435574] mb-2">Sua avaliação:</p>
          <div class="flex items-center gap-2 mb-2">
            <Star
              v-for="star in [1, 2, 3, 4, 5]"
              :key="star"
              :class="`h-5 w-5 ${
                star <= rating ? 'fill-yellow-400 text-yellow-400' : 'text-gray-300'
              }`"
            />
            <span class="text-[#263238] ml-2">{{ getRatingLabel(rating) }}</span>
          </div>
          <div v-if="selectedTags.length > 0" class="flex flex-wrap gap-2 mb-2">
            <Badge
              v-for="tagId in selectedTags"
              :key="tagId"
              variant="secondary"
            >
              {{ availableTags.find((t) => t.id === tagId)?.icon }}
              {{ availableTags.find((t) => t.id === tagId)?.label }}
            </Badge>
          </div>
          <p v-if="comment" class="text-sm text-[#435574] mt-2 italic">"{{ comment }}"</p>
        </div>

        <Button @click="handleBack" class="w-full bg-[#286AD8] hover:bg-[#435574]" size="lg">
          Voltar para Entregas
        </Button>
      </Card>
    </div>

    <div v-else class="max-w-3xl mx-auto space-y-6 p-4">
      <!-- Header -->
      <div class="flex items-center gap-4">
        <Button @click="handleBack" variant="ghost" size="icon" class="hover:bg-gray-100">
          <ArrowLeft class="h-5 w-5" />
        </Button>
        <div class="flex-1">
          <h1 class="text-[#263238]">Avaliar Cliente</h1>
          <p class="text-[#435574]">{{ serviceName }}</p>
        </div>
      </div>

      <!-- Policy Alert -->
      <Card v-if="showPolicyAlert" class="p-4 bg-blue-50 border-blue-200">
        <div class="flex items-start gap-3">
          <AlertCircle class="h-5 w-5 text-[#286AD8] mt-0.5 flex-shrink-0" />
          <div class="flex-1">
            <p class="text-sm text-[#263238] mb-2">
              <strong>Política de Conduta e Respeito</strong>
            </p>
            <p class="text-sm text-[#435574] mb-3">
              Avaliações devem ser honestas e construtivas. Comentários ofensivos, discriminatórios
              ou que violem nossa política de conduta não serão permitidos.
            </p>
            <Button
              @click="showPolicyAlert = false"
              variant="ghost"
              size="sm"
              class="text-[#286AD8] hover:bg-blue-100"
            >
              Entendi
            </Button>
          </div>
        </div>
      </Card>

      <!-- Client Card -->
      <Card class="p-6">
        <div class="flex items-center gap-4">
          <Avatar class="h-16 w-16">
            <AvatarImage :src="clientAvatar" :alt="clientName" />
            <AvatarFallback class="bg-[#286AD8] text-white text-xl">
              {{ getInitials(clientName) }}
            </AvatarFallback>
          </Avatar>
          <div>
            <h3 class="text-[#263238]">{{ clientName }}</h3>
            <p class="text-sm text-[#435574]">Como foi sua experiência com este cliente?</p>
          </div>
        </div>
      </Card>

      <!-- Rating Section -->
      <Card class="p-6">
        <h3 class="text-[#263238] mb-4">Avaliação Geral</h3>

        <div class="flex flex-col items-center gap-4 py-6">
          <div class="flex gap-2">
            <button
              v-for="star in [1, 2, 3, 4, 5]"
              :key="star"
              @click="rating = star"
              @mouseenter="hoverRating = star"
              @mouseleave="hoverRating = 0"
              class="transition-transform hover:scale-110"
            >
              <Star
                :class="`h-12 w-12 transition-colors ${
                  star <= (hoverRating || rating)
                    ? 'fill-yellow-400 text-yellow-400'
                    : 'text-gray-300 hover:text-yellow-200'
                }`"
              />
            </button>
          </div>
          <p class="text-[#435574]">{{ getRatingLabel(hoverRating || rating) }}</p>
        </div>

        <Separator class="my-6" />

        <!-- Tags Section -->
        <div class="space-y-3">
          <h4 class="text-[#263238]">Pontos Fortes (opcional)</h4>
          <p class="text-sm text-[#435574] mb-4">
            Selecione as características que se destacaram neste cliente
          </p>

          <div class="grid grid-cols-2 md:grid-cols-3 gap-3">
            <button
              v-for="tag in availableTags"
              :key="tag.id"
              @click="toggleTag(tag.id)"
              :class="`
                p-3 rounded-lg border-2 transition-all text-left
                ${
                  selectedTags.includes(tag.id)
                    ? 'border-[#286AD8] bg-blue-50 shadow-sm'
                    : 'border-gray-200 hover:border-gray-300 hover:bg-gray-50'
                }
              `"
            >
              <div class="flex items-center gap-2">
                <span class="text-xl">{{ tag.icon }}</span>
                <span class="text-sm text-[#263238]">{{ tag.label }}</span>
              </div>
            </button>
          </div>
        </div>

        <Separator class="my-6" />

        <!-- Comment Section -->
        <div class="space-y-3">
          <h4 class="text-[#263238]">Comentário (opcional)</h4>
          <p class="text-sm text-[#435574] mb-4">
            Compartilhe detalhes sobre sua experiência com este cliente
          </p>

          <Textarea
            placeholder="Conte como foi trabalhar com este cliente..."
            v-model="comment"
            :rows="5"
            :maxlength="500"
          />

          <div class="flex justify-between text-sm text-[#435574]">
            <span>Mínimo recomendado: 50 caracteres</span>
            <span>{{ comment.length }}/500</span>
          </div>
        </div>
      </Card>

      <!-- Privacy Notice -->
      <div class="bg-gray-50 rounded-lg p-4">
        <p class="text-sm text-[#435574]">
          <strong class="text-[#263238]">Privacidade:</strong> Sua avaliação será publicada
          com anonimato parcial, exibindo apenas seu primeiro nome. Você só pode avaliar uma vez por contrato concluído.
        </p>
      </div>

      <!-- Submit Button -->
      <Button
        @click="handleSubmit"
        :disabled="rating === 0 || isSubmitting"
        class="w-full bg-[#286AD8] hover:bg-[#435574] transition-colors"
        size="lg"
      >
        <span v-if="isSubmitting" class="flex items-center gap-2">
          <span class="animate-spin h-4 w-4 border-2 border-white border-t-transparent rounded-full" />
          Processando avaliação...
        </span>
        <span v-else class="flex items-center gap-2">
          <Send class="h-4 w-4" />
          Enviar Avaliação
        </span>
      </Button>
    </div>
  </MainLayout>
</template>
