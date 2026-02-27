<script setup lang="ts">
import { ref, computed, onMounted, watch } from 'vue';
import {
  ArrowLeft,
  Calendar,
  MapPin,
  DollarSign,
  Clock,
  FileText,
  Download,
  Send,
  Plus,
  Trash2,
  AlertCircle,
  CheckCircle,
  User,
  MessageSquare,
  Milestone as MilestoneIcon,
  Upload,
  X,
  Eye,
  Paperclip,
  XCircle,
  Phone,
  Mail,
  Play,
  Check,
} from 'lucide-vue-next';
import Button from '../ui/button.vue';
import Input from '../ui/input.vue';
import Label from '../ui/label.vue';
import Card from '../ui/card.vue';
import Badge from '../ui/badge.vue';
import Textarea from '../ui/textarea.vue';
import Dialog from '../ui/dialog.vue';
import DialogContent from '../ui/dialog-content.vue';
import DialogDescription from '../ui/dialog-description.vue';
import DialogFooter from '../ui/dialog-footer.vue';
import DialogHeader from '../ui/dialog-header.vue';
import DialogTitle from '../ui/dialog-title.vue';
import AlertDialog from '../ui/alert-dialog.vue';
import AlertDialogAction from '../ui/alert-dialog-action.vue';
import AlertDialogCancel from '../ui/alert-dialog-cancel.vue';
import AlertDialogContent from '../ui/alert-dialog-content.vue';
import AlertDialogDescription from '../ui/alert-dialog-description.vue';
import AlertDialogFooter from '../ui/alert-dialog-footer.vue';
import AlertDialogHeader from '../ui/alert-dialog-header.vue';
import AlertDialogTitle from '../ui/alert-dialog-title.vue';
import Tabs from '../ui/tabs.vue';
import TabsContent from '../ui/tabs-content.vue';
import TabsList from '../ui/tabs-list.vue';
import TabsTrigger from '../ui/tabs-trigger.vue';
import Avatar from '../ui/avatar.vue';
import AvatarFallback from '../ui/avatar-fallback.vue';
import AvatarImage from '../ui/avatar-image.vue';
import { useProposalStore, type Proposal, type Milestone } from '@/stores/proposalStore';
import ServiceRequestTimeline from '../service/ServiceRequestTimeline.vue';
import { toast } from '@/utils/toast';

interface Props {
  onNavigate: (page: string, params?: any) => void;
  requestId?: string;
}

const props = defineProps<Props>();
const proposalStore = useProposalStore();

const activeTab = ref('details');
const showSendProposalDialog = ref(false);
const showConfirmDialog = ref(false);
const showRejectDialog = ref(false);
const showCompleteDialog = ref(false);
const rejectReason = ref('');
const isLoading = ref(false);

const proposal = ref<Proposal | null>(null);

const proposalForm = ref({
  value: '',
  executionDays: '',
  scope: '',
  notes: '',
  attachments: [] as File[],
  attachmentUrls: [] as string[],
});

const milestones = ref<Array<Omit<Milestone, 'status'> & { status?: Milestone['status'] }>>([]);
const milestoneForm = ref({
  title: '',
  description: '',
  deadline: '',
  value: '',
});
const showAddMilestone = ref(false);

const newMessage = ref('');
const messageAttachments = ref<File[]>([]);

const messageAttachmentInput = ref<HTMLInputElement | null>(null);
const proposalAttachmentInput = ref<HTMLInputElement | null>(null);

onMounted(async () => {
  if (proposalStore.proposals.length === 0) {
    await proposalStore.loadProposals();
  }
  
  if (props.requestId) {
    proposal.value = proposalStore.getProposalById(props.requestId) || null;
    if (proposal.value) {
      proposalStore.markAsViewed(props.requestId);
      await proposalStore.loadHistory(props.requestId);
    }
  }
});

watch(() => props.requestId, async (newId) => {
  if (newId) {
    if (proposalStore.proposals.length === 0) {
      await proposalStore.loadProposals();
    }
    proposal.value = proposalStore.getProposalById(newId) || null;
    if (proposal.value) {
      proposalStore.markAsViewed(newId);
    }
  }
});

const clientMessagesCount = computed(() =>
  proposal.value?.messages?.filter((m) => m.sender === 'client').length || 0
);

const getInitials = (name: string) => {
  return name
    .split(' ')
    .map((n) => n[0])
    .join('')
    .toUpperCase()
    .slice(0, 2);
};

const formatCurrency = (value: string | number) => {
  if (typeof value === 'number') {
    return new Intl.NumberFormat('pt-BR', {
      style: 'currency',
      currency: 'BRL',
    }).format(value);
  }
  const numbers = value.replace(/\D/g, '');
  const formatted = (parseFloat(numbers) / 100).toLocaleString('pt-BR', {
    minimumFractionDigits: 2,
    maximumFractionDigits: 2,
  });
  return `R$ ${formatted}`;
};

const handleCurrencyInput = (value: string) => {
  const numbers = value.replace(/\D/g, '');
  if (numbers) {
    return formatCurrency(numbers);
  }
  return '';
};

