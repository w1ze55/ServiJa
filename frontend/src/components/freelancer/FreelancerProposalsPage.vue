<script setup lang="ts">
import { ref, computed } from 'vue';
import {
  FileText,
  Search,
  Filter,
  Clock,
  DollarSign,
  MapPin,
  Calendar,
  Eye,
  MessageSquare,
  CheckCircle,
  XCircle,
  Archive,
  AlertCircle,
} from 'lucide-vue-next';
import Button from '../ui/button.vue';
import Input from '../ui/input.vue';
import Card from '../ui/card.vue';
import Badge from '../ui/badge.vue';
import Avatar from '../ui/avatar.vue';
import AvatarImage from '../ui/avatar-image.vue';
import AvatarFallback from '../ui/avatar-fallback.vue';
import Select from '../ui/select.vue';
import SelectContent from '../ui/select-content.vue';
import SelectItem from '../ui/select-item.vue';
import SelectTrigger from '../ui/select-trigger.vue';
import SelectValue from '../ui/select-value.vue';
import Tabs from '../ui/tabs.vue';
import TabsContent from '../ui/tabs-content.vue';
import TabsList from '../ui/tabs-list.vue';
import TabsTrigger from '../ui/tabs-trigger.vue';

interface Proposal {
  id: string;
  clientName: string;
  clientAvatar?: string;
  clientLocation: string;
  service: string;
  category: string;
  description: string;
  budget: number;
  deadline: string;
  priority: 'low' | 'medium' | 'high';
  status: 'pending' | 'negotiating' | 'accepted' | 'rejected' | 'completed' | 'archived';
  receivedAt: string;
  attachments?: number;
  viewed: boolean;
}

interface Props {
  onNavigate: (page: string, params?: any) => void;
}

const props = defineProps<Props>();

const searchQuery = ref('');
const categoryFilter = ref('all');
const sortBy = ref('recent');
const currentTab = ref('pending');
const currentPage = ref(1);
const itemsPerPage = 6;

// Mock data
const proposals = ref<Proposal[]>([
  {
    id: '1',
    clientName: 'Maria Silva',
    clientAvatar: undefined,
    clientLocation: 'São Paulo, SP',
    service: 'Instalação Elétrica Residencial',
    category: 'Eletricista',
    description:
      'Preciso de instalação completa do sistema elétrico em um apartamento de 80m². Inclui pontos de luz, tomadas e quadro de distribuição.',
    budget: 2500,
    deadline: '15 dias',
    priority: 'high',
    status: 'pending',
    receivedAt: '2025-11-10 14:30',
    attachments: 3,
    viewed: false,
  },
  {
    id: '2',
    clientName: 'Carlos Mendes',
    clientAvatar: undefined,
    clientLocation: 'Guarulhos, SP',
    service: 'Manutenção Preventiva',
    category: 'Eletricista',
    description:
      'Necessito de revisão completa do sistema elétrico de uma casa com 150m². Verificação de disjuntores e fiação.',
    budget: 800,
    deadline: '7 dias',
    priority: 'medium',
    status: 'pending',
    receivedAt: '2025-11-10 10:15',
    attachments: 1,
    viewed: true,
  },
  {
    id: '3',
    clientName: 'Ana Costa',
    clientAvatar: undefined,
    clientLocation: 'Osasco, SP',
    service: 'Automação Residencial',
    category: 'Eletricista',
    description:
      'Instalação de sistema de automação residencial completo, incluindo iluminação inteligente, controle de temperatura e segurança.',
    budget: 5000,
    deadline: '30 dias',
    priority: 'medium',
    status: 'negotiating',
    receivedAt: '2025-11-09 16:45',
    attachments: 5,
    viewed: true,
  },
  {
    id: '4',
    clientName: 'Roberto Lima',
    clientAvatar: undefined,
    clientLocation: 'São Paulo, SP',
    service: 'Reparo de Circuitos',
    category: 'Eletricista',
    description: 'Preciso de reparo urgente em curto-circuito. Disjuntor caindo constantemente.',
    budget: 350,
    deadline: '2 dias',
    priority: 'high',
    status: 'accepted',
    receivedAt: '2025-11-08 09:00',
    attachments: 0,
    viewed: true,
  },
  {
    id: '5',
    clientName: 'Julia Fernandes',
    clientAvatar: undefined,
    clientLocation: 'Campinas, SP',
    service: 'Instalação de Painéis Solares',
    category: 'Eletricista',
    description:
      'Instalação de sistema de energia solar fotovoltaica residencial. Casa de 120m² com consumo médio de 450kWh/mês.',
    budget: 18000,
    deadline: '45 dias',
    priority: 'low',
    status: 'rejected',
    receivedAt: '2025-11-07 11:20',
    attachments: 8,
    viewed: true,
  },
  {
    id: '6',
    clientName: 'Pedro Oliveira',
    clientAvatar: undefined,
    clientLocation: 'São Paulo, SP',
    service: 'Instalação de Chuveiro Elétrico',
    category: 'Eletricista',
    description: 'Instalação de chuveiro elétrico 220V com fiação adequada.',
    budget: 250,
    deadline: '3 dias',
    priority: 'medium',
    status: 'completed',
    receivedAt: '2025-11-01 15:10',
    attachments: 0,
    viewed: true,
  },
]);

