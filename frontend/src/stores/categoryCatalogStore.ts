import { defineStore } from 'pinia';
import { computed, ref } from 'vue';
import { categoryService, type AdminCategoryItem, type CategoryTreeItem } from '@/api/services/categoryService';

export const useCategoryCatalogStore = defineStore('categoryCatalog', () => {
  const publicCategories = ref<CategoryTreeItem[]>([]);
  const adminCategories = ref<AdminCategoryItem[]>([]);
  const publicLoaded = ref(false);
  const adminLoaded = ref(false);
  const isLoadingPublic = ref(false);
  const isLoadingAdmin = ref(false);

  const activePublicCategories = computed(() => publicCategories.value.filter((category) => category.active));

  const loadPublicCategories = async (force = false) => {
    if (publicLoaded.value && !force) return publicCategories.value;
    isLoadingPublic.value = true;
    try {
      publicCategories.value = await categoryService.getPublicTree();
      publicLoaded.value = true;
      return publicCategories.value;
    } finally {
      isLoadingPublic.value = false;
    }
  };

  const loadAdminCategories = async (force = false) => {
    if (adminLoaded.value && !force) return adminCategories.value;
    isLoadingAdmin.value = true;
    try {
      adminCategories.value = await categoryService.getAdminCategories();
      adminLoaded.value = true;
      return adminCategories.value;
    } finally {
      isLoadingAdmin.value = false;
    }
  };

  const getCategoryById = (categoryId?: number | null) =>
    activePublicCategories.value.find((category) => category.id === categoryId) || null;

  const getSubcategoriesByCategoryId = (categoryId?: number | null) =>
    getCategoryById(categoryId)?.subcategories || [];

  return {
    publicCategories,
    adminCategories,
    activePublicCategories,
    publicLoaded,
    adminLoaded,
    isLoadingPublic,
    isLoadingAdmin,
    loadPublicCategories,
    loadAdminCategories,
    getCategoryById,
    getSubcategoriesByCategoryId,
  };
});
