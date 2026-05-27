<script setup lang="ts">
import { ref } from 'vue'
import Button from '../ui/button.vue'
import Input from '../ui/input.vue'
import Label from '../ui/label.vue'
import Alert from '../ui/alert.vue'
import AlertDescription from '../ui/alert-description.vue'
import { AlertCircle, ArrowLeft, Shield } from 'lucide-vue-next'
import { twoFactorService } from '@/api/services/twoFactorService'

interface Props {
  challengeToken: string
  onVerified: (response: any) => void
  onBack: () => void
}

const props = defineProps<Props>()

const code = ref('')
const isLoading = ref(false)
const error = ref<string | undefined>()
const useBackup = ref(false)

async function handleSubmit(e: Event) {
  e.preventDefault()
  error.value = undefined

  const trimmed = code.value.trim()
  if (!trimmed) {
    error.value = useBackup.value
      ? 'Digite um código de backup'
      : 'Digite o código de 6 dígitos'
    return
  }

  isLoading.value = true
  try {
    const data = await twoFactorService.verifyChallenge(props.challengeToken, trimmed)
    props.onVerified(data)
  } catch (err: any) {
    error.value =
      err.response?.data?.message ||
      err.response?.data?.error ||
      'Código inválido. Tente novamente.'
  } finally {
    isLoading.value = false
  }
}

function toggleBackup() {
  useBackup.value = !useBackup.value
  code.value = ''
  error.value = undefined
}
</script>

<template>
  <div class="login-page-container">
    <div class="login-page-card">
      <div class="login-page-header">
        <div class="mx-auto mb-2 flex h-12 w-12 items-center justify-center rounded-full bg-blue-50">
          <Shield class="h-6 w-6 text-blue-600" />
        </div>
        <h2 class="login-page-title">Verificação em dois fatores</h2>
        <p class="login-page-subtitle">
          {{
            useBackup
              ? 'Digite um dos códigos de backup que você salvou ao ativar o 2FA.'
              : 'Abra o Google Authenticator e digite o código de 6 dígitos.'
          }}
        </p>
      </div>

      <Alert v-if="error" variant="destructive" class="mb-6">
        <AlertCircle class="h-4 w-4" />
        <AlertDescription>{{ error }}</AlertDescription>
      </Alert>

      <form @submit="handleSubmit" class="login-page-form">
        <div class="login-page-field">
          <Label for="totp-code">{{ useBackup ? 'Código de backup' : 'Código' }}</Label>
          <Input
            id="totp-code"
            type="text"
            inputmode="text"
            autocomplete="one-time-code"
            :placeholder="useBackup ? 'XXXXX-XXXXX' : '000000'"
            v-model="code"
            :disabled="isLoading"
            :maxlength="useBackup ? 12 : 6"
          />
        </div>

        <Button type="submit" class="w-full" :disabled="isLoading">
          {{ isLoading ? 'Verificando...' : 'Verificar' }}
        </Button>

        <div class="text-center">
          <Button
            variant="link"
            type="button"
            @click="toggleBackup"
          >
            {{ useBackup ? 'Usar código do app autenticador' : 'Usar código de backup' }}
          </Button>
        </div>

        <div class="login-page-back-wrapper">
          <Button
            variant="ghost"
            size="sm"
            type="button"
            @click="props.onBack"
            class="mx-auto"
          >
            <ArrowLeft class="h-4 w-4" />
            Voltar
          </Button>
        </div>
      </form>
    </div>
  </div>
</template>

<style scoped>
@import '@/styles/auth/login-page.css';
</style>
