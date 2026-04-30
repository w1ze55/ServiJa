<script setup lang="ts">
import {
  ref,
  computed,
  onMounted,
  onUnmounted,
  watch,
  type Component,
} from "vue";
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
  Send,
  Archive,
  Trash2,
  MoreVertical,
  RefreshCw,
  Package,
} from "lucide-vue-next";
import Button from "../ui/button.vue";
import Input from "../ui/input.vue";
import Card from "../ui/card.vue";
import Badge from "../ui/badge.vue";
import Avatar from "../ui/avatar.vue";
import AvatarImage from "../ui/avatar-image.vue";
import AvatarFallback from "../ui/avatar-fallback.vue";
import Select from "../ui/select.vue";
import SelectContent from "../ui/select-content.vue";
import SelectItem from "../ui/select-item.vue";
import SelectTrigger from "../ui/select-trigger.vue";
import SelectValue from "../ui/select-value.vue";
import Tabs from "../ui/tabs.vue";
import TabsContent from "../ui/tabs-content.vue";
import TabsList from "../ui/tabs-list.vue";
import TabsTrigger from "../ui/tabs-trigger.vue";
import DropdownMenu from "../ui/dropdown-menu.vue";
import DropdownMenuContent from "../ui/dropdown-menu-content.vue";
import DropdownMenuItem from "../ui/dropdown-menu-item.vue";
import DropdownMenuSeparator from "../ui/dropdown-menu-separator.vue";
import DropdownMenuTrigger from "../ui/dropdown-menu-trigger.vue";
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
import Textarea from "../ui/textarea.vue";
import Label from "../ui/label.vue";
import WorkModelBadge from "../shared/WorkModelBadge.vue";
import { useProposalStore, type Proposal } from "@/stores/proposalStore";
import { useRouter } from "vue-router";
import { useServiceRequestWebSocket } from "@/composables/useServiceRequestWebSocket";
import { usePaymentWebSocket } from "@/composables/usePaymentWebSocket";
import { toast } from "@/utils/toast";
import { getStatusLabel } from "@/utils/statusTranslations";

const router = useRouter();

interface Props {
  onNavigate: (page: string, params?: any) => void;
}

interface FilterOption {
  value: string;
  label: string;
}

const props = defineProps<Props>();
const proposalStore = useProposalStore();

const ALL_CATEGORIES_VALUE = "all";
const ALL_SUBCATEGORIES_VALUE = "all";

const searchQuery = ref("");
const categoryFilter = ref(ALL_CATEGORIES_VALUE);
const subcategoryFilter = ref(ALL_SUBCATEGORIES_VALUE);
const sortBy = ref("Mais recentes");
const timeFilter = ref("Últimos 30 dias");
const customDateFrom = ref("");
const customDateTo = ref("");
const currentTab = ref("all");
const currentPage = ref(1);
const itemsPerPage = 6;

const showRejectDialog = ref(false);
const showArchiveDialog = ref(false);
const selectedProposal = ref<Proposal | null>(null);
const rejectReason = ref("");

// WebSocket
let wsConnection: any = null;
let paymentWsConnection: any = null;

// Mapeamento de valores de ordenação para português
const sortOptions = {
  recent: "Mais recentes",
  "budget-high": "Maior orçamento",
  "budget-low": "Menor orçamento",
};

const getSortLabel = (value: string) => {
  return sortOptions[value as keyof typeof sortOptions] || value;
};

const normalizeFilterText = (value: string) =>
  value
    .normalize("NFD")
    .replace(/[\u0300-\u036f]/g, "")
    .trim()
    .toLowerCase();

const initialTimeFilterKey = normalizeFilterText(timeFilter.value);

const getCategoryDisplayLabel = (proposal: Proposal) =>
  proposal.categoryLabel?.trim() || proposal.category?.trim() || "";

const getSubcategoryDisplayLabel = (proposal: Proposal) =>
  proposal.subcategoryLabel?.trim() || proposal.subcategory?.trim() || "";

const getCategoryFilterValue = (proposal: Proposal) => {
  const label = getCategoryDisplayLabel(proposal);

  if (!label) {
    return "";
  }

  return proposal.categoryId != null
    ? `category:${proposal.categoryId}`
    : `category:${normalizeFilterText(label)}`;
};

const getSubcategoryFilterValue = (proposal: Proposal) => {
  const label = getSubcategoryDisplayLabel(proposal);

  if (!label) {
    return "";
  }

  return proposal.subcategoryId != null
    ? `subcategory:${proposal.subcategoryId}`
    : `subcategory:${normalizeFilterText(label)}`;
};

