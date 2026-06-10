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

export const categoryService = {
  async getPublicTree(): Promise<CategoryTreeItem[]> {
    const response = await api.get<CategoryTreeResponse>('/categories/public/tree');
    return response.data?.categories || [];
  },
};
