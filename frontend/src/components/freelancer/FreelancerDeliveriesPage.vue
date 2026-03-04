<script setup lang="ts">
import { ref, computed, onMounted, watch } from 'vue';
import {
  Package,
  Upload,
  Check,
  Clock,
  FileText,
  Download,
  Eye,
  Calendar,
  CheckCircle,
  MessageSquare,
  DollarSign,
  AlertCircle,
  Loader2,
  Briefcase,
  Trash2,
  X,
} from 'lucide-vue-next';
import Button from '../ui/button.vue';
import Card from '../ui/card.vue';
import Badge from '../ui/badge.vue';
import Progress from '../ui/progress.vue';
import Avatar from '../ui/avatar.vue';
import AvatarFallback from '../ui/avatar-fallback.vue';
import AvatarImage from '../ui/avatar-image.vue';
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
import Label from '../ui/label.vue';
import Textarea from '../ui/textarea.vue';
import ServiceRequestTimeline from '../service/ServiceRequestTimeline.vue';
import { useProposalStore, type Proposal, type Milestone } from '@/stores/proposalStore';
import { toast } from '@/utils/toast';
import api from '@/api/config/axios';

interface Props {
  onNavigate: (page: string, params?: any) => void;
}

const props = defineProps<Props>();
const proposalStore = useProposalStore();

const selectedProjectId = ref<string | null>(null);
const showCompleteDialog = ref(false);
const showUploadDialog = ref(false);
const isCompleting = ref(false);
const isUploading = ref(false);
const uploadProgress = ref(0);
const uploadFiles = ref<File[]>([]);
const uploadDescription = ref('');
const activeFileTab = ref<'client' | 'freelancer'>('freelancer');

// Load data
onMounted(async () => {
  await proposalStore.loadProposals();
  if (activeProjects.value.length > 0 && !selectedProjectId.value) {
    selectedProjectId.value = activeProjects.value[0].id;
    await proposalStore.loadHistory(activeProjects.value[0].id);
  }
});

const activeProjects = computed(() =>
  proposalStore.proposals.filter((p) => p.status === 'in_progress')
);

const selectedProject = computed(() =>
  activeProjects.value.find((p) => p.id === selectedProjectId.value) || null
);

watch(selectedProjectId, async (newId) => {
  if (newId) {
    await proposalStore.loadHistory(newId);
  }
});

const projectProgress = computed(() => {
  const project = selectedProject.value;
  if (!project) return 0;
  const milestones = project.freelancerResponse?.milestones;
  if (milestones && milestones.length > 0) {
    const completed = milestones.filter((m) => m.status === 'completed').length;
    return Math.round((completed / milestones.length) * 100);
  }
  return 50;
});

const paymentProgress = computed(() => {
  const project = selectedProject.value;
  if (!project) return 0;
  const milestones = project.freelancerResponse?.milestones;
  if (milestones && milestones.length > 0) {
    const completedValue = milestones
      .filter((m) => m.status === 'completed')
      .reduce((sum, m) => sum + m.value, 0);
    const totalValue = milestones.reduce((sum, m) => sum + m.value, 0);
    return totalValue > 0 ? Math.round((completedValue / totalValue) * 100) : 0;
  }
  return 0;
});

const paidAmount = computed(() => {
  const project = selectedProject.value;
  if (!project) return 0;
  const milestones = project.freelancerResponse?.milestones;
  if (milestones && milestones.length > 0) {
    return milestones
      .filter((m) => m.status === 'completed')
      .reduce((sum, m) => sum + m.value, 0);
  }
  return 0;
});

const totalAmount = computed(() => {
  const project = selectedProject.value;
  if (!project) return 0;
  return project.freelancerResponse?.value || project.budget;
});

// Select project
const selectProject = async (id: string) => {
  selectedProjectId.value = id;
};