const buildFilterOptions = (
  proposals: Proposal[],
  valueGetter: (proposal: Proposal) => string,
  labelGetter: (proposal: Proposal) => string,
): FilterOption[] => {
  const uniqueOptions = new Map<string, string>();

  proposals.forEach((proposal) => {
    const value = valueGetter(proposal);
    const label = labelGetter(proposal);

    if (!value || !label || uniqueOptions.has(value)) {
      return;
    }

    uniqueOptions.set(value, label);
  });

  return Array.from(uniqueOptions.entries())
    .map(([value, label]) => ({ value, label }))
    .sort((a, b) => a.label.localeCompare(b.label, "pt-BR"));
};

const categoryOptions = computed(() =>
  buildFilterOptions(
    proposalStore.proposals,
    getCategoryFilterValue,
    getCategoryDisplayLabel,
  ),
);

const proposalsForSelectedCategory = computed(() => {
  if (categoryFilter.value === ALL_CATEGORIES_VALUE) {
    return proposalStore.proposals;
  }

  return proposalStore.proposals.filter(
    (proposal) => getCategoryFilterValue(proposal) === categoryFilter.value,
  );
});

const subcategoryOptions = computed(() =>
  buildFilterOptions(
    proposalsForSelectedCategory.value,
    getSubcategoryFilterValue,
    getSubcategoryDisplayLabel,
  ),
);

const selectedCategoryLabel = computed(
  () =>
    categoryOptions.value.find(
      (option) => option.value === categoryFilter.value,
    )?.label || "Todas as categorias",
);

const selectedSubcategoryLabel = computed(
  () =>
    subcategoryOptions.value.find(
      (option) => option.value === subcategoryFilter.value,
    )?.label || "Todas as subcategorias",
);

const hasActiveFilters = computed(
  () =>
    !!searchQuery.value.trim() ||
    categoryFilter.value !== ALL_CATEGORIES_VALUE ||
    subcategoryFilter.value !== ALL_SUBCATEGORIES_VALUE ||
    normalizeFilterText(timeFilter.value) !== initialTimeFilterKey ||
    !!customDateFrom.value ||
    !!customDateTo.value,
);

// Obter mensagem de estado vazio baseada na tab atual
const getEmptyMessage = () => {
  const messages = {
    all: "Você ainda não recebeu nenhuma solicitação",
    pending: "Não há solicitações pendentes no momento",
    negotiating: "Não há solicitações em negociação",
    accepted: "Não há solicitações em andamento",
    rejected: "Não há solicitações recusadas",
    completed: "Não há solicitações concluídas",
  };

  return (
    messages[currentTab.value as keyof typeof messages] ||
    "Nenhuma solicitação encontrada"
  );
};

const isClientCancelled = (proposal: Proposal) =>
  proposal.status === "archived" && !!proposal.cancelReason?.trim();

const getArchivedStatusLabel = (proposal: Proposal) =>
  isClientCancelled(proposal) ? "Cancelada" : "Arquivada";

type ProposalStatusCardKey =
  | "pending"
  | "negotiating"
  | "in_progress"
  | "rejected"
  | "completed";

interface ProposalStatusVisualConfig {
  cardLabel: string;
  badgeClass: string;
  cardIconContainerClass: string;
  cardIconClass: string;
  icon: Component;
}

const proposalStatusVisuals: Record<
  ProposalStatusCardKey,
  ProposalStatusVisualConfig
> = {
  pending: {
    cardLabel: "Pendentes",
    badgeClass: "bg-blue-100 text-blue-600 hover:bg-blue-100",
    cardIconContainerClass: "bg-blue-100",
    cardIconClass: "text-blue-600",
    icon: Clock,
  },
  negotiating: {
    cardLabel: "Negociando",
    badgeClass: "bg-purple-100 text-purple-600 hover:bg-purple-100",
    cardIconContainerClass: "bg-purple-100",
    cardIconClass: "text-purple-600",
    icon: MessageSquare,
  },
  in_progress: {
    cardLabel: "Em Andamento",
    badgeClass: "bg-amber-100 text-amber-600 hover:bg-amber-100",
    cardIconContainerClass: "bg-amber-100",
    cardIconClass: "text-amber-600",
    icon: Package,
  },
  rejected: {
    cardLabel: "Recusadas",
    badgeClass: "bg-red-100 text-red-600 hover:bg-red-100",
    cardIconContainerClass: "bg-red-100",
    cardIconClass: "text-red-600",
    icon: XCircle,
  },
  completed: {
    cardLabel: "Concluídas",
    badgeClass: "bg-green-100 text-green-600 hover:bg-green-100",
    cardIconContainerClass: "bg-green-100",
    cardIconClass: "text-green-600",
    icon: CheckCircle,
  },
};

proposalStatusVisuals.completed.cardLabel = "Conclu\u00eddas";

