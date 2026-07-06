<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { useClientRequestStore } from '@/stores/clientRequestStore';
import { useDeliveryAdjustmentStore } from '@/stores/deliveryAdjustmentStore';
import { useServiceRequestWebSocket } from '@/composables/useServiceRequestWebSocket';
import DeliveryApprovalPage from '@/components/cliente/DeliveryApprovalPage.vue';
import MainLayout from '@/components/layout/MainLayout.vue';
import Button from '@/components/ui/button.vue';
import { useNavigation } from '@/utils/navigation';
import { toast } from '@/utils/toast';
import { getStatusLabel } from '@/utils/statusTranslations';

const adjustmentStore = useDeliveryAdjustmentStore();

interface DeliveryFile {
  id: string;
  name: string;
  size: string;
  type: 'document' | 'image' | 'video' | 'other';
  url: string;
  thumbnail?: string;
}

const route = useRoute();
const router = useRouter();
const store = useClientRequestStore();
const { navigate, logout } = useNavigation();

const user = ref<any>(null);
const requestId = computed(() => route.query.requestId as string);

let wsConnection: any = null;

const request = computed(() => {
  if (!requestId.value) return null;
  return store.getRequestById(requestId.value);
});

const adjustmentAlreadyUsed = computed(() => adjustmentStore.adjustments.length > 0);

const deliveryFiles = computed((): DeliveryFile[] => {
  const source = request.value?.deliveryAttachments?.length
    ? request.value.deliveryAttachments
    : request.value?.freelancerAttachments;
  if (!source) return [];

  return source.map((att, idx) => {
    const extension = att.name.split('.').pop()?.toLowerCase() || '';
    let type: DeliveryFile['type'] = 'other';

    if (['jpg', 'jpeg', 'png', 'gif', 'webp', 'svg'].includes(extension)) {
      type = 'image';
    } else if (['mp4', 'avi', 'mov', 'wmv', 'webm'].includes(extension)) {
      type = 'video';
    } else if (['pdf', 'doc', 'docx', 'xls', 'xlsx', 'txt'].includes(extension)) {
      type = 'document';
    }

    return {
      id: att.id || `file-${idx}`,
      name: att.name,
      size: att.size || 'Tamanho desconhecido',
      type,
      url: att.url,
      thumbnail: type === 'image' ? att.url : undefined,
    };
  });
});

const formatDate = (dateString: string) => {
  const date = new Date(dateString);
  return date.toLocaleDateString('pt-BR', {
    day: '2-digit',
    month: '2-digit',
    year: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
  });
};

const handleBack = () => {
  router.push('/cliente-project-detail?requestId=' + requestId.value);
};

const handleApprove = async () => {
  if (!requestId.value) return;

  const success = await store.completeService(requestId.value);

  if (success) {
    toast.success('Entrega aprovada! Agora avalie o freelancer.');
    setTimeout(() => {
      router.push('/rate-freelancer?requestId=' + requestId.value);
    }, 1500);
  } else {
    toast.error('Erro ao aprovar entrega. Tente novamente.');
  }
};

const handleRequestAdjustments = async (reason: string) => {
  if (!requestId.value) return false;

  const success = await adjustmentStore.requestAdjustment(
    parseInt(requestId.value),
    reason
  );

  if (success) {
    toast.success('Solicitação de ajustes enviada ao freelancer.');
    await adjustmentStore.getAdjustmentsByServiceRequest(parseInt(requestId.value));
    return true;
  }

  toast.error('Erro ao enviar solicitação. Tente novamente.');
  return false;
};

const handleGoToChat = () => {
  if (!requestId.value) return;
  router.push('/cliente-request-detail?requestId=' + requestId.value + '&tab=chat');
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
  } catch (error) {
    console.error('Erro ao extrair email do token:', error);
    return null;
  }
};

const handleServiceRequestNotification = (event: any) => {
  const notification = event.detail;
  console.log('Notificação recebida em aprovação de entrega:', notification);

  if (notification.serviceRequestId === parseInt(requestId.value)) {
    switch (notification.type) {
      case 'SERVICE_REQUEST_STATUS_CHANGED':
        if (notification.status?.toLowerCase() === 'completed') {
          setTimeout(() => {
            router.push('/rate-freelancer?requestId=' + requestId.value);
          }, 2000);
        } else {
          toast.info(`Status atualizado: ${getStatusLabel(notification.status)}`);
          store.loadRequests();
        }
        break;
      case 'SERVICE_COMPLETED_REVIEW_PENDING':
        setTimeout(() => {
          router.push('/rate-freelancer?requestId=' + requestId.value);
        }, 1500);
        break;
      case 'SERVICE_REQUEST_CANCELLED':
        toast.warning('Projeto cancelado');
        setTimeout(() => {
          router.push('/cliente-projects');
        }, 2000);
        break;
    }
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

  // Sempre recarregar a solicitação para refletir a descrição/anexos da entrega mais
  // recente (o freelancer pode ter reenviado após um ajuste com uma nova descrição).
  await store.loadRequestById(requestId.value);

  await adjustmentStore.getAdjustmentsByServiceRequest(parseInt(requestId.value));

  const userEmail = getUserEmail();
  console.log('Tentando conectar WebSocket para aprovação de entrega:', userEmail);

  if (userEmail) {
    wsConnection = useServiceRequestWebSocket(userEmail);
    wsConnection.connect();

    window.addEventListener('service-request-notification', handleServiceRequestNotification);
  } else {
    console.error('Não foi possível extrair e-mail do token');
  }
});

onUnmounted(() => {
  window.removeEventListener('service-request-notification', handleServiceRequestNotification);
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
    <div v-if="!request && !store.isLoading" class="max-w-4xl mx-auto p-6">
      <div class="text-center py-12">
        <h2 class="mb-2 text-xl font-semibold text-[#263238]">Solicitação não encontrada</h2>
        <p class="mb-4 text-[#435574]">
          A solicitação que você está procurando não existe ou foi removida.
        </p>
        <Button variant="default" @click="router.push('/cliente-projects')">
          Voltar para Meus Projetos
        </Button>
      </div>
    </div>

    <div v-else-if="store.isLoading" class="flex justify-center py-12">
      <div class="h-8 w-8 animate-spin rounded-full border-4 border-[#286AD8] border-t-transparent" />
    </div>

    <DeliveryApprovalPage
      v-else-if="request"
      :service-id="request.id"
      :service-name="request.service"
      :freelancer-name="request.freelancerName || 'Freelancer'"
      :freelancer-avatar="request.freelancerAvatar"
      :delivery-description="request.deliveryDescription || ''"
      :delivery-date="formatDate(request.deliveredAt || request.createdAt)"
      :files="deliveryFiles"
      :adjustment-already-used="adjustmentAlreadyUsed"
      :is-loading="adjustmentStore.isLoading"
      :on-back="handleBack"
      :on-approve="handleApprove"
      :on-request-adjustments="handleRequestAdjustments"
      :on-go-to-chat="handleGoToChat"
    />
  </MainLayout>
</template>
