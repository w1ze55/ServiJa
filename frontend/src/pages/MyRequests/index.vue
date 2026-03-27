<script setup lang="ts">
import { useRouter } from 'vue-router';
import MainLayout from '@/components/layout/MainLayout.vue';
import ClienteRequestsPage from '@/components/cliente/ClienteRequestsPage.vue';
import { useNavigation } from '@/utils/navigation';
import { ref, onMounted } from 'vue';

const router = useRouter();
const { navigate, logout } = useNavigation();
const user = ref<any>(null);

onMounted(() => {
  const userData = localStorage.getItem('user');
  if (userData) {
    user.value = JSON.parse(userData);

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
    current-page="requests"
    :on-navigate="handleNavigate"
    :on-logout="handleLogout"
    :on-search="handleSearch"
  >
    <ClienteRequestsPage :on-navigate="handleNavigate" />
  </MainLayout>
</template>
