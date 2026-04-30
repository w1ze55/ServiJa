<script setup lang="ts">
import { ref, computed, watch } from 'vue';
import { X, AlertTriangle } from 'lucide-vue-next';
import { adminUserService, type AdminUserDetail } from '@/api/services/adminUserService';
import { toast } from '@/utils/toast';

const props = defineProps<{
  user: AdminUserDetail;
}>();

const emit = defineEmits<{
  (e: 'close'): void;
  (e: 'refresh'): void;
}>();

const name = ref(props.user.name);
const email = ref(props.user.email);
const phone = ref(props.user.phone ?? '');
const bio = ref(props.user.bio ?? '');
const userType = ref(props.user.userType);

const isSubmitting = ref(false);
const serverError = ref('');

const isAdmin = computed(() => props.user.userType === 'ADMIN');
const typeChanged = computed(() => userType.value !== props.user.userType);

const currentLocalUser = computed(() => {
  try {
    return JSON.parse(localStorage.getItem('user') || 'null');
  } catch {
    return null;
  }
});
const isOwnAccount = computed(
  () => currentLocalUser.value && String(props.user.id) === String(currentLocalUser.value.id)
);

// Validation
const errors = ref<Record<string, string>>({});

const validate = () => {
  errors.value = {};
  if (!name.value.trim() || name.value.trim().length < 3) {
    errors.value.name = 'Nome deve ter pelo menos 3 caracteres.';
  }
  if (!email.value.trim() || !/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email.value)) {
    errors.value.email = 'Email inválido.';
  }
  const phoneDigits = phone.value.replace(/\D/g, '');
  if (phone.value && (phoneDigits.length < 10 || phoneDigits.length > 11)) {
    errors.value.phone = 'Telefone deve ter 10 ou 11 dígitos.';
  }
  if (props.user.userType === 'FREELANCER' && bio.value && bio.value.length > 1000) {
    errors.value.bio = 'Bio deve ter no máximo 1000 caracteres.';
  }
  return Object.keys(errors.value).length === 0;
};

const handleSubmit = async () => {
  serverError.value = '';
  if (!validate()) return;

  isSubmitting.value = true;
  try {
    const payload: Record<string, unknown> = {
      name: name.value.trim(),
      email: email.value.trim(),
    };
    if (phone.value.trim()) payload.phone = phone.value.trim();
    if (props.user.userType === 'FREELANCER') payload.bio = bio.value;
    if (!isAdmin.value) payload.userType = userType.value;

    await adminUserService.updateUser(props.user.id, payload);
    toast.success('Usuário atualizado com sucesso.');
    emit('refresh');
  } catch (err: unknown) {
    const msg = (err as { response?: { data?: { message?: string } } })?.response?.data?.message;
    serverError.value = msg || 'Erro ao atualizar usuário.';
    toast.error(serverError.value);
  } finally {
    isSubmitting.value = false;
  }
};
</script>

