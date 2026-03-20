<script setup lang="ts">
import { ref, onMounted, onUnmounted } from 'vue';
import { useServiceRequestWebSocket, type ServiceRequestNotification } from '@/composables/useServiceRequestWebSocket';
import {
  TrendingUp,
  DollarSign,
  Briefcase,
  Clock,
  Star,
  Users,
  ArrowUp,
  ArrowDown,
  FileText,
  CheckCircle,
  XCircle,
  AlertCircle,
  Eye,
  MessageSquare,
  Calendar,
} from 'lucide-vue-next';
import Card from '../ui/card.vue';
import Badge from '../ui/badge.vue';
import Button from '../ui/button.vue';
import Avatar from '../ui/avatar.vue';
import AvatarImage from '../ui/avatar-image.vue';
import AvatarFallback from '../ui/avatar-fallback.vue';
import Progress from '../ui/progress.vue';
import { toast } from '@/utils/toast';
// Gráficos removidos - recharts não está instalado
// Para usar gráficos, instale: npm install recharts

interface Props {
  onNavigate: (page: string, params?: any) => void;
}

const props = defineProps<Props>();

let wsConnection: ReturnType<typeof useServiceRequestWebSocket> | null = null;

const selectedPeriod = ref<'week' | 'month' | 'year'>('month');

// Mock data - estatísticas
const stats = {
  revenue: {
    current: 8450,
    previous: 7200,
    change: 17.4,
  },
  activeServices: {
    current: 12,
    previous: 10,
    change: 20,
  },
  completedJobs: {
    current: 45,
    previous: 38,
    change: 18.4,
  },
  rating: {
    current: 4.9,
    previous: 4.7,
    change: 4.3,
  },
};

// Dados para gráfico de receita
const revenueData = [
  { month: 'Jun', revenue: 4500, proposals: 15 },
  { month: 'Jul', revenue: 5200, proposals: 18 },
  { month: 'Ago', revenue: 6800, proposals: 22 },
  { month: 'Set', revenue: 7200, proposals: 20 },
  { month: 'Out', revenue: 8100, proposals: 25 },
  { month: 'Nov', revenue: 8450, proposals: 27 },
];

// Dados para gráfico de serviços por categoria
const servicesByCategoryData = [
  { name: 'Instalação', value: 35, color: '#286AD8' },
  { name: 'Manutenção', value: 28, color: '#435574' },
  { name: 'Reparo', value: 22, color: '#2B2E33' },
  { name: 'Automação', value: 15, color: '#323842' },
];

// Propostas recentes
const recentProposals = [
  {
    id: '1',
    clientName: 'Maria Silva',
    clientAvatar: undefined,
    service: 'Instalação Elétrica Residencial',
    description: 'Preciso de instalação completa em apartamento novo',
    budget: 1200,
    deadline: '15 dias',
    status: 'pending' as const,
    receivedAt: '2 horas atrás',
  },
  {
    id: '2',
    clientName: 'Carlos Santos',
    clientAvatar: undefined,
    service: 'Manutenção Preventiva',
    description: 'Revisão geral do sistema elétrico',
    budget: 450,
    deadline: '5 dias',
    status: 'pending' as const,
    receivedAt: '5 horas atrás',
  },
  {
    id: '3',
    clientName: 'Ana Costa',
    clientAvatar: undefined,
    service: 'Automação Residencial',
    description: 'Instalação de sistema de automação completo',
    budget: 3500,
    deadline: '30 dias',
    status: 'pending' as const,
    receivedAt: '1 dia atrás',
  },
];

// Serviços em andamento
const activeJobs = [
  {
    id: '1',
    clientName: 'Pedro Oliveira',
    service: 'Instalação de Painéis Solares',
    progress: 65,
    deadline: '5 dias',
    status: 'in-progress' as const,
  },
  {
    id: '2',
    clientName: 'Julia Fernandes',
    service: 'Reparo de Circuitos',
    progress: 90,
    deadline: '2 dias',
    status: 'in-progress' as const,
  },
  {
    id: '3',
    clientName: 'Roberto Lima',
    service: 'Instalação Elétrica Comercial',
    progress: 30,
    deadline: '12 dias',
    status: 'in-progress' as const,
  },
];

