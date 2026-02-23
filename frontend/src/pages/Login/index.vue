<script setup lang="ts">
import { useRouter } from 'vue-router';
import AuthFlow from '@/components/auth/AuthFlow.vue';

const router = useRouter();

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
