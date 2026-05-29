<script setup lang="ts">
import { ref, watch } from 'vue'
import { AlertCircle, Check, Copy } from 'lucide-vue-next'
import { twoFactorService, type TwoFactorSetup } from '@/api/services/twoFactorService'
import Alert from '../ui/alert.vue'
import AlertDescription from '../ui/alert-description.vue'
import AlertDialog from '../ui/alert-dialog.vue'
import AlertDialogContent from '../ui/alert-dialog-content.vue'
import Button from '../ui/button.vue'
import Input from '../ui/input.vue'
import Label from '../ui/label.vue'

interface Props {
  open: boolean
  onClose: () => void
  onActivated: (backupCodes: string[]) => void
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
  <AlertDialog :open="props.open">
    <AlertDialogContent>
      <h3 class="text-lg font-semibold">Ativar autenticação em dois fatores</h3>
      <p class="text-sm text-gray-600">
        Use o Google Authenticator (ou app compatível) para escanear o QR code
        abaixo e digite o código gerado para confirmar.
      </p>

      <Alert v-if="error" variant="destructive">
        <AlertCircle class="h-4 w-4" />
        <AlertDescription>{{ error }}</AlertDescription>
      </Alert>

      <div v-if="isStarting" class="py-8 text-center text-sm text-gray-500">
        Gerando QR code...
      </div>

      <div v-else-if="setup" class="space-y-4">
        <div class="flex justify-center">
          <img :src="setup.qrCodeDataUri" alt="QR code 2FA" class="h-48 w-48" />
        </div>

        <div class="rounded-md bg-gray-50 p-3 text-center">
          <p class="text-xs text-gray-500">Não consegue escanear? Digite o código manualmente:</p>
          <div class="mt-1 flex items-center justify-center gap-2">
            <code class="font-mono text-sm">{{ setup.secret }}</code>
            <button type="button" @click="copySecret" class="text-gray-500 hover:text-gray-700">
              <component :is="copied ? Check : Copy" class="h-4 w-4" />
            </button>
          </div>
        </div>

        <div class="space-y-2">
          <Label for="setup-code" class="text-gray-600">Código de 6 digitos do app</Label>
          <Input
            id="setup-code"
            v-model="code"
            type="text"
            inputmode="numeric"
            autocomplete="one-time-code"
            placeholder="000000"
            maxlength="6"
            :disabled="isConfirming"
          />
        </div>
      </div>

      <div class="mt-4 flex justify-end gap-2">
        <Button variant="outline" @click="props.onClose" type="button" :disabled="isConfirming">
          Cancelar
        </Button>
        <Button @click="confirm" :disabled="!setup || isConfirming" type="button">
          {{ isConfirming ? 'Verificando...' : 'Ativar' }}
        </Button>
      </div>
    </AlertDialogContent>
  </AlertDialog>
</template>
