<script setup lang="ts">
import { ref, onMounted } from 'vue';
import { useRouter } from 'vue-router';
import MainLayout from '@/components/layout/MainLayout.vue';
import ContractorDashboard from '@/components/ContractorDashboard.vue';
import { useNavigation } from '@/utils/navigation';

const router = useRouter();
const { navigate, logout } = useNavigation();
const user = ref<any>(null);

onMounted(() => {
  const userData = localStorage.getItem('user');
  if (userData) {
    user.value = JSON.parse(userData);
    
    // Verificar se é realmente um cliente
    if (user.value.type !== 'contractor') {
      router.push('/freelancer-dashboard');
    }
  } else {
    router.push('/login');
  }
});

const handleNavigate = (page: string, params?: any) => {
  navigate(page, 'contractor', params);
};

const handleLogout = () => {
  logout();
};

const handleSearch = (query: string) => {
  router.push({ path: '/search', query: { q: query } });
};
</script>

<template>
  <MainLayout
    v-if="user"
    :user="user"
    current-page="contractor-dashboard"
    :on-navigate="handleNavigate"
    :on-logout="handleLogout"
    :on-search="handleSearch"
  >
    <ContractorDashboard :on-navigate="handleNavigate" :user="user" />
  </MainLayout>
</template>

