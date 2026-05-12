<script setup lang="ts">
import { onMounted } from 'vue';
import { useRoute } from 'vue-router';
import PoliticasTermos from '@/components/PoliticasTermos.vue';

const route = useRoute();

onMounted(() => {
  if (route.query.tab === 'termos') {
    // The component will handle tab state; pass it via URL if needed
  }
});
</script>

<template>
  <PoliticasTermos />
</template>
