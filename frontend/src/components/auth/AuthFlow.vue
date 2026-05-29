<template>
  <component
    :is="currentComponent"
    v-bind="currentProps"
  />
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import AuthCard from './AuthCard.vue'
import EmailVerification from './EmailVerification.vue'
import ForgotPassword from './ForgotPassword.vue'
import ResetPassword from './ResetPassword.vue'
import TwoFactorChallenge from './TwoFactorChallenge.vue'
import type { PersonalData } from './SignupPersonalData.vue'
import type { User } from '@/types'
import api from '@/api/config/axios'
import { useUserStore } from '@/stores/userStore'
import { buildUserFromApi } from '@/utils/userRoles'

type AuthPage =
  | 'auth-card'
  | 'email-verification'
  | 'forgot-password'
  | 'reset-password'
  | 'two-factor-challenge'

interface AuthFlowProps {
  onLogin: (user: User) => void
  resetToken?: string
  initialPage?: 'login' | 'signup'
  onBackToLanding?: () => void
}

const props = defineProps<AuthFlowProps>()
const hasResetToken = computed(() => Boolean(props.resetToken?.trim()))

const currentPage = ref<AuthPage>(
  hasResetToken.value ? 'reset-password' : 'auth-card'
)
const accountType = ref<'contractor' | 'provider' | null>(null)
const personalData = ref<PersonalData | null>(null)
const recoveryEmail = ref('')

let loginErrorCallback: ((error: string) => void) | null = null

const userStore = useUserStore()
const pendingChallengeToken = ref<string | null>(null)

function finalizeLogin(userData: any, fallbackEmail = '') {
  if (userData.token) {
    localStorage.setItem('token', userData.token)
  }

  const user = buildUserFromApi(userData, userData.token, fallbackEmail)
  userStore.setUser(user)
  props.onLogin(user)
}

async function handleLogin(email: string, password: string) {
  try {
    const response = await api.post('/auth/login', { email, password })
    const userData = response.data

    if (userData.requires2fa && userData.token) {
      pendingChallengeToken.value = userData.token
      currentPage.value = 'two-factor-challenge'
      return
    }

    finalizeLogin(userData, email)
  } catch (error: any) {
    const errorMessage = error.response?.data?.message ||
      error.response?.data?.error ||
      'Email ou senha incorretos'

    if (loginErrorCallback) {
      loginErrorCallback(errorMessage)
    }
  }
}

function handleTwoFactorVerified(data: any) {
  pendingChallengeToken.value = null
  finalizeLogin(data)
}

function handleTwoFactorBack() {
  pendingChallengeToken.value = null
  currentPage.value = 'auth-card'
}

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
  if (token) {
    localStorage.setItem('token', token)
  }

  const user = buildUserFromApi(userData, token)
  userStore.setUser(user)
  props.onLogin(user)
}

function handleForgotPasswordSuccess(email: string) {
  recoveryEmail.value = email
  currentPage.value = 'reset-password'
}

function handleResetPasswordSuccess() {
  currentPage.value = 'auth-card'
}

function handleResetPasswordBack() {
  currentPage.value = hasResetToken.value ? 'auth-card' : 'forgot-password'
}

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
    case 'two-factor-challenge':
      return TwoFactorChallenge
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
        resetToken: props.resetToken,
        onSuccess: handleResetPasswordSuccess,
        onBack: handleResetPasswordBack,
      }

    case 'two-factor-challenge':
      return {
        challengeToken: pendingChallengeToken.value || '',
        onVerified: handleTwoFactorVerified,
        onBack: handleTwoFactorBack,
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
