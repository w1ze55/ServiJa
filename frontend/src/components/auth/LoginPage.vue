<script setup lang="ts">
import { ref } from 'vue'
import Button from '../ui/button.vue'
import Input from '../ui/input.vue'
import Label from '../ui/label.vue'
import Alert from '../ui/alert.vue'
import AlertDescription from '../ui/alert-description.vue'
import { AlertCircle, Eye, EyeOff, ArrowLeft } from 'lucide-vue-next'

interface LoginPageProps {
  onLogin: (email: string, password: string) => void
  onForgotPassword: () => void
  onSignUp: () => void
  onBack?: () => void
  onBackToLanding?: () => void
}

const props = defineProps<LoginPageProps>()

const email = ref('')
const password = ref('')
const showPassword = ref(false)
const errors = ref<{ email?: string; password?: string; general?: string }>({})
const isLoading = ref(false)

const validateEmail = (value: string): boolean => {
  if (!value) {
    errors.value.email = 'E-mail é obrigatório'
    return false
  }
  if (value.length > 256) {
    errors.value.email = 'E-mail deve ter no máximo 256 caracteres'
    return false
  }
  const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/
  if (!emailRegex.test(value)) {
    errors.value.email = 'E-mail inválido'
    return false
  }
  errors.value.email = undefined
  return true
}

const validatePassword = (value: string): boolean => {
  if (!value) {
    errors.value.password = 'Senha é obrigatória'
    return false
  }
  if (value.length > 256) {
    errors.value.password = 'Senha deve ter no máximo 256 caracteres'
    return false
  }
  errors.value.password = undefined
  return true
}

const handleSubmit = async (e: Event) => {
  e.preventDefault()
  errors.value = {}

  const isEmailValid = validateEmail(email.value)
  const isPasswordValid = validatePassword(password.value)

  if (!isEmailValid || !isPasswordValid) return

  isLoading.value = true

  setTimeout(() => {
    isLoading.value = false
    props.onLogin(email.value, password.value)
  }, 1000)
}
</script>

<template>
  <div class="login-page-container">
    <div class="login-page-card">
      <div class="login-page-header">
        <h1 class="login-page-logo">Servi Já</h1>
        <h2 class="login-page-title">Bem-vindo de volta!</h2>
        <p class="login-page-subtitle">
          Entre com suas credenciais para acessar sua conta
        </p>
      </div>

      <Alert v-if="errors.general" variant="destructive" class="mb-6">
        <AlertCircle class="h-4 w-4" />
        <AlertDescription>{{ errors.general }}</AlertDescription>
      </Alert>

      <form @submit="handleSubmit" class="login-page-form">
        <!-- E-mail -->
        <div class="login-page-field">
          <Label for="email">E-mail *</Label>
          <Input
            id="email"
            type="email"
            placeholder="seu@email.com"
            v-model="email"
            @input="errors.email && validateEmail(email)"
            @blur="validateEmail(email)"
            :class="errors.email ? 'login-page-input-error' : ''"
            maxlength="256"
            :disabled="isLoading"
          />
          <p v-if="errors.email" class="login-page-error">
            <AlertCircle class="h-3 w-3" />
            {{ errors.email }}
          </p>
        </div>

        <!-- Senha -->
        <div class="login-page-field">
          <Label for="password">Senha *</Label>
          <div class="login-page-password-wrapper">
            <Input
              id="password"
              :type="showPassword ? 'text' : 'password'"
              placeholder="Digite sua senha"
              v-model="password"
              @input="errors.password && validatePassword(password)"
              @blur="validatePassword(password)"
              :class="[
                'pr-10',
                errors.password ? 'login-page-input-error' : ''
              ]"
              maxlength="256"
              :disabled="isLoading"
            />
            <button
              type="button"
              @click="showPassword = !showPassword"
              class="login-page-password-toggle"
              tabindex="-1"
            >
              <component :is="showPassword ? EyeOff : Eye" class="h-4 w-4" />
            </button>
          </div>
          <p v-if="errors.password" class="login-page-error">
            <AlertCircle class="h-3 w-3" />
            {{ errors.password }}
          </p>
        </div>

        <!-- Esqueci senha -->
        <div class="login-page-forgot-password-wrapper">
          <button
            type="button"
            @click="props.onForgotPassword"
            class="login-page-forgot-password"
          >
            Esqueci minha senha
          </button>
        </div>

        <!-- Entrar -->
        <Button type="submit" class="login-page-submit-button" :disabled="isLoading">
          {{ isLoading ? 'Entrando...' : 'Entrar' }}
        </Button>
      </form>

      <!-- Cadastro -->
      <div class="login-page-signup-wrapper">
        <p class="login-page-signup-text">
          Não tem uma conta?
          <button
            @click="props.onSignUp"
            class="login-page-signup-button"
          >
            Cadastre-se
          </button>
        </p>
      </div>

      <!-- Voltar -->
      <div v-if="props.onBack" class="login-page-back-wrapper">
        <button
          @click="props.onBack"
          class="login-page-back-button"
        >
          <ArrowLeft class="h-4 w-4" />
          Voltar para o início
        </button>
      </div>

      <div v-if="props.onBackToLanding" class="login-page-back-wrapper">
        <button
          @click="props.onBackToLanding"
          class="login-page-back-button"
        >
          <ArrowLeft class="h-4 w-4" />
          Voltar para a página inicial
        </button>
      </div>
    </div>
  </div>
</template>

<style scoped>
@import '@/styles/auth/login-page.css';
</style>