const parseCurrencyValue = (value: string): number => {
  const numbers = value.replace(/[^\d,]/g, '').replace(',', '.');
  return parseFloat(numbers) || 0;
};

const validateProposal = () => {
  const errors: string[] = [];

  if (!proposalForm.value.value) errors.push('Valor da proposta é obrigatório');
  if (!proposalForm.value.executionDays) errors.push('Prazo de execução é obrigatório');
  if (!proposalForm.value.scope) errors.push('Escopo do trabalho é obrigatório');

  const value = parseCurrencyValue(proposalForm.value.value);
  if (isNaN(value) || value <= 0) {
    errors.push('Valor da proposta deve ser um número válido maior que zero');
  }

  const days = parseInt(proposalForm.value.executionDays);
  if (isNaN(days) || days <= 0) {
    errors.push('Prazo de execução deve ser um número válido maior que zero');
  }

  if (milestones.value.length > 0) {
    const milestonesTotal = milestones.value.reduce((sum, m) => {
      const mValue = parseCurrencyValue(String(m.value));
      return sum + (isNaN(mValue) ? 0 : mValue);
    }, 0);

    if (Math.abs(milestonesTotal - value) > 0.01) {
      errors.push('A soma dos valores dos marcos deve ser igual ao valor total da proposta');
    }
  }

  return errors;
};

const handleAddMilestone = () => {
  if (!milestoneForm.value.title || !milestoneForm.value.deadline || !milestoneForm.value.value) {
    toast.error('Preencha todos os campos obrigatórios do marco');
    return;
  }

  const newMilestone = {
    id: Date.now().toString(),
    title: milestoneForm.value.title,
    description: milestoneForm.value.description,
    deadline: milestoneForm.value.deadline,
    value: parseCurrencyValue(milestoneForm.value.value),
    status: 'pending' as const,
  };

  milestones.value = [...milestones.value, newMilestone];
  milestoneForm.value = { title: '', description: '', deadline: '', value: '' };
  showAddMilestone.value = false;
  toast.success('Marco adicionado com sucesso');
};

const handleRemoveMilestone = (id: string) => {
  milestones.value = milestones.value.filter((m) => m.id !== id);
  toast.success('Marco removido');
};

const handleAttachmentUpload = (e: Event) => {
  const target = e.target as HTMLInputElement;
  if (!target.files) return;

  const validFiles: File[] = [];
  const maxSize = 10 * 1024 * 1024;
  const allowedTypes = [
    'application/pdf',
    'image/jpeg',
    'image/png',
    'application/msword',
    'application/vnd.openxmlformats-officedocument.wordprocessingml.document',
  ];

  Array.from(target.files).forEach((file) => {
    if (file.size > maxSize) {
      toast.error(`Arquivo ${file.name} excede o tamanho máximo de 10MB`);
      return;
    }

    if (!allowedTypes.includes(file.type)) {
      toast.error(`Tipo de arquivo ${file.name} não permitido`);
      return;
    }

    validFiles.push(file);
  });

  if (validFiles.length > 0) {
    const newUrls = validFiles.map((file) => URL.createObjectURL(file));
    proposalForm.value = {
      ...proposalForm.value,
      attachments: [...proposalForm.value.attachments, ...validFiles],
      attachmentUrls: [...proposalForm.value.attachmentUrls, ...newUrls],
    };
  }
};

const handleRemoveAttachment = (index: number) => {
  proposalForm.value = {
    ...proposalForm.value,
    attachments: proposalForm.value.attachments.filter((_, i) => i !== index),
    attachmentUrls: proposalForm.value.attachmentUrls.filter((_, i) => i !== index),
  };
};

const handleSendMessage = async () => {
  if (!newMessage.value.trim() && messageAttachments.value.length === 0) {
    toast.error('Digite uma mensagem ou adicione um anexo');
    return;
  }

  if (!proposal.value) return;

  const success = await proposalStore.sendMessage(
    proposal.value.id,
    newMessage.value,
    messageAttachments.value.length > 0 ? messageAttachments.value : undefined
  );

  if (success) {
    newMessage.value = '';
    messageAttachments.value = [];
    proposal.value = proposalStore.getProposalById(proposal.value.id) || null;
    toast.success('Mensagem enviada');
  } else {
    toast.error('Erro ao enviar mensagem');
  }
};

const handlePrepareProposal = () => {
  const errors = validateProposal();

  if (errors.length > 0) {
    toast.error(errors[0]);
    return;
  }

  showSendProposalDialog.value = false;
  showConfirmDialog.value = true;
};

const handleConfirmSendProposal = async () => {
  if (!proposal.value) return;

  isLoading.value = true;
  
  const responseData = {
    value: parseCurrencyValue(proposalForm.value.value),
    executionDays: parseInt(proposalForm.value.executionDays),
    scope: proposalForm.value.scope,
    notes: proposalForm.value.notes || undefined,
    milestones: milestones.value.length > 0 
      ? milestones.value.map(m => ({
          ...m,
          status: 'pending' as const,
        }))
      : undefined,
  };

  const success = await proposalStore.sendResponse(proposal.value.id, responseData);
  
  isLoading.value = false;

  if (success) {
    toast.success('Proposta enviada com sucesso!');
    showConfirmDialog.value = false;
    showSendProposalDialog.value = false;
    
    proposalForm.value = {
      value: '',
      executionDays: '',
      scope: '',
      notes: '',
      attachments: [],
      attachmentUrls: [],
    };
    milestones.value = [];
    
    proposal.value = proposalStore.getProposalById(proposal.value.id) || null;
  } else {
    toast.error('Erro ao enviar proposta');
  }
};

