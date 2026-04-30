<script setup lang="ts">
import { ref, onMounted, computed } from 'vue';
import { useRouter } from 'vue-router';
import { AlertTriangle, LogOut } from 'lucide-vue-next';
import { useUserStore } from '@/stores/userStore';

const router = useRouter();
const userStore = useUserStore();

const adminToken = ref<string | null>(null);
const impersonatedName = ref<string | null>(null);

const isImpersonating = computed(() => !!adminToken.value);

const checkImpersonation = () => {
  adminToken.value = localStorage.getItem('adminToken');
  impersonatedName.value = localStorage.getItem('impersonatedUserName');
};

onMounted(() => {
  checkImpersonation();
  window.addEventListener('storage', checkImpersonation);
});

const exitImpersonation = () => {
  const originalToken = localStorage.getItem('adminToken');
  const originalUser = localStorage.getItem('adminUser');

  if (originalToken) {
    localStorage.setItem('token', originalToken);
    localStorage.removeItem('adminToken');
    localStorage.removeItem('impersonatedUserName');

    // Restaura dados do admin no store e localStorage
    if (originalUser) {
      try {
        userStore.setUser(JSON.parse(originalUser));
      } catch {
        userStore.clearUser();
      }
      localStorage.removeItem('adminUser');
    }

    adminToken.value = null;
    impersonatedName.value = null;
    router.push('/admin/users');
  }
};
</script>

<template>
  <Transition name="banner">
    <div
      v-if="isImpersonating"
      class="fixed left-0 right-0 top-0 z-[100] flex items-center justify-between gap-4 bg-amber-500 px-4 py-2.5 shadow-md"
    >
      <div class="flex items-center gap-2 text-white">
        <AlertTriangle class="h-4 w-4 shrink-0" />
        <span class="text-sm font-medium">
          Você está visualizando como
          <strong>{{ impersonatedName || 'outro usuário' }}</strong>
        </span>
      </div>
      <button
        class="flex items-center gap-1.5 rounded-lg bg-white/20 px-3 py-1.5 text-sm font-medium text-white transition hover:bg-white/30"
        @click="exitImpersonation"
      >
        <LogOut class="h-4 w-4" />
        Sair da impersonação
      </button>
    </div>
  </Transition>
</template>

<style scoped>
.banner-enter-active,
.banner-leave-active {
  transition: transform 0.2s ease, opacity 0.2s ease;
}
.banner-enter-from,
.banner-leave-to {
  transform: translateY(-100%);
  opacity: 0;
}
</style>
