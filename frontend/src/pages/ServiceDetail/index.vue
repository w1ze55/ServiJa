<script setup lang="ts">
import { useRoute } from 'vue-router';
import ServiceDetailPage from '@/components/pages/ServiceDetailPage.vue';

const route = useRoute();

const providerId = Number(route.params.providerId);
const serviceId = Number(route.params.serviceId);
</script>

<template>
  <ServiceDetailPage :providerId="providerId" :serviceId="serviceId" />
</template>
