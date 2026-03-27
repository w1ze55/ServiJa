<template>
  <MainLayout
    :user="currentUser"
    :current-page="'search'"
    :on-navigate="handleNavigate"
    :on-logout="handleLogout"
  >
    <ExplorePage :on-navigate="handleNavigate" />
  </MainLayout>
</template>

<script setup lang="ts">
import { computed } from 'vue';
import { useRouter } from 'vue-router';
import MainLayout from '@/components/layout/MainLayout.vue';
import ExplorePage from '@/components/pages/ExplorePage.vue';
import { useUserStore } from '@/stores/userStore';
import { useNavigation, getRoute } from '@/utils/navigation';

const router = useRouter();
const userStore = useUserStore();
const { logout } = useNavigation();

const currentUser = computed(() => {
  const user = userStore.user;
  return {
    id: user?.id?.toString() || '',
    name: user?.name || 'Usuário',
    email: user?.email || '',
    type: (user?.type === 'provider' ? 'provider' : 'contractor') as 'provider' | 'contractor',
    avatar: user?.avatar || undefined,
  };
});

const handleNavigate = (page: string, params?: Record<string, any>) => {
  if (page === 'provider-profile' && params?.providerId) {
    router.push({
      name: 'provider-profile',
      params: { providerId: params.providerId },
      query: params.serviceId ? { serviceId: params.serviceId } : {}
    });
    return;
  }

  const route = getRoute(page, currentUser.value.type);
  if (params) {
    router.push({ path: route, query: params });
  } else {
    router.push(route);
  }
};

const handleLogout = () => {
  logout();
};
</script>
