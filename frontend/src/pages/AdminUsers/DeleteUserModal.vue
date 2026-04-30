<script setup lang="ts">
import { ref, computed } from 'vue';
import { X, AlertTriangle, Trash2, UserX } from 'lucide-vue-next';
import { adminUserService, type AdminUserDetail } from '@/api/services/adminUserService';
import { toast } from '@/utils/toast';

const props = defineProps<{
  user: AdminUserDetail;
}>();

const emit = defineEmits<{
  (e: 'close'): void;
  (e: 'refresh'): void;
}>();

type Step = 'choose' | 'confirm-hard';

const step = ref<Step>('choose');
const selectedAction = ref<'soft' | 'hard' | null>(null);
const confirmEmail = ref('');
const isSubmitting = ref(false);
const emailError = ref('');

const emailMatches = computed(
  () => confirmEmail.value.trim().toLowerCase() === props.user.email.trim().toLowerCase()
);

const proceedToConfirm = (action: 'soft' | 'hard') => {
  selectedAction.value = action;
  if (action === 'hard') {
    step.value = 'confirm-hard';
  } else {
    handleSoftDelete();
  }
};

const handleSoftDelete = async () => {
  isSubmitting.value = true;
  try {
    await adminUserService.deleteUser(props.user.id, false);
    toast.success(`Conta de "${props.user.name}" desativada com sucesso.`);
    emit('refresh');
  } catch {
    toast.error('Erro ao desativar conta.');
  } finally {
    isSubmitting.value = false;
  }
};

const handleHardDelete = async () => {
  emailError.value = '';
  if (!emailMatches.value) {
    emailError.value = 'O email digitado não corresponde ao email do usuário.';
    return;
  }
  isSubmitting.value = true;
  try {
    await adminUserService.deleteUser(props.user.id, true);
    toast.success(`Usuário "${props.user.name}" excluído permanentemente.`);
    emit('refresh');
  } catch {
    toast.error('Erro ao excluir usuário permanentemente.');
  } finally {
    isSubmitting.value = false;
  }
};
</script>

<template>
  <div class="fixed inset-0 z-[70] flex items-center justify-center p-4">
    <div class="absolute inset-0 bg-black/40" @click="emit('close')" />
    <div class="relative w-full max-w-md rounded-2xl bg-white shadow-2xl">
      <!-- Header -->
      <div class="flex items-center justify-between border-b border-slate-200 px-6 py-4">
        <h2 class="text-base font-semibold text-slate-900">Remover usuário</h2>
        <button
          class="rounded-lg p-1 text-slate-400 transition hover:bg-slate-100 hover:text-slate-600"
          @click="emit('close')"
        >
          <X class="h-5 w-5" />
        </button>
      </div>

      <!-- Step 1: Choose action -->
      <div v-if="step === 'choose'" class="p-6">
        <p class="mb-5 text-sm text-slate-600">
          Escolha como deseja remover <strong>{{ user.name }}</strong> da plataforma:
        </p>

        <div class="space-y-3">
          <!-- Soft delete -->
          <button
            class="w-full rounded-xl border-2 border-slate-200 p-4 text-left transition hover:border-orange-300 hover:bg-orange-50 focus:outline-none"
            :disabled="isSubmitting"
            @click="proceedToConfirm('soft')"
          >
            <div class="flex items-start gap-3">
              <div class="flex h-9 w-9 shrink-0 items-center justify-center rounded-full bg-orange-100">
                <UserX class="h-4 w-4 text-orange-600" />
              </div>
              <div>
                <p class="text-sm font-medium text-slate-900">Desativar conta</p>
                <p class="mt-0.5 text-xs text-slate-500">
                  A conta é bloqueada e ocultada, mas os dados são mantidos. Esta ação é reversível.
                </p>
              </div>
            </div>
          </button>

          <!-- Hard delete -->
          <button
            class="w-full rounded-xl border-2 border-slate-200 p-4 text-left transition hover:border-red-300 hover:bg-red-50 focus:outline-none"
            :disabled="isSubmitting"
            @click="proceedToConfirm('hard')"
          >
            <div class="flex items-start gap-3">
              <div class="flex h-9 w-9 shrink-0 items-center justify-center rounded-full bg-red-100">
                <Trash2 class="h-4 w-4 text-red-600" />
              </div>
              <div>
                <p class="text-sm font-medium text-red-700">Excluir permanentemente</p>
                <p class="mt-0.5 text-xs text-slate-500">
                  Todos os dados do usuário são removidos definitivamente. Esta ação é <strong>irreversível</strong>.
                </p>
              </div>
            </div>
          </button>
        </div>

        <button
          class="mt-4 w-full rounded-lg border border-slate-200 px-4 py-2 text-sm font-medium text-slate-700 transition hover:bg-slate-50"
          @click="emit('close')"
        >
          Cancelar
        </button>
      </div>

      <!-- Step 2: Hard delete confirmation -->
      <div v-else-if="step === 'confirm-hard'" class="p-6">
        <div class="mb-4 rounded-lg border border-red-200 bg-red-50 p-3">
          <div class="flex items-start gap-2">
            <AlertTriangle class="mt-0.5 h-4 w-4 shrink-0 text-red-600" />
            <div>
              <p class="text-sm font-semibold text-red-700">Ação irreversível</p>
              <p class="mt-0.5 text-xs text-red-600">
                Todos os dados, histórico e informações de <strong>{{ user.name }}</strong> serão excluídos permanentemente.
              </p>
            </div>
          </div>
        </div>

        <p class="mb-3 text-sm text-slate-700">
          Para confirmar, digite o email do usuário:
          <strong class="block mt-1 font-mono text-slate-900">{{ user.email }}</strong>
        </p>

        <input
          v-model="confirmEmail"
          type="email"
          placeholder="Digite o email para confirmar"
          class="w-full rounded-lg border px-3 py-2 text-sm outline-none transition focus:ring-2"
          :class="emailError ? 'border-red-300 focus:border-red-400 focus:ring-red-100' : 'border-slate-200 focus:border-blue-400 focus:ring-blue-100'"
          @keydown.enter="handleHardDelete"
        />
        <p v-if="emailError" class="mt-1 text-xs text-red-500">{{ emailError }}</p>

        <div class="mt-4 flex gap-3">
          <button
            class="flex-1 rounded-lg border border-slate-200 px-4 py-2 text-sm font-medium text-slate-700 transition hover:bg-slate-50"
            :disabled="isSubmitting"
            @click="step = 'choose'"
          >
            Voltar
          </button>
          <button
            class="flex-1 rounded-lg bg-red-600 px-4 py-2 text-sm font-medium text-white transition hover:bg-red-700 disabled:opacity-60"
            :disabled="isSubmitting || !emailMatches"
            @click="handleHardDelete"
          >
            {{ isSubmitting ? 'Excluindo...' : 'Excluir permanentemente' }}
          </button>
        </div>
      </div>
    </div>
  </div>
</template>