// Atividades recentes
const recentActivities = [
  {
    id: '1',
    type: 'proposal',
    icon: FileText,
    title: 'Nova proposta recebida',
    description: 'Maria Silva solicitou orçamento para Instalação Elétrica',
    time: '2 horas atrás',
    color: 'text-blue-600',
    bgColor: 'bg-blue-100',
  },
  {
    id: '2',
    type: 'review',
    icon: Star,
    title: 'Nova avaliação',
    description: 'Pedro Oliveira avaliou seu serviço com 5 estrelas',
    time: '4 horas atrás',
    color: 'text-yellow-600',
    bgColor: 'bg-yellow-100',
  },
  {
    id: '3',
    type: 'message',
    icon: MessageSquare,
    title: 'Nova mensagem',
    description: 'Julia Fernandes enviou uma mensagem',
    time: '6 horas atrás',
    color: 'text-green-600',
    bgColor: 'bg-green-100',
  },
  {
    id: '4',
    type: 'completion',
    icon: CheckCircle,
    title: 'Serviço concluído',
    description: 'Reparo de Circuitos foi finalizado com sucesso',
    time: '1 dia atrás',
    color: 'text-green-600',
    bgColor: 'bg-green-100',
  },
];

const formatCurrency = (value: number) => {
  return new Intl.NumberFormat('pt-BR', {
    style: 'currency',
    currency: 'BRL',
  }).format(value);
};

const getInitials = (name: string) => {
  return name
    .split(' ')
    .map((n) => n[0])
    .join('')
    .toUpperCase()
    .slice(0, 2);
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
  const notification: ServiceRequestNotification = event.detail;
  console.log('📩 Notificação recebida no Dashboard:', notification);

  switch (notification.type) {
    case 'NEW_SERVICE_REQUEST':
      toast.success(`Nova solicitação: ${notification.serviceName} - ${notification.contractorName}`);
      break;
    case 'PROPOSAL_ACCEPTED':
      toast.success(`Proposta aceita por ${notification.contractorName}!`);
      break;
    case 'SERVICE_REQUEST_STATUS_CHANGED':
      if (notification.status === 'IN_PROGRESS') {
        toast.success('Pagamento confirmado! Serviço em andamento');
      } else {
        toast.info(notification.message || `Status alterado para ${notification.status}`);
      }
      break;
    case 'SERVICE_REQUEST_CANCELLED':
      toast.info(`Solicitação cancelada: ${notification.message}`);
      break;
    case 'NEW_REVIEW':
      toast.success(`${notification.reviewerName} avaliou seu serviço com ${notification.rating} estrelas`);
      break;
    default:
      toast.info(notification.message);
  }
};

onMounted(async () => {
  const userEmail = getUserEmail();
  if (userEmail) {
    console.log('🔌 Inicializando WebSocket no Dashboard para:', userEmail);
    wsConnection = useServiceRequestWebSocket(userEmail);
    try {
      await wsConnection.connect();
      console.log('✅ WebSocket conectado com sucesso no Dashboard');
    } catch (error) {
      console.error('❌ Erro ao conectar WebSocket no Dashboard:', error);
    }
    window.addEventListener('service-request-notification', handleNotification);
  } else {
    console.warn('⚠️ Email do usuário não encontrado para WebSocket');
  }
});

onUnmounted(() => {
  console.log('🔌 Desconectando WebSocket do Dashboard');
  window.removeEventListener('service-request-notification', handleNotification);
  if (wsConnection) {
    wsConnection.disconnect();
  }
});
</script>

