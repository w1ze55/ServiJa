<script setup lang="ts">
import { ref, computed } from "vue";
import {
  X,
  Star,
  Lock,
  Unlock,
  Edit2,
  KeyRound,
  CheckCircle,
  XCircle,
  Trash2,
  AlertTriangle,
  Phone,
  Mail,
  CreditCard,
  Calendar,
  Briefcase,
  ShieldCheck,
  ShieldX,
  Package,
} from "lucide-vue-next";
import {
  adminUserService,
  type AdminUserDetail,
} from "@/api/services/adminUserService";
import { toast } from "@/utils/toast";
import EditUserModal from "./EditUserModal.vue";
import DeleteUserModal from "./DeleteUserModal.vue";
const props = defineProps<{
  user: AdminUserDetail | null;
  open: boolean;
}>();

const emit = defineEmits<{
  (e: "close"): void;
  (e: "refresh"): void;
}>();

// Dialog states
const showBlockConfirm = ref(false);
const showResetPasswordConfirm = ref(false);
const showKycConfirm = ref(false);
const showEditModal = ref(false);
const showDeleteModal = ref(false);

const isSubmitting = ref(false);

const currentLocalUser = computed(() => {
  try {
    return JSON.parse(localStorage.getItem("user") || "null");
  } catch {
    return null;
  }
});

const isOwnAccount = computed(
  () =>
    props.user &&
    currentLocalUser.value &&
    String(props.user.id) === String(currentLocalUser.value.id),
);

// Helpers
const getInitials = (name: string) =>
  name
    .split(" ")
    .slice(0, 2)
    .map((n) => n[0])
    .join("")
    .toUpperCase();

const avatarColors = [
  "bg-blue-500",
  "bg-purple-500",
  "bg-emerald-500",
  "bg-orange-500",
  "bg-rose-500",
  "bg-cyan-500",
];
const getAvatarColor = (id: number) => avatarColors[id % avatarColors.length];

const userTypeBadge = (type: string) => {
  switch (type) {
    case "FREELANCER":
      return { label: "Freelancer", cls: "bg-blue-50 text-blue-700" };
    case "CONTRACTOR":
      return { label: "Contratante", cls: "bg-purple-50 text-purple-700" };
    case "ADMIN":
      return { label: "Admin", cls: "bg-red-50 text-red-700" };
    default:
      return { label: type, cls: "bg-slate-100 text-slate-600" };
  }
};

const formatDate = (dateStr: string) =>
  new Date(dateStr).toLocaleDateString("pt-BR", {
    day: "2-digit",
    month: "2-digit",
    year: "numeric",
  });

const formatPhone = (phone: string | null) => {
  if (!phone) return "—";
  const d = phone.replace(/\D/g, "");
  if (d.length === 11)
    return `(${d.slice(0, 2)}) ${d.slice(2, 7)}-${d.slice(7)}`;
  if (d.length === 10)
    return `(${d.slice(0, 2)}) ${d.slice(2, 6)}-${d.slice(6)}`;
  return phone;
};

const formatCpf = (cpf: string | null) => {
  if (!cpf) return "—";
  const d = cpf.replace(/\D/g, "");
  if (d.length === 11)
    return `${d.slice(0, 3)}.${d.slice(3, 6)}.${d.slice(6, 9)}-${d.slice(9)}`;
  return cpf;
};

// --- Actions ---

const handleBlockToggle = async () => {
  if (!props.user) return;
  isSubmitting.value = true;
  try {
    await adminUserService.updateUserStatus(props.user.id, !props.user.active);
    toast.success(
      props.user.active
        ? "Usuário bloqueado com sucesso."
        : "Usuário desbloqueado com sucesso.",
    );
    showBlockConfirm.value = false;
    emit("refresh");
  } catch {
    toast.error("Erro ao atualizar status do usuário.");
  } finally {
    isSubmitting.value = false;
  }
};

const handleResetPassword = async () => {
  if (!props.user) return;
  isSubmitting.value = true;
  try {
    await adminUserService.resetPassword(props.user.id);
    toast.success(`Link de redefinição enviado para ${props.user.email}`);
    showResetPasswordConfirm.value = false;
  } catch {
    toast.error("Erro ao enviar link de redefinição de senha.");
  } finally {
    isSubmitting.value = false;
  }
};