<template>
  <div class="fixed inset-0 z-[70] flex items-center justify-center p-4">
    <div class="absolute inset-0 bg-black/40" @click="emit('close')" />
    <div class="relative w-full max-w-lg rounded-2xl bg-white shadow-2xl">
      <!-- Header -->
      <div class="flex items-center justify-between border-b border-slate-200 px-6 py-4">
        <h2 class="text-base font-semibold text-slate-900">Editar usuário</h2>
        <button
          class="rounded-lg p-1 text-slate-400 transition hover:bg-slate-100 hover:text-slate-600"
          @click="emit('close')"
        >
          <X class="h-5 w-5" />
        </button>
      </div>

      <!-- Warning own account -->
      <div v-if="isOwnAccount" class="mx-6 mt-4 rounded-lg border border-amber-200 bg-amber-50 p-3">
        <div class="flex items-start gap-2">
          <AlertTriangle class="mt-0.5 h-4 w-4 shrink-0 text-amber-600" />
          <p class="text-xs text-amber-700">Você está editando sua própria conta.</p>
        </div>
      </div>

      <!-- Form -->
      <form class="space-y-4 px-6 py-5" @submit.prevent="handleSubmit">
        <!-- Name -->
        <div>
          <label class="mb-1 block text-sm font-medium text-slate-700">
            Nome <span class="text-red-500">*</span>
          </label>
          <input
            v-model="name"
            type="text"
            class="w-full rounded-lg border px-3 py-2 text-sm outline-none transition focus:ring-2"
            :class="errors.name ? 'border-red-300 focus:border-red-400 focus:ring-red-100' : 'border-slate-200 focus:border-blue-400 focus:ring-blue-100'"
          />
          <p v-if="errors.name" class="mt-1 text-xs text-red-500">{{ errors.name }}</p>
        </div>

        <!-- Email -->
        <div>
          <label class="mb-1 block text-sm font-medium text-slate-700">
            Email <span class="text-red-500">*</span>
          </label>
          <input
            v-model="email"
            type="email"
            class="w-full rounded-lg border px-3 py-2 text-sm outline-none transition focus:ring-2"
            :class="errors.email ? 'border-red-300 focus:border-red-400 focus:ring-red-100' : 'border-slate-200 focus:border-blue-400 focus:ring-blue-100'"
          />
          <p v-if="errors.email" class="mt-1 text-xs text-red-500">{{ errors.email }}</p>
        </div>

        <!-- Phone -->
        <div>
          <label class="mb-1 block text-sm font-medium text-slate-700">Telefone</label>
          <input
            v-model="phone"
            type="tel"
            placeholder="(11) 99999-9999"
            class="w-full rounded-lg border px-3 py-2 text-sm outline-none transition focus:ring-2"
            :class="errors.phone ? 'border-red-300 focus:border-red-400 focus:ring-red-100' : 'border-slate-200 focus:border-blue-400 focus:ring-blue-100'"
          />
          <p v-if="errors.phone" class="mt-1 text-xs text-red-500">{{ errors.phone }}</p>
        </div>

        <!-- Bio (FREELANCER only) -->
        <div v-if="user.userType === 'FREELANCER'">
          <label class="mb-1 block text-sm font-medium text-slate-700">
            Bio <span class="text-slate-400">(max. 1000)</span>
          </label>
          <textarea
            v-model="bio"
            rows="3"
            class="w-full rounded-lg border px-3 py-2 text-sm outline-none transition focus:ring-2 resize-none"
            :class="errors.bio ? 'border-red-300 focus:border-red-400 focus:ring-red-100' : 'border-slate-200 focus:border-blue-400 focus:ring-blue-100'"
          />
          <div class="flex items-center justify-between">
            <p v-if="errors.bio" class="mt-1 text-xs text-red-500">{{ errors.bio }}</p>
            <p class="mt-1 ml-auto text-xs text-slate-400">{{ bio.length }}/1000</p>
          </div>
        </div>

        <!-- User Type -->
        <div>
          <label class="mb-1 block text-sm font-medium text-slate-700">Tipo de conta</label>
          <select
            v-model="userType"
            :disabled="isAdmin"
            class="w-full rounded-lg border border-slate-200 px-3 py-2 text-sm outline-none transition focus:border-blue-400 focus:ring-2 focus:ring-blue-100 disabled:cursor-not-allowed disabled:bg-slate-50 disabled:text-slate-400"
          >
            <option v-if="isAdmin" value="ADMIN">Admin</option>
            <template v-else>
              <option value="FREELANCER">Freelancer</option>
              <option value="CONTRACTOR">Contratante</option>
            </template>
          </select>
          <p v-if="isAdmin" class="mt-1 text-xs text-slate-400">
            Tipo Admin não pode ser alterado por aqui.
          </p>
          <div v-if="typeChanged && !isAdmin" class="mt-2 rounded-lg border border-amber-200 bg-amber-50 p-2">
            <div class="flex items-start gap-2">
              <AlertTriangle class="mt-0.5 h-3.5 w-3.5 shrink-0 text-amber-600" />
              <p class="text-xs text-amber-700">
                Alterar o tipo afeta as permissões e a experiência do usuário.
              </p>
            </div>
          </div>
        </div>

        <!-- Server error -->
        <div v-if="serverError" class="rounded-lg border border-red-200 bg-red-50 p-3">
          <p class="text-sm text-red-700">{{ serverError }}</p>
        </div>

        <!-- Actions -->
        <div class="flex gap-3 pt-2">
          <button
            type="button"
            class="flex-1 rounded-lg border border-slate-200 px-4 py-2 text-sm font-medium text-slate-700 transition hover:bg-slate-50"
            :disabled="isSubmitting"
            @click="emit('close')"
          >
            Cancelar
          </button>
          <button
            type="submit"
            class="flex-1 rounded-lg bg-blue-600 px-4 py-2 text-sm font-medium text-white transition hover:bg-blue-700 disabled:opacity-60"
            :disabled="isSubmitting"
          >
            {{ isSubmitting ? 'Salvando...' : 'Salvar alterações' }}
          </button>
        </div>
      </form>
    </div>
  </div>
</template>
