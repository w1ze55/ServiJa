import api from '@/api/config/axios';

export interface CategorySubcategory {
  id: number;
  categoryId: number;
  slug: string;
  name: string;
  sortOrder: number;
  active: boolean;
}

export interface CategoryTreeItem {
  id: number;
  slug: string;
  name: string;
  sortOrder: number;
  active: boolean;
  subcategories: CategorySubcategory[];
}

export interface CategoryTreeResponse {
  categories: CategoryTreeItem[];
}

export interface AdminCategoryItem extends CategoryTreeItem {
  createdAt?: string;
  updatedAt?: string;
}

export interface AdminSubcategoryItem extends CategorySubcategory {
  categorySlug?: string;
  categoryName?: string;
  createdAt?: string;
  updatedAt?: string;
}

export interface CategoryPayload {
  name: string;
  sortOrder: number;
}

export interface SubcategoryPayload extends CategoryPayload {
  categoryId: number;
}

export const categoryService = {
  async getPublicTree(): Promise<CategoryTreeItem[]> {
    const response = await api.get<CategoryTreeResponse>('/categories/public/tree');
    return response.data?.categories || [];
  },

  async getAdminCategories(): Promise<AdminCategoryItem[]> {
    const response = await api.get<AdminCategoryItem[]>('/admin/categories');
    return response.data || [];
  },

  async createCategory(payload: CategoryPayload): Promise<AdminCategoryItem> {
    const response = await api.post<AdminCategoryItem>('/admin/categories', payload);
    return response.data;
  },

  async updateCategory(categoryId: number, payload: CategoryPayload): Promise<AdminCategoryItem> {
    const response = await api.put<AdminCategoryItem>(`/admin/categories/${categoryId}`, payload);
    return response.data;
  },

  async archiveCategory(categoryId: number): Promise<AdminCategoryItem> {
    const response = await api.patch<AdminCategoryItem>(`/admin/categories/${categoryId}/archive`);
    return response.data;
  },

  async restoreCategory(categoryId: number): Promise<AdminCategoryItem> {
    const response = await api.patch<AdminCategoryItem>(`/admin/categories/${categoryId}/restore`);
    return response.data;
  },

  async getAdminSubcategories(categoryId?: number): Promise<AdminSubcategoryItem[]> {
    const response = await api.get<AdminSubcategoryItem[]>('/admin/subcategories', {
      params: categoryId ? { categoryId } : undefined,
    });
    return response.data || [];
  },

  async createSubcategory(payload: SubcategoryPayload): Promise<AdminSubcategoryItem> {
    const response = await api.post<AdminSubcategoryItem>('/admin/subcategories', payload);
    return response.data;
  },

  async updateSubcategory(subcategoryId: number, payload: SubcategoryPayload): Promise<AdminSubcategoryItem> {
    const response = await api.put<AdminSubcategoryItem>(`/admin/subcategories/${subcategoryId}`, payload);
    return response.data;
  },

  async archiveSubcategory(subcategoryId: number): Promise<AdminSubcategoryItem> {
    const response = await api.patch<AdminSubcategoryItem>(`/admin/subcategories/${subcategoryId}/archive`);
    return response.data;
  },

  async restoreSubcategory(subcategoryId: number): Promise<AdminSubcategoryItem> {
    const response = await api.patch<AdminSubcategoryItem>(`/admin/subcategories/${subcategoryId}/restore`);
    return response.data;
  },
};
