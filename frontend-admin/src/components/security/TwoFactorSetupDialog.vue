<script setup lang="ts">
import { ref, watch } from 'vue'
import { AlertCircle, Copy, Check } from 'lucide-vue-next'
import Modal from './Modal.vue'
import { twoFactorService, type TwoFactorSetup } from '@/api/services/twoFactorService'

interface Props {
  open: boolean
  onClose: () => void
  onActivated: (codes: string[]) => void
}
const props = defineProps<Props>()

const setup = ref<TwoFactorSetup | null>(null)
const code = ref('')
const error = ref<string | undefined>()
const isStarting = ref(false)
const isConfirming = ref(false)
const copied = ref(false)

watch(
  () => props.open,
  async (open) => {
    if (!open) {
      setup.value = null
      code.value = ''
      error.value = undefined
      return
    }
    isStarting.value = true
    error.value = undefined
    try {
      setup.value = await twoFactorService.setup()
    } catch (e: any) {
      error.value = e.response?.data?.message || 'Falha ao iniciar configuração do 2FA'
    } finally {
      isStarting.value = false
    }
  },
)

async function copySecret() {
  if (!setup.value) return
  await navigator.clipboard.writeText(setup.value.secret)
  copied.value = true
  setTimeout(() => (copied.value = false), 2000)
}

async function confirm() {
  error.value = undefined
  if (!code.value.trim()) {
    error.value = 'Digite o código gerado pelo aplicativo'
    return
  }
  isConfirming.value = true
  try {
    const res = await twoFactorService.enable(code.value.trim())
    props.onActivated(res.backupCodes)
  } catch (e: any) {
    error.value = e.response?.data?.message || 'Código inválido. Tente novamente.'
  } finally {
    isConfirming.value = false
  }
}
</script>

<template>
  <Modal :open="props.open" :on-close="props.onClose">
    <h3 class="text-lg font-semibold text-slate-900">
      Ativar autenticação em dois fatores
    </h3>
    <p class="mt-1 text-sm text-slate-600">
      Use o Google Authenticator e digite o código gerado para confirmar.
    </p>

    <div
      v-if="error"
      class="mt-3 flex items-center gap-2 rounded-lg border border-red-200 bg-red-50 p-3 text-sm text-red-700"
    >
      <AlertCircle class="h-4 w-4" />
      <span>{{ error }}</span>
    </div>

    <div v-if="isStarting" class="py-8 text-center text-sm text-slate-500">
      Gerando QR code...
    </div>

    <div v-else-if="setup" class="mt-4 space-y-4">
      <div class="flex justify-center">
        <img :src="setup.qrCodeDataUri" alt="QR code 2FA" class="h-48 w-48" />
      </div>
      <div class="rounded-md bg-slate-50 p-3 text-center">
        <p class="text-xs text-slate-500">
          Não consegue escanear? Digite o código manualmente:
        </p>
        <div class="mt-1 flex items-center justify-center gap-2">
          <code class="font-mono text-sm">{{ setup.secret }}</code>
          <button type="button" @click="copySecret" class="text-slate-500 hover:text-slate-700">
            <component :is="copied ? Check : Copy" class="h-4 w-4" />
          </button>
        </div>
      </div>

      <div>
        <label class="mb-1 block text-sm font-medium text-slate-700">
          Código de 6 dígitos
        </label>
        <input
          v-model="code"
          type="text"
          inputmode="numeric"
          autocomplete="one-time-code"
          placeholder="000000"
          maxlength="6"
          :disabled="isConfirming"
          class="w-full rounded-lg border border-slate-200 px-3 py-2.5 text-sm outline-none transition focus:border-blue-400 focus:ring-2 focus:ring-blue-100"
        />
      </div>
    </div>

    <div class="mt-6 flex justify-end gap-2">
      <button
        type="button"
        class="rounded-lg border border-slate-200 px-4 py-2 text-sm font-medium text-slate-700 hover:bg-slate-50"
        :disabled="isConfirming"
        @click="props.onClose"
      >
        Cancelar
      </button>
      <button
        type="button"
        class="rounded-lg bg-[#10243d] px-4 py-2 text-sm font-medium text-white transition hover:bg-[#1a3a5c] disabled:opacity-60"
        :disabled="!setup || isConfirming"
        @click="confirm"
      >
        {{ isConfirming ? 'Verificando...' : 'Ativar' }}
      </button>
    </div>
  </Modal>
</template>
