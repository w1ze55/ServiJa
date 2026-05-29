import { defineStore } from 'pinia';
import { ref } from 'vue';
import {
  adminUserService,
  type AdminUserSummary,
  type AdminUserDetail,
  type AdminUsersListParams,
} from '@/api/services/adminUserService';

export const useAdminUserStore = defineStore('adminUser', () => {
  const users = ref<AdminUserSummary[]>([]);
  const totalElements = ref(0);
  const totalPages = ref(0);
  const currentPage = ref(0);
  const isLoading = ref(false);
  const selectedUser = ref<AdminUserDetail | null>(null);
  const filters = ref<AdminUsersListParams>({
    search: '',
    type: '',
    status: '',
    dateFrom: '',
    dateTo: '',
    page: 0,
    size: 25,
    includeDeleted: false,
  });

  const loadUsers = async (params?: AdminUsersListParams) => {
    isLoading.value = true;
    try {
      const mergedParams = { ...filters.value, ...params };
      const response = await adminUserService.listUsers(mergedParams);
      users.value = response.content;
      totalElements.value = response.totalElements;
      totalPages.value = response.totalPages;
      currentPage.value = response.currentPage;
    } finally {
      isLoading.value = false;
    }
  };

  const loadUserById = async (id: number) => {
    isLoading.value = true;
    try {
      selectedUser.value = await adminUserService.getUserById(id);
    } finally {
      isLoading.value = false;
    }
  };

  const clearSelectedUser = () => {
    selectedUser.value = null;
  };

  return {
    users,
    totalElements,
    totalPages,
    currentPage,
    isLoading,
    selectedUser,
    filters,
    loadUsers,
    loadUserById,
    clearSelectedUser,
  };
});
