<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted } from 'vue';
import { useRouter } from 'vue-router';
import {
  Search,
  Filter,
  Clock,
  CheckCircle,
  AlertCircle,
  DollarSign,
  Calendar,
  MessageSquare,
  ChevronRight,
  Package,
  Star,
  ArrowLeft,
  Send,
  RefreshCw,
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
import MainLayout from '@/components/layout/MainLayout.vue';
import { useClientRequestStore, type ServiceRequest } from '@/stores/clientRequestStore';
import { useServiceRequestWebSocket } from '@/composables/useServiceRequestWebSocket';
import { useNavigation } from '@/utils/navigation';
import { getStatusLabel } from '@/utils/statusTranslations';
import { toast } from '@/utils/toast';

const router = useRouter();
const store = useClientRequestStore();
const { navigate, logout } = useNavigation();

const user = ref<any>(null);
const searchQuery = ref('');
const categoryFilter = ref('all');
const currentTab = ref('active');

// WebSocket
let wsConnection: any = null;

const categories = ['Eletricista', 'Encanador', 'Pintor', 'Pedreiro', 'Designer', 'Desenvolvedor'];

// Estatísticas baseadas nos projetos (status accepted e in_progress)
const stats = computed(() => {
  const activeProjects = store.requests.filter(
    r => r.status === 'accepted' || r.status === 'in_progress' || r.status === 'delivered'
  );
  
  return {
    inProgress: activeProjects.filter(r => r.status === 'in_progress').length,
    needAction: activeProjects.filter(r => r.status === 'delivered').length,
    completed: store.requests.filter(r => r.status === 'completed').length,
    totalInvested: activeProjects.reduce((sum, r) => 
      sum + (r.freelancerResponse?.value || r.budget), 0
    ),
  };
});

// Converter ServiceRequest em formato de projeto
const getProjectFromRequest = (request: ServiceRequest) => {
  const isDelivered = request.status === 'delivered';
  const isInProgress = request.status === 'in_progress';
  const isCompleted = request.status === 'completed';
  const isAccepted = request.status === 'accepted';
  
  // Calcular progresso baseado no status
  let progress = 0;
  let completedSteps = 0;
  const totalSteps = 3;
  
  if (isAccepted) {
    progress = 33;
    completedSteps = 1;
  } else if (isInProgress) {
    progress = 66;
    completedSteps = 2;
  } else if (isDelivered || isCompleted) {
    progress = 100;
    completedSteps = 3;
  }
  
  return {
    ...request,
    freelancerRating: 5.0, // Pode ser adicionado ao backend futuramente
    progress,
    totalSteps,
    completedSteps,
    badge: isDelivered ? 'Ação Necessária' : undefined,
    badgeVariant: isDelivered ? 'warning' as const : undefined,
    currentStep: isDelivered 
      ? 'Entrega final aguardando aprovação'
      : isInProgress
      ? 'Trabalho em andamento'
      : isAccepted
      ? 'Aguardando início do trabalho'
      : 'Projeto concluído',
  };
};

const filteredProjects = computed(() => {
  // Apenas projetos aceitos ou em andamento (accepted, in_progress, delivered, completed)
  let filtered = store.requests
    .filter(r => ['accepted', 'in_progress', 'delivered', 'completed'].includes(r.status))
    .map(getProjectFromRequest);
  
  // Filtros
  filtered = filtered.filter(project => {
    const matchesSearch = 
      project.service.toLowerCase().includes(searchQuery.value.toLowerCase()) ||
      (project.freelancerName || '').toLowerCase().includes(searchQuery.value.toLowerCase());
    
    const matchesCategory = categoryFilter.value === 'all' || project.category === categoryFilter.value;
    
    const matchesTab = 
      currentTab.value === 'active' 
        ? project.status !== 'completed'
        : currentTab.value === 'need_action'
        ? project.status === 'delivered'
        : project.status === 'completed';
    
    return matchesSearch && matchesCategory && matchesTab;
  });
  
  return filtered;
});

const formatCurrency = (value: number) => {
  return new Intl.NumberFormat('pt-BR', {
    style: 'currency',
    currency: 'BRL',
  }).format(value);
};

const formatDate = (dateString: string) => {
  const date = new Date(dateString);
  return date.toLocaleDateString('pt-BR', {
    day: '2-digit',
    month: '2-digit',
    year: 'numeric',
  });
};

const getTimeAgo = (dateString: string) => {
  const date = new Date(dateString);
  const now = new Date();
  const diffMs = now.getTime() - date.getTime();
  const diffMins = Math.floor(diffMs / 60000);
  const diffHours = Math.floor(diffMins / 60);
  const diffDays = Math.floor(diffHours / 24);
  
  if (diffMins < 60) return `Atualizado há ${diffMins} minuto${diffMins !== 1 ? 's' : ''}`;
  if (diffHours < 24) return `Atualizado há ${diffHours} hora${diffHours !== 1 ? 's' : ''}`;
  if (diffDays === 1) return 'Atualizado há 1 dia';
  return `Atualizado há ${diffDays} dias`;
};

const getInitials = (name: string) => {
  return name
    .split(' ')
    .map(n => n[0])
    .join('')
    .toUpperCase()
    .slice(0, 2);
};

const getStatusColor = (status: string) => {
  const colors: Record<string, string> = {
    delivered: 'bg-amber-100 text-amber-700',
    in_progress: 'bg-blue-100 text-blue-700',
    accepted: 'bg-green-100 text-green-700',
    completed: 'bg-teal-100 text-teal-700',
  };
  return colors[status] || 'bg-gray-100 text-gray-700';
};

const getStatusIcon = (status: string) => {
  const icons: Record<string, any> = {
    delivered: AlertCircle,
    in_progress: Package,
    accepted: CheckCircle,
    completed: CheckCircle,
  };
  return icons[status] || Clock;
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

const handleBack = () => {
  router.push('/cliente-dashboard');
};

const handleViewProject = (projectId: string) => {
  router.push(`/cliente-request-detail?requestId=${projectId}`);
};

const handleApproveDelivery = (projectId: string) => {
  router.push(`/cliente-delivery-approval?requestId=${projectId}&from=projects`);
};

const handleChat = (projectId: string) => {
  router.push(`/messages?projectId=${projectId}`);
};

const handleRefresh = () => {
  store.loadRequests();
  toast.info('Atualizando projetos...');
};

// Extrair email do usuário do JWT
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

// Lidar com notificações via WebSocket
const handleServiceRequestNotification = (event: any) => {
  const notification = event.detail;
  console.log('📩 Notificação recebida em Projetos:', notification);
  
  // Mostrar toast baseado no tipo de notificação
  switch (notification.type) {
    case 'NEW_PROPOSAL':
      toast.success(`${notification.freelancerName} enviou uma proposta!`);
      store.loadRequests();
      break;
    case 'SERVICE_REQUEST_STATUS_CHANGED':
      // Verificar se é um projeto (status accepted, in_progress, delivered, completed)
      if (['accepted', 'in_progress', 'delivered', 'completed'].includes(notification.status?.toLowerCase())) {
        if (notification.status?.toLowerCase() === 'delivered') {
          toast.success('🎉 Entrega concluída! Revise e aprove o trabalho.');
        } else {
          toast.info(`Status atualizado: ${getStatusLabel(notification.status)}`);
        }
        store.loadRequests();
      }
      break;
    case 'PROPOSAL_ACCEPTED':
      toast.success(notification.message);
      store.loadRequests();
      break;
    case 'SERVICE_REQUEST_CANCELLED':
      toast.warning('Projeto cancelado');
      store.loadRequests();
      break;
  }
};

onMounted(() => {
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
  
  store.loadRequests();
  
  // Conectar WebSocket
  const userEmail = getUserEmail();
  console.log('🔍 Tentando conectar WebSocket para projetos:', userEmail);
  
  if (userEmail) {
    wsConnection = useServiceRequestWebSocket(userEmail);
    wsConnection.connect();
    
    console.log('✅ WebSocket inicializado para projetos (Cliente)');
    
    // Escutar notificações
    window.addEventListener('service-request-notification', handleServiceRequestNotification);
  } else {
    console.error('❌ Não foi possível extrair email do token');
  }
});

onUnmounted(() => {
  console.log('🔌 Desconectando WebSocket de projetos (Cliente)');
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
    <div class="max-w-7xl mx-auto space-y-6 p-6">
      <!-- Header -->
      <div class="flex items-center gap-4 mb-4">
        <Button @click="handleBack" variant="ghost" size="icon">
          <ArrowLeft class="h-5 w-5" />
        </Button>
        <div class="flex-1">
          <h1 class="text-2xl font-bold text-[#263238] mb-2">Meus Projetos</h1>
          <p class="text-[#435574]">Acompanhe o progresso dos seus serviços contratados</p>
        </div>
        <Button @click="handleRefresh" variant="outline" :disabled="store.isLoading">
          <RefreshCw :class="`h-4 w-4 mr-2 ${store.isLoading ? 'animate-spin' : ''}`" />
          Atualizar
        </Button>
      </div>

    <!-- Stats Cards -->
    <div class="grid grid-cols-1 md:grid-cols-4 gap-4">
      <Card class="p-4">
        <div class="flex items-center gap-3">
          <div class="w-12 h-12 rounded-full bg-blue-100 flex items-center justify-center">
            <Package class="h-6 w-6 text-blue-600" />
          </div>
          <div>
            <p class="text-sm text-[#435574]">Em Andamento</p>
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
            <p class="text-sm text-[#435574]">Requer Ação</p>
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
            <p class="text-sm text-[#435574]">Concluídos</p>
            <p class="text-2xl font-bold text-[#263238]">{{ stats.completed }}</p>
          </div>
        </div>
      </Card>

      <Card class="p-4">
        <div class="flex items-center gap-3">
          <div class="w-12 h-12 rounded-full bg-purple-100 flex items-center justify-center">
            <DollarSign class="h-6 w-6 text-purple-600" />
          </div>
          <div>
            <p class="text-sm text-[#435574]">Investido</p>
            <p class="text-2xl font-bold text-[#263238]">{{ formatCurrency(stats.totalInvested) }}</p>
          </div>
        </div>
      </Card>
    </div>

    <!-- Filters -->
    <Card class="p-4">
      <div class="flex flex-col md:flex-row gap-4">
        <div class="flex-1 relative">
          <Search class="absolute left-3 top-1/2 -translate-y-1/2 h-5 w-5 text-[#435574]" />
          <Input
            v-model="searchQuery"
            placeholder="Buscar por serviço ou freelancer..."
            class="pl-10"
          />
        </div>
        
        <Select v-model="categoryFilter">
          <SelectTrigger class="w-full md:w-[200px]">
            <SelectValue placeholder="Todas Categorias" />
          </SelectTrigger>
          <SelectContent>
            <SelectItem value="all">Todas Categorias</SelectItem>
            <SelectItem value="Eletricista">Eletricista</SelectItem>
            <SelectItem value="Designer">Designer</SelectItem>
            <SelectItem value="Encanador">Encanador</SelectItem>
            <SelectItem value="Pintor">Pintor</SelectItem>
          </SelectContent>
        </Select>

        <Button variant="outline" class="w-full md:w-auto">
          <Filter class="h-4 w-4 mr-2" />
          Filtros
        </Button>
      </div>
    </Card>

    <!-- Tabs -->
    <Tabs v-model="currentTab">
      <TabsList class="grid w-full grid-cols-3">
        <TabsTrigger value="active">
          Ativos ({{ stats.inProgress + stats.needAction }})
        </TabsTrigger>
        <TabsTrigger value="need_action">
          Requer Ação ({{ stats.needAction }})
        </TabsTrigger>
        <TabsTrigger value="completed">
          Concluídos ({{ stats.completed }})
        </TabsTrigger>
      </TabsList>

      <TabsContent :value="currentTab" class="mt-6">
        <!-- Loading State -->
        <div v-if="store.isLoading" class="flex justify-center py-12">
          <div class="animate-spin h-8 w-8 border-4 border-[#286AD8] border-t-transparent rounded-full"></div>
        </div>

        <!-- Empty State -->
        <Card v-else-if="filteredProjects.length === 0" class="p-12 text-center">
          <Package class="h-12 w-12 text-gray-400 mx-auto mb-4" />
          <h3 class="text-lg font-semibold text-[#263238] mb-2">
            Nenhum projeto encontrado
          </h3>
          <p class="text-[#435574]">
            {{ searchQuery || categoryFilter !== 'all'
              ? 'Tente ajustar os filtros de busca'
              : 'Você não possui projetos nesta categoria' }}
          </p>
        </Card>

        <!-- Projects List -->
        <div v-else class="grid grid-cols-1 gap-4">
          <Card
            v-for="project in filteredProjects"
            :key="project.id"
            class="p-6 hover:shadow-lg transition-all cursor-pointer"
            @click="handleViewProject(project.id)"
          >
            <div class="space-y-4">
              <!-- Header -->
              <div class="flex items-start justify-between">
                <div class="flex-1">
                  <div class="flex items-center gap-2 mb-2 flex-wrap">
                    <h3 class="font-semibold text-[#263238] text-lg">{{ project.service }}</h3>
                    <Badge
                      v-if="project.badge"
                      :class="project.badgeVariant === 'warning' 
                        ? 'bg-amber-100 text-amber-700' 
                        : project.badgeVariant === 'success'
                        ? 'bg-green-100 text-green-700'
                        : 'bg-blue-100 text-blue-700'"
                    >
                      {{ project.badge }}
                    </Badge>
                  </div>
                  <p class="text-sm text-[#435574]">{{ project.category }}</p>
                </div>

                <Badge :class="getStatusColor(project.status)">
                  <component :is="getStatusIcon(project.status)" class="h-3 w-3 mr-1" />
                  {{ getStatusLabel(project.status) }}
                </Badge>
              </div>

              <!-- Freelancer Info -->
              <div class="flex items-center gap-3">
                <Avatar class="h-10 w-10">
                  <AvatarImage :src="project.freelancerAvatar" />
                  <AvatarFallback class="bg-[#286AD8] text-white">
                    {{ getInitials(project.freelancerName || 'Freelancer') }}
                  </AvatarFallback>
                </Avatar>
                <div class="flex-1">
                  <p class="font-medium text-[#263238]">{{ project.freelancerName || 'Profissional' }}</p>
                  <div class="flex items-center gap-1">
                    <Star class="h-3 w-3 fill-amber-400 text-amber-400" />
                    <span class="text-sm text-[#435574]">{{ project.freelancerRating || 5.0 }}</span>
                  </div>
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

              <!-- Progress -->
              <div class="space-y-2">
                <div class="flex items-center justify-between text-sm">
                  <span class="text-[#435574]">{{ project.currentStep }}</span>
                  <span class="font-medium text-[#263238]">{{ project.progress }}%</span>
                </div>
                <Progress :model-value="project.progress" class="h-2" />
                <p class="text-xs text-[#435574]">
                  {{ project.completedSteps }} de {{ project.totalSteps }} etapas concluídas
                </p>
              </div>

              <!-- Details -->
              <div class="flex flex-wrap items-center gap-4 text-sm text-[#435574]">
                <div class="flex items-center gap-1">
                  <Calendar class="h-4 w-4" />
                  <span>Início: {{ formatDate(project.createdAt) }}</span>
                </div>
                <div class="flex items-center gap-1">
                  <Clock class="h-4 w-4" />
                  <span>Prazo: {{ project.freelancerResponse?.executionDays ? project.freelancerResponse.executionDays + ' dias' : project.deadline }}</span>
                </div>
                <div class="flex items-center gap-1 font-medium text-[#263238]">
                  <DollarSign class="h-4 w-4 text-green-600" />
                  <span>{{ formatCurrency(project.freelancerResponse?.value || project.budget) }}</span>
                </div>
              </div>

              <div class="flex items-center justify-between pt-2 border-t">
                <p class="text-xs text-[#435574]">{{ getTimeAgo(project.createdAt) }}</p>
                
                <div class="flex items-center gap-2">
                  <Button
                    v-if="project.status === 'delivered'"
                    @click.stop="handleApproveDelivery(project.id)"
                    class="bg-[#286AD8] hover:bg-[#435574]"
                    size="sm"
                  >
                    <CheckCircle class="h-4 w-4 mr-2" />
                    Aprovar Entrega
                  </Button>
                  
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
            </div>
          </Card>
        </div>
      </TabsContent>
    </Tabs>
  </div>
  </MainLayout>
</template>

<style scoped>
.cursor-pointer {
  cursor: pointer;
}
</style>
