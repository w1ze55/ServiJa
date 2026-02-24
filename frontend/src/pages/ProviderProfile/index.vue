<script setup lang="ts">
import { useRouter, useRoute } from 'vue-router';
import ProviderProfilePage from '@/components/pages/ProviderProfilePage.vue';

const router = useRouter();
const route = useRoute();

const providerId = Number(route.params.providerId);

const handleNavigate = (page: string, params?: any) => {
  if (params && params.providerId) {
    router.push({ name: page, params: { providerId: params.providerId } });
  } else {
    router.push({ name: page });
  }
};
</script>

<template>
  <ProviderProfilePage
    :providerId="providerId"
    :onNavigate="handleNavigate"
  />
</template>
