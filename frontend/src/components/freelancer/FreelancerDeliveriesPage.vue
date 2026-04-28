<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted, watch } from "vue";
import {
  Package,
  Upload,
  Check,
  Clock,
  FileText,
  Calendar,
  CheckCircle,
  MessageSquare,
  AlertCircle,
  Loader2,
} from "lucide-vue-next";
import AttachmentUploadField from "../shared/AttachmentUploadField.vue";
import FileAttachment from "../shared/FileAttachment.vue";
import Button from "../ui/button.vue";
import Card from "../ui/card.vue";
import Badge from "../ui/badge.vue";
import Checkbox from "../ui/checkbox.vue";
import Progress from "../ui/progress.vue";
import Avatar from "../ui/avatar.vue";
import AvatarFallback from "../ui/avatar-fallback.vue";
import AvatarImage from "../ui/avatar-image.vue";
import AlertDialog from "../ui/alert-dialog.vue";
import AlertDialogAction from "../ui/alert-dialog-action.vue";
import AlertDialogCancel from "../ui/alert-dialog-cancel.vue";
import AlertDialogContent from "../ui/alert-dialog-content.vue";
import AlertDialogDescription from "../ui/alert-dialog-description.vue";
import AlertDialogFooter from "../ui/alert-dialog-footer.vue";
import AlertDialogHeader from "../ui/alert-dialog-header.vue";
import AlertDialogTitle from "../ui/alert-dialog-title.vue";
import Dialog from "../ui/dialog.vue";
import DialogContent from "../ui/dialog-content.vue";
import DialogDescription from "../ui/dialog-description.vue";
import DialogFooter from "../ui/dialog-footer.vue";
import DialogHeader from "../ui/dialog-header.vue";
import DialogTitle from "../ui/dialog-title.vue";
import ServiceRequestTimeline from "../service/ServiceRequestTimeline.vue";
import {
  useProposalStore,
  type Proposal,
  type Milestone,
} from "@/stores/proposalStore";
import { useDeliveryAdjustmentStore } from "@/stores/deliveryAdjustmentStore";
import {
  useServiceRequestWebSocket,
  type ServiceRequestNotification,
} from "@/composables/useServiceRequestWebSocket";
import { toast } from "@/utils/toast";
import api from "@/api/config/axios";

interface Props {
  onNavigate: (page: string, params?: any) => void;
}

const props = defineProps<Props>();
const proposalStore = useProposalStore();
const adjustmentStore = useDeliveryAdjustmentStore();

const selectedProjectId = ref<string | null>(null);
const showCompleteDialog = ref(false);
const showCompleteAdjustmentDialog = ref(false);
const showUploadDialog = ref(false);
const isCompleting = ref(false);
const isCompletingAdjustment = ref(false);
const isUploading = ref(false);
const uploadProgress = ref(0);
const uploadFiles = ref<File[]>([]);
const uploadDescription = ref("");
const activeFileTab = ref<"client" | "freelancer">("freelancer");

// WebSocket
let wsConnection: ReturnType<typeof useServiceRequestWebSocket> | null = null;

const getUserEmail = () => {
  const token = localStorage.getItem("token");
  if (!token) return null;
  try {
    const payload = JSON.parse(atob(token.split(".")[1]));
    return payload.sub || payload.email;
  } catch {
    return null;
  }
};

const handleServiceRequestNotification = (event: Event) => {
  const notification = (event as CustomEvent<ServiceRequestNotification>)
    .detail;

  switch (notification.type) {
    case "SERVICE_REQUEST_STATUS_CHANGED":
      if (notification.status?.toLowerCase() === "completed") {
        toast.success("O cliente aprovou a entrega! Serviço concluído.");
      } else if (notification.status?.toLowerCase() === "cancelled") {
        toast.warning("Uma solicitação de serviço foi cancelada.");
      }
      proposalStore.loadProposals();
      break;
    case "SERVICE_COMPLETED_REVIEW_PENDING":
      proposalStore.loadProposals();
      break;
    case "ADJUSTMENTS_REQUESTED":
      toast.warning("O cliente solicitou ajustes na entrega.");
      adjustmentStore.getReceived();
      proposalStore.loadProposals();
      break;
    case "ADJUSTMENT_STATUS_CHANGED":
      adjustmentStore.getReceived();
      break;
    case "ADJUSTMENT_DELETED":
      toast.info("O cliente removeu a solicitação de ajuste.");
      adjustmentStore.getReceived();
      proposalStore.loadProposals();
      break;
    case "SERVICE_REQUEST_CANCELLED":
      toast.warning("Projeto cancelado pelo cliente.");
      proposalStore.loadProposals();
      break;
  }
};

