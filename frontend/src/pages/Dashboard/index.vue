<script setup lang="ts">
import { onMounted } from 'vue';
import { useRouter } from 'vue-router';

const router = useRouter();

onMounted(() => {
  const userData = localStorage.getItem('user');
  
  if (!userData) {
    router.push('/login');
    return;
  }
  
  const user = JSON.parse(userData);
  
  // Redirecionar baseado no tipo de usuário
  if (user.type === 'provider') {
    router.push('/freelancer-dashboard');
  } else if (user.type === 'contractor') {
    router.push('/cliente-dashboard');
  } else if (user.type === 'admin') {
    router.push('/admin/categories');
  } else {
    // Fallback para landing page se tipo desconhecido
    router.push('/');
  }
});
</script>

<template>
  <div class="flex items-center justify-center min-h-screen">
    <div class="text-center">
      <div class="animate-spin rounded-full h-12 w-12 border-b-2 border-gray-900 mx-auto"></div>
      <p class="mt-4 text-gray-600">Redirecionando...</p>
    </div>
  </div>
</template>
