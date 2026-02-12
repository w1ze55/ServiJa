<script setup lang="ts">
import { useRouter } from 'vue-router';
import MainLayout from '@/components/layout/MainLayout.vue';
import { useNavigation } from '@/utils/navigation';
import { ref, onMounted } from 'vue';
import { MessageSquare } from 'lucide-vue-next';

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
  const userType = user.value?.type === 'provider' ? 'provider' : 'contractor';
  navigate(page, userType, params);
};
const handleLogout = () => logout();
const handleSearch = (query: string) => router.push({ path: '/search', query: { q: query } });
</script>

<template>
  <MainLayout v-if="user" :user="user" current-page="messages" :on-navigate="handleNavigate" :on-logout="handleLogout" :on-search="handleSearch">
    <div class="max-w-4xl mx-auto text-center py-16">
      <div class="inline-flex p-6 bg-blue-100 rounded-full mb-6">
        <MessageSquare class="h-16 w-16 text-blue-600" />
      </div>
      <h1 class="text-3xl font-bold text-gray-900 mb-4">Mensagens</h1>
      <p class="text-gray-600 mb-8">Esta página está em desenvolvimento.</p>
      <p class="text-sm text-gray-500">Em breve você poderá gerenciar suas mensagens aqui.</p>
    </div>
  </MainLayout>
</template>