// Load data
onMounted(async () => {
  await Promise.all([
    proposalStore.loadProposals(),
    adjustmentStore.getReceived(),
  ]);

  if (activeProjects.value.length > 0 && !selectedProjectId.value) {
    selectedProjectId.value = activeProjects.value[0].id;
    await proposalStore.loadHistory(activeProjects.value[0].id);
  }

  // Conectar WebSocket
  const userEmail = getUserEmail();
  if (userEmail) {
    wsConnection = useServiceRequestWebSocket(userEmail);
    wsConnection.connect();
    window.addEventListener(
      "service-request-notification",
      handleServiceRequestNotification,
    );
  }
});

onUnmounted(() => {
  window.removeEventListener(
    "service-request-notification",
    handleServiceRequestNotification,
  );
  if (wsConnection) {
    wsConnection.disconnect();
  }
});

const activeProjects = computed(() =>
  proposalStore.proposals.filter(
    (p) => p.status === "in_progress" || p.status === "delivered",
  ),
);

const selectedProject = computed(
  () =>
    activeProjects.value.find((p) => p.id === selectedProjectId.value) || null,
);

const getMilestoneProgress = (project: Proposal | null) => {
  if (!project) return 0;

  const milestones = project.freelancerResponse?.milestones;
  if (milestones && milestones.length > 0) {
    const completed = milestones.filter(
      (milestone) => milestone.status === "completed",
    ).length;
    return Math.round((completed / milestones.length) * 100);
  }

  return 50;
};

const selectedProjectMilestones = computed(
  () => selectedProject.value?.freelancerResponse?.milestones ?? [],
);

const selectedProjectCompletedMilestones = computed(
  () =>
    selectedProjectMilestones.value.filter(
      (milestone) => milestone.status === "completed",
    ).length,
);

const pendingAdjustmentForSelectedProject = computed(() => {
  if (!selectedProject.value) return null;
  return adjustmentStore.adjustments.find(
    (a) =>
      a.serviceRequestId.toString() === selectedProject.value!.id &&
      (a.status === "PENDING" || a.status === "ACKNOWLEDGED"),
  );
});

watch(selectedProjectId, async (newId) => {
  if (newId) {
    await proposalStore.loadHistory(newId);
  }
});

const projectProgress = computed(() =>
  getMilestoneProgress(selectedProject.value),
);

const remainingDays = computed(() => {
  const project = selectedProject.value;
  if (!project) return 0;
  const startDate = new Date(
    project.freelancerResponse?.sentAt || project.receivedAt,
  );
  startDate.setHours(0, 0, 0, 0);
  const executionDays =
    project.freelancerResponse?.executionDays ||
    parseInt(project.deadline) ||
    30;
  const deadlineDate = new Date(startDate);
  deadlineDate.setDate(deadlineDate.getDate() + executionDays);
  const today = new Date();
  today.setHours(0, 0, 0, 0);
  const diffMs = deadlineDate.getTime() - today.getTime();
  return Math.round(diffMs / (1000 * 60 * 60 * 24));
});

// Select project
const selectProject = async (id: string) => {
  selectedProjectId.value = id;
};

// Format currency
const formatCurrency = (value?: number | null) =>
  value == null
    ? "A combinar"
    : new Intl.NumberFormat("pt-BR", {
        style: "currency",
        currency: "BRL",
      }).format(value);

// Client initials
const getInitials = (name: string) =>
  name
    .split(" ")
    .map((n) => n[0])
    .join("")
    .substring(0, 2)
    .toUpperCase();

// Open chat
const openChat = () => {
  props.onNavigate("chat");
};

