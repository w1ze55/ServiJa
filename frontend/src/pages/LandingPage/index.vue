<script setup lang="ts">
import { useRouter } from 'vue-router';
import LandingPage from '@/components/LandingPage.vue';

const router = useRouter();

const handleLogin = () => {
  router.push('/login');
};

const handleSignup = () => {
  router.push('/login');
};
</script>

<template>
  <LandingPage :on-login="handleLogin" :on-signup="handleSignup" />
</template>
