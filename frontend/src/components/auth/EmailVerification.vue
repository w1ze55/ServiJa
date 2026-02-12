<template>
  <div class="min-h-screen bg-gradient-to-br from-blue-50 to-indigo-100 flex items-center justify-center p-4">
    <div class="w-full max-w-md bg-white rounded-lg shadow-xl p-8">
      <!-- Cabeçalho -->
      <div class="text-center mb-8">
        <div class="inline-flex p-4 bg-blue-100 rounded-full mb-4">
          <Mail class="h-8 w-8 text-blue-600" />
        </div>
        <h2 class="text-gray-900 mb-2">Verifique seu e-mail</h2>
        <p class="text-gray-600">Enviamos um código de 6 dígitos para</p>
        <p class="text-gray-900 mt-1">{{ maskedEmail }}</p>
      </div>

      <!-- Alertas -->
      <Alert v-if="error" variant="destructive" class="mb-6">
        <AlertCircle class="h-4 w-4" />
        <AlertDescription>{{ error }}</AlertDescription>
      </Alert>

      <Alert v-if="success" class="mb-6 bg-green-50 border-green-200">
        <CheckCircle class="h-4 w-4 text-green-600" />
        <AlertDescription class="text-green-800">
          E-mail verificado com sucesso! Redirecionando…
        </AlertDescription>
      </Alert>

      <!-- Inputs de código -->
      <div class="mb-6">
        <p class="text-sm text-gray-600 text-center mb-4">Digite o código de verificação</p>

        <div class="flex gap-2 justify-center" @paste="handlePaste">
          <input
            v-for="(digit, index) in code"
            :key="index"
            ref="inputRefs"
            type="text"
            inputmode="numeric"
            maxlength="1"
            :value="digit"
            @input="(e) => handleChange(index, (e.target as HTMLInputElement).value)"
            @keydown="(e) => handleKeyDown(index, e as KeyboardEvent)"
            :disabled="isVerifying || success"
            class="w-12 h-14 text-center border-2 rounded-lg transition-all
                   focus:outline-none focus:ring-2 focus:ring-blue-600 focus:border-transparent
                   disabled:opacity-50 disabled:cursor-not-allowed"
            :class="[
              digit ? 'border-blue-600' : 'border-gray-300',
              error ? 'border-red-500' : '',
              success ? 'border-green-500 bg-green-50' : ''
            ]"
          />
        </div>

        <p class="text-xs text-gray-500 text-center mt-4">O código expira em 10 minutos</p>
      </div>

      <!-- Reenvio -->
      <div class="text-center">
        <p class="text-gray-600 mb-2">Não recebeu o código?</p>
        <Button
          v-if="canResend"
          variant="link"
          @click="handleResend"
          :disabled="isVerifying || success"
        >
          Reenviar código
        </Button>
        <p v-else class="text-sm text-gray-500">Reenviar código em {{ timeLeft }} s</p>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, watch } from 'vue'
import Button from '../ui/button.vue'
import Alert from '../ui/alert.vue'
import AlertDescription from '../ui/alert-description.vue'
import { AlertCircle, CheckCircle, Mail } from 'lucide-vue-next'
import { API_ENDPOINTS } from '../../utils/api'

interface EmailVerificationProps {
  email: string
  onVerified: (token: string, userData: any) => void
}
const props = defineProps<EmailVerificationProps>()

const code = ref<string[]>(['', '', '', '', '', ''])
const error = ref('')
const success = ref(false)
const isVerifying = ref(false)
const canResend = ref(false)
const timeLeft = ref(60)

const inputRefs = ref<HTMLInputElement[]>([])

// foca no primeiro input ao montar
onMounted(() => {
  inputRefs.value[0]?.focus()
})

// timer de reenvio
watch([timeLeft, canResend], ([t, c]) => {
  if (t > 0 && !c) {
    const timer = setTimeout(() => (timeLeft.value -= 1), 1000)
    return () => clearTimeout(timer)
  } else if (t === 0) {
    canResend.value = true
  }
})

function handleChange(index: number, value: string) {
  if (!/^\d*$/.test(value)) return
  const newCode = [...code.value]
  newCode[index] = value.slice(-1)
  code.value = newCode
  error.value = ''

  if (value && index < 5) inputRefs.value[index + 1]?.focus()

  if (index === 5 && value) {
    const fullCode = [...newCode.slice(0, 5), value].join('')
    verifyCode(fullCode)
  }
}

function handleKeyDown(index: number, e: KeyboardEvent) {
  if (e.key === 'Backspace' && !code.value[index] && index > 0) {
    inputRefs.value[index - 1]?.focus()
  }
}

function handlePaste(e: ClipboardEvent) {
  e.preventDefault()
  const pastedData = e.clipboardData?.getData('text')?.replace(/\D/g, '').slice(0, 6) || ''
  const newCode = [...code.value]
  for (let i = 0; i < pastedData.length; i++) newCode[i] = pastedData[i]
  code.value = newCode

  if (pastedData.length === 6) verifyCode(pastedData)
  else if (pastedData.length > 0) inputRefs.value[Math.min(pastedData.length, 5)]?.focus()
}

async function verifyCode(fullCode: string) {
  isVerifying.value = true
  error.value = ''

  try {
    const response = await fetch(API_ENDPOINTS.VERIFY_EMAIL, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify({
        email: props.email,
        code: fullCode
      })
    })

    if (response.ok) {
      const data = await response.json()
      success.value = true
      
      // Extrair token e dados do usuário da resposta
      const token = data.token
      const userData = data.user
      
      setTimeout(() => props.onVerified(token, userData), 1500)
    } else {
      const data = await response.json()
      error.value = data.message || 'Código inválido. Tente novamente.'
      code.value = ['', '', '', '', '', '']
      inputRefs.value[0]?.focus()
    }
  } catch (err) {
    console.error('Erro ao verificar código:', err)
    error.value = 'Erro ao verificar código. Tente novamente.'
    code.value = ['', '', '', '', '', '']
    inputRefs.value[0]?.focus()
  } finally {
    isVerifying.value = false
  }
}

async function handleResend() {
  canResend.value = false
  timeLeft.value = 60
  code.value = ['', '', '', '', '', '']
  error.value = ''

  try {
    const response = await fetch(`${API_ENDPOINTS.RESEND_VERIFICATION}?email=${encodeURIComponent(props.email)}`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      }
    })

    if (response.ok) {
      // Mostra mensagem de sucesso (opcional)
      console.log('Código de verificação reenviado com sucesso')
    } else {
      const data = await response.json()
      error.value = data.message || 'Erro ao reenviar código.'
    }
  } catch (err) {
    console.error('Erro ao reenviar código:', err)
    error.value = 'Erro ao reenviar código. Tente novamente.'
  }

  inputRefs.value[0]?.focus()
}

const maskedEmail = computed(() => {
  const [username, domain] = props.email.split('@')
  if (username.length <= 3) return props.email
  return `${username.slice(0, 3)}***@${domain}`
})
</script>
