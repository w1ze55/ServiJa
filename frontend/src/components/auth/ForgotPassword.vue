<script setup lang="ts">
import { ref } from 'vue'
import Button from '../ui/button.vue'
import Input from '../ui/input.vue'
import Label from '../ui/label.vue'
import Alert from '../ui/alert.vue'
import AlertDescription from '../ui/alert-description.vue'
import { AlertCircle, ArrowLeft, CheckCircle, Mail } from 'lucide-vue-next'
import api from '@/api/config/axios'
import { toast } from '@/utils/toast'

interface ForgotPasswordProps {
  onBack: () => void
  onSuccess: (email: string) => void
}

const props = defineProps<ForgotPasswordProps>()

const email = ref('')
const error = ref('')
const success = ref(false)
const isLoading = ref(false)
const attempts = ref(0)
const blockedUntil = ref<number | null>(null)

const validateEmail = (value: string): boolean => {
  if (!value) {
    error.value = 'E-mail é obrigatório'
    return false
  }
  if (value.length > 256) {
    error.value = 'E-mail deve ter no máximo 256 caracteres'
    return false
  }
  const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/
  if (!emailRegex.test(value)) {
    error.value = 'E-mail inválido'
    return false
  }
  error.value = ''
  return true
}

const handleSubmit = async (e: Event) => {
  e.preventDefault()

  // Verifica se está bloqueado
  if (blockedUntil.value && Date.now() < blockedUntil.value) {
    const secondsLeft = Math.ceil((blockedUntil.value - Date.now()) / 1000)
    error.value = `Aguarde ${secondsLeft} segundos antes de tentar novamente`
    return
  }

  if (!validateEmail(email.value)) return

  // Verifica limite de tentativas (3 tentativas a cada 5 minutos)
  if (attempts.value >= 3) {
    const blockTime = Date.now() + 5 * 60 * 1000 // 5 minutos
    blockedUntil.value = blockTime
    error.value = 'Muitas tentativas. Tente novamente em 5 minutos'
    return
  }

  isLoading.value = true
  error.value = ''

  try {
    await api.post('/auth/forgot-password', { email: email.value })
    
    success.value = true
    attempts.value++
    toast.success('Código de recuperação enviado!')

    // Redireciona para a página de reset após 2 segundos
    setTimeout(() => {
      props.onSuccess(email.value)
    }, 2000)
  } catch (err: any) {
    // Mesmo em caso de erro, mostramos sucesso por segurança
    // (não revelamos se o email existe ou não)
    success.value = true
    attempts.value++
    
    setTimeout(() => {
      props.onSuccess(email.value)
    }, 2000)
  } finally {
    isLoading.value = false
  }
}
</script>

<template>
  <div
    v-if="success"
    class="min-h-screen bg-gradient-to-br from-gray-50 to-gray-100 flex items-center justify-center p-4"
  >
    <div class="w-full max-w-md bg-white rounded-lg shadow-xl p-8">
      <div class="text-center">
        <div class="inline-flex p-4 bg-green-100 rounded-full mb-4">
          <CheckCircle class="h-8 w-8 text-green-600" />
        </div>
        <h2 class="text-gray-900 mb-2">E-mail enviado!</h2>
        <p class="text-gray-600 mb-4">
          Se o e-mail <strong>{{ email }}</strong> estiver cadastrado, você receberá
          um código de 6 dígitos para redefinir sua senha.
        </p>
        <p class="text-sm text-gray-500 mb-6">
          O código expira em 30 minutos. Verifique também sua caixa de spam.
        </p>

        <Alert class="mb-6 bg-blue-50 border-blue-200 text-left">
          <Mail class="h-4 w-4 text-blue-600" />
          <AlertDescription class="text-gray-700">
            Você será redirecionado para digitar o código de recuperação...
          </AlertDescription>
        </Alert>

        <Button @click="props.onSuccess(email)" class="w-full">
          Digitar código agora
        </Button>
      </div>
    </div>
  </div>

  <div
    v-else
    class="min-h-screen bg-gradient-to-br from-gray-50 to-gray-100 flex items-center justify-center p-4"
  >
    <div class="w-full max-w-md bg-white rounded-lg shadow-xl p-8">
      <Button
        variant="ghost"
        @click="props.onBack"
        class="mb-6"
      >
        <ArrowLeft class="h-4 w-4" />
        Voltar para login
      </Button>

      <div class="text-center mb-8">
        <div class="inline-flex p-4 bg-blue-100 rounded-full mb-4">
          <Mail class="h-8 w-8 text-blue-600" />
        </div>
        <h2 class="text-gray-900 mb-2">Esqueceu sua senha?</h2>
        <p class="text-gray-600">
          Sem problemas! Digite seu e-mail e enviaremos um código de 6 dígitos para redefinir sua senha.
        </p>
      </div>

      <Alert v-if="error" variant="destructive" class="mb-6">
        <AlertCircle class="h-4 w-4" />
        <AlertDescription>{{ error }}</AlertDescription>
      </Alert>

      <form @submit="handleSubmit" class="space-y-6">
        <div class="space-y-2">
          <Label for="email">E-mail cadastrado</Label>
          <Input
            id="email"
            type="email"
            placeholder="seu@email.com"
            v-model="email"
            @blur="validateEmail(email)"
            :class="error ? 'border-red-500 focus-visible:ring-red-500' : ''"
            maxlength="256"
            :disabled="isLoading"
            autofocus
          />
        </div>

        <Button type="submit" class="w-full" :disabled="isLoading">
          {{ isLoading ? 'Enviando...' : 'Enviar código de recuperação' }}
        </Button>
      </form>

      <div class="mt-6 p-4 bg-gray-50 rounded-lg border border-gray-200">
        <h5 class="text-sm text-gray-900 mb-2">Segurança</h5>
        <ul class="text-sm text-gray-700 space-y-1">
          <li>• O código de recuperação expira em 30 minutos</li>
          <li>• Você pode solicitar no máximo 3 códigos a cada 5 minutos</li>
          <li>• Seu código anterior será invalidado ao solicitar um novo</li>
        </ul>
      </div>
    </div>
  </div>
</template>
