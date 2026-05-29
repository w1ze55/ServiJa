<script setup lang="ts">
import { onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import AuthFlow from '@/components/auth/AuthFlow.vue'
import { toast } from '@/utils/toast'

const router = useRouter()
const route = useRoute()

onMounted(() => {
  if (route.query.expired === 'true') {
    toast.error('Sua sessão expirou. Por favor, faça login novamente.')

    const nextQuery = { ...route.query }
    delete nextQuery.expired
    router.replace({ query: nextQuery })
  }
})

const handleLogin = (userData: any) => {
  localStorage.setItem('user', JSON.stringify(userData))
  router.push('/dashboard')
}

const handleBackToLanding = () => {
  router.push('/')
}
</script>

<template>
  <AuthFlow
    :on-login="handleLogin"
    :reset-token="typeof route.query.resetToken === 'string' ? route.query.resetToken : undefined"
    initial-page="login"
    :on-back-to-landing="handleBackToLanding"
  />
</template>
