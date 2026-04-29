<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted } from 'vue';
import {
  Search,
  AlertCircle,
  CheckCircle,
  DollarSign,
  Calendar,
  MessageSquare,
  ChevronRight,
  Package,
  RefreshCw,
  Clock,
} from 'lucide-vue-next';
import Button from '@/components/ui/button.vue';
import Input from '@/components/ui/input.vue';
import Card from '@/components/ui/card.vue';
import Badge from '@/components/ui/badge.vue';
import Avatar from '@/components/ui/avatar.vue';
import AvatarFallback from '@/components/ui/avatar-fallback.vue';
import AvatarImage from '@/components/ui/avatar-image.vue';
import Select from '@/components/ui/select.vue';
import SelectContent from '@/components/ui/select-content.vue';
import SelectItem from '@/components/ui/select-item.vue';
import SelectTrigger from '@/components/ui/select-trigger.vue';
import SelectValue from '@/components/ui/select-value.vue';
import Progress from '@/components/ui/progress.vue';
import Tabs from '@/components/ui/tabs.vue';
import TabsContent from '@/components/ui/tabs-content.vue';
import TabsList from '@/components/ui/tabs-list.vue';
import TabsTrigger from '@/components/ui/tabs-trigger.vue';
import { useProposalStore, type Proposal } from '@/stores/proposalStore';
import { useDeliveryAdjustmentStore } from '@/stores/deliveryAdjustmentStore';
import {
  useServiceRequestWebSocket,
  type ServiceRequestNotification,
} from '@/composables/useServiceRequestWebSocket';
import { formatApiDate, formatTimeAgoFromApiDate } from '@/utils/apiDate';
import { getStatusLabel } from '@/utils/statusTranslations';
import { toast } from '@/utils/toast';

interface Props {
  onNavigate: (page: string, params?: any) => void;
}

const props = defineProps<Props>();
const proposalStore = useProposalStore();
const adjustmentStore = useDeliveryAdjustmentStore();

const searchQuery = ref('');
const categoryFilter = ref('all');
const currentTab = ref<'in_progress' | 'need_action' | 'awaiting_approval'>('in_progress');

let wsConnection: ReturnType<typeof useServiceRequestWebSocket> | null = null;

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

const hasPendingAdjustment = (project: Proposal) =>
  adjustmentStore.adjustments.some(
    (adjustment) =>
      adjustment.serviceRequestId.toString() === project.id &&
      (adjustment.status === 'PENDING' || adjustment.status === 'ACKNOWLEDGED')
  );

const getMilestoneProgress = (project: Proposal) => {
  const milestones = project.freelancerResponse?.milestones;
  if (milestones && milestones.length > 0) {
    const completed = milestones.filter((milestone) => milestone.status === 'completed').length;
    return Math.round((completed / milestones.length) * 100);
  }

  if (project.status === 'delivered') {
    return 100;
  }

  return project.status === 'in_progress' ? 66 : 0;
};

const getProjectAmount = (project: Proposal) => project.freelancerResponse?.value ?? project.budget ?? 0;

const allProjects = computed(() =>
  proposalStore.proposals.filter((project) =>
    ['in_progress', 'delivered'].includes(project.status)
  )
);

const stats = computed(() => {
  const projectsAwaitingApproval = allProjects.value.filter(
    (project) => project.status === 'delivered' && !hasPendingAdjustment(project)
  );
  const projectsNeedingAction = allProjects.value.filter((project) => hasPendingAdjustment(project));

  return {
    inProgress: allProjects.value.filter((project) => project.status === 'in_progress').length,
    needAction: projectsNeedingAction.length,
    awaitingApproval: projectsAwaitingApproval.length,
    totalPipeline: allProjects.value.reduce((sum, project) => sum + getProjectAmount(project), 0),
  };
});

const categoryOptions = computed(() => {
  const options = allProjects.value
    .map((project) => project.category)
    .filter((category): category is string => Boolean(category))
    .filter((category, index, categories) => categories.indexOf(category) === index)
    .sort((left, right) => left.localeCompare(right, 'pt-BR'));

  return [{ value: 'all', label: 'Todas Categorias' }, ...options.map((category) => ({ value: category, label: category }))];
});

