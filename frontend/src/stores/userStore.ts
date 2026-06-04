import { computed, ref } from 'vue';
import { defineStore } from 'pinia';
import type { User, UserRole } from '@/types';
import { normalizeStoredUser } from '@/utils/userRoles';

export const useUserStore = defineStore('user', () => {
  const user = ref<User | null>(null);

  const persistUser = (nextUser: User | null) => {
    user.value = nextUser;

    if (nextUser) {
      localStorage.setItem('user', JSON.stringify(nextUser));
      if (nextUser.token) {
        localStorage.setItem('token', nextUser.token);
      }
      return;
    }

    localStorage.removeItem('user');
  };

  const init = () => {
    const storedUser = localStorage.getItem('user');
    if (!storedUser) {
      user.value = null;
      return;
    }

    try {
      const parsedUser = JSON.parse(storedUser);
      const normalizedUser = normalizeStoredUser(parsedUser);
      persistUser(normalizedUser);
    } catch (error) {
      console.error('Erro ao parsear usuario do localStorage:', error);
      user.value = null;
      localStorage.removeItem('user');
    }
  };

  const setUser = (userData: User | null) => {
    const normalizedUser = normalizeStoredUser(userData);
    persistUser(normalizedUser);
  };

  const updateUser = (data: Partial<User>) => {
    if (!user.value) {
      return;
    }

    setUser({
      ...user.value,
      ...data,
    });
  };

  const clearUser = () => {
    user.value = null;
    localStorage.removeItem('user');
    localStorage.removeItem('token');
  };

  const hasRole = (role: UserRole) => user.value?.roles.includes(role) ?? false;
  const switchableRoles = computed(() =>
    (user.value?.roles || []).filter((role) => role !== 'admin'),
  );

  init();

  return {
    user,
    switchableRoles,
    setUser,
    updateUser,
    clearUser,
    hasRole,
    init,
  };
});
