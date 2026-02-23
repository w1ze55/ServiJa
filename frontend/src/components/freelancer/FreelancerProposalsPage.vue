<script setup lang="ts">
import { ref, computed, onMounted, watch } from 'vue';
import {
  FileText,
  Search,
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
  Trash2,
  MoreVertical,
  RefreshCw,
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
import DropdownMenu from '../ui/dropdown-menu.vue';
import DropdownMenuContent from '../ui/dropdown-menu-content.vue';
import DropdownMenuItem from '../ui/dropdown-menu-item.vue';
import DropdownMenuSeparator from '../ui/dropdown-menu-separator.vue';
import DropdownMenuTrigger from '../ui/dropdown-menu-trigger.vue';
import AlertDialog from '../ui/alert-dialog.vue';
import AlertDialogAction from '../ui/alert-dialog-action.vue';
import AlertDialogCancel from '../ui/alert-dialog-cancel.vue';
import AlertDialogContent from '../ui/alert-dialog-content.vue';
import AlertDialogDescription from '../ui/alert-dialog-description.vue';
import AlertDialogFooter from '../ui/alert-dialog-footer.vue';
import AlertDialogHeader from '../ui/alert-dialog-header.vue';
import AlertDialogTitle from '../ui/alert-dialog-title.vue';
import Dialog from '../ui/dialog.vue';
import DialogContent from '../ui/dialog-content.vue';
import DialogDescription from '../ui/dialog-description.vue';
import DialogFooter from '../ui/dialog-footer.vue';
import DialogHeader from '../ui/dialog-header.vue';
import DialogTitle from '../ui/dialog-title.vue';
import Textarea from '../ui/textarea.vue';
import Label from '../ui/label.vue';
import { useProposalStore, type Proposal } from '@/stores/proposalStore';
import { toast } from '@/utils/toast';

interface Props {
  onNavigate: (page: string, params?: any) => void;
}

const props = defineProps<Props>();
const proposalStore = useProposalStore();

const searchQuery = ref('');
const categoryFilter = ref('all');
const sortBy = ref('recent');
const currentTab = ref('pending');
const currentPage = ref(1);
const itemsPerPage = 6;

const showRejectDialog = ref(false);
const showArchiveDialog = ref(false);
const selectedProposal = ref<Proposal | null>(null);
const rejectReason = ref('');

const categories = ['Eletricista', 'Encanador', 'Pintor', 'Pedreiro', 'Designer', 'Desenvolvedor'];

onMounted(() => {
  proposalStore.loadProposals();
});

const filteredProposals = computed(() => {
  let filtered = proposalStore.proposals.filter((proposal) => {
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
      (currentTab.value === 'rejected' && proposal.status === 'rejected') ||
      (currentTab.value === 'completed' && (proposal.status === 'completed' || proposal.status === 'archived'));

    return matchesSearch && matchesCategory && matchesTab;
  });

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

const totalPages = computed(() => Math.ceil(filteredProposals.value.length / itemsPerPage));
const startIndex = computed(() => (currentPage.value - 1) * itemsPerPage);
const currentProposals = computed(() =>
  filteredProposals.value.slice(startIndex.value, startIndex.value + itemsPerPage)
);

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
    hour: '2-digit',
    minute: '2-digit',
  });
};

const getInitials = (name: string) => {
  return name
    .split(' ')
    .map((n) => n[0])
    .join('')
    .toUpperCase()
    .slice(0, 2);
};

const handleSearchChange = (value: string) => {
  searchQuery.value = value;
  currentPage.value = 1;
};

const handleViewDetails = (proposal: Proposal) => {
  proposalStore.markAsViewed(proposal.id);
  props.onNavigate('freelancer-proposal-detail', { requestId: proposal.id });
};

const handleOpenRejectDialog = (proposal: Proposal) => {
  selectedProposal.value = proposal;
  rejectReason.value = '';
  showRejectDialog.value = true;
};

const handleRejectProposal = async () => {
  if (!selectedProposal.value) return;
  
  if (!rejectReason.value.trim()) {
    toast.error('Por favor, informe o motivo da recusa');
    return;
  }

  const success = await proposalStore.rejectProposal(selectedProposal.value.id, rejectReason.value);
  if (success) {
    toast.success('Solicitação recusada com sucesso');
    showRejectDialog.value = false;
    selectedProposal.value = null;
    rejectReason.value = '';
  } else {
    toast.error('Erro ao recusar solicitação');
  }
};