<template>
  <div class="max-w-7xl mx-auto space-y-6">
    <!-- Header -->
    <div>
      <h1 class="text-[#263238] mb-2">Dashboard</h1>
      <p class="text-[#435574]">Bem-vindo de volta! Aqui está um resumo do seu desempenho.</p>
    </div>

    <!-- Period Selector -->
    <div class="flex gap-2">
      <Button
        @click="selectedPeriod = 'week'"
        :variant="selectedPeriod === 'week' ? 'default' : 'outline'"
        size="sm"
        :class="selectedPeriod === 'week' ? 'bg-[#286AD8] hover:bg-[#435574]' : ''"
      >
        Semana
      </Button>
      <Button
        @click="selectedPeriod = 'month'"
        :variant="selectedPeriod === 'month' ? 'default' : 'outline'"
        size="sm"
        :class="selectedPeriod === 'month' ? 'bg-[#286AD8] hover:bg-[#435574]' : ''"
      >
        Mês
      </Button>
      <Button
        @click="selectedPeriod = 'year'"
        :variant="selectedPeriod === 'year' ? 'default' : 'outline'"
        size="sm"
        :class="selectedPeriod === 'year' ? 'bg-[#286AD8] hover:bg-[#435574]' : ''"
      >
        Ano
      </Button>
    </div>

    <!-- Stats Cards -->
    <div class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4">
      <!-- Revenue -->
      <Card class="p-6">
        <div class="flex items-start justify-between mb-4">
          <div class="w-12 h-12 rounded-full bg-green-100 flex items-center justify-center">
            <DollarSign class="h-6 w-6 text-green-600" />
          </div>
          <Badge
            :class="`${
              stats.revenue.change >= 0
                ? 'bg-green-100 text-green-700 hover:bg-green-100'
                : 'bg-red-100 text-red-700 hover:bg-red-100'
            }`"
          >
            <span class="flex items-center gap-1">
              <ArrowUp v-if="stats.revenue.change >= 0" class="h-3 w-3" />
              <ArrowDown v-else class="h-3 w-3" />
              {{ Math.abs(stats.revenue.change) }}%
            </span>
          </Badge>
        </div>
        <div>
          <p class="text-sm text-[#435574] mb-1">Receita Total</p>
          <p class="text-2xl text-[#263238]">{{ formatCurrency(stats.revenue.current) }}</p>
          <p class="text-xs text-[#435574] mt-1">
            vs {{ formatCurrency(stats.revenue.previous) }} mês anterior
          </p>
        </div>
      </Card>

      <!-- Active Services -->
      <Card class="p-6">
        <div class="flex items-start justify-between mb-4">
          <div class="w-12 h-12 rounded-full bg-blue-100 flex items-center justify-center">
            <Briefcase class="h-6 w-6 text-[#286AD8]" />
          </div>
          <Badge
            :class="`${
              stats.activeServices.change >= 0
                ? 'bg-green-100 text-green-700 hover:bg-green-100'
                : 'bg-red-100 text-red-700 hover:bg-red-100'
            }`"
          >
            <span class="flex items-center gap-1">
              <ArrowUp v-if="stats.activeServices.change >= 0" class="h-3 w-3" />
              <ArrowDown v-else class="h-3 w-3" />
              {{ Math.abs(stats.activeServices.change) }}%
            </span>
          </Badge>
        </div>
        <div>
          <p class="text-sm text-[#435574] mb-1">Serviços Ativos</p>
          <p class="text-2xl text-[#263238]">{{ stats.activeServices.current }}</p>
          <p class="text-xs text-[#435574] mt-1">
            vs {{ stats.activeServices.previous }} mês anterior
          </p>
        </div>
      </Card>

      <!-- Completed Jobs -->
      <Card class="p-6">
        <div class="flex items-start justify-between mb-4">
          <div class="w-12 h-12 rounded-full bg-purple-100 flex items-center justify-center">
            <CheckCircle class="h-6 w-6 text-purple-600" />
          </div>
          <Badge
            :class="`${
              stats.completedJobs.change >= 0
                ? 'bg-green-100 text-green-700 hover:bg-green-100'
                : 'bg-red-100 text-red-700 hover:bg-red-100'
            }`"
          >
            <span class="flex items-center gap-1">
              <ArrowUp v-if="stats.completedJobs.change >= 0" class="h-3 w-3" />
              <ArrowDown v-else class="h-3 w-3" />
              {{ Math.abs(stats.completedJobs.change) }}%
            </span>
          </Badge>
        </div>
        <div>
          <p class="text-sm text-[#435574] mb-1">Trabalhos Concluídos</p>
          <p class="text-2xl text-[#263238]">{{ stats.completedJobs.current }}</p>
          <p class="text-xs text-[#435574] mt-1">
            Total no período selecionado
          </p>
        </div>
      </Card>

      <!-- Rating -->
      <Card class="p-6">
        <div class="flex items-start justify-between mb-4">
          <div class="w-12 h-12 rounded-full bg-yellow-100 flex items-center justify-center">
            <Star class="h-6 w-6 text-yellow-600" />
          </div>
          <Badge
            :class="`${
              stats.rating.change >= 0
                ? 'bg-green-100 text-green-700 hover:bg-green-100'
                : 'bg-red-100 text-red-700 hover:bg-red-100'
            }`"
          >
            <span class="flex items-center gap-1">
              <ArrowUp v-if="stats.rating.change >= 0" class="h-3 w-3" />
              <ArrowDown v-else class="h-3 w-3" />
              {{ Math.abs(stats.rating.change) }}%
            </span>
          </Badge>
        </div>
        <div>
          <p class="text-sm text-[#435574] mb-1">Avaliação Média</p>
          <p class="text-2xl text-[#263238]">{{ stats.rating.current.toFixed(1) }} ⭐</p>
          <p class="text-xs text-[#435574] mt-1">
            Baseado em 127 avaliações
          </p>
        </div>
      </Card>
    </div>

    <!-- Charts Row -->
    <div class="grid grid-cols-1 lg:grid-cols-3 gap-6">
      <!-- Revenue Chart (Simplified) -->
      <Card class="p-6 lg:col-span-2">
        <div class="flex items-center justify-between mb-6">
          <div>
            <h3 class="text-[#263238] mb-1">Receita e Propostas</h3>
            <p class="text-sm text-[#435574]">Últimos 6 meses</p>
          </div>
          <TrendingUp class="h-5 w-5 text-green-600" />
        </div>
        <div class="h-[280px] flex flex-col justify-end gap-4">
          <div class="flex items-end justify-between gap-2 h-full">
            <div
              v-for="(item, index) in revenueData"
              :key="index"
              class="flex-1 flex flex-col items-center gap-2"
            >
              <div class="relative w-full h-full flex flex-col justify-end gap-1">
                <div
                  class="w-full bg-[#286AD8] rounded-t"
                  :style="{ height: `${(item.revenue / 8500) * 100}%` }"
                  :title="`Receita: ${formatCurrency(item.revenue)}`"
                />
                <div
                  class="w-full bg-[#435574] rounded-t opacity-70"
                  :style="{ height: `${(item.proposals / 30) * 100}%` }"
                  :title="`Propostas: ${item.proposals}`"
                />
              </div>
              <span class="text-xs text-[#435574]">{{ item.month }}</span>
            </div>
          </div>
          <div class="flex items-center justify-center gap-4 mt-2">
            <div class="flex items-center gap-2">
              <div class="w-4 h-4 bg-[#286AD8] rounded"></div>
              <span class="text-sm text-[#435574]">Receita</span>
            </div>
            <div class="flex items-center gap-2">
              <div class="w-4 h-4 bg-[#435574] rounded"></div>
              <span class="text-sm text-[#435574]">Propostas</span>
            </div>
          </div>
        </div>
      </Card>

      <!-- Services by Category (Simplified) -->
      <Card class="p-6">
        <div class="mb-6">
          <h3 class="text-[#263238] mb-1">Serviços por Categoria</h3>
          <p class="text-sm text-[#435574]">Distribuição atual</p>
        </div>
        <div class="h-[280px] flex flex-col justify-center gap-4">
          <div
            v-for="item in servicesByCategoryData"
            :key="item.name"
            class="space-y-2"
          >
            <div class="flex items-center justify-between text-sm">
              <div class="flex items-center gap-2">
                <div
                  class="w-3 h-3 rounded-full"
                  :style="{ backgroundColor: item.color }"
                />
                <span class="text-[#435574]">{{ item.name }}</span>
              </div>
              <span class="text-[#263238] font-semibold">{{ item.value }}%</span>
            </div>
            <div class="w-full bg-gray-200 rounded-full h-2">
              <div
                class="h-2 rounded-full transition-all"
                :style="{ 
                  width: `${item.value}%`,
                  backgroundColor: item.color 
                }"
              />
            </div>
          </div>
        </div>
      </Card>
    </div>

    <!-- Recent Proposals & Active Jobs -->
    <div class="grid grid-cols-1 lg:grid-cols-2 gap-6">
      <!-- Recent Proposals -->
      <Card class="p-6">
        <div class="flex items-center justify-between mb-6">
          <div>
            <h3 class="text-[#263238] mb-1">Propostas Recentes</h3>
            <p class="text-sm text-[#435574]">{{ recentProposals.length }} aguardando resposta</p>
          </div>
          <Button
            @click="props.onNavigate('freelancer-proposals')"
            variant="outline"
            size="sm"
          >
            Ver Todas
          </Button>
        </div>
        <div class="space-y-4">
          <div
            v-for="proposal in recentProposals"
            :key="proposal.id"
            class="p-4 rounded-lg border border-gray-200 hover:border-[#286AD8] transition-colors cursor-pointer"
            @click="props.onNavigate('freelancer-proposals', { proposalId: proposal.id })"
          >
            <div class="flex items-start gap-3 mb-3">
              <Avatar class="h-10 w-10">
                <AvatarImage :src="proposal.clientAvatar" :alt="proposal.clientName" />
                <AvatarFallback class="bg-[#286AD8] text-white">
                  {{ getInitials(proposal.clientName) }}
                </AvatarFallback>
              </Avatar>
              <div class="flex-1">
                <div class="flex items-start justify-between mb-1">
                  <div>
                    <p class="text-sm text-[#263238]">{{ proposal.clientName }}</p>
                    <p class="text-xs text-[#435574]">{{ proposal.receivedAt }}</p>
                  </div>
                  <Badge variant="secondary" class="text-xs">
                    {{ proposal.service }}
                  </Badge>
                </div>
                <p class="text-sm text-[#435574] line-clamp-2 mb-2">
                  {{ proposal.description }}
                </p>
                <div class="flex items-center gap-4 text-xs text-[#435574]">
                  <span class="flex items-center gap-1">
                    <DollarSign class="h-3 w-3" />
                    {{ formatCurrency(proposal.budget) }}
                  </span>
                  <span class="flex items-center gap-1">
                    <Clock class="h-3 w-3" />
                    {{ proposal.deadline }}
                  </span>
                </div>
              </div>
            </div>
            <div class="flex gap-2">
              <Button size="sm" class="flex-1 bg-[#286AD8] hover:bg-[#435574]">
                Responder
              </Button>
              <Button size="sm" variant="outline" class="flex-1">
                Recusar
              </Button>
            </div>
          </div>
        </div>
      </Card>

      <!-- Active Jobs -->
      <Card class="p-6">
        <div class="flex items-center justify-between mb-6">
          <div>
            <h3 class="text-[#263238] mb-1">Serviços em Andamento</h3>
            <p class="text-sm text-[#435574]">{{ activeJobs.length }} projetos ativos</p>
          </div>
          <Button
            @click="props.onNavigate('freelancer-proposals')"
            variant="outline"
            size="sm"
          >
            Ver Todos
          </Button>
        </div>
        <div class="space-y-4">
          <div
            v-for="job in activeJobs"
            :key="job.id"
            class="p-4 rounded-lg border border-gray-200 hover:border-[#286AD8] transition-colors cursor-pointer"
          >
            <div class="flex items-start justify-between mb-3">
              <div>
                <p class="text-sm text-[#263238] mb-1">{{ job.service }}</p>
                <p class="text-xs text-[#435574]">Cliente: {{ job.clientName }}</p>
              </div>
              <Badge variant="secondary" class="text-xs">
                {{ job.deadline }}
              </Badge>
            </div>
            <div class="space-y-2">
              <div class="flex items-center justify-between text-xs">
                <span class="text-[#435574]">Progresso</span>
                <span class="text-[#263238]">{{ job.progress }}%</span>
              </div>
              <Progress :value="job.progress" class="h-2" />
            </div>
            <div class="flex gap-2 mt-3">
              <Button size="sm" variant="outline" class="flex-1">
                <MessageSquare class="h-3 w-3 mr-1" />
                Mensagens
              </Button>
              <Button size="sm" variant="outline" class="flex-1">
                <Eye class="h-3 w-3 mr-1" />
                Ver Detalhes
              </Button>
            </div>
          </div>
        </div>
      </Card>
    </div>

    <!-- Recent Activities -->
    <Card class="p-6">
      <div class="mb-6">
        <h3 class="text-[#263238] mb-1">Atividades Recentes</h3>
        <p class="text-sm text-[#435574]">Últimas atualizações</p>
      </div>
      <div class="space-y-4">
        <div
          v-for="activity in recentActivities"
          :key="activity.id"
          class="flex items-start gap-4"
        >
          <div :class="`w-10 h-10 rounded-full ${activity.bgColor} flex items-center justify-center flex-shrink-0`">
            <component :is="activity.icon" :class="`h-5 w-5 ${activity.color}`" />
          </div>
          <div class="flex-1 min-w-0">
            <p class="text-sm text-[#263238]">{{ activity.title }}</p>
            <p class="text-sm text-[#435574]">{{ activity.description }}</p>
            <p class="text-xs text-[#435574] mt-1">{{ activity.time }}</p>
          </div>
        </div>
      </div>
    </Card>
  </div>
</template>