const selectedCategoryLabel = computed(
  () => categoryOptions.value.find((option) => option.value === categoryFilter.value)?.label || 'Todas Categorias'
);

const getProjectStatusMeta = (project: Proposal) => {
  if (hasPendingAdjustment(project)) {
    return {
      label: 'Requer ação',
      class: 'bg-amber-100 text-amber-700',
      step: 'Cliente solicitou ajustes na entrega',
    };
  }

  if (project.status === 'delivered') {
    return {
      label: 'Aguardando aprovação',
      class: 'bg-emerald-100 text-emerald-700',
      step: 'Entrega enviada, aguardando retorno do cliente',
    };
  }

  return {
    label: 'Em andamento',
    class: 'bg-blue-100 text-blue-700',
    step: 'Execução em andamento',
  };
};

const filteredProjects = computed(() => {
  return allProjects.value.filter((project) => {
    const matchesSearch =
      project.service.toLowerCase().includes(searchQuery.value.toLowerCase()) ||
      project.clientName.toLowerCase().includes(searchQuery.value.toLowerCase());

    const matchesCategory = categoryFilter.value === 'all' || project.category === categoryFilter.value;

    const matchesTab =
      currentTab.value === 'in_progress'
        ? project.status === 'in_progress'
        : currentTab.value === 'need_action'
          ? hasPendingAdjustment(project)
          : project.status === 'delivered' && !hasPendingAdjustment(project);

    return matchesSearch && matchesCategory && matchesTab;
  });
});

const formatCurrency = (value?: number | null) => {
  if (value == null) return 'A combinar';
  return new Intl.NumberFormat('pt-BR', {
    style: 'currency',
    currency: 'BRL',
  }).format(value);
};

const formatDate = (dateString: string) =>
  formatApiDate(dateString, {
    day: '2-digit',
    month: '2-digit',
    year: 'numeric',
  });

const getTimeAgo = (dateString: string) => formatTimeAgoFromApiDate(dateString);

const getInitials = (name: string) =>
  name
    .split(' ')
    .map((part) => part[0])
    .join('')
    .toUpperCase()
    .slice(0, 2);

const handleViewProject = (projectId: string) => {
  props.onNavigate('freelancer-project-detail', { requestId: projectId });
};

const handleChat = (projectId: string) => {
  props.onNavigate('messages', { projectId });
};

const handleRefresh = async () => {
  await Promise.all([proposalStore.loadProposals(), adjustmentStore.getReceived()]);
  toast.info('Atualizando projetos...');
};

const handleServiceRequestNotification = async (event: Event) => {
  const notification = (event as CustomEvent<ServiceRequestNotification>).detail;

  switch (notification.type) {
    case 'SERVICE_REQUEST_STATUS_CHANGED':
    case 'SERVICE_COMPLETED_REVIEW_PENDING':
    case 'ADJUSTMENTS_REQUESTED':
    case 'ADJUSTMENT_STATUS_CHANGED':
    case 'ADJUSTMENT_DELETED':
    case 'SERVICE_REQUEST_CANCELLED':
      await Promise.all([proposalStore.loadProposals(), adjustmentStore.getReceived()]);
      break;
    default:
      break;
  }
};

onMounted(async () => {
  await Promise.all([proposalStore.loadProposals(), adjustmentStore.getReceived()]);

  const userEmail = getUserEmail();
  if (userEmail) {
    wsConnection = useServiceRequestWebSocket(userEmail);
    wsConnection.connect().catch((error) => {
      console.error('Erro ao conectar WebSocket dos projetos do freelancer:', error);
    });
    window.addEventListener('service-request-notification', handleServiceRequestNotification);
  }
});

onUnmounted(() => {
  window.removeEventListener('service-request-notification', handleServiceRequestNotification);
  wsConnection?.disconnect();
});
</script>