// Handle complete delivery
const handleCompleteDelivery = async () => {
  if (!selectedProject.value) return;
  isCompleting.value = true;
  try {
    const success = await proposalStore.completeProposal(
      selectedProject.value.id,
    );
    if (success) {
      toast.success(
        "Entrega concluída com sucesso! O cliente será notificado.",
      );
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
      toast.error("Erro ao concluir a entrega. Tente novamente.");
    }
  } catch {
    toast.error("Erro ao concluir a entrega. Tente novamente.");
  } finally {
    isCompleting.value = false;
  }
};

// Handle complete delivery (novo envio pós-ajuste)
const handleCompleteAdjustment = async () => {
  if (!selectedProject.value) return;
  const pendingAdjustment = pendingAdjustmentForSelectedProject.value;
  if (!pendingAdjustment) return;

  isCompletingAdjustment.value = true;
  try {
    // 1. Marcar o ajuste como concluído (responded)
    // Usando IN_PROGRESS ou COMPLETED na store? A store ClientDeliveryApprovalPage trata isso?
    // O correto para "enviei novamente" é que o ajuste está "resolvido", ou seja, COMPLETED.
    // E o status do request volta pra DELIVERED aguardando nova aprovação.
    const successAdjustment = await adjustmentStore.updateStatus(
      pendingAdjustment.id,
      "COMPLETED",
      "Ajustes realizados e nova entrega enviada.",
    );

    if (successAdjustment) {
      // 2. Chamar o endpoint que conclui a proposta de novo para forçar o status DELIVERED
      await proposalStore.completeProposal(selectedProject.value.id);

      toast.success(
        "Ajustes finalizados e enviados ao cliente para nova avaliação!",
      );
      showCompleteAdjustmentDialog.value = false;

      // Reload to update the list
      await Promise.all([
        proposalStore.loadProposals(),
        adjustmentStore.getReceived(),
      ]);
    } else {
      toast.error("Erro ao finalizar ajuste. Tente novamente.");
    }
  } catch {
    toast.error("Erro ao finalizar ajuste. Tente novamente.");
  } finally {
    isCompletingAdjustment.value = false;
  }
};

// Handle milestone status update
const handleMilestoneComplete = async (milestone: Milestone) => {
  if (!selectedProject.value) return;
  const newStatus = milestone.status === "completed" ? "pending" : "completed";
  const success = await proposalStore.updateMilestoneStatus(
    selectedProject.value.id,
    milestone.id,
    newStatus,
  );
  if (success) {
    const label =
      newStatus === "completed" ? "marcado como concluído" : "desmarcado";
    toast.success(`Marco "${milestone.title}" ${label}.`);
  } else {
    toast.error("Erro ao atualizar marco.");
  }
};

// File upload
const handleUploadFileRejected = (message: string) => {
  toast.error(message);
};

const openUploadDialog = () => {
  uploadFiles.value = [];
  uploadDescription.value = "";
  uploadProgress.value = 0;
  isUploading.value = false;
  showUploadDialog.value = true;
};

const handleUploadFiles = async () => {
  if (!selectedProject.value) {
    toast.error("Selecione um projeto antes de enviar arquivos");
    return;
  }

  if (uploadFiles.value.length === 0) {
    toast.error("Selecione pelo menos um arquivo");
    return;
  }

  isUploading.value = true;
  uploadProgress.value = 0;
  let progressInterval: ReturnType<typeof setInterval> | null = null;

  try {
    const formData = new FormData();
    uploadFiles.value.forEach((file) => formData.append("files", file));

    // Simulate progress
    progressInterval = setInterval(() => {
      if (uploadProgress.value < 90) uploadProgress.value += 10;
    }, 300);

    const response = await api.post("/upload/images", formData, {
      headers: { "Content-Type": "multipart/form-data" },
    });

    if (progressInterval) {
      clearInterval(progressInterval);
    }

    const urls = response.data.urls || [];
    if (urls.length > 0) {
      await api.post(
        `/service-requests/${selectedProject.value.id}/freelancer-attachments`,
        {
          attachments: urls.map((url: string, index: number) => ({
            url,
            name: uploadFiles.value[index]?.name || "Arquivo",
            description: uploadDescription.value.trim() || null,
          })),
        },
      );
    }

    uploadProgress.value = 100;
    toast.success("Arquivos enviados com sucesso!");
    showUploadDialog.value = false;

    // Reload proposals to get updated attachments
    await proposalStore.loadProposals();
  } catch {
    toast.error("Erro ao enviar arquivos. Tente novamente.");
  } finally {
    if (progressInterval) {
      clearInterval(progressInterval);
    }
    isUploading.value = false;
  }
};