// Format currency
const formatCurrency = (value: number) =>
  new Intl.NumberFormat('pt-BR', { style: 'currency', currency: 'BRL' }).format(value);

// Client initials
const getInitials = (name: string) =>
  name
    .split(' ')
    .map((n) => n[0])
    .join('')
    .substring(0, 2)
    .toUpperCase();

// Open chat
const openChat = () => {
  props.onNavigate('chat');
};

// Handle complete delivery
const handleCompleteDelivery = async () => {
  if (!selectedProject.value) return;
  isCompleting.value = true;
  try {
    const success = await proposalStore.completeProposal(selectedProject.value.id);
    if (success) {
      toast.success('Entrega concluída com sucesso! O cliente será notificado.');
      showCompleteDialog.value = false;
      // Reload to update the list
      await proposalStore.loadProposals();
      // Select next project if available
      if (activeProjects.value.length > 0) {
        selectedProjectId.value = activeProjects.value[0].id;
      } else {
        selectedProjectId.value = null;
      }
    } else {
      toast.error('Erro ao concluir a entrega. Tente novamente.');
    }
  } catch {
    toast.error('Erro ao concluir a entrega. Tente novamente.');
  } finally {
    isCompleting.value = false;
  }
};

// Handle milestone status update
const handleMilestoneComplete = async (milestone: Milestone) => {
  if (!selectedProject.value) return;
  const newStatus = milestone.status === 'completed' ? 'pending' : 'completed';
  const success = await proposalStore.updateMilestoneStatus(
    selectedProject.value.id,
    milestone.id,
    newStatus
  );
  if (success) {
    const label = newStatus === 'completed' ? 'concluído' : 'reaberto';
    toast.success(`Marco "${milestone.title}" ${label}!`);
  } else {
    toast.error('Erro ao atualizar marco.');
  }
};

// File upload
const handleFileSelect = (e: Event) => {
  const target = e.target as HTMLInputElement;
  if (target.files) {
    uploadFiles.value = [...uploadFiles.value, ...Array.from(target.files)];
  }
};

const removeUploadFile = (index: number) => {
  uploadFiles.value = uploadFiles.value.filter((_, i) => i !== index);
};

const openUploadDialog = () => {
  uploadFiles.value = [];
  uploadDescription.value = '';
  uploadProgress.value = 0;
  isUploading.value = false;
  showUploadDialog.value = true;
};

const handleUploadFiles = async () => {
  if (uploadFiles.value.length === 0) {
    toast.error('Selecione pelo menos um arquivo');
    return;
  }

  const oversized = uploadFiles.value.filter((f) => f.size > 10 * 1024 * 1024);
  if (oversized.length > 0) {
    toast.error('Alguns arquivos excedem o limite de 10 MB');
    return;
  }

  isUploading.value = true;
  uploadProgress.value = 0;

  try {
    const formData = new FormData();
    uploadFiles.value.forEach((file) => formData.append('files', file));

    // Simulate progress
    const progressInterval = setInterval(() => {
      if (uploadProgress.value < 90) uploadProgress.value += 10;
    }, 300);

    const response = await api.post('/upload/images', formData, {
      headers: { 'Content-Type': 'multipart/form-data' },
    });

    clearInterval(progressInterval);

    // Vincular URLs ao service request
    const urls = response.data.urls || [];
    if (urls.length > 0 && selectedProject.value) {
      await api.post(`/service-requests/${selectedProject.value.id}/freelancer-attachments`, { urls });
    }

    uploadProgress.value = 100;
    toast.success('Arquivos enviados com sucesso!');
    showUploadDialog.value = false;

    // Reload proposals to get updated attachments
    await proposalStore.loadProposals();
  } catch {
    toast.error('Erro ao enviar arquivos. Tente novamente.');
  } finally {
    isUploading.value = false;
  }
};

// View attachment
const handleViewAttachment = (url: string) => {
  window.open(url, '_blank');
};

