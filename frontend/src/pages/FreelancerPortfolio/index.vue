<script setup lang="ts">
import { ref, onMounted } from 'vue';
import { useRouter } from 'vue-router';
import MainLayout from '@/components/layout/MainLayout.vue';
import FreelancerPortfolioPage from '@/components/freelancer/FreelancerPortfolioPage.vue';
import { useNavigation } from '@/utils/navigation';

const router = useRouter();
const { navigate, logout } = useNavigation();
const user = ref<any>(null);

onMounted(() => {
  const userData = localStorage.getItem('user');
  if (userData) {
    user.value = JSON.parse(userData);
    // Verificar se é realmente um freelancer
    if (user.value.type !== 'provider') {
      router.push('/cliente-dashboard');
    }
  } else {
    router.push('/login');
  }
});

const handleNavigate = (page: string, params?: any) => {
  navigate(page, 'provider', params);
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
    current-page="freelancer-portfolio"
    :on-navigate="handleNavigate"
    :on-logout="handleLogout"
    :on-search="handleSearch"
  >
    <FreelancerPortfolioPage :on-navigate="handleNavigate" />
  </MainLayout>
</template>
