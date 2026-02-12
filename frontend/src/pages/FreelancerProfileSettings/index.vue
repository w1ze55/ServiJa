<script setup lang="ts">
import { ref, onMounted } from 'vue';
import { useRouter } from 'vue-router';
import MainLayout from '@/components/layout/MainLayout.vue';
import FreelancerProfileSettings from '@/components/freelancer/FreelancerProfileSettings.vue';
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
      router.push('/cliente-profile-settings');
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
    current-page="freelancer-profile-settings"
    :on-navigate="handleNavigate"
    :on-logout="handleLogout"
    :on-search="handleSearch"
  >
    <FreelancerProfileSettings :on-navigate="handleNavigate" />
  </MainLayout>
</template>