const proposalStatusCards = computed(() => [
  {
    key: "pending" as const,
    count: proposalStore.stats.pending,
    ...proposalStatusVisuals.pending,
  },
  {
    key: "negotiating" as const,
    count: proposalStore.stats.negotiating,
    ...proposalStatusVisuals.negotiating,
  },
  {
    key: "in_progress" as const,
    count: proposalStore.stats.inProgress,
    ...proposalStatusVisuals.in_progress,
  },
  {
    key: "rejected" as const,
    count: proposalStore.stats.rejected,
    ...proposalStatusVisuals.rejected,
  },
  {
    key: "completed" as const,
    count: proposalStore.stats.completed,
    ...proposalStatusVisuals.completed,
  },
]);

const getProposalStatusVisualKey = (
  proposal: Proposal,
): ProposalStatusCardKey => {
  switch (proposal.status) {
    case "negotiating":
    case "accepted":
      return "negotiating";
    case "in_progress":
    case "delivered":
      return "in_progress";
    case "rejected":
      return "rejected";
    case "completed":
    case "archived":
      return "completed";
    case "pending":
    default:
      return "pending";
  }
};

const getProposalStatusBadge = (proposal: Proposal) => {
  const visual = proposalStatusVisuals[getProposalStatusVisualKey(proposal)];

  return {
    ...visual,
    label:
      proposal.status === "archived"
        ? getArchivedStatusLabel(proposal)
        : getStatusLabel(proposal.status),
  };
};

onMounted(() => {
  proposalStore.loadProposals();

  // Conectar WebSocket
  const userEmail = getUserEmail();

  if (userEmail) {
    wsConnection = useServiceRequestWebSocket(userEmail);
    wsConnection.connect();

    // Escutar notificações de solicitações
    window.addEventListener(
      "service-request-notification",
      handleServiceRequestNotification,
    );

    // Conectar WebSocket de pagamentos
    paymentWsConnection = usePaymentWebSocket(userEmail);
    paymentWsConnection.connect();

    // Escutar notificações de pagamentos
    window.addEventListener("payment-notification", handlePaymentNotification);
  } else {
    console.error("❌ Não foi possível extrair email do token");
  }
});

onUnmounted(() => {
  window.removeEventListener(
    "service-request-notification",
    handleServiceRequestNotification,
  );
  window.removeEventListener("payment-notification", handlePaymentNotification);
  if (wsConnection) {
    wsConnection.disconnect();
  }
  if (paymentWsConnection) {
    paymentWsConnection.disconnect();
  }
});

// Extrair email do usuário do JWT
const getUserEmail = () => {
  const token = localStorage.getItem("token");
  if (!token) return null;

  try {
    const payload = JSON.parse(atob(token.split(".")[1]));
    return payload.sub || payload.email;
  } catch (error) {
    console.error("Erro ao extrair email do token:", error);
    return null;
  }
};

// Lidar com notificações de solicitações via WebSocket
const handleServiceRequestNotification = (event: any) => {
  const notification = event.detail;
  console.log("📩 Notificação recebida:", notification);

  // Mostrar toast baseado no tipo de notificação
  switch (notification.type) {
    case "NEW_SERVICE_REQUEST":
      toast.success(`Nova solicitação de ${notification.contractorName}!`);
      // Recarregar lista de propostas
      proposalStore.loadProposals();
      break;
    case "COUNTER_PROPOSAL":
      toast.info(
        `${notification.contractorName} fez uma contra-proposta de ${notification.counterProposalAmount}`,
      );
      proposalStore.loadProposals();
      break;
    case "PROPOSAL_ACCEPTED":
      toast.success(notification.message);
      proposalStore.loadProposals();
      break;
    case "SERVICE_REQUEST_CANCELLED":
      toast.warning(
        notification.reason
          ? `Solicitação cancelada pelo cliente. Motivo: ${notification.reason}`
          : "Solicitação cancelada pelo cliente.",
      );
      proposalStore.loadProposals();
      break;
    case "SERVICE_REQUEST_STATUS_CHANGED":
      if (notification.status === "COMPLETED") {
        toast.success("Serviço Concluído! O cliente confirmou a entrega.");
      } else if (notification.status === "IN_PROGRESS") {
        toast.success("Pagamento confirmado! O serviço está em andamento");
      } else {
        toast.info(`Status atualizado: ${getStatusLabel(notification.status)}`);
      }
      proposalStore.loadProposals();
      break;
    case "SERVICE_COMPLETED_REVIEW_PENDING":
      toast.success("Serviço concluído pelo cliente!");
      proposalStore.loadProposals();
      break;
    case "NEW_REVIEW":
      toast.info(notification.reviewerName + " enviou uma avaliação!");
      break;
  }
};