// Download attachment
const handleDownloadAttachment = async (url: string, fileName: string) => {
  try {
    const response = await fetch(url);
    const blob = await response.blob();
    const blobUrl = window.URL.createObjectURL(blob);
    const link = document.createElement('a');
    link.href = blobUrl;
    link.download = fileName;
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
    window.URL.revokeObjectURL(blobUrl);
  } catch {
    // Fallback: abrir em nova aba
    window.open(url, '_blank');
  }
};

// Format date
const formatDate = (dateString: string) => {
  try {
    return new Date(dateString).toLocaleDateString('pt-BR');
  } catch {
    return dateString;
  }
};

// Get status badge config for project cards
const getProjectStatusConfig = (project: Proposal) => {
  // Check milestones for more nuanced status
  const milestones = project.freelancerResponse?.milestones;
  if (milestones && milestones.length > 0) {
    const allCompleted = milestones.every((m) => m.status === 'completed');
    if (allCompleted) {
      return { label: 'Revisão Solicitada', class: 'bg-orange-100 text-orange-700 hover:bg-orange-100', icon: AlertCircle };
    }
    const hasInProgress = milestones.some((m) => m.status === 'in_progress');
    if (hasInProgress) {
      return { label: 'Em Andamento', class: 'bg-blue-100 text-blue-700 hover:bg-blue-100', icon: Clock };
    }
  }
  return { label: 'Em Andamento', class: 'bg-blue-100 text-blue-700 hover:bg-blue-100', icon: Clock };
};

// Get milestone status badge
const getMilestoneStatusConfig = (status: Milestone['status']) => {
  switch (status) {
    case 'completed':
      return { label: 'Concluído', class: 'bg-green-100 text-green-700 hover:bg-green-100' };
    case 'in_progress':
      return { label: 'Em Andamento', class: 'bg-blue-100 text-blue-700 hover:bg-blue-100' };
    default:
      return { label: 'Pendente', class: 'bg-gray-100 text-gray-600 hover:bg-gray-100' };
  }
};
</script>

