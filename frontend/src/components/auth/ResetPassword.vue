<script setup lang="ts">
import { ref, computed, onMounted, watch } from 'vue'
import Button from '../ui/button.vue'
import Input from '../ui/input.vue'
import Label from '../ui/label.vue'
import Alert from '../ui/alert.vue'
import AlertDescription from '../ui/alert-description.vue'
import {
  AlertCircle,
  Eye,
  EyeOff,
  Check,
  X,
  CheckCircle,
  Lock,
  ArrowLeft
} from 'lucide-vue-next'
import api from '@/api/config/axios'
import { toast } from '@/utils/toast'

interface ResetPasswordProps {
  email: string
  onSuccess: () => void
  onBack: () => void
}

const props = defineProps<ResetPasswordProps>()

// Código de 6 dígitos
const code = ref<string[]>(['', '', '', '', '', ''])
const inputRefs = ref<HTMLInputElement[]>([])

// Formulário de nova senha
const password = ref('')
const confirmPassword = ref('')
const showPassword = ref(false)
const showConfirmPassword = ref(false)
const errors = ref<{ code?: string; password?: string; confirmPassword?: string }>({})

// Estados
const isLoading = ref(false)
const isCodeVerified = ref(false)
const success = ref(false)
const canResend = ref(false)
const timeLeft = ref(60)

