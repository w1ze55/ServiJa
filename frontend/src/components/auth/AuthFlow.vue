<template>
  <component
    :is="currentComponent"
    v-bind="currentProps"
  />
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import AuthCard from './AuthCard.vue'
import EmailVerification from './EmailVerification.vue'
import ForgotPassword from './ForgotPassword.vue'
import ResetPassword from './ResetPassword.vue'
import type { PersonalData } from './SignupPersonalData.vue'
import type { User } from '@/types'
import api from '@/api/config/axios'

type AuthPage =
  | 'auth-card'
  | 'email-verification'
  | 'forgot-password'
  | 'reset-password'

interface AuthFlowProps {
  onLogin: (user: User) => void
  resetToken?: string
  initialPage?: 'login' | 'signup'
  onBackToLanding?: () => void
}

const props = defineProps<AuthFlowProps>()

// estado principal
const currentPage = ref<AuthPage>(
  props.resetToken ? 'reset-password' : 'auth-card'
)
const accountType = ref<'contractor' | 'provider' | null>(null)
const personalData = ref<PersonalData | null>(null)
const recoveryEmail = ref('')

// Armazena o callback de erro para comunicação com AuthCard
let loginErrorCallback: ((error: string) => void) | null = null

import { useUserStore } from '@/stores/userStore'

const userStore = useUserStore()

async function handleLogin(email: string, password: string) {
  try {
    const response = await api.post('/auth/login', { email, password })
    
    const userData = response.data
    
    // Salvar token no localStorage
    if (userData.token) {
      localStorage.setItem('token', userData.token)
    }
    
    // Mapear tipo de usuário da API para o formato do frontend
    const apiUserType = userData.user?.userType
    const userType = apiUserType === 'ADMIN'
      ? 'admin'
      : apiUserType === 'FREELANCER'
        ? 'provider'
        : 'contractor'
    
    const user: User = {
      id: userData.user?.id?.toString() || '',
      name: userData.user?.name || '',
      email: userData.user?.email || email,
      type: userType,
      avatar: userData.user?.avatarUrl || undefined,
    }
    
    // Atualizar store global
    userStore.setUser(user)
    
    props.onLogin(user)
  } catch (error: any) {
    const errorMessage = error.response?.data?.message || 
                         error.response?.data?.error || 
                         'Email ou senha incorretos'
    if (loginErrorCallback) {
      loginErrorCallback(errorMessage)
    }
  }
}

// Wrapper que permite passar callback de erro
function handleLoginWithError(email: string, password: string, onError?: (error: string) => void) {
  loginErrorCallback = onError || null
  handleLogin(email, password)
}

function handleSignupComplete(
  type: 'contractor' | 'provider',
  data: PersonalData
) {
  accountType.value = type
  personalData.value = data
  currentPage.value = 'email-verification'
}

function handleEmailVerified(token: string, userData: any) {
  // Salvar token no localStorage
  if (token) {
    localStorage.setItem('token', token)
  }
  
  // Mapear tipo de usuário da API para o formato do frontend
  const userType = userData.userType === 'ADMIN'
    ? 'admin'
    : userData.userType === 'FREELANCER'
      ? 'provider'
      : 'contractor'
  
  const user: User = {
    id: userData.id?.toString() || '',
    name: userData.name || '',
    email: userData.email || '',
    type: userType,
    avatar: userData.avatarUrl || undefined,
  }
  
  // Atualizar store global
  userStore.setUser(user)
  
  props.onLogin(user)
}

function handleForgotPasswordSuccess(email: string) {
  recoveryEmail.value = email
  // Ir para a página de reset de senha com código
  currentPage.value = 'reset-password'
}

function handleResetPasswordSuccess() {
  currentPage.value = 'auth-card'
}

function handleResetPasswordBack() {
  currentPage.value = 'forgot-password'
}

// --- renderização dinâmica ---
const currentComponent = computed(() => {
  switch (currentPage.value) {
    case 'auth-card':
      return AuthCard
    case 'email-verification':
      return EmailVerification
    case 'forgot-password':
      return ForgotPassword
    case 'reset-password':
      return ResetPassword
    default:
      return AuthCard
  }
})

const currentProps = computed(() => {
  switch (currentPage.value) {
    case 'auth-card':
      return {
        onLogin: handleLoginWithError,
        onForgotPassword: () => (currentPage.value = 'forgot-password'),
        onSignupComplete: handleSignupComplete,
        onBackToLanding: props.onBackToLanding,
        initialPage: props.initialPage,
      }

    case 'email-verification':
      return {
        email: personalData.value?.email || '',
        onVerified: handleEmailVerified,
      }

    case 'forgot-password':
      return {
        onBack: () => (currentPage.value = 'auth-card'),
        onSuccess: handleForgotPasswordSuccess,
      }

    case 'reset-password':
      return {
        email: recoveryEmail.value,
        onSuccess: handleResetPasswordSuccess,
        onBack: handleResetPasswordBack,
      }

    default:
      return {
        onLogin: handleLogin,
        onForgotPassword: () => (currentPage.value = 'forgot-password'),
        onSignupComplete: handleSignupComplete,
        onBackToLanding: props.onBackToLanding,
        initialPage: props.initialPage,
      }
  }
})
</script>
