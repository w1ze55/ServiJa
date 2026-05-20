<script setup lang="ts">
import { computed, ref, watch } from 'vue';
import { Loader2, ShieldCheck, X, XCircle } from 'lucide-vue-next';
import type { SupportRequest } from '@/api/services/supportService';

const props = defineProps<{
  open: boolean;
  request: SupportRequest | null;
  mode: 'approve' | 'reject' | null;
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

const title = computed(() =>
  props.mode === 'approve' ? 'Aprovar solicitacao' : 'Recusar solicitacao'
);

const description = computed(() => {
  if (!props.request) return '';

  if (props.mode === 'approve') {
    return `Escreva a mensagem que sera enviada por e-mail ao usuario para aprovar a solicitacao #${props.request.id}.`;
  }

  return `Explique por e-mail o motivo da recusa da solicitacao #${props.request.id}.`;
});

const confirmLabel = computed(() => (props.mode === 'approve' ? 'Aprovar' : 'Recusar'));
const iconComponent = computed(() => (props.mode === 'approve' ? ShieldCheck : XCircle));
const iconClasses = computed(() =>
  props.mode === 'approve' ? 'bg-emerald-50 text-emerald-600' : 'bg-red-50 text-red-600'
);
const buttonClasses = computed(() =>
  props.mode === 'approve' ? 'bg-emerald-600 hover:bg-emerald-700' : 'bg-red-600 hover:bg-red-700'
);

const placeholder = computed(() =>
  props.mode === 'approve'
    ? 'Ex: Solicitacao aprovada. Nossa equipe seguira com a analise do seu pedido.'
    : 'Explique o motivo da recusa para o usuario.'
);

const handleSubmit = () => {
  if (!message.value.trim()) {
    error.value = 'A mensagem e obrigatoria para continuar.';
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
              {{ request?.userName || 'Solicitacao de suporte' }}
            </p>
            <p class="mt-1 text-sm text-slate-600">{{ description }}</p>
          </div>
        </div>

        <div class="space-y-2">
          <label class="block text-sm font-medium text-slate-700">
            Mensagem <span class="text-red-500">*</span>
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