// Lidar com notificações de pagamentos via WebSocket
const handlePaymentNotification = (event: any) => {
  const notification = event.detail;
  console.log("💰 Notificação de pagamento recebida:", notification);

  switch (notification.type) {
    case "PIX_CONFIRMED":
      toast.success("💰 Pagamento confirmado! O serviço está em andamento.");
      proposalStore.loadProposals();
      break;
    case "PAYMENT_STATUS_CHANGED":
      toast.info(`Status do pagamento: ${getStatusLabel(notification.status)}`);
      proposalStore.loadProposals();
      break;
  }
};

const filteredProposals = computed(() => {
  let filtered = proposalStore.proposals.filter((proposal) => {
    const matchesSearch =
      proposal.clientName
        .toLowerCase()
        .includes(searchQuery.value.toLowerCase()) ||
      proposal.service
        .toLowerCase()
        .includes(searchQuery.value.toLowerCase()) ||
      proposal.description
        .toLowerCase()
        .includes(searchQuery.value.toLowerCase());
    const matchesCategory =
      categoryFilter.value === ALL_CATEGORIES_VALUE ||
      getCategoryFilterValue(proposal) === categoryFilter.value;
    const matchesSubcategory =
      subcategoryFilter.value === ALL_SUBCATEGORIES_VALUE ||
      getSubcategoryFilterValue(proposal) === subcategoryFilter.value;
    const matchesTab =
      currentTab.value === "all" ||
      (currentTab.value === "pending" && proposal.status === "pending") ||
      (currentTab.value === "negotiating" &&
        (proposal.status === "negotiating" ||
          proposal.status === "accepted")) ||
      (currentTab.value === "in_progress" &&
        (proposal.status === "in_progress" ||
          proposal.status === "delivered")) ||
      (currentTab.value === "rejected" && proposal.status === "rejected") ||
      (currentTab.value === "completed" &&
        (proposal.status === "completed" || proposal.status === "archived"));
    // Filtro por período
    const matchesTime = (() => {
      const proposalDate = new Date(proposal.receivedAt);
      const now = new Date();
      const startOfToday = new Date(
        now.getFullYear(),
        now.getMonth(),
        now.getDate(),
      );

      switch (timeFilter.value) {
        case "Hoje":
          return proposalDate >= startOfToday;
        case "Ontem": {
          const startOfYesterday = new Date(startOfToday);
          startOfYesterday.setDate(startOfYesterday.getDate() - 1);
          return (
            proposalDate >= startOfYesterday && proposalDate < startOfToday
          );
        }
        case "Últimos 7 dias": {
          const sevenDaysAgo = new Date(startOfToday);
          sevenDaysAgo.setDate(sevenDaysAgo.getDate() - 7);
          return proposalDate >= sevenDaysAgo;
        }
        case "Últimos 30 dias": {
          const thirtyDaysAgo = new Date(startOfToday);
          thirtyDaysAgo.setDate(thirtyDaysAgo.getDate() - 30);
          return proposalDate >= thirtyDaysAgo;
        }
        case "Mês atual":
          return (
            proposalDate.getMonth() === now.getMonth() &&
            proposalDate.getFullYear() === now.getFullYear()
          );
        case "Último mês": {
          const lastMonth = new Date(now.getFullYear(), now.getMonth() - 1, 1);
          const endOfLastMonth = new Date(
            now.getFullYear(),
            now.getMonth(),
            0,
            23,
            59,
            59,
          );
          return proposalDate >= lastMonth && proposalDate <= endOfLastMonth;
        }
        case "Intervalo personalizado": {
          const from = customDateFrom.value
            ? new Date(customDateFrom.value + "T00:00:00")
            : null;
          const to = customDateTo.value
            ? new Date(customDateTo.value + "T23:59:59")
            : null;
          if (from && to) return proposalDate >= from && proposalDate <= to;
          if (from) return proposalDate >= from;
          if (to) return proposalDate <= to;
          return true;
        }
        default:
          return true;
      }
    })();

    return (
      matchesSearch &&
      matchesCategory &&
      matchesSubcategory &&
      matchesTab &&
      matchesTime
    );
  });

  if (sortBy.value === "Mais recentes") {
    filtered.sort(
      (a, b) =>
        new Date(b.receivedAt).getTime() - new Date(a.receivedAt).getTime(),
    );
  } else if (sortBy.value === "Maior orçamento") {
    filtered.sort(
      (a, b) =>
        (b.freelancerResponse?.value ?? b.budget ?? 0) -
        (a.freelancerResponse?.value ?? a.budget ?? 0),
    );
  } else if (sortBy.value === "Menor orçamento") {
    filtered.sort(
      (a, b) =>
        (a.freelancerResponse?.value ?? a.budget ?? 0) -
        (b.freelancerResponse?.value ?? b.budget ?? 0),
    );
  }

  return filtered;
});

