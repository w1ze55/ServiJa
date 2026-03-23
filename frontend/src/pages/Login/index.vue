<script setup lang="ts">
import { onMounted } from 'vue';
import { useRouter, useRoute } from 'vue-router';
import AuthFlow from '@/components/auth/AuthFlow.vue';
import { toast } from '@/utils/toast';

const router = useRouter();
const route = useRoute();

onMounted(() => {
  if (route.query.expired === 'true') {
    toast.error('Sua sessão expirou. Por favor, faça login novamente.');
    
    // Limpa a URL para não exibir novamente após um refresh manual
    router.replace({ query: {} });
  }
});

const handleLogin = (userData: any) => {
  localStorage.setItem('user', JSON.stringify(userData));
  router.push('/dashboard');
};

const handleBackToLanding = () => {
  router.push('/');
};
</script>

<template>
  <AuthFlow 
    :on-login="handleLogin"
    initial-page="login"
    :on-back-to-landing="handleBackToLanding"
  />
</template>
