<script setup lang="ts">
import ImpersonationBanner from '@/components/ImpersonationBanner.vue';
</script>

<template>
  <div class="min-h-screen">
    <ImpersonationBanner />
    <router-view />
  </div>
</template>

<style scoped>
/* Estilos globais podem ser adicionados aqui se necessário */
</style>