const handleKycToggle = async () => {
  if (!props.user) return;
  isSubmitting.value = true;
  const approve = !props.user.kycCompleted;
  try {
    await adminUserService.updateKyc(props.user.id, approve);
    toast.success(
      approve ? "KYC aprovado com sucesso." : "KYC revogado com sucesso.",
    );
    showKycConfirm.value = false;
    emit("refresh");
  } catch {
    toast.error("Erro ao atualizar status de KYC.");
  } finally {
    isSubmitting.value = false;
  }
};
</script>

<template>
  <!-- Overlay -->
  <Transition name="overlay">
    <div
      v-if="open"
      class="fixed inset-0 z-40 bg-black/30 backdrop-blur-sm"
      @click="emit('close')"
    />
  </Transition>

  <!-- Drawer -->
  <Transition name="drawer">
    <div
      v-if="open"
      class="fixed right-0 top-0 z-50 flex h-full w-full max-w-lg flex-col overflow-hidden bg-white shadow-2xl"
    >
      <!-- Header -->
      <div
        class="flex shrink-0 items-start justify-between border-b border-slate-200 px-6 py-5"
      >
        <div class="flex items-center gap-4">
          <div
            v-if="user?.avatarUrl"
            class="h-12 w-12 overflow-hidden rounded-full"
          >
            <img
              :src="user.avatarUrl"
              :alt="user.name"
              class="h-full w-full object-cover"
            />
          </div>
          <div
            v-else-if="user"
            :class="[
              'flex h-12 w-12 items-center justify-center rounded-full text-sm font-bold text-white',
              getAvatarColor(user.id),
            ]"
          >
            {{ getInitials(user.name) }}
          </div>
          <div v-if="user">
            <div class="flex items-center gap-2">
              <p class="text-base font-semibold text-slate-900">
                {{ user.name }}
              </p>
              <span
                v-if="isOwnAccount"
                class="rounded-full bg-amber-100 px-2 py-0.5 text-xs font-medium text-amber-700"
                >Você</span
              >
            </div>
            <p class="text-sm text-slate-500">{{ user.email }}</p>
            <div class="mt-1.5 flex items-center gap-2">
              <span
                :class="[
                  'inline-flex items-center rounded-full px-2 py-0.5 text-xs font-medium',
                  userTypeBadge(user.userType).cls,
                ]"
              >
                {{ userTypeBadge(user.userType).label }}
              </span>
              <span
                v-if="user.deletedAt"
                class="inline-flex items-center rounded-full border border-red-200 bg-red-50 px-2 py-0.5 text-xs font-medium text-red-700"
              >
                Deletado
              </span>
              <span
                v-else-if="user.active"
                class="inline-flex items-center rounded-full bg-emerald-50 px-2 py-0.5 text-xs font-medium text-emerald-700"
              >
                Ativo
              </span>
              <span
                v-else
                class="inline-flex items-center rounded-full bg-slate-100 px-2 py-0.5 text-xs font-medium text-slate-600"
              >
                Bloqueado
              </span>
            </div>
          </div>
          <div v-else class="h-12 w-72 animate-pulse rounded-lg bg-slate-200" />
        </div>
        <button
          class="rounded-lg p-1 text-slate-400 transition hover:bg-slate-100 hover:text-slate-600"
          @click="emit('close')"
        >
          <X class="h-5 w-5" />
        </button>
      </div>

      <!-- Loading state -->
      <div
        v-if="!user"
        class="flex flex-1 flex-col gap-4 overflow-y-auto px-6 py-6"
      >
        <div
          v-for="i in 6"
          :key="i"
          class="h-8 w-full animate-pulse rounded-lg bg-slate-100"
        />
      </div>

      <!-- Content -->
      <div v-else class="flex-1 overflow-y-auto px-6 py-6">
        <div class="space-y-6">
          <!-- Dados básicos -->
          <section>
            <h3
              class="mb-3 text-xs font-semibold uppercase tracking-wide text-slate-400"
            >
              Dados básicos
            </h3>
            <dl class="space-y-2.5">
              <div class="flex items-center gap-3 text-sm">
                <Phone class="h-4 w-4 shrink-0 text-slate-400" />
                <span class="text-slate-500">Telefone:</span>
                <span class="text-slate-900">{{
                  formatPhone(user.phone)
                }}</span>
              </div>
              <div class="flex items-center gap-3 text-sm">
                <CreditCard class="h-4 w-4 shrink-0 text-slate-400" />
                <span class="text-slate-500">CPF:</span>
                <span class="text-slate-900">{{ formatCpf(user.cpf) }}</span>
              </div>
              <div class="flex items-center gap-3 text-sm">
                <Mail class="h-4 w-4 shrink-0 text-slate-400" />
                <span class="text-slate-500">Email verificado:</span>
                <span
                  :class="
                    user.emailVerified ? 'text-emerald-600' : 'text-slate-500'
                  "
                >
                  {{ user.emailVerified ? "Sim" : "Não" }}
                </span>
              </div>
              <div class="flex items-center gap-3 text-sm">
                <Calendar class="h-4 w-4 shrink-0 text-slate-400" />
                <span class="text-slate-500">Membro desde:</span>
                <span class="text-slate-900">{{
                  formatDate(user.createdAt)
                }}</span>
              </div>
            </dl>
          </section>

          <!-- Dados do freelancer -->
          <section v-if="user.userType === 'FREELANCER'">
            <h3
              class="mb-3 text-xs font-semibold uppercase tracking-wide text-slate-400"
            >
              Dados do perfil
            </h3>
            <dl class="space-y-2.5 text-sm">
              <div v-if="user.professionalTitle">
                <span class="text-slate-500">Título:</span>
                <span class="ml-2 text-slate-900">{{
                  user.professionalTitle
                }}</span>
              </div>
              <div v-if="user.category">
                <span class="text-slate-500">Categoria:</span>
                <span class="ml-2 text-slate-900">{{ user.category }}</span>
              </div>
              <div v-if="user.bio" class="rounded-lg bg-slate-50 p-3">
                <p class="mb-1 text-xs text-slate-400">Bio</p>
                <p class="text-slate-700">{{ user.bio }}</p>
              </div>
              <div class="flex items-center gap-2">
                <span class="text-slate-500">Avaliação:</span>
                <div class="flex items-center gap-1">
                  <Star class="h-3.5 w-3.5 fill-amber-400 text-amber-400" />
                  <span class="font-medium text-slate-900">{{
                    user.rating?.toFixed(1) ?? "—"
                  }}</span>
                  <span class="text-slate-400"
                    >({{ user.totalReviews ?? 0 }} avaliações)</span
                  >
                </div>
              </div>
              <div class="grid grid-cols-2 gap-2">
                <div class="rounded-lg bg-slate-50 p-2 text-center">
                  <p class="text-lg font-bold text-slate-900">
                    {{ user.completedJobs ?? 0 }}
                  </p>
                  <p class="text-xs text-slate-500">Trabalhos concluídos</p>
                </div>
                <div class="rounded-lg bg-slate-50 p-2 text-center">
                  <p class="text-lg font-bold text-slate-900">
                    {{ user.activeJobs ?? 0 }}
                  </p>
                  <p class="text-xs text-slate-500">Trabalhos ativos</p>
                </div>
              </div>
              <div class="flex items-center gap-3">
                <span class="text-slate-500">KYC:</span>
                <span
                  v-if="user.kycCompleted"
                  class="inline-flex items-center gap-1 rounded-full bg-emerald-50 px-2.5 py-0.5 text-xs font-medium text-emerald-700"
                >
                  <CheckCircle class="h-3 w-3" /> Aprovado
                </span>
                <span
                  v-else
                  class="inline-flex items-center gap-1 rounded-full bg-amber-50 px-2.5 py-0.5 text-xs font-medium text-amber-700"
                >
                  <AlertTriangle class="h-3 w-3" /> Pendente
                </span>
              </div>
              <div class="flex items-center gap-3">
                <span class="text-slate-500">Perfil completo:</span>
                <span
                  :class="
                    user.profileCompleted
                      ? 'text-emerald-600'
                      : 'text-slate-500'
                  "
                >
                  {{ user.profileCompleted ? "Sim" : "Não" }}
                </span>
              </div>
            </dl>
          </section>

          <!-- Dados do contratante -->
          <section v-if="user.userType === 'CONTRACTOR'">
            <h3
              class="mb-3 text-xs font-semibold uppercase tracking-wide text-slate-400"
            >
              Dados do contratante
            </h3>
            <div class="grid grid-cols-3 gap-2 text-sm">
              <div class="rounded-lg bg-slate-50 p-2 text-center">
                <p class="text-lg font-bold text-slate-900">
                  {{ user.totalContracts ?? 0 }}
                </p>
                <p class="text-xs text-slate-500">Total</p>
              </div>
              <div class="rounded-lg bg-slate-50 p-2 text-center">
                <p class="text-lg font-bold text-blue-600">
                  {{ user.activeContracts ?? 0 }}
                </p>
                <p class="text-xs text-slate-500">Ativos</p>
              </div>
              <div class="rounded-lg bg-slate-50 p-2 text-center">
                <p class="text-lg font-bold text-emerald-600">
                  {{ user.completedContracts ?? 0 }}
                </p>
                <p class="text-xs text-slate-500">Concluídos</p>
              </div>
            </div>
            <div
              v-if="user.contractorTotalReviews"
              class="mt-2 flex items-center gap-2 text-sm"
            >
              <span class="text-slate-500">Avaliação média:</span>
              <div class="flex items-center gap-1">
                <Star class="h-3.5 w-3.5 fill-amber-400 text-amber-400" />
                <span class="font-medium text-slate-900">
                  {{ user.contractorRating?.toFixed(1) ?? "—" }}
                </span>
                <span class="text-slate-400"
                  >({{ user.contractorTotalReviews }} avaliações)</span
                >
              </div>
            </div>
          </section>

          <!-- Serviços (freelancer) -->
          <section
            v-if="
              user.userType === 'FREELANCER' &&
              user.services &&
              user.services.length > 0
            "
          >
            <h3
              class="mb-3 text-xs font-semibold uppercase tracking-wide text-slate-400"
            >
              <div class="flex items-center gap-1.5">
                <Package class="h-3.5 w-3.5" />
                Serviços ({{ user.services.length }})
              </div>
            </h3>
            <div class="space-y-2">
              <div
                v-for="svc in user.services"
                :key="svc.id"
                class="flex items-center justify-between rounded-lg bg-slate-50 px-3 py-2 text-sm"
              >
                <div class="min-w-0 flex-1">
                  <p class="truncate font-medium text-slate-800">
                    {{ svc.title }}
                  </p>
                  <p class="text-xs text-slate-500">{{ svc.category }}</p>
                </div>
                <div class="ml-3 flex shrink-0 flex-col items-end gap-1">
                  <span class="font-semibold text-slate-900">
                    R$ {{ Number(svc.price).toFixed(2).replace(".", ",") }}
                  </span>
                  <span
                    :class="
                      svc.isActive
                        ? 'bg-emerald-50 text-emerald-700'
                        : 'bg-slate-100 text-slate-500'
                    "
                    class="rounded-full px-2 py-0.5 text-[10px] font-medium"
                  >
                    {{ svc.isActive ? "Ativo" : "Inativo" }}
                  </span>
                </div>
              </div>
            </div>
          </section>

          <!-- Serviços (freelancer sem serviços) -->
          <section v-else-if="user.userType === 'FREELANCER' && user.services">
            <h3
              class="mb-2 flex items-center gap-1.5 text-xs font-semibold uppercase tracking-wide text-slate-400"
            >
              <Package class="h-3.5 w-3.5" />
              Serviços
            </h3>
            <p class="text-sm text-slate-400">Nenhum serviço cadastrado.</p>
          </section>

          <!-- Ações -->
          <section v-if="!user.deletedAt">
            <h3
              class="mb-3 text-xs font-semibold uppercase tracking-wide text-slate-400"
            >
              Ações
            </h3>
            <div class="grid grid-cols-2 gap-2">
              <!-- Edit -->
              <button
                class="flex items-center justify-center gap-2 rounded-lg border border-slate-200 px-3 py-2 text-sm font-medium text-slate-700 transition hover:bg-slate-50"
                @click="showEditModal = true"
              >
                <Edit2 class="h-4 w-4" />
                Editar
              </button>

              <!-- Reset Password -->
              <button
                class="flex items-center justify-center gap-2 rounded-lg border border-slate-200 px-3 py-2 text-sm font-medium text-slate-700 transition hover:bg-slate-50"
                @click="showResetPasswordConfirm = true"
              >
                <KeyRound class="h-4 w-4" />
                Resetar Senha
              </button>

              <!-- Block/Unblock -->
              <button
                class="flex items-center justify-center gap-2 rounded-lg border px-3 py-2 text-sm font-medium transition"
                :class="
                  user.active
                    ? 'border-slate-200 text-slate-700 hover:bg-slate-50'
                    : 'border-emerald-200 bg-emerald-50 text-emerald-700 hover:bg-emerald-100'
                "
                @click="showBlockConfirm = true"
              >
                <Lock v-if="user.active" class="h-4 w-4" />
                <Unlock v-else class="h-4 w-4" />
                {{ user.active ? "Bloquear" : "Desbloquear" }}
              </button>

              <!-- KYC (FREELANCER only) -->
              <button
                v-if="user.userType === 'FREELANCER'"
                class="flex items-center justify-center gap-2 rounded-lg border px-3 py-2 text-sm font-medium transition"
                :class="
                  user.kycCompleted
                    ? 'border-amber-200 bg-amber-50 text-amber-700 hover:bg-amber-100'
                    : 'border-emerald-200 bg-emerald-50 text-emerald-700 hover:bg-emerald-100'
                "
                @click="showKycConfirm = true"
              >
                <ShieldCheck v-if="!user.kycCompleted" class="h-4 w-4" />
                <ShieldX v-else class="h-4 w-4" />
                {{ user.kycCompleted ? "Revogar KYC" : "Aprovar KYC" }}
              </button>

              <!-- Delete -->
              <button
                class="flex items-center justify-center gap-2 rounded-lg border border-red-200 bg-red-50 px-3 py-2 text-sm font-medium text-red-700 transition hover:bg-red-100"
                @click="showDeleteModal = true"
              >
                <Trash2 class="h-4 w-4" />
                Excluir
              </button>
            </div>
          </section>
        </div>
      </div>
    </div>
  </Transition>

  <!-- Block/Unblock Confirmation -->
  <Transition name="modal">
    <div
      v-if="showBlockConfirm"
      class="fixed inset-0 z-[60] flex items-center justify-center p-4"
    >
      <div
        class="absolute inset-0 bg-black/40"
        @click="showBlockConfirm = false"
      />
      <div class="relative w-full max-w-sm rounded-2xl bg-white p-6 shadow-xl">
        <div class="mb-4 flex items-center gap-3">
          <div
            class="flex h-10 w-10 items-center justify-center rounded-full bg-amber-100"
          >
            <AlertTriangle class="h-5 w-5 text-amber-600" />
          </div>
          <h3 class="text-base font-semibold text-slate-900">
            {{ user?.active ? "Bloquear usuário" : "Desbloquear usuário" }}
          </h3>
        </div>
        <p class="mb-6 text-sm text-slate-600">
          {{
            user?.active
              ? `Tem certeza que deseja bloquear o acesso de "${user?.name}"? O usuário não poderá acessar a plataforma.`
              : `Tem certeza que deseja desbloquear o acesso de "${user?.name}"?`
          }}
        </p>
        <div class="flex gap-3">
          <button
            class="flex-1 rounded-lg border border-slate-200 px-4 py-2 text-sm font-medium text-slate-700 transition hover:bg-slate-50"
            :disabled="isSubmitting"
            @click="showBlockConfirm = false"
          >
            Cancelar
          </button>
          <button
            class="flex-1 rounded-lg px-4 py-2 text-sm font-medium text-white transition disabled:opacity-60"
            :class="
              user?.active
                ? 'bg-amber-500 hover:bg-amber-600'
                : 'bg-emerald-500 hover:bg-emerald-600'
            "
            :disabled="isSubmitting"
            @click="handleBlockToggle"
          >
            {{
              isSubmitting
                ? "Aguarde..."
                : user?.active
                  ? "Bloquear"
                  : "Desbloquear"
            }}
          </button>
        </div>
      </div>
    </div>
  </Transition>

  <!-- Reset Password Confirmation -->
  <Transition name="modal">
    <div
      v-if="showResetPasswordConfirm"
      class="fixed inset-0 z-[60] flex items-center justify-center p-4"
    >
      <div
        class="absolute inset-0 bg-black/40"
        @click="showResetPasswordConfirm = false"
      />
      <div class="relative w-full max-w-sm rounded-2xl bg-white p-6 shadow-xl">
        <div class="mb-4 flex items-center gap-3">
          <div
            class="flex h-10 w-10 items-center justify-center rounded-full bg-blue-100"
          >
            <KeyRound class="h-5 w-5 text-blue-600" />
          </div>
          <h3 class="text-base font-semibold text-slate-900">Resetar senha</h3>
        </div>
        <p class="mb-6 text-sm text-slate-600">
          Deseja enviar um link de redefinição de senha para
          <strong>{{ user?.email }}</strong
          >?
        </p>
        <div class="flex gap-3">
          <button
            class="flex-1 rounded-lg border border-slate-200 px-4 py-2 text-sm font-medium text-slate-700 transition hover:bg-slate-50"
            :disabled="isSubmitting"
            @click="showResetPasswordConfirm = false"
          >
            Cancelar
          </button>
          <button
            class="flex-1 rounded-lg bg-blue-600 px-4 py-2 text-sm font-medium text-white transition hover:bg-blue-700 disabled:opacity-60"
            :disabled="isSubmitting"
            @click="handleResetPassword"
          >
            {{ isSubmitting ? "Enviando..." : "Enviar link" }}
          </button>
        </div>
      </div>
    </div>
  </Transition>

  <!-- KYC Confirmation -->
  <Transition name="modal">
    <div
      v-if="showKycConfirm"
      class="fixed inset-0 z-[60] flex items-center justify-center p-4"
    >
      <div
        class="absolute inset-0 bg-black/40"
        @click="showKycConfirm = false"
      />
      <div class="relative w-full max-w-sm rounded-2xl bg-white p-6 shadow-xl">
        <div class="mb-4 flex items-center gap-3">
          <div
            :class="[
              'flex h-10 w-10 items-center justify-center rounded-full',
              user?.kycCompleted ? 'bg-amber-100' : 'bg-emerald-100',
            ]"
          >
            <ShieldCheck
              v-if="!user?.kycCompleted"
              class="h-5 w-5 text-emerald-600"
            />
            <ShieldX v-else class="h-5 w-5 text-amber-600" />
          </div>
          <h3 class="text-base font-semibold text-slate-900">
            {{ user?.kycCompleted ? "Revogar KYC" : "Aprovar KYC" }}
          </h3>
        </div>
        <p class="mb-6 text-sm text-slate-600">
          {{
            user?.kycCompleted
              ? `Tem certeza que deseja revogar o KYC de "${user?.name}"?`
              : `Tem certeza que deseja aprovar o KYC de "${user?.name}"?`
          }}
        </p>
        <div class="flex gap-3">
          <button
            class="flex-1 rounded-lg border border-slate-200 px-4 py-2 text-sm font-medium text-slate-700 transition hover:bg-slate-50"
            :disabled="isSubmitting"
            @click="showKycConfirm = false"
          >
            Cancelar
          </button>
          <button
            class="flex-1 rounded-lg px-4 py-2 text-sm font-medium text-white transition disabled:opacity-60"
            :class="
              user?.kycCompleted
                ? 'bg-amber-500 hover:bg-amber-600'
                : 'bg-emerald-500 hover:bg-emerald-600'
            "
            :disabled="isSubmitting"
            @click="handleKycToggle"
          >
            {{
              isSubmitting
                ? "Aguarde..."
                : user?.kycCompleted
                  ? "Revogar"
                  : "Aprovar"
            }}
          </button>
        </div>
      </div>
    </div>
  </Transition>

  <!-- Edit Modal -->
  <EditUserModal
    v-if="showEditModal && user"
    :user="user"
    @close="showEditModal = false"
    @refresh="
      () => {
        showEditModal = false;
        emit('refresh');
      }
    "
  />

  <!-- Delete Modal -->
  <DeleteUserModal
    v-if="showDeleteModal && user"
    :user="user"
    @close="showDeleteModal = false"
    @refresh="
      () => {
        showDeleteModal = false;
        emit('close');
        emit('refresh');
      }
    "
  />
</template>

<style scoped>
.overlay-enter-active,
.overlay-leave-active {
  transition: opacity 0.2s ease;
}
.overlay-enter-from,
.overlay-leave-to {
  opacity: 0;
}

.drawer-enter-active,
.drawer-leave-active {
  transition: transform 0.3s ease;
}
.drawer-enter-from,
.drawer-leave-to {
  transform: translateX(100%);
}

.modal-enter-active,
.modal-leave-active {
  transition: opacity 0.15s ease;
}
.modal-enter-from,
.modal-leave-to {
  opacity: 0;
}
</style>