const handleOpenArchiveDialog = (proposal: Proposal) => {
  selectedProposal.value = proposal;
  showArchiveDialog.value = true;
};

const handleArchiveProposal = async () => {
  if (!selectedProposal.value) return;

  const success = await proposalStore.archiveProposal(selectedProposal.value.id);
  if (success) {
    toast.success('Solicitação arquivada com sucesso');
    showArchiveDialog.value = false;
    selectedProposal.value = null;
  } else {
    toast.error('Erro ao arquivar solicitação');
  }
};

const handleRefresh = () => {
  proposalStore.loadProposals();
  toast.info('Atualizando solicitações...');
};

watch(currentTab, () => {
  currentPage.value = 1;
});
</script>

<template>
  <div class="max-w-7xl mx-auto space-y-6">
    <!-- Header -->
    <div class="flex items-start justify-between">
      <div>
        <h1 class="text-2xl font-bold text-[#263238] mb-2">Solicitações Recebidas</h1>
        <p class="text-[#435574]">Gerencie as propostas enviadas pelos clientes</p>
      </div>
      <Button @click="handleRefresh" variant="outline" :disabled="proposalStore.isLoading">
        <RefreshCw :class="`h-4 w-4 mr-2 ${proposalStore.isLoading ? 'animate-spin' : ''}`" />
        Atualizar
      </Button>
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
            <p class="text-xl font-bold text-[#263238]">{{ proposalStore.stats.pending }}</p>
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
            <p class="text-xl font-bold text-[#263238]">{{ proposalStore.stats.negotiating }}</p>
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
            <p class="text-xl font-bold text-[#263238]">{{ proposalStore.stats.accepted }}</p>
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
            <p class="text-xl font-bold text-[#263238]">{{ proposalStore.stats.completed }}</p>
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
    <Tabs :model-value="currentTab" @update:model-value="(val: string) => currentTab = val">
      <TabsList class="grid w-full grid-cols-6">
        <TabsTrigger value="all">Todas</TabsTrigger>
        <TabsTrigger value="pending">
          Pendentes {{ proposalStore.stats.pending > 0 ? `(${proposalStore.stats.pending})` : '' }}
        </TabsTrigger>
        <TabsTrigger value="negotiating">Negociando</TabsTrigger>
        <TabsTrigger value="accepted">Aceitas</TabsTrigger>
        <TabsTrigger value="rejected">Recusadas</TabsTrigger>
        <TabsTrigger value="completed">Concluídas</TabsTrigger>
      </TabsList>

      <TabsContent :value="currentTab" class="mt-6">
        <!-- Loading State -->
        <div v-if="proposalStore.isLoading" class="flex justify-center py-12">
          <div class="animate-spin h-8 w-8 border-4 border-[#286AD8] border-t-transparent rounded-full"></div>
        </div>

        <!-- Empty State -->
        <Card v-else-if="currentProposals.length === 0" class="p-12 text-center">
          <FileText class="h-12 w-12 text-gray-400 mx-auto mb-4" />
          <h3 class="text-lg font-semibold text-[#263238] mb-2">Nenhuma solicitação encontrada</h3>
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
              :class="`p-6 hover:shadow-md transition-all ${
                !proposal.viewed ? 'border-l-4 border-l-[#286AD8] bg-blue-50/30' : ''
              }`"
            >
              <div class="flex items-start gap-4">
                <Avatar class="h-12 w-12 cursor-pointer" @click="handleViewDetails(proposal)">
                  <AvatarImage :src="proposal.clientAvatar" :alt="proposal.clientName" />
                  <AvatarFallback class="bg-[#286AD8] text-white">
                    {{ getInitials(proposal.clientName) }}
                  </AvatarFallback>
                </Avatar>

                <div class="flex-1 min-w-0">
                  <div class="flex items-start justify-between mb-2">
                    <div class="flex-1 cursor-pointer" @click="handleViewDetails(proposal)">
                      <div class="flex items-center gap-2 mb-1">
                        <h3 class="font-semibold text-[#263238] hover:text-[#286AD8]">{{ proposal.service }}</h3>
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
                          {{ formatDate(proposal.receivedAt) }}
                        </span>
                      </div>
                    </div>
                    <div class="flex items-center gap-2">
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
                          <Badge class="bg-red-100 text-red-700 hover:bg-red-100">
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

                      <!-- Actions Menu -->
                      <DropdownMenu>
                        <DropdownMenuTrigger as-child>
                          <Button variant="ghost" size="icon">
                            <MoreVertical class="h-4 w-4" />
                          </Button>
                        </DropdownMenuTrigger>
                        <DropdownMenuContent align="end">
                          <DropdownMenuItem @click="handleViewDetails(proposal)">
                            <Eye class="h-4 w-4 mr-2" />
                            Ver Detalhes
                          </DropdownMenuItem>
                          <DropdownMenuSeparator />
                          <DropdownMenuItem
                            v-if="proposal.status === 'pending'"
                            @click="handleOpenRejectDialog(proposal)"
                            class="text-red-600"
                          >
                            <XCircle class="h-4 w-4 mr-2" />
                            Recusar
                          </DropdownMenuItem>
                          <DropdownMenuItem
                            v-if="proposal.status === 'completed' || proposal.status === 'rejected'"
                            @click="handleOpenArchiveDialog(proposal)"
                          >
                            <Archive class="h-4 w-4 mr-2" />
                            Arquivar
                          </DropdownMenuItem>
                        </DropdownMenuContent>
                      </DropdownMenu>
                    </div>
                  </div>

                  <p class="text-sm text-[#435574] mb-3 line-clamp-2">
                    {{ proposal.description }}
                  </p>

                  <div class="flex items-center justify-between">
                    <div class="flex items-center gap-4 text-sm">
                      <span class="flex items-center gap-1 font-medium text-[#263238]">
                        <DollarSign class="h-4 w-4 text-green-600" />
                        {{ formatCurrency(proposal.budget) }}
                      </span>
                      <span class="flex items-center gap-1 text-[#435574]">
                        <Clock class="h-4 w-4" />
                        Prazo: {{ proposal.deadline }}
                      </span>
                      <span
                        v-if="proposal.attachments && proposal.attachments.length > 0"
                        class="flex items-center gap-1 text-[#435574]"
                      >
                        <FileText class="h-4 w-4" />
                        {{ proposal.attachments.length }} anexo{{ proposal.attachments.length > 1 ? 's' : '' }}
                      </span>
                    </div>

                    <Button
                      @click="handleViewDetails(proposal)"
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

    <!-- Reject Dialog -->
    <Dialog :open="showRejectDialog" @update:open="(val) => showRejectDialog = val">
      <DialogContent class="sm:max-w-md">
        <DialogHeader>
          <DialogTitle>Recusar Solicitação</DialogTitle>
          <DialogDescription>
            Informe o motivo da recusa para "{{ selectedProposal?.service }}"
          </DialogDescription>
        </DialogHeader>
        <div class="space-y-4">
          <div class="space-y-2">
            <Label for="reject-reason">Motivo da recusa *</Label>
            <Textarea
              id="reject-reason"
              :model-value="rejectReason"
              @update:model-value="(val) => rejectReason = val as string"
              placeholder="Ex: Não trabalho com esse tipo de serviço, agenda indisponível, etc."
              :rows="4"
            />
          </div>
        </div>
        <DialogFooter>
          <Button @click="showRejectDialog = false" variant="outline">
            Cancelar
          </Button>
          <Button @click="handleRejectProposal" class="bg-red-600 hover:bg-red-700">
            Confirmar Recusa
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>

    <!-- Archive Dialog -->
    <AlertDialog :open="showArchiveDialog" @update:open="(val) => showArchiveDialog = val">
      <AlertDialogContent>
        <AlertDialogHeader>
          <AlertDialogTitle>Arquivar Solicitação</AlertDialogTitle>
          <AlertDialogDescription>
            Tem certeza que deseja arquivar a solicitação "{{ selectedProposal?.service }}"?
            Ela será movida para a aba de concluídas/arquivadas.
          </AlertDialogDescription>
        </AlertDialogHeader>
        <AlertDialogFooter>
          <AlertDialogCancel @click="showArchiveDialog = false">Cancelar</AlertDialogCancel>
          <AlertDialogAction @click="handleArchiveProposal" class="bg-[#286AD8] hover:bg-[#435574]">
            Arquivar
          </AlertDialogAction>
        </AlertDialogFooter>
      </AlertDialogContent>
    </AlertDialog>
  </div>
</template>