// Validação dinâmica da senha
const passwordRequirements = computed(() => ({
  minLength: password.value.length >= 8,
  hasUpperCase: /[A-Z]/.test(password.value),
  hasLowerCase: /[a-z]/.test(password.value),
  hasNumber: /[0-9]/.test(password.value),
  hasSpecial: /[!@#$%^&*(),.?":{}|<>]/.test(password.value)
}))

const isPasswordValid = computed(() =>
  Object.values(passwordRequirements.value).every(Boolean)
)

const maskedEmail = computed(() => {
  const [username, domain] = props.email.split('@')
  if (username.length <= 3) return props.email
  return `${username.slice(0, 3)}***@${domain}`
})

// Timer de reenvio
watch([timeLeft, canResend], ([t, c]) => {
  if (t > 0 && !c) {
    const timer = setTimeout(() => (timeLeft.value -= 1), 1000)
    return () => clearTimeout(timer)
  } else if (t === 0) {
    canResend.value = true
  }
})

onMounted(() => {
  inputRefs.value[0]?.focus()
})

// Handlers do código
function handleChange(index: number, value: string) {
  if (!/^\d*$/.test(value)) return
  const newCode = [...code.value]
  newCode[index] = value.slice(-1)
  code.value = newCode
  errors.value.code = ''

  if (value && index < 5) inputRefs.value[index + 1]?.focus()

  // Auto-verificar quando completar 6 dígitos
  if (index === 5 && value) {
    const fullCode = newCode.join('')
    if (fullCode.length === 6) {
      isCodeVerified.value = true
      toast.success('Código verificado! Agora defina sua nova senha.')
    }
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

  if (pastedData.length === 6) {
    isCodeVerified.value = true
    toast.success('Código verificado! Agora defina sua nova senha.')
  } else if (pastedData.length > 0) {
    inputRefs.value[Math.min(pastedData.length, 5)]?.focus()
  }
}

async function handleResend() {
  canResend.value = false
  timeLeft.value = 60
  code.value = ['', '', '', '', '', '']
  errors.value = {}
  isCodeVerified.value = false

  try {
    await api.post(`/auth/resend-reset-code?email=${encodeURIComponent(props.email)}`)
    toast.success('Novo código enviado!')
    inputRefs.value[0]?.focus()
  } catch (err: any) {
    toast.error('Erro ao reenviar código. Tente novamente.')
  }
}

// Validação e submit da nova senha
const validatePassword = (): boolean => {
  const newErrors: { password?: string; confirmPassword?: string } = {}

  if (!password.value) {
    newErrors.password = 'Senha é obrigatória'
  } else if (password.value.length > 256) {
    newErrors.password = 'Senha deve ter no máximo 256 caracteres'
  } else if (!isPasswordValid.value) {
    newErrors.password = 'Senha não atende aos requisitos'
  }

  if (!confirmPassword.value) {
    newErrors.confirmPassword = 'Confirmação de senha é obrigatória'
  } else if (password.value !== confirmPassword.value) {
    newErrors.confirmPassword = 'As senhas não coincidem'
  }

  errors.value = newErrors
  return Object.keys(newErrors).length === 0
}

async function handleSubmit(e: Event) {
  e.preventDefault()
  
  if (!validatePassword()) return

  const fullCode = code.value.join('')
  if (fullCode.length !== 6) {
    errors.value.code = 'Digite o código de 6 dígitos'
    return
  }

  isLoading.value = true
  try {
    await api.post('/auth/reset-password', {
      email: props.email,
      code: fullCode,
      newPassword: password.value,
      confirmPassword: confirmPassword.value
    })

    success.value = true
    toast.success('Senha redefinida com sucesso!')

    // Redireciona após sucesso
    setTimeout(() => props.onSuccess(), 2000)
  } catch (err: any) {
    const errorMessage = err.response?.data?.message || 'Erro ao redefinir senha'
    toast.error(errorMessage)
    
    if (errorMessage.toLowerCase().includes('código') || errorMessage.toLowerCase().includes('inválido') || errorMessage.toLowerCase().includes('expirado')) {
      errors.value.code = errorMessage
      code.value = ['', '', '', '', '', '']
      isCodeVerified.value = false
      inputRefs.value[0]?.focus()
    }
  } finally {
    isLoading.value = false
  }
}
</script>

<template>
  <!-- SUCESSO -->
  <div
    v-if="success"
    class="min-h-screen bg-gradient-to-br from-blue-50 to-indigo-100 flex items-center justify-center p-4"
  >
    <div class="w-full max-w-md bg-white rounded-lg shadow-xl p-8">
      <div class="text-center">
        <div class="inline-flex p-4 bg-green-100 rounded-full mb-4">
          <CheckCircle class="h-8 w-8 text-green-600" />
        </div>
        <h2 class="text-gray-900 mb-2">Senha redefinida!</h2>
        <p class="text-gray-600 mb-4">Sua senha foi alterada com sucesso.</p>
        <p class="text-sm text-gray-500 mb-6">
          Você será redirecionado para o login em instantes...
        </p>

        <Alert class="mb-6 bg-blue-50 border-blue-200 text-left">
          <AlertCircle class="h-4 w-4 text-blue-600" />
          <AlertDescription class="text-gray-700">
            Por segurança, você precisará fazer login com sua nova senha.
          </AlertDescription>
        </Alert>

        <Button @click="props.onSuccess" class="w-full">
          Ir para o login
        </Button>
      </div>
    </div>
  </div>

  <!-- FORMULÁRIO -->
  <div
    v-else
    class="min-h-screen bg-gradient-to-br from-blue-50 to-indigo-100 flex items-center justify-center p-4"
  >
    <div class="w-full max-w-md bg-white rounded-lg shadow-xl p-8">
      <Button
        variant="ghost"
        @click="props.onBack"
        class="mb-6"
      >
        <ArrowLeft class="h-4 w-4" />
        Voltar
      </Button>

      <div class="text-center mb-6">
        <div class="inline-flex p-4 bg-red-100 rounded-full mb-4">
          <Lock class="h-8 w-8 text-red-600" />
        </div>
        <h2 class="text-gray-900 mb-2">Redefinir senha</h2>
        <p class="text-gray-600">
          Digite o código enviado para <strong>{{ maskedEmail }}</strong>
        </p>
      </div>

      <!-- Alerta de erro do código -->
      <Alert v-if="errors.code" variant="destructive" class="mb-4">
        <AlertCircle class="h-4 w-4" />
        <AlertDescription>{{ errors.code }}</AlertDescription>
      </Alert>

      <!-- Inputs de código -->
      <div class="mb-6">
        <p class="text-sm text-gray-600 text-center mb-4">Código de recuperação</p>

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
            :disabled="isLoading || success"
            class="w-12 h-14 text-center text-xl font-bold border-2 rounded-lg transition-all
                   focus:outline-none focus:ring-2 focus:ring-red-600 focus:border-transparent
                   disabled:opacity-50 disabled:cursor-not-allowed"
            :class="[
              digit ? 'border-red-600 bg-red-50' : 'border-gray-300',
              errors.code ? 'border-red-500' : '',
              isCodeVerified ? 'border-green-500 bg-green-50' : ''
            ]"
          />
        </div>

        <p class="text-xs text-gray-500 text-center mt-2">O código expira em 1 hora</p>

        <!-- Reenvio -->
        <div class="text-center mt-4">
          <p class="text-gray-600 text-sm mb-1">Não recebeu o código?</p>
          <Button
            v-if="canResend"
            variant="link"
            size="sm"
            @click="handleResend"
            :disabled="isLoading"
          >
            Reenviar código
          </Button>
          <p v-else class="text-sm text-gray-500">Reenviar código em {{ timeLeft }}s</p>
        </div>
      </div>

      <!-- Formulário de nova senha (aparece após código verificado) -->
      <form v-if="isCodeVerified" @submit="handleSubmit" class="space-y-4 border-t pt-6">
        <div class="text-center mb-4">
          <p class="text-sm text-green-600 flex items-center justify-center gap-2">
            <Check class="h-4 w-4" />
            Código verificado! Agora defina sua nova senha.
          </p>
        </div>

        <!-- Nova Senha -->
        <div class="space-y-2">
          <Label for="password">Nova senha *</Label>
          <div class="relative">
            <Input
              id="password"
              :type="showPassword ? 'text' : 'password'"
              placeholder="Digite sua nova senha"
              v-model="password"
              @input="errors.password && (errors.password = undefined)"
              :class="errors.password ? 'border-red-500 pr-10' : 'pr-10'"
              maxlength="256"
              :disabled="isLoading"
            />
            <button
              type="button"
              @click="showPassword = !showPassword"
              class="absolute right-3 top-1/2 -translate-y-1/2 text-gray-500 hover:text-gray-700"
              tabindex="-1"
            >
              <component :is="showPassword ? EyeOff : Eye" class="h-4 w-4" />
            </button>
          </div>

          <div
            v-if="password"
            class="mt-3 p-3 bg-gray-50 rounded-md space-y-2"
          >
            <p class="text-sm text-gray-700">A senha deve conter:</p>
            <div
              class="flex items-center gap-2 text-sm"
              :class="passwordRequirements.minLength ? 'text-green-600' : 'text-gray-500'"
            >
              <component :is="passwordRequirements.minLength ? Check : X" class="h-3 w-3" />
              <span>Mínimo 8 caracteres</span>
            </div>
            <div
              class="flex items-center gap-2 text-sm"
              :class="passwordRequirements.hasUpperCase ? 'text-green-600' : 'text-gray-500'"
            >
              <component :is="passwordRequirements.hasUpperCase ? Check : X" class="h-3 w-3" />
              <span>1 letra maiúscula</span>
            </div>
            <div
              class="flex items-center gap-2 text-sm"
              :class="passwordRequirements.hasLowerCase ? 'text-green-600' : 'text-gray-500'"
            >
              <component :is="passwordRequirements.hasLowerCase ? Check : X" class="h-3 w-3" />
              <span>1 letra minúscula</span>
            </div>
            <div
              class="flex items-center gap-2 text-sm"
              :class="passwordRequirements.hasNumber ? 'text-green-600' : 'text-gray-500'"
            >
              <component :is="passwordRequirements.hasNumber ? Check : X" class="h-3 w-3" />
              <span>1 número</span>
            </div>
            <div
              class="flex items-center gap-2 text-sm"
              :class="passwordRequirements.hasSpecial ? 'text-green-600' : 'text-gray-500'"
            >
              <component :is="passwordRequirements.hasSpecial ? Check : X" class="h-3 w-3" />
              <span>1 caractere especial</span>
            </div>
          </div>

          <p
            v-if="errors.password"
            class="text-sm text-red-500 flex items-center gap-1"
          >
            <AlertCircle class="h-3 w-3" />
            {{ errors.password }}
          </p>
        </div>

        <!-- Confirmar Senha -->
        <div class="space-y-2">
          <Label for="confirmPassword">Confirmar nova senha *</Label>
          <div class="relative">
            <Input
              id="confirmPassword"
              :type="showConfirmPassword ? 'text' : 'password'"
              placeholder="Digite novamente sua senha"
              v-model="confirmPassword"
              @input="errors.confirmPassword && (errors.confirmPassword = undefined)"
              :class="errors.confirmPassword ? 'border-red-500 pr-10' : 'pr-10'"
              maxlength="256"
              :disabled="isLoading"
            />
            <button
              type="button"
              @click="showConfirmPassword = !showConfirmPassword"
              class="absolute right-3 top-1/2 -translate-y-1/2 text-gray-500 hover:text-gray-700"
              tabindex="-1"
            >
              <component
                :is="showConfirmPassword ? EyeOff : Eye"
                class="h-4 w-4"
              />
            </button>
          </div>

          <p
            v-if="errors.confirmPassword"
            class="text-sm text-red-500 flex items-center gap-1"
          >
            <AlertCircle class="h-3 w-3" />
            {{ errors.confirmPassword }}
          </p>

          <p
            v-if="confirmPassword && password === confirmPassword"
            class="text-sm text-green-600 flex items-center gap-1"
          >
            <Check class="h-3 w-3" />
            As senhas coincidem
          </p>
        </div>

        <Button
          type="submit"
          class="w-full"
          :disabled="isLoading"
          size="lg"
        >
          {{ isLoading ? 'Redefinindo...' : 'Redefinir senha' }}
        </Button>
      </form>

      <!-- Dica de desenvolvimento -->
      <div class="mt-6 p-4 bg-amber-50 rounded-lg border border-amber-200" v-if="!isCodeVerified">
        <h5 class="text-sm text-gray-900 mb-2">💡 Dica</h5>
        <p class="text-sm text-gray-700">
          Verifique sua caixa de entrada e spam. O código de 6 dígitos foi enviado para o e-mail cadastrado.
        </p>
      </div>
    </div>
  </div>
</template>