// Format date
const formatDate = (dateString: string) => {
  try {
    return new Date(dateString).toLocaleDateString("pt-BR");
  } catch {
    return dateString;
  }
};

// Get status badge config for project cards
const getProjectStatusConfig = (project: Proposal) => {
  // Verificar se há ajustes pendentes
  const pendingAdjustment = adjustmentStore.adjustments.find(
    (a) =>
      a.serviceRequestId.toString() === project.id &&
      (a.status === "PENDING" || a.status === "ACKNOWLEDGED"),
  );

  if (pendingAdjustment) {
    return {
      label: "Ajustes Solicitados",
      class: "bg-yellow-100 text-yellow-700 hover:bg-yellow-100",
      icon: AlertCircle,
    };
  }

  if (project.status === "delivered") {
    return {
      label: "Aguardando Aprovação",
      class: "bg-emerald-100 text-emerald-700 hover:bg-emerald-100",
      icon: CheckCircle,
    };
  }

  // Check milestones for more nuanced status
  const milestones = project.freelancerResponse?.milestones;
  if (milestones && milestones.length > 0) {
    const allCompleted = milestones.every((m) => m.status === "completed");
    if (allCompleted) {
      return {
        label: "Revisão Interna",
        class: "bg-orange-100 text-orange-700 hover:bg-orange-100",
        icon: AlertCircle,
      };
    }
    const hasInProgress = milestones.some((m) => m.status === "in_progress");
    if (hasInProgress) {
      return {
        label: "Em Andamento",
        class: "bg-blue-100 text-blue-700 hover:bg-blue-100",
        icon: Clock,
      };
    }
  }
  return {
    label: "Em Andamento",
    class: "bg-blue-100 text-blue-700 hover:bg-blue-100",
    icon: Clock,
  };
};

// Get milestone status badge
const getMilestoneStatusConfig = (status: Milestone["status"]) => {
  switch (status) {
    case "completed":
      return {
        label: "Concluído",
        class: "bg-green-100 text-green-700 hover:bg-green-100",
      };
    case "in_progress":
      return {
        label: "Em Andamento",
        class: "bg-blue-100 text-blue-700 hover:bg-blue-100",
      };
    default:
      return {
        label: "Pendente",
        class: "bg-gray-100 text-gray-600 hover:bg-gray-100",
      };
  }
};
</script>

