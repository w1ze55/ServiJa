<script setup lang="ts">
import { ref, watch } from 'vue'
import Button from '../ui/button.vue'
import Input from '../ui/input.vue'
import Label from '../ui/label.vue'
import Alert from '../ui/alert.vue'
import AlertDescription from '../ui/alert-description.vue'
import AlertDialog from '../ui/alert-dialog.vue'
import AlertDialogContent from '../ui/alert-dialog-content.vue'
import { AlertCircle } from 'lucide-vue-next'
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
    error.value = e.response?.data?.message || 'Não foi possível desativar. Verifique senha e código.'
  } finally {
    isSubmitting.value = false
  }
}
</script>

<template>
  <AlertDialog :open="props.open">
    <AlertDialogContent>
      <h3 class="text-lg font-semibold">Desativar 2FA</h3>
      <p class="text-sm text-gray-600">
        Para desativar, confirme com sua senha e um código atual (TOTP ou backup).
      </p>

      <Alert v-if="error" variant="destructive">
        <AlertCircle class="h-4 w-4" />
        <AlertDescription>{{ error }}</AlertDescription>
      </Alert>

      <div class="space-y-3">
        <div>
          <Label for="disable-password">Senha</Label>
          <Input id="disable-password" type="password" v-model="password" :disabled="isSubmitting" />
        </div>
        <div>
          <Label for="disable-code">Código TOTP ou backup</Label>
          <Input id="disable-code" type="text" v-model="code" :disabled="isSubmitting" />
        </div>
      </div>

      <div class="mt-4 flex justify-end gap-2">
        <Button variant="outline" @click="props.onClose" type="button" :disabled="isSubmitting">
          Cancelar
        </Button>
        <Button variant="destructive" @click="confirm" type="button" :disabled="isSubmitting">
          {{ isSubmitting ? 'Desativando...' : 'Desativar 2FA' }}
        </Button>
      </div>
    </AlertDialogContent>
  </AlertDialog>
</template>