const handleOpenRejectDialog = () => {
  rejectReason.value = '';
  showRejectDialog.value = true;
};

const handleRejectProposal = async () => {
  if (!proposal.value) return;

  if (!rejectReason.value.trim()) {
    toast.error('Por favor, informe o motivo da recusa');
    return;
  }

  isLoading.value = true;
  const success = await proposalStore.rejectProposal(proposal.value.id, rejectReason.value);
  isLoading.value = false;

  if (success) {
    toast.success('Solicitação recusada');
    showRejectDialog.value = false;
    proposal.value = proposalStore.getProposalById(proposal.value.id) || null;
  } else {
    toast.error('Erro ao recusar solicitação');
  }
};

const handleCompleteProposal = async () => {
  if (!proposal.value) return;

  isLoading.value = true;
  const success = await proposalStore.completeProposal(proposal.value.id);
  isLoading.value = false;

  if (success) {
    toast.success('Serviço marcado como concluído!');
    showCompleteDialog.value = false;
    proposal.value = proposalStore.getProposalById(proposal.value.id) || null;
  } else {
    toast.error('Erro ao marcar como concluído');
  }
};

const handleUpdateMilestoneStatus = async (milestoneId: string, status: 'pending' | 'in_progress' | 'completed') => {
  if (!proposal.value) return;

  const success = await proposalStore.updateMilestoneStatus(proposal.value.id, milestoneId, status);
  
  if (success) {
    toast.success('Marco atualizado');
    proposal.value = proposalStore.getProposalById(proposal.value.id) || null;
  } else {
    toast.error('Erro ao atualizar marco');
  }
};

const handleMessageAttachmentChange = (e: Event) => {
  const target = e.target as HTMLInputElement;
  if (target.files) {
    messageAttachments.value = [...messageAttachments.value, ...Array.from(target.files)];
  }
};

const handleViewAttachment = (url: string) => {
  window.open(url, '_blank');
};

const handleDownloadAttachment = async (url: string, fileName: string) => {
  try {
    const response = await fetch(url);
    const blob = await response.blob();
    const blobUrl = URL.createObjectURL(blob);
    const link = document.createElement('a');
    link.href = blobUrl;
    link.download = fileName;
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
    URL.revokeObjectURL(blobUrl);
  } catch {
    window.open(url, '_blank');
  }
};

const getPriorityVariant = (priority: string) => {
  if (priority === 'URGENT') return 'destructive';
  if (priority === 'HIGH') return 'default';
  if (priority === 'MEDIUM') return 'secondary';
  return 'outline';
};

const getStatusBadge = (status: string) => {
  const badges: Record<string, { class: string; label: string; icon: any }> = {
    pending: { class: 'bg-blue-100 text-blue-700', label: 'Pendente', icon: Clock },
    negotiating: { class: 'bg-purple-100 text-purple-700', label: 'Negociando', icon: MessageSquare },
    accepted: { class: 'bg-green-100 text-green-700', label: 'Aceita', icon: CheckCircle },
    rejected: { class: 'bg-red-100 text-red-700', label: 'Recusada', icon: XCircle },
    completed: { class: 'bg-green-100 text-green-700', label: 'Concluída', icon: CheckCircle },
    archived: { class: 'bg-gray-100 text-gray-700', label: 'Arquivada', icon: FileText },
  };
  return badges[status] || badges.pending;
};
</script>