const totalPages = computed(() =>
  Math.ceil(filteredProposals.value.length / itemsPerPage),
);
const startIndex = computed(() => (currentPage.value - 1) * itemsPerPage);
const currentProposals = computed(() =>
  filteredProposals.value.slice(
    startIndex.value,
    startIndex.value + itemsPerPage,
  ),
);

const formatCurrency = (value?: number | null) => {
  if (value == null) return "A combinar";
  return new Intl.NumberFormat("pt-BR", {
    style: "currency",
    currency: "BRL",
  }).format(value);
};

const formatDate = (dateString: string) => {
  const date = new Date(dateString);
  return date.toLocaleDateString("pt-BR", {
    day: "2-digit",
    month: "2-digit",
    year: "numeric",
    hour: "2-digit",
    minute: "2-digit",
  });
};

const getInitials = (name: string) => {
  return name
    .split(" ")
    .map((n) => n[0])
    .join("")
    .toUpperCase()
    .slice(0, 2);
};

const handleCategoryFilterChange = (value: string) => {
  categoryFilter.value = value;
  subcategoryFilter.value = ALL_SUBCATEGORIES_VALUE;
  currentPage.value = 1;
};

const handleSubcategoryFilterChange = (value: string) => {
  subcategoryFilter.value = value;
  currentPage.value = 1;
};

const handleTimeFilterChange = (value: string) => {
  timeFilter.value = value;
  currentPage.value = 1;
};

const handleSortChange = (value: string) => {
  sortBy.value = value;
  currentPage.value = 1;
};

const handleSearchChange = (value: string) => {
  searchQuery.value = value;
  currentPage.value = 1;
};

const handleViewDetails = (proposal: Proposal) => {
  proposalStore.markAsViewed(proposal.id);
  props.onNavigate("freelancer-proposal-detail", { requestId: proposal.id });
};

const handleOpenRejectDialog = (proposal: Proposal) => {
  selectedProposal.value = proposal;
  rejectReason.value = "";
  showRejectDialog.value = true;
};

const handleRejectProposal = async () => {
  if (!selectedProposal.value) return;

  if (!rejectReason.value.trim()) {
    toast.error("Por favor, informe o motivo da recusa");
    return;
  }

  const success = await proposalStore.rejectProposal(
    selectedProposal.value.id,
    rejectReason.value,
  );
  if (success) {
    toast.success("Solicitação recusada com sucesso");
    showRejectDialog.value = false;
    selectedProposal.value = null;
    rejectReason.value = "";
  } else {
    toast.error("Erro ao recusar solicitação");
  }
};

const handleOpenArchiveDialog = (proposal: Proposal) => {
  selectedProposal.value = proposal;
  showArchiveDialog.value = true;
};

const handleArchiveProposal = async () => {
  if (!selectedProposal.value) return;

  const success = await proposalStore.archiveProposal(
    selectedProposal.value.id,
  );
  if (success) {
    toast.success("Solicitação arquivada com sucesso");
    showArchiveDialog.value = false;
    selectedProposal.value = null;
  } else {
    toast.error("Erro ao arquivar solicitação");
  }
};

const handleRefresh = () => {
  proposalStore.loadProposals();
  toast.info("Atualizando solicitações...");
};

watch(currentTab, () => {
  currentPage.value = 1;
});

watch(categoryOptions, (options) => {
  if (
    categoryFilter.value !== ALL_CATEGORIES_VALUE &&
    !options.some((option) => option.value === categoryFilter.value)
  ) {
    categoryFilter.value = ALL_CATEGORIES_VALUE;
    subcategoryFilter.value = ALL_SUBCATEGORIES_VALUE;
  }
});

watch(subcategoryOptions, (options) => {
  if (
    subcategoryFilter.value !== ALL_SUBCATEGORIES_VALUE &&
    !options.some((option) => option.value === subcategoryFilter.value)
  ) {
    subcategoryFilter.value = ALL_SUBCATEGORIES_VALUE;
  }
});

watch(totalPages, (pages) => {
  if (pages === 0) {
    currentPage.value = 1;
    return;
  }

  if (currentPage.value > pages) {
    currentPage.value = pages;
  }
});
</script>

