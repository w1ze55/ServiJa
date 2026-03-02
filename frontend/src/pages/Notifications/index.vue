<script setup lang="ts">
import { useRouter } from 'vue-router';
import MainLayout from '@/components/layout/MainLayout.vue';
import NotificationsPage from '@/components/pages/NotificationsPage.vue';
import { useNavigation } from '@/utils/navigation';
import { ref, onMounted } from 'vue';

const router = useRouter();
const { navigate, logout } = useNavigation();
const user = ref<any>(null);

onMounted(() => {
  const userData = localStorage.getItem('user');
  if (userData) {
    user.value = JSON.parse(userData);
  } else {
    router.push('/login');
  }
});

const handleNavigate = (page: string, params?: any) => {
  // Tratamento especial para checkout
  if (page === 'checkout') {
    router.push({
      name: 'checkout',
      query: params,
    });
    return;
  }

  // Tratamento especial para outras páginas com params
  if (page === 'service-timeline' || page === 'quote-detail') {
    router.push({
      name: 'cliente-request-detail',
      query: params,
    });
    return;
  }

  if (page === 'chat' || page === 'messages') {
    router.push({
      name: 'messages',
      query: params,
    });
    return;
  }

  if (page === 'contracts') {
    router.push('/contracts');
    return;
  }

  // Fallback para navegação padrão
  const userType = user.value?.type === 'provider' ? 'provider' : 'contractor';
  navigate(page, userType, params);
};

const handleLogout = () => logout();
const handleSearch = (query: string) => router.push({ path: '/search', query: { q: query } });
</script>

<template>
  <MainLayout v-if="user" :user="user" current-page="notifications" :on-navigate="handleNavigate" :on-logout="handleLogout" :on-search="handleSearch">
    <NotificationsPage :on-navigate="handleNavigate" />
  </MainLayout>
</template>