<template>
  <div class="max-w-7xl mx-auto space-y-6">
    <!-- Not Found State -->
    <Card v-if="!proposal" class="p-12 text-center">
      <AlertCircle class="h-12 w-12 text-gray-400 mx-auto mb-4" />
      <h3 class="text-lg font-semibold text-[#263238] mb-2">Solicitação não encontrada</h3>
      <p class="text-[#435574] mb-6">A solicitação que você está procurando não existe ou foi removida.</p>
      <Button @click="props.onNavigate('freelancer-proposals')" class="bg-[#286AD8] hover:bg-[#435574]">
        <ArrowLeft class="h-4 w-4 mr-2" />
        Voltar para Solicitações
      </Button>
    </Card>

    <template v-else>
      <!-- Header -->
      <div class="flex items-center justify-between">
        <div class="flex items-center gap-4">
          <Button
            @click="props.onNavigate('freelancer-proposals')"
            variant="ghost"
            size="icon"
          >
            <ArrowLeft class="h-5 w-5" />
          </Button>
          <div>
            <div class="flex items-center gap-3 mb-1">
              <h1 class="text-2xl font-bold text-[#263238]">{{ proposal.service }}</h1>
              <Badge :class="`${getStatusBadge(proposal.status).class} hover:${getStatusBadge(proposal.status).class}`">
                <component :is="getStatusBadge(proposal.status).icon" class="h-3 w-3 mr-1" />
                {{ getStatusBadge(proposal.status).label }}
              </Badge>
            </div>
            <div class="flex items-center gap-3 text-sm text-[#435574]">
              <Badge variant="secondary">{{ proposal.category }}</Badge>
              <span class="flex items-center gap-1">
                <Calendar class="h-4 w-4" />
                Recebido em {{ new Date(proposal.receivedAt).toLocaleDateString('pt-BR') }}
              </span>
            </div>
          </div>
        </div>
        <div class="flex items-center gap-2">
          <Button
            v-if="proposal.status === 'pending'"
            @click="handleOpenRejectDialog"
            variant="outline"
            class="text-red-600 border-red-200 hover:bg-red-50"
          >
            <XCircle class="h-4 w-4 mr-2" />
            Recusar
          </Button>
          <Button
            v-if="proposal.status === 'pending' || proposal.status === 'negotiating'"
            @click="showSendProposalDialog = true"
            class="bg-[#286AD8] hover:bg-[#435574]"
          >
            <Send class="h-4 w-4 mr-2" />
            {{ proposal.freelancerResponse ? 'Atualizar Proposta' : 'Enviar Proposta' }}
          </Button>
          <Button
            v-if="proposal.status === 'accepted'"
            @click="showCompleteDialog = true"
            class="bg-green-600 hover:bg-green-700"
          >
            <CheckCircle class="h-4 w-4 mr-2" />
            Marcar Concluído
          </Button>
        </div>
      </div>

      <!-- Client Info -->
      <Card class="p-6">
        <div class="flex items-center justify-between">
          <div class="flex items-center gap-4">
            <Avatar class="h-16 w-16">
              <AvatarImage :src="proposal.clientAvatar" :alt="proposal.clientName" />
              <AvatarFallback class="bg-[#286AD8] text-white text-lg">
                {{ getInitials(proposal.clientName) }}
              </AvatarFallback>
            </Avatar>
            <div>
              <h3 class="text-lg font-semibold text-[#263238] mb-1">{{ proposal.clientName }}</h3>
              <div class="flex items-center gap-4 text-sm text-[#435574]">
                <span class="flex items-center gap-1">
                  <MapPin class="h-4 w-4" />
                  {{ proposal.clientLocation }}
                </span>
                <span v-if="proposal.clientEmail" class="flex items-center gap-1">
                  <Mail class="h-4 w-4" />
                  {{ proposal.clientEmail }}
                </span>
                <span v-if="proposal.clientPhone" class="flex items-center gap-1">
                  <Phone class="h-4 w-4" />
                  {{ proposal.clientPhone }}
                </span>
              </div>
            </div>
          </div>
          <Button variant="outline" @click="activeTab = 'messages'">
            <MessageSquare class="h-4 w-4 mr-2" />
            Enviar Mensagem
          </Button>
        </div>
      </Card>

      <!-- My Proposal (if sent) -->
      <Card v-if="proposal.freelancerResponse" class="p-6 border-l-4 border-l-[#286AD8]">
        <h3 class="text-lg font-semibold text-[#263238] mb-4">Sua Proposta Enviada</h3>
        <div class="grid grid-cols-1 md:grid-cols-3 gap-4 mb-4">
          <div>
            <p class="text-sm text-[#435574]">Valor Proposto</p>
            <p class="text-xl font-bold text-green-600">
              {{ formatCurrency(proposal.freelancerResponse.value) }}
            </p>
          </div>
          <div>
            <p class="text-sm text-[#435574]">Prazo de Execução</p>
            <p class="text-xl font-bold text-[#263238]">
              {{ proposal.freelancerResponse.executionDays }} dias
            </p>
          </div>
          <div>
            <p class="text-sm text-[#435574]">Enviada em</p>
            <p class="text-[#263238]">
              {{ new Date(proposal.freelancerResponse.sentAt).toLocaleDateString('pt-BR', {
                day: '2-digit', month: '2-digit', year: 'numeric', hour: '2-digit', minute: '2-digit'
              }) }}
            </p>
          </div>
        </div>
        <div class="mb-4">
          <p class="text-sm text-[#435574] mb-1">Escopo do Trabalho</p>
          <p class="text-[#263238] whitespace-pre-wrap">{{ proposal.freelancerResponse.scope }}</p>
        </div>
        <div v-if="proposal.freelancerResponse.notes">
          <p class="text-sm text-[#435574] mb-1">Observações</p>
          <p class="text-[#263238]">{{ proposal.freelancerResponse.notes }}</p>
        </div>
        
        <!-- Milestones -->
        <div v-if="proposal.freelancerResponse.milestones && proposal.freelancerResponse.milestones.length > 0" class="mt-6">
          <h4 class="font-semibold text-[#263238] mb-3">Marcos de Entrega</h4>
          <div class="space-y-3">
            <Card
              v-for="milestone in proposal.freelancerResponse.milestones"
              :key="milestone.id"
              class="p-4 bg-gray-50"
            >
              <div class="flex items-start justify-between">
                <div class="flex-1">
                  <div class="flex items-center gap-2 mb-1">
                    <MilestoneIcon class="h-4 w-4 text-[#286AD8]" />
                    <h5 class="font-medium text-[#263238]">{{ milestone.title }}</h5>
                    <Badge
                      :class="
                        milestone.status === 'completed' ? 'bg-green-100 text-green-700' :
                        milestone.status === 'in_progress' ? 'bg-blue-100 text-blue-700' :
                        'bg-gray-100 text-gray-700'
                      "
                    >
                      {{ milestone.status === 'completed' ? 'Concluído' :
                         milestone.status === 'in_progress' ? 'Em andamento' : 'Pendente' }}
                    </Badge>
                  </div>
                  <p v-if="milestone.description" class="text-sm text-[#435574] mb-2">
                    {{ milestone.description }}
                  </p>
                  <div class="flex items-center gap-4 text-sm text-[#435574]">
                    <span class="flex items-center gap-1">
                      <Calendar class="h-4 w-4" />
                      {{ new Date(milestone.deadline).toLocaleDateString('pt-BR') }}
                    </span>
                    <span class="flex items-center gap-1">
                      <DollarSign class="h-4 w-4" />
                      {{ formatCurrency(milestone.value) }}
                    </span>
                  </div>
                </div>
                <div v-if="proposal.status === 'accepted'" class="flex gap-2">
                  <Button
                    v-if="milestone.status === 'pending'"
                    @click="handleUpdateMilestoneStatus(milestone.id, 'in_progress')"
                    size="sm"
                    variant="outline"
                  >
                    <Play class="h-3 w-3 mr-1" />
                    Iniciar
                  </Button>
                  <Button
                    v-if="milestone.status === 'in_progress'"
                    @click="handleUpdateMilestoneStatus(milestone.id, 'completed')"
                    size="sm"
                    class="bg-green-600 hover:bg-green-700"
                  >
                    <Check class="h-3 w-3 mr-1" />
                    Concluir
                  </Button>
                </div>
              </div>
            </Card>
          </div>
        </div>
      </Card>

      <!-- Tabs -->
      <Tabs :model-value="activeTab" @update:model-value="(val: string) => activeTab = val">
        <TabsList class="grid w-full grid-cols-4">
          <TabsTrigger value="details">Detalhes</TabsTrigger>
          <TabsTrigger value="messages">
            Mensagens
            <Badge v-if="clientMessagesCount > 0" class="ml-2 bg-red-500 text-white">
              {{ clientMessagesCount }}
            </Badge>
          </TabsTrigger>
          <TabsTrigger value="attachments">
            Anexos ({{ proposal.attachments?.length || 0 }})
          </TabsTrigger>
          <TabsTrigger value="history">Histórico</TabsTrigger>
        </TabsList>

        <!-- Details Tab -->
        <TabsContent value="details" class="space-y-6">
          <Card class="p-6">
            <h3 class="text-lg font-semibold text-[#263238] mb-4">Informações da Solicitação</h3>

            <div class="grid grid-cols-1 md:grid-cols-3 gap-4 mb-6">
              <div>
                <Label class="text-sm text-[#435574]">Data Desejada</Label>
                <p class="text-[#263238] mt-1">
                  {{ proposal.desiredDate ? new Date(proposal.desiredDate).toLocaleDateString('pt-BR') : 'Não informada' }}
                </p>
              </div>
              <div>
                <Label class="text-sm text-[#435574]">Orçamento Estimado</Label>
                <p class="text-[#263238] mt-1 font-semibold">{{ formatCurrency(proposal.budget) }}</p>
              </div>
              <div>
                <Label class="text-sm text-[#435574]">Prioridade</Label>
                <p class="text-[#263238] mt-1">
                  <Badge :variant="getPriorityVariant(proposal.priority || 'MEDIUM')">
                    {{ 
                      proposal.priority === 'URGENT' ? 'Urgente' :
                      proposal.priority === 'HIGH' ? 'Alta' :
                      proposal.priority === 'MEDIUM' ? 'Média' : 'Baixa'
                    }}
                  </Badge>
                </p>
              </div>
            </div>

            <div v-if="proposal.address" class="mb-6">
              <Label class="text-sm text-[#435574]">Endereço do Serviço</Label>
              <p class="text-[#263238] mt-1 flex items-start gap-2">
                <MapPin class="h-4 w-4 mt-1 flex-shrink-0" />
                {{ proposal.address }}
              </p>
            </div>

            <div>
              <Label class="text-sm text-[#435574] mb-2 block">Descrição Detalhada</Label>
              <div class="bg-gray-50 rounded-lg p-4 whitespace-pre-wrap text-[#263238]">
                {{ proposal.description }}
              </div>
            </div>
          </Card>
        </TabsContent>

        <!-- Messages Tab -->
        <TabsContent value="messages" class="space-y-4">
          <Card class="p-6">
            <div class="space-y-4 mb-6 max-h-[500px] overflow-y-auto">
              <div v-if="!proposal.messages || proposal.messages.length === 0" class="text-center py-8">
                <MessageSquare class="h-12 w-12 text-gray-300 mx-auto mb-3" />
                <p class="text-[#435574]">Nenhuma mensagem ainda</p>
              </div>
              <div
                v-for="msg in proposal.messages"
                :key="msg.id"
                :class="`flex gap-3 ${msg.sender === 'freelancer' ? 'flex-row-reverse' : ''}`"
              >
                <Avatar class="h-8 w-8 flex-shrink-0">
                  <AvatarFallback :class="msg.sender === 'freelancer' ? 'bg-[#286AD8] text-white' : 'bg-gray-300'">
                    {{ getInitials(msg.senderName) }}
                  </AvatarFallback>
                </Avatar>
                <div :class="`flex-1 ${msg.sender === 'freelancer' ? 'text-right' : ''}`">
                  <div
                    :class="`inline-block max-w-[70%] ${
                      msg.sender === 'freelancer'
                        ? 'bg-[#286AD8] text-white'
                        : 'bg-gray-100 text-[#263238]'
                    } rounded-lg p-4`"
                  >
                    <p class="mb-1">{{ msg.message }}</p>
                    <div v-if="msg.attachments && msg.attachments.length > 0" class="mt-3 space-y-2">
                      <div
                        v-for="att in msg.attachments"
                        :key="att.id"
                        :class="`flex items-center gap-2 p-2 rounded ${
                          msg.sender === 'freelancer' ? 'bg-[#435574]' : 'bg-white'
                        }`"
                      >
                        <Paperclip class="h-4 w-4" />
                        <span class="text-sm flex-1">{{ att.name }}</span>
                        <span class="text-xs opacity-70">{{ att.size }}</span>
                      </div>
                    </div>
                  </div>
                  <p class="text-xs text-[#435574] mt-1">
                    {{ msg.senderName }} •
                    {{ new Date(msg.timestamp).toLocaleString('pt-BR', {
                      day: '2-digit',
                      month: '2-digit',
                      hour: '2-digit',
                      minute: '2-digit',
                    }) }}
                  </p>
                </div>
              </div>
            </div>

            <!-- Message Input -->
            <div class="space-y-3 border-t pt-4">
              <div v-if="messageAttachments.length > 0" class="flex flex-wrap gap-2">
                <Badge
                  v-for="(file, idx) in messageAttachments"
                  :key="idx"
                  variant="secondary"
                  class="pl-3 pr-1"
                >
                  {{ file.name }}
                  <button
                    @click="messageAttachments = messageAttachments.filter((_, i) => i !== idx)"
                    class="ml-2 hover:bg-gray-300 rounded-full p-0.5"
                  >
                    <X class="h-3 w-3" />
                  </button>
                </Badge>
              </div>

              <div class="flex gap-2">
                <Textarea
                  :model-value="newMessage"
                  @update:model-value="(val) => newMessage = val as string"
                  placeholder="Digite sua mensagem..."
                  :rows="3"
                  class="flex-1"
                />
              </div>

              <div class="flex gap-2">
                <input
                  ref="messageAttachmentInput"
                  type="file"
                  multiple
                  class="hidden"
                  @change="handleMessageAttachmentChange"
                />
                <Button
                  @click="messageAttachmentInput?.click()"
                  variant="outline"
                >
                  <Paperclip class="h-4 w-4 mr-2" />
                  Anexar
                </Button>
                <Button
                  @click="handleSendMessage"
                  class="bg-[#286AD8] hover:bg-[#435574] flex-1"
                >
                  <Send class="h-4 w-4 mr-2" />
                  Enviar Mensagem
                </Button>
              </div>
            </div>
          </Card>
        </TabsContent>

        <!-- Attachments Tab -->
        <TabsContent value="attachments" class="space-y-4">
          <Card class="p-6">
            <h3 class="text-lg font-semibold text-[#263238] mb-4">Anexos do Cliente</h3>
            <div v-if="!proposal.attachments || proposal.attachments.length === 0" class="text-center py-8">
              <FileText class="h-12 w-12 text-gray-300 mx-auto mb-3" />
              <p class="text-[#435574]">Nenhum anexo enviado pelo cliente</p>
            </div>
            <div v-else class="space-y-3">
              <div
                v-for="att in proposal.attachments"
                :key="att.id"
                class="flex items-center justify-between p-4 bg-gray-50 rounded-lg hover:bg-gray-100 transition-colors"
              >
                <div class="flex items-center gap-3">
                  <div class="w-10 h-10 bg-blue-100 rounded-lg flex items-center justify-center">
                    <FileText class="h-5 w-5 text-blue-600" />
                  </div>
                  <div>
                    <p class="font-medium text-[#263238]">{{ att.name }}</p>
                    <p class="text-sm text-[#435574]">{{ att.size }}</p>
                  </div>
                </div>
                <div class="flex gap-2">
                  <Button size="sm" variant="outline" @click="handleViewAttachment(att.url)">
                    <Eye class="h-4 w-4 mr-2" />
                    Visualizar
                  </Button>
                  <Button size="sm" variant="outline" @click="handleDownloadAttachment(att.url, att.name)">
                    <Download class="h-4 w-4 mr-2" />
                    Baixar
                  </Button>
                </div>
              </div>
            </div>
          </Card>
        </TabsContent>

        <!-- History Tab -->
        <TabsContent value="history" class="space-y-4">
          <ServiceRequestTimeline
            :entries="proposal.auditLog || []"
            title="Histórico de Ações"
          />
        </TabsContent>
      </Tabs>
    </template>

    <!-- Send Proposal Dialog -->
    <Dialog :open="showSendProposalDialog" @update:open="(val) => showSendProposalDialog = val">
      <DialogContent class="sm:max-w-3xl max-h-[90vh] overflow-y-auto">
        <DialogHeader>
          <DialogTitle>{{ proposal?.freelancerResponse ? 'Atualizar Proposta' : 'Enviar Proposta' }}</DialogTitle>
          <DialogDescription>
            Preencha os detalhes da sua proposta para este serviço
          </DialogDescription>
        </DialogHeader>

        <div class="space-y-6">
          <!-- Value and Deadline -->
          <div class="grid grid-cols-2 gap-4">
            <div class="space-y-2">
              <Label for="value">Valor Total da Proposta *</Label>
              <Input
                id="value"
                :model-value="proposalForm.value"
                @update:model-value="(val) => proposalForm.value = handleCurrencyInput(val as string)"
                placeholder="R$ 0,00"
              />
            </div>

            <div class="space-y-2">
              <Label for="executionDays">Prazo de Execução (dias) *</Label>
              <Input
                id="executionDays"
                type="number"
                :model-value="proposalForm.executionDays"
                @update:model-value="(val) => proposalForm.executionDays = val as string"
                placeholder="Ex: 15"
              />
            </div>
          </div>

          <!-- Scope -->
          <div class="space-y-2">
            <Label for="scope">Escopo do Trabalho *</Label>
            <Textarea
              id="scope"
              :model-value="proposalForm.scope"
              @update:model-value="(val) => proposalForm.scope = val as string"
              placeholder="Descreva detalhadamente o que será realizado..."
              :rows="6"
            />
          </div>

          <!-- Milestones -->
          <div class="space-y-3">
            <div class="flex items-center justify-between">
              <Label>Marcos de Entrega (opcional)</Label>
              <Button
                @click="showAddMilestone = !showAddMilestone"
                variant="outline"
                size="sm"
              >
                <Plus class="h-4 w-4 mr-2" />
                Adicionar Marco
              </Button>
            </div>

            <Card v-if="showAddMilestone" class="p-4 bg-gray-50">
              <div class="space-y-3">
                <Input
                  :model-value="milestoneForm.title"
                  @update:model-value="(val) => milestoneForm.title = val as string"
                  placeholder="Título do marco"
                />
                <Textarea
                  :model-value="milestoneForm.description"
                  @update:model-value="(val) => milestoneForm.description = val as string"
                  placeholder="Descrição do marco"
                  :rows="2"
                />
                <div class="grid grid-cols-2 gap-3">
                  <Input
                    type="date"
                    :model-value="milestoneForm.deadline"
                    @update:model-value="(val) => milestoneForm.deadline = val as string"
                  />
                  <Input
                    :model-value="milestoneForm.value"
                    @update:model-value="(val) => milestoneForm.value = handleCurrencyInput(val as string)"
                    placeholder="Valor do marco"
                  />
                </div>
                <div class="flex gap-2">
                  <Button @click="handleAddMilestone" size="sm" class="flex-1">
                    Adicionar
                  </Button>
                  <Button
                    @click="showAddMilestone = false"
                    variant="outline"
                    size="sm"
                  >
                    Cancelar
                  </Button>
                </div>
              </div>
            </Card>

            <div v-if="milestones.length > 0" class="space-y-2">
              <Card
                v-for="(milestone, index) in milestones"
                :key="milestone.id"
                class="p-4"
              >
                <div class="flex items-start justify-between">
                  <div class="flex-1">
                    <div class="flex items-center gap-2 mb-1">
                      <MilestoneIcon class="h-4 w-4 text-[#286AD8]" />
                      <h4 class="font-medium text-[#263238]">
                        {{ index + 1 }}. {{ milestone.title }}
                      </h4>
                    </div>
                    <p v-if="milestone.description" class="text-sm text-[#435574] mb-2">
                      {{ milestone.description }}
                    </p>
                    <div class="flex items-center gap-4 text-sm text-[#435574]">
                      <span class="flex items-center gap-1">
                        <Calendar class="h-4 w-4" />
                        {{ new Date(milestone.deadline).toLocaleDateString('pt-BR') }}
                      </span>
                      <span class="flex items-center gap-1">
                        <DollarSign class="h-4 w-4" />
                        {{ formatCurrency(milestone.value) }}
                      </span>
                    </div>
                  </div>
                  <Button
                    @click="handleRemoveMilestone(milestone.id)"
                    variant="ghost"
                    size="icon"
                    class="text-red-600 hover:text-red-700 hover:bg-red-50"
                  >
                    <Trash2 class="h-4 w-4" />
                  </Button>
                </div>
              </Card>
            </div>
          </div>

          <!-- Notes -->
          <div class="space-y-2">
            <Label for="notes">Observações (opcional)</Label>
            <Textarea
              id="notes"
              :model-value="proposalForm.notes"
              @update:model-value="(val) => proposalForm.notes = val as string"
              placeholder="Informações adicionais, condições especiais, etc."
              :rows="3"
            />
          </div>

          <!-- Attachments -->
          <div class="space-y-2">
            <Label>Anexos (opcional)</Label>
            <div class="border-2 border-dashed border-gray-300 rounded-lg p-6 text-center hover:border-[#286AD8] transition-colors cursor-pointer">
              <Upload class="h-8 w-8 text-gray-400 mx-auto mb-2" />
              <p class="text-sm text-[#435574] mb-2">
                Adicione documentos, certificados ou imagens
              </p>
              <p class="text-xs text-[#435574] mb-3">
                PDF, JPG, PNG, DOC até 10MB cada
              </p>
              <input
                ref="proposalAttachmentInput"
                type="file"
                multiple
                accept=".pdf,.jpg,.jpeg,.png,.doc,.docx"
                @change="handleAttachmentUpload"
                class="hidden"
              />
              <Button
                @click="proposalAttachmentInput?.click()"
                size="sm"
                variant="outline"
              >
                Selecionar Arquivos
              </Button>
            </div>

            <div v-if="proposalForm.attachments.length > 0" class="space-y-2">
              <div
                v-for="(file, index) in proposalForm.attachments"
                :key="index"
                class="flex items-center justify-between p-3 bg-gray-50 rounded-lg"
              >
                <div class="flex items-center gap-2">
                  <FileText class="h-4 w-4 text-[#435574]" />
                  <span class="text-sm text-[#263238]">{{ file.name }}</span>
                  <span class="text-xs text-[#435574]">
                    ({{ (file.size / 1024).toFixed(1) }} KB)
                  </span>
                </div>
                <button
                  @click="handleRemoveAttachment(index)"
                  class="text-red-600 hover:text-red-700"
                >
                  <X class="h-4 w-4" />
                </button>
              </div>
            </div>
          </div>
        </div>

        <DialogFooter>
          <Button @click="showSendProposalDialog = false" variant="outline">
            Cancelar
          </Button>
          <Button @click="handlePrepareProposal" class="bg-[#286AD8] hover:bg-[#435574]">
            <Send class="h-4 w-4 mr-2" />
            Enviar Proposta
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>

    <!-- Confirmation Dialog -->
    <AlertDialog :open="showConfirmDialog" @update:open="(val) => showConfirmDialog = val">
      <AlertDialogContent>
        <AlertDialogHeader>
          <AlertDialogTitle>Confirmar Envio da Proposta</AlertDialogTitle>
          <AlertDialogDescription>
            Você está prestes a enviar uma proposta de
            <strong>{{ proposalForm.value }}</strong> com prazo de
            <strong>{{ proposalForm.executionDays }} dias</strong>.
            <template v-if="milestones.length > 0">
              <br />
              <br />
              A proposta inclui <strong>{{ milestones.length }}</strong>
              {{ milestones.length === 1 ? 'marco' : 'marcos' }} de entrega.
            </template>
            <br />
            <br />
            Deseja confirmar o envio?
          </AlertDialogDescription>
        </AlertDialogHeader>
        <AlertDialogFooter>
          <AlertDialogCancel @click="showConfirmDialog = false">Revisar</AlertDialogCancel>
          <AlertDialogAction
            @click="handleConfirmSendProposal"
            class="bg-[#286AD8] hover:bg-[#435574]"
            :disabled="isLoading"
          >
            {{ isLoading ? 'Enviando...' : 'Confirmar Envio' }}
          </AlertDialogAction>
        </AlertDialogFooter>
      </AlertDialogContent>
    </AlertDialog>

    <!-- Reject Dialog -->
    <Dialog :open="showRejectDialog" @update:open="(val) => showRejectDialog = val">
      <DialogContent class="sm:max-w-md">
        <DialogHeader>
          <DialogTitle>Recusar Solicitação</DialogTitle>
          <DialogDescription>
            Informe o motivo da recusa para "{{ proposal?.service }}"
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
          <Button @click="handleRejectProposal" class="bg-red-600 hover:bg-red-700" :disabled="isLoading">
            {{ isLoading ? 'Recusando...' : 'Confirmar Recusa' }}
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>

    <!-- Complete Dialog -->
    <AlertDialog :open="showCompleteDialog" @update:open="(val) => showCompleteDialog = val">
      <AlertDialogContent>
        <AlertDialogHeader>
          <AlertDialogTitle>Marcar como Concluído</AlertDialogTitle>
          <AlertDialogDescription>
            Tem certeza que deseja marcar o serviço "{{ proposal?.service }}" como concluído?
            <br /><br />
            Esta ação indica que o trabalho foi finalizado com sucesso.
          </AlertDialogDescription>
        </AlertDialogHeader>
        <AlertDialogFooter>
          <AlertDialogCancel @click="showCompleteDialog = false">Cancelar</AlertDialogCancel>
          <AlertDialogAction
            @click="handleCompleteProposal"
            class="bg-green-600 hover:bg-green-700"
            :disabled="isLoading"
          >
            {{ isLoading ? 'Processando...' : 'Confirmar Conclusão' }}
          </AlertDialogAction>
        </AlertDialogFooter>
      </AlertDialogContent>
    </AlertDialog>
  </div>
</template>
