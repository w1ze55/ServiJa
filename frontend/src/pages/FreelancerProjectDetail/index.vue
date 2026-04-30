<script setup lang="ts">
import { computed, onMounted, ref } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import MainLayout from '@/components/layout/MainLayout.vue';
import FreelancerProjectDetailPage from '@/components/freelancer/FreelancerProjectDetailPage.vue';
import { useNavigation } from '@/utils/navigation';

const route = useRoute();
const router = useRouter();
const { navigate, logout } = useNavigation();
const user = ref<any>(null);

const requestId = computed(() => route.query.requestId as string || route.params.requestId as string);

onMounted(() => {
  const userData = localStorage.getItem('user');
  if (userData) {
    user.value = JSON.parse(userData);
    if (user.value.type !== 'provider') router.push('/cliente-home');
  } else {
    router.push('/login');
  }
});

const handleNavigate = (page: string, params?: any) => navigate(page, 'provider', params);
const handleLogout = () => logout();
const handleSearch = (query: string) => router.push({ path: '/search', query: { q: query } });
</script>

<template>
  <MainLayout
    v-if="user"
    :user="user"
    current-page="freelancer-deliveries"
    :on-navigate="handleNavigate"
    :on-logout="handleLogout"
    :on-search="handleSearch"
  >
    <FreelancerProjectDetailPage
      :on-navigate="handleNavigate"
      :request-id="requestId"
    />
  </MainLayout>
</template>