<template>
  <div class="max-w-7xl mx-auto space-y-6">
    <!-- Header -->
    <div class="flex items-start justify-between">
      <div>
        <h1 class="text-2xl font-bold text-[#263238] mb-2">Projetos em Andamento</h1>
        <p class="text-[#435574]">Acompanhe e gerencie seus projetos ativos</p>
      </div>
      <Badge
        v-if="activeProjects.length > 0"
        class="bg-blue-100 text-blue-700 hover:bg-blue-100 text-sm px-3 py-1"
      >
        {{ activeProjects.length }} Projeto{{ activeProjects.length !== 1 ? 's' : '' }} Ativo{{ activeProjects.length !== 1 ? 's' : '' }}
      </Badge>
    </div>

    <!-- Loading State -->
    <div v-if="proposalStore.isLoading" class="flex items-center justify-center py-16">
      <Loader2 class="h-8 w-8 text-[#286AD8] animate-spin mr-3" />
      <span class="text-[#435574]">Carregando projetos...</span>
    </div>

    <!-- Empty State -->
    <Card v-else-if="activeProjects.length === 0" class="p-12 text-center">
      <div class="inline-flex p-6 bg-gray-100 rounded-full mb-6">
        <Package class="h-16 w-16 text-gray-400" />
      </div>
      <h2 class="text-xl font-semibold text-[#263238] mb-2">Nenhum projeto em andamento</h2>
      <p class="text-[#435574] mb-6">Quando um cliente aceitar sua proposta, o projeto aparecerá aqui.</p>
      <Button @click="props.onNavigate('freelancer-proposals')" class="bg-[#286AD8] hover:bg-[#435574]">
        <FileText class="h-4 w-4 mr-2" />
        Ver Solicitações
      </Button>
    </Card>

    <template v-else>
      <!-- Project Selection Cards -->
      <div class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
        <Card
          v-for="project in activeProjects"
          :key="project.id"
          :class="[
            'p-5 cursor-pointer transition-all duration-200',
            selectedProjectId === project.id
              ? 'ring-2 ring-[#286AD8] border-[#286AD8] shadow-md'
              : 'hover:shadow-md hover:border-gray-300',
          ]"
          @click="selectProject(project.id)"
        >
          <div class="flex items-start justify-between mb-3">
            <h3 class="font-semibold text-[#263238] text-sm leading-tight flex-1 mr-2">
              {{ project.service }}
            </h3>
            <Badge :class="getProjectStatusConfig(project).class" class="text-xs whitespace-nowrap flex-shrink-0">
              <component :is="getProjectStatusConfig(project).icon" class="h-3 w-3 mr-1" />
              {{ getProjectStatusConfig(project).label }}
            </Badge>
          </div>

          <div class="flex items-center gap-2 mb-3">
            <Avatar class="h-6 w-6">
              <AvatarImage v-if="project.clientAvatar" :src="project.clientAvatar" />
              <AvatarFallback class="text-xs bg-gray-200 text-gray-600">
                {{ getInitials(project.clientName) }}
              </AvatarFallback>
            </Avatar>
            <span class="text-sm text-[#435574]">{{ project.clientName }}</span>
          </div>

          <div class="space-y-1 mb-3">
            <div class="flex items-center justify-between text-xs">
              <span class="text-[#435574]">Progresso</span>
              <span class="text-[#263238] font-medium">
                {{ project.freelancerResponse?.milestones?.length
                  ? Math.round(
                      (project.freelancerResponse.milestones.filter((m) => m.status === 'completed').length /
                        project.freelancerResponse.milestones.length) *
                        100
                    )
                  : 50
                }}%
              </span>
            </div>
            <Progress
              :value="
                project.freelancerResponse?.milestones?.length
                  ? (project.freelancerResponse.milestones.filter((m) => m.status === 'completed').length /
                      project.freelancerResponse.milestones.length) *
                    100
                  : 50
              "
              class="h-1.5"
            />
          </div>

          <div class="flex items-center justify-between text-xs text-[#435574]">
            <span class="flex items-center gap-1">
              <Calendar class="h-3 w-3" />
              Prazo: {{ project.deadline }}
            </span>
            <span class="font-medium text-[#263238]">
              {{ formatCurrency(project.freelancerResponse?.value || project.budget) }}
            </span>
          </div>
        </Card>
      </div>

      <!-- Selected Project Details -->
      <template v-if="selectedProject">
        <div class="grid grid-cols-1 lg:grid-cols-3 gap-6">
          <!-- Main Column -->
          <div class="lg:col-span-2 space-y-6">
            <!-- Project Details Card -->
            <Card class="p-6">
              <div class="flex items-start justify-between mb-4">
                <div>
                  <div class="flex items-center gap-3 mb-2">
                    <h2 class="text-lg font-semibold text-[#263238]">{{ selectedProject.service }}</h2>
                    <Badge :class="getProjectStatusConfig(selectedProject).class">
                      <component :is="getProjectStatusConfig(selectedProject).icon" class="h-3 w-3 mr-1" />
                      {{ getProjectStatusConfig(selectedProject).label }}
                    </Badge>
                  </div>
                  <p class="text-sm text-[#435574] leading-relaxed">{{ selectedProject.description }}</p>
                </div>
              </div>

              <!-- Progress Bars -->
              <div class="space-y-4 mb-6">
                <div class="space-y-1.5">
                  <div class="flex items-center justify-between text-sm">
                    <span class="text-[#435574]">Progresso do Projeto</span>
                    <span class="text-[#263238] font-medium">{{ projectProgress }}%</span>
                  </div>
                  <Progress :value="projectProgress" class="h-2" />
                </div>

                <div class="space-y-1.5">
                  <div class="flex items-center justify-between text-sm">
                    <span class="text-[#435574]">Pagamento</span>
                    <span class="text-[#263238] font-medium">
                      {{ formatCurrency(paidAmount) }} / {{ formatCurrency(totalAmount) }}
                    </span>
                  </div>
                  <Progress :value="paymentProgress" class="h-2" />
                </div>
              </div>

              <!-- Dates -->
              <div class="flex gap-8 mb-6">
                <div>
                  <p class="text-xs text-[#435574] mb-0.5">Data de Início</p>
                  <p class="text-sm font-medium text-[#263238]">{{ formatDate(selectedProject.receivedAt) }}</p>
                </div>
                <div>
                  <p class="text-xs text-[#435574] mb-0.5">Prazo de Entrega</p>
                  <p class="text-sm font-medium text-[#263238]">{{ selectedProject.deadline }}</p>
                </div>
              </div>

              <!-- Action Buttons -->
              <div class="flex flex-wrap gap-3">
                <Button @click="openUploadDialog" variant="outline" class="border-[#286AD8] text-[#286AD8] hover:bg-blue-50">
                  <Upload class="h-4 w-4 mr-2" />
                  Enviar Arquivos
                </Button>
                <Button variant="outline" @click="openChat">
                  <MessageSquare class="h-4 w-4 mr-2" />
                  Mensagem
                </Button>
                <Button
                  @click="showCompleteDialog = true"
                  class="bg-[#286AD8] hover:bg-[#435574]"
                >
                  <Check class="h-4 w-4 mr-2" />
                  Concluir
                </Button>
              </div>
            </Card>

            <!-- Milestones Section -->
            <Card
              v-if="selectedProject.freelancerResponse?.milestones && selectedProject.freelancerResponse.milestones.length > 0"
              class="p-6"
            >
              <h3 class="text-lg font-semibold text-[#263238] mb-4">Marcos de Entrega</h3>
              <div class="space-y-3">
                <div
                  v-for="milestone in selectedProject.freelancerResponse.milestones"
                  :key="milestone.id"
                  :class="[
                    'p-4 rounded-lg border transition-colors',
                    milestone.status === 'completed'
                      ? 'bg-green-50 border-green-200'
                      : milestone.status === 'in_progress'
                      ? 'bg-blue-50 border-blue-200'
                      : 'bg-gray-50 border-gray-200',
                  ]"
                >
                  <div class="flex items-start justify-between">
                    <div class="flex-1">
                      <div class="flex items-center gap-2 mb-1">
                        <h4 class="font-medium text-[#263238]">{{ milestone.title }}</h4>
                        <Badge :class="getMilestoneStatusConfig(milestone.status).class" class="text-xs">
                          {{ getMilestoneStatusConfig(milestone.status).label }}
                        </Badge>
                      </div>
                      <p class="text-sm text-[#435574] mb-2">{{ milestone.description }}</p>
                      <div class="flex items-center gap-4 text-xs text-[#435574]">
                        <span class="flex items-center gap-1">
                          <Calendar class="h-3 w-3" />
                          Prazo: {{ milestone.deadline }}
                        </span>
                        <span class="flex items-center gap-1">
                          <DollarSign class="h-3 w-3" />
                          {{ formatCurrency(milestone.value) }}
                        </span>
                      </div>
                    </div>
                    <Button
                      v-if="milestone.status !== 'completed'"
                      @click.stop="handleMilestoneComplete(milestone)"
                      size="sm"
                      class="bg-[#286AD8] hover:bg-[#435574] ml-3"
                    >
                      <Check class="h-3 w-3 mr-1" />
                      Concluir Marco
                    </Button>
                    <Button
                      v-else
                      @click.stop="handleMilestoneComplete(milestone)"
                      size="sm"
                      variant="outline"
                      class="text-green-700 border-green-300 hover:bg-green-50 ml-3"
                    >
                      <CheckCircle class="h-3 w-3 mr-1" />
                      Concluído
                    </Button>
                  </div>
                </div>
              </div>
            </Card>

            <!-- Project Files -->
            <Card class="p-6">
              <div class="flex items-center justify-between mb-4">
                <h3 class="text-lg font-semibold text-[#263238]">Arquivos do Projeto</h3>
              </div>

              <!-- Tabs -->
              <div class="flex border-b border-gray-200 mb-4">
                <button
                  @click="activeFileTab = 'client'"
                  :class="[
                    'px-4 py-2 text-sm font-medium border-b-2 transition-colors -mb-px',
                    activeFileTab === 'client'
                      ? 'border-[#286AD8] text-[#286AD8]'
                      : 'border-transparent text-[#435574] hover:text-[#263238]'
                  ]"
                >
                  Cliente
                  <Badge v-if="selectedProject.attachments && selectedProject.attachments.length > 0" variant="secondary" class="ml-2">
                    {{ selectedProject.attachments.length }}
                  </Badge>
                </button>
                <button
                  @click="activeFileTab = 'freelancer'"
                  :class="[
                    'px-4 py-2 text-sm font-medium border-b-2 transition-colors -mb-px',
                    activeFileTab === 'freelancer'
                      ? 'border-[#286AD8] text-[#286AD8]'
                      : 'border-transparent text-[#435574] hover:text-[#263238]'
                  ]"
                >
                  Freelancer
                  <Badge v-if="selectedProject.freelancerAttachments && selectedProject.freelancerAttachments.length > 0" variant="secondary" class="ml-2">
                    {{ selectedProject.freelancerAttachments.length }}
                  </Badge>
                </button>
              </div>

              <!-- Client Files Tab -->
              <div v-if="activeFileTab === 'client'">
                <div v-if="!selectedProject.attachments || selectedProject.attachments.length === 0" class="text-center py-8">
                  <FileText class="h-12 w-12 text-gray-300 mx-auto mb-3" />
                  <p class="text-[#435574]">Nenhum arquivo enviado pelo cliente</p>
                </div>
                <div v-else class="space-y-2">
                  <div
                    v-for="attachment in selectedProject.attachments"
                    :key="attachment.id"
                    class="flex items-center justify-between p-3 bg-gray-50 rounded-lg hover:bg-gray-100 transition-colors"
                  >
                    <div class="flex items-center gap-3">
                      <div class="w-10 h-10 rounded-lg bg-blue-100 flex items-center justify-center">
                        <FileText class="h-5 w-5 text-[#286AD8]" />
                      </div>
                      <div>
                        <p class="text-sm font-medium text-[#263238]">{{ attachment.name }}</p>
                        <p v-if="attachment.size" class="text-xs text-[#435574]">{{ attachment.size }}</p>
                      </div>
                    </div>
                    <div class="flex gap-1">
                      <Button @click="handleViewAttachment(attachment.url)" variant="ghost" size="icon" class="text-[#435574] hover:text-[#286AD8]">
                        <Eye class="h-4 w-4" />
                      </Button>
                      <Button @click="handleDownloadAttachment(attachment.url, attachment.name)" variant="ghost" size="icon" class="text-[#435574] hover:text-[#286AD8]">
                        <Download class="h-4 w-4" />
                      </Button>
                    </div>
                  </div>
                </div>
              </div>

              <!-- Freelancer Files Tab -->
              <div v-if="activeFileTab === 'freelancer'">
                <div v-if="!selectedProject.freelancerAttachments || selectedProject.freelancerAttachments.length === 0" class="text-center py-8">
                  <FileText class="h-12 w-12 text-gray-300 mx-auto mb-3" />
                  <p class="text-[#435574]">Nenhum arquivo enviado pelo freelancer</p>
                </div>
                <div v-else class="space-y-2">
                  <div
                    v-for="attachment in selectedProject.freelancerAttachments"
                    :key="attachment.id"
                    class="flex items-center justify-between p-3 bg-gray-50 rounded-lg hover:bg-gray-100 transition-colors"
                  >
                    <div class="flex items-center gap-3">
                      <div class="w-10 h-10 rounded-lg bg-green-100 flex items-center justify-center">
                        <FileText class="h-5 w-5 text-green-600" />
                      </div>
                      <div>
                        <p class="text-sm font-medium text-[#263238]">{{ attachment.name }}</p>
                        <p v-if="attachment.size" class="text-xs text-[#435574]">{{ attachment.size }}</p>
                      </div>
                    </div>
                    <div class="flex gap-1">
                      <Button @click="handleViewAttachment(attachment.url)" variant="ghost" size="icon" class="text-[#435574] hover:text-[#286AD8]">
                        <Eye class="h-4 w-4" />
                      </Button>
                      <Button @click="handleDownloadAttachment(attachment.url, attachment.name)" variant="ghost" size="icon" class="text-[#435574] hover:text-[#286AD8]">
                        <Download class="h-4 w-4" />
                      </Button>
                    </div>
                  </div>
                </div>
              </div>
            </Card>
          </div>

          <!-- Sidebar Column -->
          <div class="space-y-6">
            <!-- Client Info Card -->
            <Card class="p-6">
              <h3 class="text-lg font-semibold text-[#263238] mb-4">Informações do Cliente</h3>
              <div class="flex items-center gap-3 mb-4">
                <Avatar class="h-12 w-12">
                  <AvatarImage v-if="selectedProject.clientAvatar" :src="selectedProject.clientAvatar" />
                  <AvatarFallback class="bg-amber-100 text-amber-700 font-semibold">
                    {{ getInitials(selectedProject.clientName) }}
                  </AvatarFallback>
                </Avatar>
                <div>
                  <p class="font-semibold text-[#263238]">{{ selectedProject.clientName }}</p>
                  <p class="text-sm text-[#435574]">Cliente</p>
                </div>
              </div>
              <Button
                @click="openChat"
                variant="outline"
                class="w-full"
              >
                <MessageSquare class="h-4 w-4 mr-2" />
                Abrir Chat
              </Button>
            </Card>

            <!-- Activity History Card -->
            <ServiceRequestTimeline
              :request-id="selectedProject.id"
              title="Histórico de Atividades"
            />
          </div>
        </div>
      </template>
    </template>

    <!-- Complete Delivery Confirmation Dialog -->
    <AlertDialog :open="showCompleteDialog" @update:open="(val) => (showCompleteDialog = val)">
      <AlertDialogContent>
        <AlertDialogHeader>
          <AlertDialogTitle>Concluir Entrega do Projeto</AlertDialogTitle>
          <AlertDialogDescription>
            <template v-if="selectedProject">
              Você está prestes a marcar o projeto
              <strong>"{{ selectedProject.service }}"</strong> como concluído.
              <br /><br />
              O cliente será notificado e poderá revisar a entrega. Certifique-se de que todos os
              arquivos e marcos estão finalizados antes de confirmar.
              <br /><br />
              <span class="text-amber-600 font-medium">Esta ação não pode ser desfeita.</span>
            </template>
          </AlertDialogDescription>
        </AlertDialogHeader>
        <AlertDialogFooter>
          <AlertDialogCancel @click="showCompleteDialog = false" :disabled="isCompleting">Cancelar</AlertDialogCancel>
          <AlertDialogAction
            @click="handleCompleteDelivery"
            :disabled="isCompleting"
            class="bg-[#286AD8] hover:bg-[#435574]"
          >
            <Loader2 v-if="isCompleting" class="h-4 w-4 mr-2 animate-spin" />
            <Check v-else class="h-4 w-4 mr-2" />
            {{ isCompleting ? 'Concluindo...' : 'Confirmar Conclusão' }}
          </AlertDialogAction>
        </AlertDialogFooter>
      </AlertDialogContent>
    </AlertDialog>

    <!-- Upload Files Dialog -->
    <Dialog :open="showUploadDialog" @update:open="(val) => (showUploadDialog = val)">
      <DialogContent class="sm:max-w-lg">
        <DialogHeader>
          <DialogTitle>Enviar Arquivos</DialogTitle>
          <DialogDescription>
            Faça upload de arquivos relacionados ao projeto
            <template v-if="selectedProject">"{{ selectedProject.service }}"</template>
          </DialogDescription>
        </DialogHeader>

        <div class="space-y-4">
          <div class="space-y-2">
            <Label for="upload-description">Descrição (opcional)</Label>
            <Textarea
              id="upload-description"
              v-model="uploadDescription"
              placeholder="Adicione uma descrição sobre os arquivos..."
              :rows="2"
            />
          </div>

          <div class="space-y-2">
            <Label>Arquivos *</Label>
            <div
              class="border-2 border-dashed border-gray-300 rounded-lg p-8 text-center hover:border-[#286AD8] transition-colors cursor-pointer"
              @click="($refs.fileInput as HTMLInputElement)?.click()"
            >
              <Upload class="h-12 w-12 text-gray-400 mx-auto mb-4" />
              <p class="text-[#263238] mb-2">
                Clique para fazer upload ou arraste os arquivos
              </p>
              <p class="text-sm text-[#435574] mb-4">
                PDF, JPG, PNG, ZIP • Máximo 10 MB por arquivo
              </p>
              <input
                ref="fileInput"
                type="file"
                multiple
                @change="handleFileSelect"
                class="hidden"
              />
              <Button @click.stop="($refs.fileInput as HTMLInputElement)?.click()" size="sm" variant="outline">
                Selecionar Arquivos
              </Button>
            </div>

            <div v-if="uploadFiles.length > 0" class="mt-3 space-y-2">
              <p class="text-sm font-medium text-[#263238]">
                Arquivos selecionados ({{ uploadFiles.length }}):
              </p>
              <div
                v-for="(file, index) in uploadFiles"
                :key="index"
                class="flex items-center justify-between p-2 bg-gray-50 rounded"
              >
                <div class="flex items-center gap-2">
                  <FileText class="h-4 w-4 text-[#286AD8]" />
                  <span class="text-sm text-[#263238]">{{ file.name }}</span>
                </div>
                <div class="flex items-center gap-2">
                  <span class="text-xs text-[#435574]">
                    {{ (file.size / (1024 * 1024)).toFixed(2) }} MB
                  </span>
                  <Button
                    @click="removeUploadFile(index)"
                    variant="ghost"
                    size="icon"
                    class="h-6 w-6 text-red-500 hover:text-red-700 hover:bg-red-50"
                  >
                    <X class="h-3 w-3" />
                  </Button>
                </div>
              </div>
            </div>
          </div>

          <div v-if="isUploading" class="space-y-2">
            <div class="flex items-center justify-between text-sm">
              <span class="text-[#435574]">Enviando arquivos...</span>
              <span class="text-[#263238]">{{ uploadProgress }}%</span>
            </div>
            <Progress :value="uploadProgress" class="h-2" />
          </div>
        </div>

        <DialogFooter>
          <Button @click="showUploadDialog = false" variant="outline" :disabled="isUploading">
            Cancelar
          </Button>
          <Button
            @click="handleUploadFiles"
            :disabled="isUploading || uploadFiles.length === 0"
            class="bg-[#286AD8] hover:bg-[#435574]"
          >
            <Loader2 v-if="isUploading" class="h-4 w-4 mr-2 animate-spin" />
            <Upload v-else class="h-4 w-4 mr-2" />
            {{ isUploading ? 'Enviando...' : 'Enviar Arquivos' }}
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  </div>
</template>