<template>
  <div class="max-w-7xl mx-auto space-y-6">
    <!-- Header -->
    <div class="flex items-start justify-between">
      <div>
        <h1 class="text-2xl font-bold text-[#263238] mb-2">
          Projetos em Andamento
        </h1>
        <p class="text-[#435574]">Acompanhe e gerencie seus projetos ativos</p>
      </div>
      <Badge
        v-if="activeProjects.length > 0"
        class="bg-blue-100 text-blue-700 hover:bg-blue-100 text-sm px-3 py-1"
      >
        {{ activeProjects.length }} Projeto{{
          activeProjects.length !== 1 ? "s" : ""
        }}
        Ativo{{ activeProjects.length !== 1 ? "s" : "" }}
      </Badge>
    </div>

    <!-- Loading State -->
    <div
      v-if="proposalStore.isLoading"
      class="flex items-center justify-center py-16"
    >
      <Loader2 class="h-8 w-8 text-[#286AD8] animate-spin mr-3" />
      <span class="text-[#435574]">Carregando projetos...</span>
    </div>

    <!-- Empty State -->
    <Card
      v-else-if="activeProjects.length === 0"
      class="p-12 flex flex-col items-center justify-center text-center min-h-[320px]"
    >
      <div class="inline-flex p-6 bg-gray-100 rounded-full mb-6">
        <Package class="h-16 w-16 text-gray-400" />
      </div>
      <h2 class="text-xl font-semibold text-[#263238] mb-2">
        Nenhum projeto em andamento
      </h2>
      <p class="text-[#435574] mb-6">
        Quando um cliente aceitar sua proposta, o projeto aparecerá aqui.
      </p>
      <Button
        @click="props.onNavigate('freelancer-proposals')"
        variant="default"
      >
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
            <h3
              class="font-semibold text-[#263238] text-sm leading-tight flex-1 mr-2"
            >
              {{ project.service }}
            </h3>
            <Badge
              :class="getProjectStatusConfig(project).class"
              class="text-xs whitespace-nowrap flex-shrink-0"
            >
              <component
                :is="getProjectStatusConfig(project).icon"
                class="h-3 w-3 mr-1"
              />
              {{ getProjectStatusConfig(project).label }}
            </Badge>
          </div>

          <div class="flex items-center gap-2 mb-3">
            <Avatar class="h-6 w-6">
              <AvatarImage
                v-if="project.clientAvatar"
                :src="project.clientAvatar"
              />
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
                {{ getMilestoneProgress(project) }}%
              </span>
            </div>
            <Progress :value="getMilestoneProgress(project)" class="h-1.5" />
          </div>

          <div class="flex items-center justify-between text-xs text-[#435574]">
            <span class="flex items-center gap-1">
              <Calendar class="h-3 w-3" />
              Prazo:
              {{
                project.freelancerResponse?.executionDays ||
                parseInt(project.deadline)
              }}
              dias
            </span>
            <span class="font-medium text-[#263238]">
              {{
                formatCurrency(
                  project.freelancerResponse?.value || project.budget,
                )
              }}
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
                    <h2 class="text-lg font-semibold text-[#263238]">
                      {{ selectedProject.service }}
                    </h2>
                    <Badge
                      :class="getProjectStatusConfig(selectedProject).class"
                    >
                      <component
                        :is="getProjectStatusConfig(selectedProject).icon"
                        class="h-3 w-3 mr-1"
                      />
                      {{ getProjectStatusConfig(selectedProject).label }}
                    </Badge>
                  </div>
                  <p class="text-sm text-[#435574] leading-relaxed">
                    {{ selectedProject.description }}
                  </p>
                </div>
              </div>

              <!-- Adjustment Alert -->
              <div
                v-if="pendingAdjustmentForSelectedProject"
                class="mb-6 bg-yellow-50 border border-yellow-200 rounded-lg p-4"
              >
                <div class="flex items-start gap-3">
                  <AlertCircle class="h-5 w-5 text-yellow-600 mt-0.5" />
                  <div>
                    <h4 class="text-sm font-semibold text-yellow-800 mb-1">
                      O cliente solicitou ajustes na entrega
                    </h4>
                    <p class="text-sm text-yellow-700 whitespace-pre-wrap">
                      {{ pendingAdjustmentForSelectedProject.reason }}
                    </p>
                  </div>
                </div>
              </div>

              <div class="space-y-4 mb-6">
                <div class="space-y-1.5">
                  <div class="flex items-center justify-between text-sm">
                    <span class="text-[#435574]">Progresso do Projeto</span>
                    <span class="text-[#263238] font-medium"
                      >{{ projectProgress }}%</span
                    >
                  </div>
                  <Progress :value="projectProgress" class="h-2" />
                </div>

                <div
                  v-if="selectedProjectMilestones.length > 0"
                  class="rounded-xl border border-slate-200 bg-slate-50 p-4"
                >
                  <div class="flex items-start justify-between gap-4 mb-4">
                    <div>
                      <h3 class="text-sm font-semibold text-[#263238]">
                        Checklist do Projeto
                      </h3>
                      <p class="text-sm text-[#435574]">
                        {{ selectedProjectCompletedMilestones }} de
                        {{ selectedProjectMilestones.length }}
                        {{
                          selectedProjectMilestones.length === 1
                            ? "marco concluído"
                            : "marcos concluídos"
                        }}
                      </p>
                    </div>
                    <Badge
                      class="bg-white text-[#263238] border border-slate-200 hover:bg-white"
                    >
                      {{ projectProgress }}%
                    </Badge>
                  </div>

                  <div class="space-y-3">
                    <label
                      v-for="milestone in selectedProjectMilestones"
                      :key="milestone.id"
                      class="flex items-start gap-3 rounded-lg border border-slate-200 bg-white px-3 py-3"
                    >
                      <Checkbox
                        class="mt-0.5"
                        :checked="milestone.status === 'completed'"
                        @update:checked="
                          () => handleMilestoneComplete(milestone)
                        "
                      />

                      <div class="min-w-0 flex-1">
                        <div class="flex items-center gap-2 flex-wrap">
                          <p
                            :class="[
                              'text-sm font-medium',
                              milestone.status === 'completed'
                                ? 'text-slate-500 line-through'
                                : 'text-[#263238]',
                            ]"
                          >
                            {{ milestone.title }}
                          </p>
                          <Badge
                            :class="
                              getMilestoneStatusConfig(milestone.status).class
                            "
                            class="text-xs"
                          >
                            {{
                              getMilestoneStatusConfig(milestone.status).label
                            }}
                          </Badge>
                        </div>
                        <p
                          v-if="milestone.description"
                          class="text-sm text-[#435574] mt-1"
                        >
                          {{ milestone.description }}
                        </p>
                      </div>
                    </label>
                  </div>
                </div>
              </div>

              <div class="flex gap-8 mb-6">
                <div>
                  <p class="text-xs text-[#435574] mb-0.5">Data de Início</p>
                  <p class="text-sm font-medium text-[#263238]">
                    {{ formatDate(selectedProject.receivedAt) }}
                  </p>
                </div>
                <div>
                  <p class="text-xs text-[#435574] mb-0.5">Prazo de Entrega</p>
                  <p class="text-sm font-medium text-[#263238]">
                    {{
                      selectedProject.freelancerResponse?.executionDays ||
                      parseInt(selectedProject.deadline)
                    }}
                    dias
                  </p>
                </div>
                <div>
                  <p class="text-xs text-[#435574] mb-0.5">Tempo Restante</p>
                  <p
                    :class="[
                      'text-sm font-medium',
                      remainingDays < 0
                        ? 'text-red-600'
                        : remainingDays <= 7
                          ? 'text-amber-600'
                          : 'text-green-600',
                    ]"
                  >
                    <template v-if="remainingDays < 0"
                      >{{ Math.abs(remainingDays) }} dias de atraso</template
                    >
                    <template v-else-if="remainingDays === 0"
                      >Vence hoje</template
                    >
                    <template v-else
                      >{{ remainingDays }} dias restantes</template
                    >
                  </p>
                </div>
              </div>

              <!-- Action Buttons -->
              <div class="flex flex-wrap gap-3">
                <Button variant="outline" @click="openChat">
                  <MessageSquare class="h-4 w-4 mr-2" />
                  Mensagem
                </Button>
                <!-- Botão de Concluir Entrega (1a vez) -->
                <Button
                  v-if="
                    selectedProject.status !== 'delivered' &&
                    !pendingAdjustmentForSelectedProject
                  "
                  @click="showCompleteDialog = true"
                  variant="default"
                >
                  <Check class="h-4 w-4 mr-2" />
                  Concluir Entrega
                </Button>
                <!-- Botão de Finalizar Ajustes (se houver ajuste pendente) -->
                <Button
                  v-if="pendingAdjustmentForSelectedProject"
                  @click="showCompleteAdjustmentDialog = true"
                  variant="warning"
                >
                  <CheckCircle class="h-4 w-4 mr-2" />
                  Finalizar Ajustes
                </Button>
              </div>
            </Card>

            <!-- Project Files -->
            <Card class="p-6">
              <div class="flex items-center justify-between mb-4">
                <h3 class="text-lg font-semibold text-[#263238]">
                  Arquivos do Projeto
                </h3>
                <Button @click="openUploadDialog" variant="outline">
                  <Upload class="h-4 w-4 mr-2" />
                  Enviar Arquivos
                </Button>
              </div>

              <!-- Tabs -->
              <div class="flex border-b border-gray-200 mb-4">
                <button
                  @click="activeFileTab = 'client'"
                  :class="[
                    'px-4 py-2 text-sm font-medium border-b-2 transition-colors -mb-px',
                    activeFileTab === 'client'
                      ? 'border-[#286AD8] text-[#286AD8]'
                      : 'border-transparent text-[#435574] hover:text-[#263238]',
                  ]"
                >
                  Cliente
                  <Badge
                    v-if="
                      selectedProject.attachments &&
                      selectedProject.attachments.length > 0
                    "
                    variant="secondary"
                    class="ml-2"
                  >
                    {{ selectedProject.attachments.length }}
                  </Badge>
                </button>
                <button
                  @click="activeFileTab = 'freelancer'"
                  :class="[
                    'px-4 py-2 text-sm font-medium border-b-2 transition-colors -mb-px',
                    activeFileTab === 'freelancer'
                      ? 'border-[#286AD8] text-[#286AD8]'
                      : 'border-transparent text-[#435574] hover:text-[#263238]',
                  ]"
                >
                  Freelancer
                  <Badge
                    v-if="
                      selectedProject.freelancerAttachments &&
                      selectedProject.freelancerAttachments.length > 0
                    "
                    variant="secondary"
                    class="ml-2"
                  >
                    {{ selectedProject.freelancerAttachments.length }}
                  </Badge>
                </button>
              </div>

              <!-- Client Files Tab -->
              <div v-if="activeFileTab === 'client'">
                <div
                  v-if="
                    !selectedProject.attachments ||
                    selectedProject.attachments.length === 0
                  "
                  class="text-center py-8"
                >
                  <FileText class="h-12 w-12 text-gray-300 mx-auto mb-3" />
                  <p class="text-[#435574]">
                    Nenhum arquivo enviado pelo cliente
                  </p>
                </div>
                <div v-else class="space-y-2">
                  <FileAttachment
                    v-for="attachment in selectedProject.attachments"
                    :key="attachment.id"
                    :name="attachment.name"
                    :url="attachment.url"
                    :size="attachment.size"
                    variant="blue"
                    sender-info="Enviado pelo cliente"
                  />
                </div>
              </div>

              <!-- Freelancer Files Tab -->
              <div v-if="activeFileTab === 'freelancer'">
                <div
                  v-if="
                    !selectedProject.freelancerAttachments ||
                    selectedProject.freelancerAttachments.length === 0
                  "
                  class="text-center py-8"
                >
                  <FileText class="h-12 w-12 text-gray-300 mx-auto mb-3" />
                  <p class="text-[#435574]">
                    Nenhum arquivo enviado pelo freelancer
                  </p>
                </div>
                <div v-else class="space-y-2">
                  <FileAttachment
                    v-for="attachment in selectedProject.freelancerAttachments"
                    :key="attachment.id"
                    :name="attachment.name"
                    :url="attachment.url"
                    :size="attachment.size"
                    :description="attachment.description"
                    variant="amber"
                    sender-info="Enviado por você"
                  />
                </div>
              </div>
            </Card>
          </div>

          <!-- Sidebar Column -->
          <div class="space-y-6">
            <!-- Client Info Card -->
            <Card class="p-6">
              <h3 class="text-lg font-semibold text-[#263238] mb-4">
                Informações do Cliente
              </h3>
              <div class="flex items-center gap-3 mb-4">
                <Avatar class="h-12 w-12">
                  <AvatarImage
                    v-if="selectedProject.clientAvatar"
                    :src="selectedProject.clientAvatar"
                  />
                  <AvatarFallback
                    class="bg-amber-100 text-amber-700 font-semibold"
                  >
                    {{ getInitials(selectedProject.clientName) }}
                  </AvatarFallback>
                </Avatar>
                <div>
                  <p class="font-semibold text-[#263238]">
                    {{ selectedProject.clientName }}
                  </p>
                  <p class="text-sm text-[#435574]">Cliente</p>
                </div>
              </div>
              <Button @click="openChat" variant="outline" class="w-full">
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
    <AlertDialog
      :open="showCompleteDialog"
      @update:open="(val) => (showCompleteDialog = val)"
    >
      <AlertDialogContent>
        <AlertDialogHeader>
          <AlertDialogTitle>Concluir Entrega do Projeto</AlertDialogTitle>
          <AlertDialogDescription>
            <template v-if="selectedProject">
              Você está prestes a marcar o projeto
              <strong>"{{ selectedProject.service }}"</strong> como concluído.
              <br /><br />
              O cliente será notificado e poderá revisar a entrega.
              Certifique-se de que todos os arquivos e marcos estão finalizados
              antes de confirmar.
              <br /><br />
              <span class="text-amber-600 font-medium"
                >Esta ação não pode ser desfeita.</span
              >
            </template>
          </AlertDialogDescription>
        </AlertDialogHeader>
        <AlertDialogFooter>
          <AlertDialogCancel
            @click="showCompleteDialog = false"
            :disabled="isCompleting"
            >Cancelar</AlertDialogCancel
          >
          <AlertDialogAction
            @click="handleCompleteDelivery"
            :disabled="isCompleting"
          >
            <Loader2 v-if="isCompleting" class="h-4 w-4 mr-2 animate-spin" />
            <Check v-else class="h-4 w-4 mr-2" />
            {{ isCompleting ? "Concluindo..." : "Confirmar Conclusão" }}
          </AlertDialogAction>
        </AlertDialogFooter>
      </AlertDialogContent>
    </AlertDialog>

    <!-- Complete Adjustment Confirmation Dialog -->
    <AlertDialog
      :open="showCompleteAdjustmentDialog"
      @update:open="(val) => (showCompleteAdjustmentDialog = val)"
    >
      <AlertDialogContent>
        <AlertDialogHeader>
          <AlertDialogTitle>Finalizar e Reenviar Projeto</AlertDialogTitle>
          <AlertDialogDescription>
            <template v-if="selectedProject">
              Você está prestes a finalizar os ajustes do projeto
              <strong>"{{ selectedProject.service }}"</strong>. <br /><br />
              Certifique-se de que todas as correções solicitadas pelo cliente
              foram realizadas e que você fez o
              <strong class="text-blue-600">upload dos novos arquivos</strong>
              (caso necessário) antes de confirmar. <br /><br />
              O projeto será novamente enviado para avaliação e aprovação final
              do cliente.
            </template>
          </AlertDialogDescription>
        </AlertDialogHeader>
        <AlertDialogFooter>
          <AlertDialogCancel
            @click="showCompleteAdjustmentDialog = false"
            :disabled="isCompletingAdjustment"
            >Cancelar</AlertDialogCancel
          >
          <AlertDialogAction
            @click="handleCompleteAdjustment"
            :disabled="isCompletingAdjustment"
            class="bg-warning text-warning-foreground hover:bg-warning/90"
          >
            <Loader2
              v-if="isCompletingAdjustment"
              class="h-4 w-4 mr-2 animate-spin"
            />
            <CheckCircle v-else class="h-4 w-4 mr-2" />
            {{ isCompletingAdjustment ? "Finalizando..." : "Confirmar Envio" }}
          </AlertDialogAction>
        </AlertDialogFooter>
      </AlertDialogContent>
    </AlertDialog>

    <!-- Upload Files Dialog -->
    <Dialog
      :open="showUploadDialog"
      @update:open="(val) => (showUploadDialog = val)"
    >
      <DialogContent class="sm:max-w-lg">
        <DialogHeader>
          <DialogTitle>Enviar Arquivos</DialogTitle>
          <DialogDescription>
            Faça upload de arquivos relacionados ao projeto
            <template v-if="selectedProject"
              >"{{ selectedProject.service }}"</template
            >
          </DialogDescription>
        </DialogHeader>

        <div class="space-y-4">
          <AttachmentUploadField
            v-model:files="uploadFiles"
            v-model:description="uploadDescription"
            description-enabled
            label="Arquivos *"
            empty-message="Clique para fazer upload ou arraste os arquivos"
            helper-text="PDF, JPG, PNG, ZIP • Máximo 10 MB por arquivo"
            @file-rejected="handleUploadFileRejected"
          />

          <div v-if="isUploading" class="space-y-2">
            <div class="flex items-center justify-between text-sm">
              <span class="text-[#435574]">Enviando arquivos...</span>
              <span class="text-[#263238]">{{ uploadProgress }}%</span>
            </div>
            <Progress :value="uploadProgress" class="h-2" />
          </div>
        </div>

        <DialogFooter>
          <Button
            @click="showUploadDialog = false"
            variant="outline"
            :disabled="isUploading"
          >
            Cancelar
          </Button>
          <Button
            @click="handleUploadFiles"
            :disabled="isUploading || uploadFiles.length === 0"
            variant="default"
          >
            <Loader2 v-if="isUploading" class="h-4 w-4 mr-2 animate-spin" />
            <Upload v-else class="h-4 w-4 mr-2" />
            {{ isUploading ? "Enviando..." : "Enviar Arquivos" }}
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  </div>
</template>