const categories = ['Eletricista', 'Encanador', 'Pintor', 'Designer', 'Desenvolvedor'];

// Filter proposals
const filteredProposals = computed(() => {
  let filtered = proposals.value.filter((proposal) => {
    const matchesSearch =
      proposal.clientName.toLowerCase().includes(searchQuery.value.toLowerCase()) ||
      proposal.service.toLowerCase().includes(searchQuery.value.toLowerCase()) ||
      proposal.description.toLowerCase().includes(searchQuery.value.toLowerCase());
    const matchesCategory = categoryFilter.value === 'all' || proposal.category === categoryFilter.value;
    const matchesTab =
      currentTab.value === 'all' ||
      (currentTab.value === 'pending' && proposal.status === 'pending') ||
      (currentTab.value === 'negotiating' && proposal.status === 'negotiating') ||
      (currentTab.value === 'accepted' && proposal.status === 'accepted') ||
      (currentTab.value === 'completed' && (proposal.status === 'completed' || proposal.status === 'archived'));

    return matchesSearch && matchesCategory && matchesTab;
  });

  // Sort proposals
  if (sortBy.value === 'recent') {
    filtered.sort((a, b) => new Date(b.receivedAt).getTime() - new Date(a.receivedAt).getTime());
  } else if (sortBy.value === 'budget-high') {
    filtered.sort((a, b) => b.budget - a.budget);
  } else if (sortBy.value === 'budget-low') {
    filtered.sort((a, b) => a.budget - b.budget);
  } else if (sortBy.value === 'priority') {
    const priorityOrder = { high: 3, medium: 2, low: 1 };
    filtered.sort((a, b) => priorityOrder[b.priority] - priorityOrder[a.priority]);
  }

  return filtered;
});

// Pagination
const totalPages = computed(() => Math.ceil(filteredProposals.value.length / itemsPerPage));
const startIndex = computed(() => (currentPage.value - 1) * itemsPerPage);
const currentProposals = computed(() =>
  filteredProposals.value.slice(startIndex.value, startIndex.value + itemsPerPage)
);

const stats = computed(() => ({
  pending: proposals.value.filter((p) => p.status === 'pending').length,
  negotiating: proposals.value.filter((p) => p.status === 'negotiating').length,
  accepted: proposals.value.filter((p) => p.status === 'accepted').length,
  completed: proposals.value.filter((p) => p.status === 'completed' || p.status === 'archived').length,
}));

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

// Status and Priority badges removed - using templates directly

const handleSearchChange = (value: string) => {
  searchQuery.value = value;
  currentPage.value = 1;
};
</script>

