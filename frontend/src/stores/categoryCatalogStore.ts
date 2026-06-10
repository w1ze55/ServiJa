import { defineStore } from 'pinia';
import { computed, ref } from 'vue';
import { categoryService, type CategoryTreeItem } from '@/api/services/categoryService';

export const useCategoryCatalogStore = defineStore('categoryCatalog', () => {
  const publicCategories = ref<CategoryTreeItem[]>([]);
  const publicLoaded = ref(false);
  const isLoadingPublic = ref(false);

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

  const getCategoryById = (categoryId?: number | null) =>
    activePublicCategories.value.find((category) => category.id === categoryId) || null;

  const getSubcategoriesByCategoryId = (categoryId?: number | null) =>
    getCategoryById(categoryId)?.subcategories || [];

  return {
    publicCategories,
    activePublicCategories,
    publicLoaded,
    isLoadingPublic,
    loadPublicCategories,
    getCategoryById,
    getSubcategoriesByCategoryId,
  };
});
