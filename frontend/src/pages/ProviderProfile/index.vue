<script setup lang="ts">
import { useRouter, useRoute } from 'vue-router';
import ProviderProfilePage from '@/components/pages/ProviderProfilePage.vue';

const router = useRouter();
const route = useRoute();

const providerId = Number(route.params.providerId);

const handleNavigate = (page: string, params?: any) => {
  if (params && params.providerId) {
    const pushParams: any = { 
      name: page, 
      params: { providerId: params.providerId } 
    };
    
    if (params.serviceId) {
      pushParams.query = { serviceId: params.serviceId };
    }
    
    router.push(pushParams);
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
