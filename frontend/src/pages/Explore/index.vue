<template>
  <MainLayout
    :user="currentUser"
    :current-page="'explore'"
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
  // Caso especial para provider-profile que usa parâmetro de rota
  if (page === 'provider-profile' && params?.providerId) {
    const pushParams: any = {
      name: 'provider-profile',
      params: { providerId: params.providerId }
    };
    if (params.serviceId) {
      pushParams.query = { serviceId: params.serviceId };
    }
    router.push(pushParams);
    return;
  }

  if (page === 'service-detail' && params?.providerId && params?.serviceId) {
    router.push({
      name: 'service-detail',
      params: {
        providerId: params.providerId,
        serviceId: params.serviceId,
      }
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