<template>
  <div class="max-w-7xl mx-auto space-y-6">
    <!-- Header -->
    <div>
      <h1 class="text-[#263238] mb-2">Solicitações Recebidas</h1>
      <p class="text-[#435574]">Gerencie as propostas enviadas pelos clientes</p>
    </div>

    <!-- Stats Cards -->
    <div class="grid grid-cols-1 md:grid-cols-4 gap-4">
      <Card
        class="p-4 cursor-pointer hover:shadow-md transition-shadow"
        @click="currentTab = 'pending'"
      >
        <div class="flex items-center gap-3">
          <div class="w-10 h-10 rounded-full bg-blue-100 flex items-center justify-center">
            <Clock class="h-5 w-5 text-blue-600" />
          </div>
          <div>
            <p class="text-sm text-[#435574]">Pendentes</p>
            <p class="text-xl text-[#263238]">{{ stats.pending }}</p>
          </div>
        </div>
      </Card>

      <Card
        class="p-4 cursor-pointer hover:shadow-md transition-shadow"
        @click="currentTab = 'negotiating'"
      >
        <div class="flex items-center gap-3">
          <div class="w-10 h-10 rounded-full bg-purple-100 flex items-center justify-center">
            <MessageSquare class="h-5 w-5 text-purple-600" />
          </div>
          <div>
            <p class="text-sm text-[#435574]">Negociando</p>
            <p class="text-xl text-[#263238]">{{ stats.negotiating }}</p>
          </div>
        </div>
      </Card>

      <Card
        class="p-4 cursor-pointer hover:shadow-md transition-shadow"
        @click="currentTab = 'accepted'"
      >
        <div class="flex items-center gap-3">
          <div class="w-10 h-10 rounded-full bg-green-100 flex items-center justify-center">
            <CheckCircle class="h-5 w-5 text-green-600" />
          </div>
          <div>
            <p class="text-sm text-[#435574]">Aceitas</p>
            <p class="text-xl text-[#263238]">{{ stats.accepted }}</p>
          </div>
        </div>
      </Card>

      <Card
        class="p-4 cursor-pointer hover:shadow-md transition-shadow"
        @click="currentTab = 'completed'"
      >
        <div class="flex items-center gap-3">
          <div class="w-10 h-10 rounded-full bg-gray-100 flex items-center justify-center">
            <Archive class="h-5 w-5 text-gray-600" />
          </div>
          <div>
            <p class="text-sm text-[#435574]">Concluídas</p>
            <p class="text-xl text-[#263238]">{{ stats.completed }}</p>
          </div>
        </div>
      </Card>
    </div>

    <!-- Filters -->
    <Card class="p-4">
      <div class="grid grid-cols-1 md:grid-cols-3 gap-4">
        <div class="relative">
          <Search class="absolute left-3 top-1/2 -translate-y-1/2 h-5 w-5 text-[#435574]" />
          <Input
            placeholder="Buscar solicitações..."
            :model-value="searchQuery"
            @update:model-value="handleSearchChange"
            class="pl-10"
          />
        </div>

        <Select :model-value="categoryFilter" @update:model-value="(val: string) => categoryFilter = val">
          <SelectTrigger>
            <SelectValue placeholder="Todas as categorias" />
          </SelectTrigger>
          <SelectContent>
            <SelectItem value="all">Todas as categorias</SelectItem>
            <SelectItem v-for="cat in categories" :key="cat" :value="cat">
              {{ cat }}
            </SelectItem>
          </SelectContent>
        </Select>

        <Select :model-value="sortBy" @update:model-value="(val: string) => sortBy = val">
          <SelectTrigger>
            <SelectValue placeholder="Ordenar por" />
          </SelectTrigger>
          <SelectContent>
            <SelectItem value="recent">Mais recentes</SelectItem>
            <SelectItem value="priority">Prioridade</SelectItem>
            <SelectItem value="budget-high">Maior orçamento</SelectItem>
            <SelectItem value="budget-low">Menor orçamento</SelectItem>
          </SelectContent>
        </Select>
      </div>
    </Card>

    <!-- Tabs -->
    <Tabs :model-value="currentTab" @update:model-value="(val: string) => { currentTab = val; currentPage = 1; }">
      <TabsList class="grid w-full grid-cols-5">
        <TabsTrigger value="all">Todas</TabsTrigger>
        <TabsTrigger value="pending">
          Pendentes {{ stats.pending > 0 ? `(${stats.pending})` : '' }}
        </TabsTrigger>
        <TabsTrigger value="negotiating">Negociando</TabsTrigger>
        <TabsTrigger value="accepted">Aceitas</TabsTrigger>
        <TabsTrigger value="completed">Concluídas</TabsTrigger>
      </TabsList>

      <TabsContent :value="currentTab" class="mt-6">
        <Card v-if="currentProposals.length === 0" class="p-12 text-center">
          <FileText class="h-12 w-12 text-gray-400 mx-auto mb-4" />
          <h3 class="text-[#263238] mb-2">Nenhuma solicitação encontrada</h3>
          <p class="text-[#435574]">
            {{ searchQuery || categoryFilter !== 'all'
              ? 'Tente ajustar os filtros'
              : 'Você receberá notificações quando novos clientes enviarem propostas' }}
          </p>
        </Card>
        <template v-else>
          <!-- Proposals List -->
          <div class="grid grid-cols-1 gap-4">
            <Card
              v-for="proposal in currentProposals"
              :key="proposal.id"
              :class="`p-6 hover:shadow-md transition-all cursor-pointer ${
                !proposal.viewed ? 'border-l-4 border-l-[#286AD8] bg-blue-50/30' : ''
              }`"
              @click="props.onNavigate('freelancer-proposal-detail', { proposalId: proposal.id })"
            >
              <div class="flex items-start gap-4">
                <Avatar class="h-12 w-12">
                  <AvatarImage :src="proposal.clientAvatar" :alt="proposal.clientName" />
                  <AvatarFallback class="bg-[#286AD8] text-white">
                    {{ getInitials(proposal.clientName) }}
                  </AvatarFallback>
                </Avatar>

                <div class="flex-1 min-w-0">
                  <div class="flex items-start justify-between mb-2">
                    <div class="flex-1">
                      <div class="flex items-center gap-2 mb-1">
                        <h3 class="text-[#263238]">{{ proposal.service }}</h3>
                        <Badge v-if="!proposal.viewed" class="bg-[#286AD8] hover:bg-[#286AD8] text-xs">
                          Novo
                        </Badge>
                      </div>
                      <div class="flex items-center gap-3 text-sm text-[#435574]">
                        <span class="flex items-center gap-1">
                          <Avatar class="h-4 w-4">
                            <AvatarFallback class="bg-gray-300 text-[10px]">
                              {{ getInitials(proposal.clientName) }}
                            </AvatarFallback>
                          </Avatar>
                          {{ proposal.clientName }}
                        </span>
                        <span class="flex items-center gap-1">
                          <MapPin class="h-3 w-3" />
                          {{ proposal.clientLocation }}
                        </span>
                        <span class="flex items-center gap-1">
                          <Calendar class="h-3 w-3" />
                          {{ proposal.receivedAt }}
                        </span>
                      </div>
                    </div>
                    <div class="flex flex-col items-end gap-2">
                      <!-- Status Badge -->
                      <template v-if="proposal.status === 'pending'">
                        <Badge class="bg-blue-100 text-blue-700 hover:bg-blue-100">
                          <Clock class="h-3 w-3 mr-1" />
                          Pendente
                        </Badge>
                      </template>
                      <template v-else-if="proposal.status === 'negotiating'">
                        <Badge class="bg-purple-100 text-purple-700 hover:bg-purple-100">
                          <MessageSquare class="h-3 w-3 mr-1" />
                          Negociando
                        </Badge>
                      </template>
                      <template v-else-if="proposal.status === 'accepted'">
                        <Badge class="bg-green-100 text-green-700 hover:bg-green-100">
                          <CheckCircle class="h-3 w-3 mr-1" />
                          Aceita
                        </Badge>
                      </template>
                      <template v-else-if="proposal.status === 'rejected'">
                        <Badge class="bg-gray-100 text-gray-700 hover:bg-gray-100">
                          <XCircle class="h-3 w-3 mr-1" />
                          Recusada
                        </Badge>
                      </template>
                      <template v-else-if="proposal.status === 'completed'">
                        <Badge class="bg-green-100 text-green-700 hover:bg-green-100">
                          <CheckCircle class="h-3 w-3 mr-1" />
                          Concluída
                        </Badge>
                      </template>
                      <template v-else-if="proposal.status === 'archived'">
                        <Badge variant="secondary">
                          <Archive class="h-3 w-3 mr-1" />
                          Arquivada
                        </Badge>
                      </template>

                      <!-- Priority Badge -->
                      <template v-if="proposal.priority === 'high'">
                        <Badge class="bg-red-100 text-red-700 hover:bg-red-100">
                          <AlertCircle class="h-3 w-3 mr-1" />
                          Alta
                        </Badge>
                      </template>
                      <template v-else-if="proposal.priority === 'medium'">
                        <Badge class="bg-yellow-100 text-yellow-700 hover:bg-yellow-100">
                          Média
                        </Badge>
                      </template>
                      <template v-else-if="proposal.priority === 'low'">
                        <Badge variant="secondary">Baixa</Badge>
                      </template>
                    </div>
                  </div>

                  <p class="text-sm text-[#435574] mb-3 line-clamp-2">
                    {{ proposal.description }}
                  </p>

                  <div class="flex items-center justify-between">
                    <div class="flex items-center gap-4 text-sm">
                      <span class="flex items-center gap-1 text-[#263238]">
                        <DollarSign class="h-4 w-4 text-green-600" />
                        {{ formatCurrency(proposal.budget) }}
                      </span>
                      <span class="flex items-center gap-1 text-[#435574]">
                        <Clock class="h-4 w-4" />
                        Prazo: {{ proposal.deadline }}
                      </span>
                      <span
                        v-if="proposal.attachments && proposal.attachments > 0"
                        class="flex items-center gap-1 text-[#435574]"
                      >
                        <FileText class="h-4 w-4" />
                        {{ proposal.attachments }} anexo{{ proposal.attachments > 1 ? 's' : '' }}
                      </span>
                    </div>

                    <Button
                      v-if="proposal.status === 'pending'"
                      @click.stop="props.onNavigate('freelancer-proposal-detail', { requestId: proposal.id })"
                      variant="outline"
                      size="sm"
                    >
                      <Eye class="h-4 w-4 mr-2" />
                      Ver Detalhes
                    </Button>
                  </div>
                </div>
              </div>
            </Card>
          </div>

          <!-- Pagination -->
          <div v-if="totalPages > 1" class="flex items-center justify-center gap-2 mt-6">
            <Button
              @click="currentPage = Math.max(1, currentPage - 1)"
              :disabled="currentPage === 1"
              variant="outline"
              size="sm"
            >
              Anterior
            </Button>
            <div class="flex items-center gap-1">
              <Button
                v-for="pageNum in Array.from({ length: Math.min(5, totalPages) }, (_, i) => {
                  if (totalPages <= 5) return i + 1;
                  if (currentPage <= 3) return i + 1;
                  if (currentPage >= totalPages - 2) return totalPages - 4 + i;
                  return currentPage - 2 + i;
                })"
                :key="pageNum"
                @click="currentPage = pageNum"
                :variant="currentPage === pageNum ? 'default' : 'outline'"
                size="sm"
                :class="currentPage === pageNum ? 'bg-[#286AD8] hover:bg-[#435574]' : ''"
              >
                {{ pageNum }}
              </Button>
            </div>
            <Button
              @click="currentPage = Math.min(totalPages, currentPage + 1)"
              :disabled="currentPage === totalPages"
              variant="outline"
              size="sm"
            >
              Próxima
            </Button>
          </div>
        </template>
      </TabsContent>
    </Tabs>
  </div>
</template>