<template>
  <div class="max-w-7xl mx-auto space-y-6 p-6">
    <div class="flex items-center justify-between gap-4 flex-wrap">
      <div class="flex-1">
        <h1 class="text-2xl font-bold text-[#263238] mb-2">Minhas Entregas</h1>
        <p class="text-[#435574]">Acompanhe o progresso dos serviços aceitos e finalize suas entregas.</p>
      </div>

      <Button variant="outline" @click="handleRefresh">
        <RefreshCw class="h-4 w-4 mr-2" />
        Atualizar
      </Button>
    </div>

    <div class="grid grid-cols-1 md:grid-cols-4 gap-4">
      <Card class="p-4">
        <div class="flex items-center gap-3">
          <div class="w-12 h-12 rounded-full bg-blue-100 flex items-center justify-center">
            <Package class="h-6 w-6 text-blue-600" />
          </div>
          <div>
            <p class="text-sm text-[#435574]">Em andamento</p>
            <p class="text-2xl font-bold text-[#263238]">{{ stats.inProgress }}</p>
          </div>
        </div>
      </Card>

      <Card class="p-4">
        <div class="flex items-center gap-3">
          <div class="w-12 h-12 rounded-full bg-amber-100 flex items-center justify-center">
            <AlertCircle class="h-6 w-6 text-amber-600" />
          </div>
          <div>
            <p class="text-sm text-[#435574]">Requer ação</p>
            <p class="text-2xl font-bold text-[#263238]">{{ stats.needAction }}</p>
          </div>
        </div>
      </Card>

      <Card class="p-4">
        <div class="flex items-center gap-3">
          <div class="w-12 h-12 rounded-full bg-green-100 flex items-center justify-center">
            <CheckCircle class="h-6 w-6 text-green-600" />
          </div>
          <div>
            <p class="text-sm text-[#435574]">Aguardando aprovação</p>
            <p class="text-2xl font-bold text-[#263238]">{{ stats.awaitingApproval }}</p>
          </div>
        </div>
      </Card>

      <Card class="p-4">
        <div class="flex items-center gap-3">
          <div class="w-12 h-12 rounded-full bg-purple-100 flex items-center justify-center">
            <DollarSign class="h-6 w-6 text-purple-600" />
          </div>
          <div>
            <p class="text-sm text-[#435574]">Em carteira</p>
            <p class="text-2xl font-bold text-[#263238]">{{ formatCurrency(stats.totalPipeline) }}</p>
          </div>
        </div>
      </Card>
    </div>

    <Card class="p-4">
      <div class="flex flex-col md:flex-row gap-4">
        <div class="flex-1 relative">
          <Search class="absolute left-3 top-1/2 -translate-y-1/2 h-5 w-5 text-[#435574]" />
          <Input
            v-model="searchQuery"
            placeholder="Buscar por servico ou cliente..."
            class="pl-10"
          />
        </div>

        <Select v-model="categoryFilter">
          <SelectTrigger class="w-full md:w-[220px]">
            <SelectValue placeholder="Todas Categorias" :label="selectedCategoryLabel" />
          </SelectTrigger>
          <SelectContent>
            <SelectItem
              v-for="option in categoryOptions"
              :key="option.value"
              :value="option.value"
            >
              {{ option.label }}
            </SelectItem>
          </SelectContent>
        </Select>
      </div>
    </Card>

    <Tabs v-model="currentTab">
      <TabsList class="grid w-full grid-cols-3">
        <TabsTrigger value="in_progress">
          Em andamento ({{ stats.inProgress }})
        </TabsTrigger>
        <TabsTrigger value="need_action">
          Requer ação ({{ stats.needAction }})
        </TabsTrigger>
        <TabsTrigger value="awaiting_approval">
          Aguardando aprovação ({{ stats.awaitingApproval }})
        </TabsTrigger>
      </TabsList>

      <TabsContent :value="currentTab" class="mt-6">
        <div v-if="proposalStore.isLoading" class="flex justify-center py-12">
          <div class="animate-spin h-8 w-8 border-4 border-[#286AD8] border-t-transparent rounded-full"></div>
        </div>

        <Card v-else-if="filteredProjects.length === 0" class="p-12 text-center">
          <Package class="h-12 w-12 text-gray-400 mx-auto mb-4" />
          <h3 class="text-lg font-semibold text-[#263238] mb-2">Nenhum projeto encontrado</h3>
          <p class="text-[#435574]">
            {{ searchQuery || categoryFilter !== 'all'
              ? 'Tente ajustar os filtros de busca.'
              : 'Seus projetos em andamento, com ajustes pendentes e aguardando aprovação aparecerão aqui.' }}
          </p>
        </Card>

        <div v-else class="grid grid-cols-1 gap-4">
          <Card
            v-for="project in filteredProjects"
            :key="project.id"
            class="p-6 hover:shadow-lg transition-all cursor-pointer"
            @click="handleViewProject(project.id)"
          >
            <div class="space-y-4">
              <div class="flex items-start justify-between">
                <div class="flex-1">
                  <div class="flex items-center gap-2 mb-2 flex-wrap">
                    <h3 class="font-semibold text-[#263238] text-lg">{{ project.service }}</h3>
                    <Badge :class="getProjectStatusMeta(project).class">
                      {{ getProjectStatusMeta(project).label }}
                    </Badge>
                  </div>
                  <p class="text-sm text-[#435574]">{{ project.category }}</p>
                </div>

                <Badge class="bg-slate-100 text-slate-700">
                  {{ getStatusLabel(project.status) }}
                </Badge>
              </div>

              <div class="flex items-center gap-3">
                <Avatar class="h-10 w-10">
                  <AvatarImage :src="project.clientAvatar" />
                  <AvatarFallback class="bg-amber-100 text-amber-700">
                    {{ getInitials(project.clientName) }}
                  </AvatarFallback>
                </Avatar>
                <div class="flex-1">
                  <p class="font-medium text-[#263238]">{{ project.clientName }}</p>
                  <p class="text-sm text-[#435574]">{{ project.clientLocation }}</p>
                </div>
                <Button
                  variant="outline"
                  size="sm"
                  @click.stop="handleChat(project.id)"
                >
                  <MessageSquare class="h-4 w-4 mr-2" />
                  Chat
                </Button>
              </div>

              <div class="space-y-2">
                <div class="flex items-center justify-between text-sm">
                  <span class="text-[#435574]">{{ getProjectStatusMeta(project).step }}</span>
                  <span class="font-medium text-[#263238]">{{ getMilestoneProgress(project) }}%</span>
                </div>
                <Progress :value="getMilestoneProgress(project)" class="h-2" />
              </div>

              <div class="flex flex-wrap items-center gap-4 text-sm text-[#435574]">
                <div class="flex items-center gap-1">
                  <Calendar class="h-4 w-4" />
                  <span>Início: {{ formatDate(project.receivedAt) }}</span>
                </div>
                <div class="flex items-center gap-1">
                  <Clock class="h-4 w-4" />
                  <span>Prazo: {{ project.freelancerResponse?.executionDays ? project.freelancerResponse.executionDays + ' dias' : project.deadline }}</span>
                </div>
                <div class="flex items-center gap-1 font-medium text-[#263238]">
                  <DollarSign class="h-4 w-4 text-green-600" />
                  <span>{{ formatCurrency(getProjectAmount(project)) }}</span>
                </div>
              </div>

              <div class="flex items-center justify-between pt-2 border-t">
                <p class="text-xs text-[#435574]">{{ getTimeAgo(project.updatedAt || project.receivedAt) }}</p>

                <Button
                  @click.stop="handleViewProject(project.id)"
                  variant="outline"
                  size="sm"
                >
                  Ver Detalhes
                  <ChevronRight class="h-4 w-4 ml-2" />
                </Button>
              </div>
            </div>
          </Card>
        </div>
      </TabsContent>
    </Tabs>
  </div>
</template>