<template>
  <div class="max-w-7xl mx-auto space-y-6">
    <!-- Header -->
    <div class="flex flex-wrap items-start justify-between gap-3">
      <div>
        <h1 class="text-2xl font-bold text-[#263238] mb-2">
          Solicitações Recebidas
        </h1>
        <p class="text-[#435574]">
          Gerencie as propostas enviadas pelos clientes
        </p>
      </div>
    </div>

    <!-- Stats Cards -->
    <div class="grid grid-cols-1 md:grid-cols-5 gap-4">
      <Card
        v-for="statusCard in proposalStatusCards"
        :key="statusCard.key"
        class="p-4 cursor-pointer hover:shadow-md transition-shadow"
        @click="currentTab = statusCard.key"
      >
        <div class="flex items-center gap-3">
          <div
            :class="[
              'w-10 h-10 rounded-full flex items-center justify-center',
              statusCard.cardIconContainerClass,
            ]"
          >
            <component
              :is="statusCard.icon"
              :class="['h-5 w-5', statusCard.cardIconClass]"
            />
          </div>
          <div>
            <p class="text-sm text-[#435574]">{{ statusCard.cardLabel }}</p>
            <p class="text-xl font-bold text-[#263238]">{{ statusCard.count }}</p>
          </div>
        </div>
      </Card>

      <Card
        v-if="false"
        class="p-4 cursor-pointer hover:shadow-md transition-shadow"
        @click="currentTab = 'completed'"
      >
        <div class="flex items-center gap-3">
          <div
            class="w-10 h-10 rounded-full bg-green-100 flex items-center justify-center"
          >
            <CheckCircle class="h-5 w-5 text-green-600" />
          </div>
          <div>
            <p class="text-sm text-[#435574]">Concluídas</p>
            <p class="text-xl font-bold text-[#263238]">
              {{ proposalStore.stats.completed }}
            </p>
          </div>
        </div>
      </Card>
    </div>

    <!-- Filters -->
    <Card class="p-4">
      <div class="grid grid-cols-2 md:grid-cols-5 gap-4">
        <div class="relative col-span-2 md:col-span-1">
          <Search
            class="absolute left-3 top-1/2 -translate-y-1/2 h-5 w-5 text-[#435574]"
          />
          <Input
            placeholder="Buscar solicitações..."
            :model-value="searchQuery"
            @update:model-value="handleSearchChange"
            class="pl-10"
          />
        </div>

        <Select
          :model-value="categoryFilter"
          @update:model-value="handleCategoryFilterChange"
        >
          <SelectTrigger>
            <SelectValue
              :label="selectedCategoryLabel"
              placeholder="Todas as categorias"
            />
          </SelectTrigger>
          <SelectContent>
            <SelectItem :value="ALL_CATEGORIES_VALUE"
              >Todas as categorias</SelectItem
            >
            <SelectItem
              v-for="category in categoryOptions"
              :key="category.value"
              :value="category.value"
            >
              {{ category.label }}
            </SelectItem>
          </SelectContent>
        </Select>

        <Select
          :model-value="subcategoryFilter"
          @update:model-value="handleSubcategoryFilterChange"
        >
          <SelectTrigger>
            <SelectValue
              :label="selectedSubcategoryLabel"
              placeholder="Todas as subcategorias"
            />
          </SelectTrigger>
          <SelectContent>
            <SelectItem :value="ALL_SUBCATEGORIES_VALUE"
              >Todas as subcategorias</SelectItem
            >
            <SelectItem
              v-for="subcategory in subcategoryOptions"
              :key="subcategory.value"
              :value="subcategory.value"
            >
              {{ subcategory.label }}
            </SelectItem>
          </SelectContent>
        </Select>

        <Select
          :model-value="timeFilter"
          @update:model-value="handleTimeFilterChange"
        >
          <SelectTrigger>
            <SelectValue placeholder="Últimos 30 dias" />
          </SelectTrigger>
          <SelectContent>
            <SelectItem value="Hoje">Hoje</SelectItem>
            <SelectItem value="Ontem">Ontem</SelectItem>
            <SelectItem value="Últimos 7 dias">Últimos 7 dias</SelectItem>
            <SelectItem value="Últimos 30 dias">Últimos 30 dias</SelectItem>
            <SelectItem value="Mês atual">Mês atual</SelectItem>
            <SelectItem value="Último mês">Último mês</SelectItem>
            <SelectItem value="Intervalo personalizado"
              >Intervalo personalizado</SelectItem
            >
          </SelectContent>
        </Select>

        <Select :model-value="sortBy" @update:model-value="handleSortChange">
          <SelectTrigger>
            <SelectValue placeholder="Mais recentes" />
          </SelectTrigger>
          <SelectContent>
            <SelectItem value="Mais recentes">Mais recentes</SelectItem>
            <SelectItem value="Maior orçamento">Maior orçamento</SelectItem>
            <SelectItem value="Menor orçamento">Menor orçamento</SelectItem>
          </SelectContent>
        </Select>
      </div>

      <!-- Custom Date Range -->
      <div
        v-if="timeFilter === 'Intervalo personalizado'"
        class="grid grid-cols-1 md:grid-cols-2 gap-4 mt-4 pt-4 border-t"
      >
        <div class="space-y-1">
          <label class="text-sm font-medium text-[#435574]">De</label>
          <Input
            type="date"
            :model-value="customDateFrom"
            @update:model-value="
              (val: string) => {
                customDateFrom = val;
                currentPage = 1;
              }
            "
          />
        </div>
        <div class="space-y-1">
          <label class="text-sm font-medium text-[#435574]">Até</label>
          <Input
            type="date"
            :model-value="customDateTo"
            @update:model-value="
              (val: string) => {
                customDateTo = val;
                currentPage = 1;
              }
            "
          />
        </div>
      </div>
    </Card>

    <!-- Tabs -->
    <Tabs
      :model-value="currentTab"
      @update:model-value="(val: string) => (currentTab = val)"
    >
      <div class="overflow-x-auto -mx-1 px-1">
        <TabsList class="flex w-max min-w-full">
          <TabsTrigger value="all" class="flex-shrink-0 text-xs sm:text-sm"
            >Todas ({{ proposalStore.proposals.length }})</TabsTrigger
          >
          <TabsTrigger value="pending" class="flex-shrink-0 text-xs sm:text-sm">
            Pendentes ({{ proposalStore.stats.pending }})
          </TabsTrigger>
          <TabsTrigger
            value="negotiating"
            class="flex-shrink-0 text-xs sm:text-sm"
            >Negociando ({{ proposalStore.stats.negotiating }})</TabsTrigger
          >
          <TabsTrigger
            value="in_progress"
            class="flex-shrink-0 text-xs sm:text-sm"
            >Em Andamento ({{ proposalStore.stats.inProgress }})</TabsTrigger
          >
          <TabsTrigger value="rejected" class="flex-shrink-0 text-xs sm:text-sm"
            >Recusadas ({{ proposalStore.stats.rejected }})</TabsTrigger
          >
          <TabsTrigger
            value="completed"
            class="flex-shrink-0 text-xs sm:text-sm"
            >Concluídas ({{ proposalStore.stats.completed }})</TabsTrigger
          >
        </TabsList>
      </div>

      <TabsContent :value="currentTab" class="mt-6">
        <!-- Loading State -->
        <div v-if="proposalStore.isLoading" class="flex justify-center py-12">
          <div
            class="animate-spin h-8 w-8 border-4 border-[#286AD8] border-t-transparent rounded-full"
          ></div>
        </div>

        <!-- Empty State -->
        <Card
          v-else-if="currentProposals.length === 0"
          class="p-12 text-center"
        >
          <FileText class="h-12 w-12 text-gray-400 mx-auto mb-4" />
          <h3 class="text-lg font-semibold text-[#263238] mb-2">
            {{
              hasActiveFilters
                ? "Nenhuma solicitação encontrada"
                : getEmptyMessage()
            }}
          </h3>
          <p class="text-[#435574]">
            {{
              hasActiveFilters
                ? "Tente ajustar os filtros de busca"
                : "Você receberá notificações quando novos clientes enviarem solicitações"
            }}
          </p>
        </Card>

        <template v-else>
          <!-- Proposals List -->
          <div class="grid grid-cols-1 gap-4">
            <Card
              v-for="proposal in currentProposals"
              :key="proposal.id"
              :class="`p-6 hover:shadow-md transition-all ${
                proposal.status === 'pending'
                  ? 'border-l-4 border-l-blue-500 bg-blue-50/30'
                  : !proposal.viewed
                    ? 'border-l-4 border-l-[#286AD8]'
                    : ''
              }`"
            >
              <div class="flex items-start gap-3">
                <Avatar
                  class="h-10 w-10 sm:h-12 sm:w-12 cursor-pointer flex-shrink-0"
                  @click="handleViewDetails(proposal)"
                >
                  <AvatarImage
                    :src="proposal.clientAvatar"
                    :alt="proposal.clientName"
                  />
                  <AvatarFallback class="bg-[#286AD8] text-white">
                    {{ getInitials(proposal.clientName) }}
                  </AvatarFallback>
                </Avatar>

                <div class="flex-1 min-w-0">
                  <div class="flex items-start justify-between gap-2 mb-2">
                    <div
                      class="flex-1 min-w-0 cursor-pointer"
                      @click="handleViewDetails(proposal)"
                    >
                      <div class="flex flex-wrap items-center gap-2 mb-1">
                        <h3
                          class="font-semibold text-[#263238] hover:text-[#286AD8] truncate"
                        >
                          {{ proposal.service }}
                        </h3>
                        <Badge
                          v-if="!proposal.viewed"
                          class="bg-primary hover:bg-primary text-xs flex-shrink-0"
                        >
                          Novo
                        </Badge>
                      </div>
                      <div
                        class="flex flex-wrap items-center gap-x-3 gap-y-1 text-sm text-[#435574]"
                      >
                        <span class="flex items-center gap-1">
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
                        <template
                          v-for="statusBadge in [getProposalStatusBadge(proposal)]"
                          :key="`${proposal.id}-status`"
                        >
                          <Badge :class="statusBadge.badgeClass">
                            <component
                              :is="statusBadge.icon"
                              class="h-3 w-3 mr-1"
                            />
                            {{ statusBadge.label }}
                          </Badge>
                        </template>
                        <template v-if="false">
                          <Badge
                            class="bg-green-100 text-green-700 hover:bg-green-100"
                          >
                            <CheckCircle class="h-3 w-3 mr-1" />
                            Concluída
                          </Badge>
                        </template>

                        <WorkModelBadge
                          v-if="proposal.workModel"
                          :work-model="proposal.workModel"
                        />
                      </div>
                    </div>
                  </div>

                  <p class="text-sm text-[#435574] mb-3 line-clamp-2">
                    {{ proposal.description }}
                  </p>

                  <div
                    v-if="isClientCancelled(proposal)"
                    class="mb-3 rounded-lg border border-gray-200 bg-gray-50 p-3"
                  >
                    <p class="text-sm font-semibold text-[#263238]">
                      Cancelada pelo cliente
                    </p>
                    <p class="text-sm text-[#435574] italic">
                      "{{ proposal.cancelReason }}"
                    </p>
                  </div>

                  <div
                    class="flex flex-wrap items-center justify-between gap-2"
                  >
                    <div
                      class="flex flex-wrap items-center gap-x-4 gap-y-1 text-sm"
                    >
                      <span
                        class="flex items-center gap-1 font-medium text-[#263238]"
                      >
                        <DollarSign class="h-4 w-4 text-green-600" />
                        {{
                          formatCurrency(
                            ["accepted", "in_progress", "completed"].includes(
                              proposal.status,
                            ) && proposal.freelancerResponse
                              ? proposal.freelancerResponse.value
                              : proposal.budget,
                          )
                        }}
                      </span>
                      <span class="flex items-center gap-1 text-[#435574]">
                        <Clock class="h-4 w-4" />
                        Prazo:
                        {{
                          ["accepted", "in_progress", "completed"].includes(
                            proposal.status,
                          ) && proposal.freelancerResponse
                            ? proposal.freelancerResponse.executionDays +
                              " dias"
                            : proposal.deadline
                        }}
                      </span>
                      <span
                        v-if="
                          proposal.attachments &&
                          proposal.attachments.length > 0
                        "
                        class="flex items-center gap-1 text-[#435574]"
                      >
                        <FileText class="h-4 w-4" />
                        {{ proposal.attachments.length }} anexo{{
                          proposal.attachments.length > 1 ? "s" : ""
                        }}
                      </span>
                    </div>

                    <Button
                      @click="handleViewDetails(proposal)"
                      variant="outline"
                      size="sm"
                      class="flex-shrink-0"
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
          <div
            v-if="totalPages > 1"
            class="flex items-center justify-center gap-2 mt-6"
          >
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
                v-for="pageNum in Array.from(
                  { length: Math.min(5, totalPages) },
                  (_, i) => {
                    if (totalPages <= 5) return i + 1;
                    if (currentPage <= 3) return i + 1;
                    if (currentPage >= totalPages - 2)
                      return totalPages - 4 + i;
                    return currentPage - 2 + i;
                  },
                )"
                :key="pageNum"
                @click="currentPage = pageNum"
                :variant="currentPage === pageNum ? 'default' : 'outline'"
                size="sm"
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
    <Dialog
      :open="showRejectDialog"
      @update:open="(val) => (showRejectDialog = val)"
    >
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
              @update:model-value="(val) => (rejectReason = val as string)"
              placeholder="Ex: Não trabalho com esse tipo de serviço, agenda indisponível, etc."
              :rows="4"
            />
          </div>
        </div>
        <DialogFooter>
          <Button @click="showRejectDialog = false" variant="outline">
            Cancelar
          </Button>
          <Button @click="handleRejectProposal" variant="destructive">
            Confirmar Recusa
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>

    <!-- Archive Dialog -->
    <AlertDialog
      :open="showArchiveDialog"
      @update:open="(val) => (showArchiveDialog = val)"
    >
      <AlertDialogContent>
        <AlertDialogHeader>
          <AlertDialogTitle>Arquivar Solicitação</AlertDialogTitle>
          <AlertDialogDescription>
            Tem certeza que deseja arquivar a solicitação "{{
              selectedProposal?.service
            }}"? Ela será movida para a aba de concluídas/arquivadas.
          </AlertDialogDescription>
        </AlertDialogHeader>
        <AlertDialogFooter>
          <AlertDialogCancel @click="showArchiveDialog = false"
            >Cancelar</AlertDialogCancel
          >
          <AlertDialogAction @click="handleArchiveProposal">
            Arquivar
          </AlertDialogAction>
        </AlertDialogFooter>
      </AlertDialogContent>
    </AlertDialog>
  </div>
</template>
