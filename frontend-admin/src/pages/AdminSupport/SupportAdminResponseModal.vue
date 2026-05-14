<script setup lang="ts">
import { computed, ref, watch } from 'vue';
import { Loader2, MessageSquareText, ShieldCheck, X, XCircle } from 'lucide-vue-next';
import type { SupportRequest } from '@/api/services/supportService';

const props = defineProps<{
  open: boolean;
  request: SupportRequest | null;
  mode: 'approve' | 'reply' | 'reject' | null;
  isSubmitting?: boolean;
}>();

const emit = defineEmits<{
  (e: 'close'): void;
  (e: 'submit', message: string): void;
}>();

const message = ref('');
const error = ref('');

watch(
  () => [props.open, props.mode, props.request?.id],
  () => {
    message.value = '';
    error.value = '';
  }
);

const isMessageRequired = computed(() => props.mode === 'reply' || props.mode === 'reject');

const title = computed(() => {
  if (props.mode === 'approve') return 'Aprovar solicitação';
  if (props.mode === 'reject') return 'Recusar solicitação';
  return 'Responder solicitação';
});

const description = computed(() => {
  if (!props.request) return '';

  if (props.mode === 'approve') {
    return `Você está aprovando a solicitação #${props.request.id}. A mensagem é opcional, mas pode ajudar o usuário a entender os próximos passos.`;
  }

  if (props.mode === 'reject') {
    return `Explique o motivo da recusa da solicitação #${props.request.id}.`;
  }

  return `Envie uma resposta administrativa para a solicitação #${props.request.id}.`;
});

const confirmLabel = computed(() => {
  if (props.mode === 'approve') return 'Aprovar';
  if (props.mode === 'reject') return 'Recusar';
  return 'Responder';
});

const iconComponent = computed(() => {
  if (props.mode === 'approve') return ShieldCheck;
  if (props.mode === 'reject') return XCircle;
  return MessageSquareText;
});

const iconClasses = computed(() => {
  if (props.mode === 'approve') return 'bg-emerald-50 text-emerald-600';
  if (props.mode === 'reject') return 'bg-red-50 text-red-600';
  return 'bg-blue-50 text-blue-600';
});

const buttonClasses = computed(() => {
  if (props.mode === 'approve') return 'bg-emerald-600 hover:bg-emerald-700';
  if (props.mode === 'reject') return 'bg-red-600 hover:bg-red-700';
  return 'bg-blue-600 hover:bg-blue-700';
});

const placeholder = computed(() => {
  if (props.mode === 'approve') {
    return 'Ex: Solicitação aprovada. Vamos avaliar a criação da nova categoria.';
  }
  if (props.mode === 'reject') {
    return 'Explique o motivo da recusa para o usuário.';
  }
  return 'Digite a resposta que será enviada ao usuário.';
});

const handleSubmit = () => {
  if (isMessageRequired.value && !message.value.trim()) {
    error.value = 'Essa mensagem é obrigatória para continuar.';
    return;
  }

  emit('submit', message.value.trim());
};
</script>

<template>
  <div v-if="open" class="fixed inset-0 z-[80] flex items-center justify-center p-4">
    <div class="absolute inset-0 bg-black/40" @click="!isSubmitting && emit('close')" />
    <div class="relative w-full max-w-lg rounded-2xl bg-white shadow-2xl">
      <div class="flex items-center justify-between border-b border-slate-200 px-6 py-4">
        <h2 class="text-base font-semibold text-slate-900">{{ title }}</h2>
        <button
          class="rounded-lg p-1 text-slate-400 transition hover:bg-slate-100 hover:text-slate-600"
          :disabled="isSubmitting"
          @click="emit('close')"
        >
          <X class="h-5 w-5" />
        </button>
      </div>

      <div class="space-y-5 p-6">
        <div class="flex items-start gap-3 rounded-xl border border-slate-200 bg-slate-50 p-4">
          <div :class="['flex h-10 w-10 shrink-0 items-center justify-center rounded-full', iconClasses]">
            <component :is="iconComponent" class="h-5 w-5" />
          </div>
          <div>
            <p class="text-sm font-medium text-slate-900">
              {{ request?.userName || 'Solicitação de suporte' }}
            </p>
            <p class="mt-1 text-sm text-slate-600">{{ description }}</p>
          </div>
        </div>

        <div class="space-y-2">
          <label class="block text-sm font-medium text-slate-700">
            Mensagem
            <span v-if="isMessageRequired" class="text-red-500">*</span>
          </label>
          <textarea
            v-model="message"
            rows="5"
            :placeholder="placeholder"
            class="w-full rounded-xl border px-3 py-2 text-sm outline-none transition focus:ring-2"
            :class="error ? 'border-red-300 focus:border-red-400 focus:ring-red-100' : 'border-slate-200 focus:border-blue-400 focus:ring-blue-100'"
          />
          <p v-if="error" class="text-xs text-red-500">{{ error }}</p>
        </div>
      </div>

      <div class="flex gap-3 border-t border-slate-200 px-6 py-4">
        <button
          type="button"
          class="flex-1 rounded-lg border border-slate-200 px-4 py-2 text-sm font-medium text-slate-700 transition hover:bg-slate-50"
          :disabled="isSubmitting"
          @click="emit('close')"
        >
          Cancelar
        </button>
        <button
          type="button"
          :class="['flex flex-1 items-center justify-center rounded-lg px-4 py-2 text-sm font-medium text-white transition disabled:opacity-60', buttonClasses]"
          :disabled="isSubmitting"
          @click="handleSubmit"
        >
          <Loader2 v-if="isSubmitting" class="mr-2 h-4 w-4 animate-spin" />
          {{ confirmLabel }}
        </button>
      </div>
    </div>
  </div>
</template>
