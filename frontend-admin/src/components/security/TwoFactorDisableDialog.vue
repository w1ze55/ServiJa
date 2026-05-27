<script setup lang="ts">
import { ref, watch } from 'vue'
import { AlertCircle } from 'lucide-vue-next'
import Modal from './Modal.vue'
import { twoFactorService } from '@/api/services/twoFactorService'

interface Props {
  open: boolean
  onClose: () => void
  onDisabled: () => void
}
const props = defineProps<Props>()

const password = ref('')
const code = ref('')
const error = ref<string | undefined>()
const isSubmitting = ref(false)

watch(
  () => props.open,
  (open) => {
    if (!open) {
      password.value = ''
      code.value = ''
      error.value = undefined
    }
  },
)

async function confirm() {
  error.value = undefined
  if (!password.value || !code.value.trim()) {
    error.value = 'Informe senha e código'
    return
  }
  isSubmitting.value = true
  try {
    await twoFactorService.disable(password.value, code.value.trim())
    props.onDisabled()
  } catch (e: any) {
    error.value = e.response?.data?.message || 'Não foi possível desativar.'
  } finally {
    isSubmitting.value = false
  }
}
</script>

<template>
  <Modal :open="props.open" :on-close="props.onClose">
    <h3 class="text-lg font-semibold text-slate-900">Desativar 2FA</h3>
    <p class="mt-1 text-sm text-slate-600">
      Para desativar, confirme com sua senha e um código atual (TOTP ou backup).
    </p>

    <div
      v-if="error"
      class="mt-3 flex items-center gap-2 rounded-lg border border-red-200 bg-red-50 p-3 text-sm text-red-700"
    >
      <AlertCircle class="h-4 w-4" />
      <span>{{ error }}</span>
    </div>

    <div class="mt-4 space-y-3">
      <div>
        <label class="mb-1 block text-sm font-medium text-slate-700">Senha</label>
        <input
          v-model="password"
          type="password"
          :disabled="isSubmitting"
          class="w-full rounded-lg border border-slate-200 px-3 py-2.5 text-sm outline-none transition focus:border-blue-400 focus:ring-2 focus:ring-blue-100"
        />
      </div>
      <div>
        <label class="mb-1 block text-sm font-medium text-slate-700">
          Código TOTP ou backup
        </label>
        <input
          v-model="code"
          type="text"
          :disabled="isSubmitting"
          class="w-full rounded-lg border border-slate-200 px-3 py-2.5 text-sm outline-none transition focus:border-blue-400 focus:ring-2 focus:ring-blue-100"
        />
      </div>
    </div>

    <div class="mt-6 flex justify-end gap-2">
      <button
        type="button"
        class="rounded-lg border border-slate-200 px-4 py-2 text-sm font-medium text-slate-700 hover:bg-slate-50"
        :disabled="isSubmitting"
        @click="props.onClose"
      >
        Cancelar
      </button>
      <button
        type="button"
        class="rounded-lg bg-red-600 px-4 py-2 text-sm font-medium text-white transition hover:bg-red-700 disabled:opacity-60"
        :disabled="isSubmitting"
        @click="confirm"
      >
        {{ isSubmitting ? 'Desativando...' : 'Desativar 2FA' }}
      </button>
    </div>
  </Modal>
</template>
