import api from '@/api/config/axios';

export interface AdminUserSummary {
  id: number;
  name: string;
  email: string;
  userType: 'FREELANCER' | 'CONTRACTOR' | 'ADMIN';
  active: boolean;
  createdAt: string;
  avatarUrl: string | null;
  kycCompleted: boolean;
  deletedAt: string | null;
}

export interface AdminServiceSummary {
  id: number;
  title: string;
  category: string | null;
  price: number;
  priceType: 'FIXED' | 'HOURLY' | 'DAILY' | null;
  isActive: boolean;
  createdAt: string;
}

export interface AdminUserDetail extends AdminUserSummary {
  phone: string | null;
  cpf: string | null;
  emailVerified: boolean;
  updatedAt: string;
  // FREELANCER only
  bio?: string | null;
  professionalTitle?: string | null;
  rating?: number | null;
  totalReviews?: number | null;
  completedJobs?: number | null;
  activeJobs?: number | null;
  profileCompleted?: boolean;
  category?: string | null;
  services?: AdminServiceSummary[];
  // CONTRACTOR only
  totalContracts?: number | null;
  activeContracts?: number | null;
  completedContracts?: number | null;
  contractorRating?: number | null;
  contractorTotalReviews?: number | null;
}

export interface AdminUsersListResponse {
  content: AdminUserSummary[];
  totalElements: number;
  totalPages: number;
  currentPage: number;
  pageSize: number;
}

export interface AdminUsersListParams {
  search?: string;
  type?: 'CONTRACTOR' | 'FREELANCER' | 'ADMIN' | '';
  status?: 'active' | 'inactive' | '';
  kycStatus?: 'approved' | 'pending' | '';
  dateFrom?: string;
  dateTo?: string;
  page?: number;
  size?: number;
  includeDeleted?: boolean;
}

export interface UpdateUserPayload {
  name?: string;
  email?: string;
  phone?: string;
  bio?: string;
  userType?: string;
}

export const adminUserService = {
  async listUsers(params: AdminUsersListParams = {}): Promise<AdminUsersListResponse> {
    const cleanParams: Record<string, unknown> = {};
    if (params.search) cleanParams.search = params.search;
    if (params.type) cleanParams.type = params.type;
    if (params.status) cleanParams.status = params.status;
    if (params.kycStatus) cleanParams.kycStatus = params.kycStatus;
    if (params.dateFrom) cleanParams.dateFrom = params.dateFrom;
    if (params.dateTo) cleanParams.dateTo = params.dateTo;
    if (params.page !== undefined) cleanParams.page = params.page;
    if (params.size !== undefined) cleanParams.size = params.size;
    if (params.includeDeleted) cleanParams.includeDeleted = params.includeDeleted;

    const response = await api.get<AdminUsersListResponse>('/admin/users', { params: cleanParams });
    return response.data;
  },

  async getUserById(id: number): Promise<AdminUserDetail> {
    const response = await api.get<AdminUserDetail>(`/admin/users/${id}`);
    return response.data;
  },

  async updateUserStatus(id: number, active: boolean): Promise<AdminUserSummary> {
    const response = await api.patch<AdminUserSummary>(`/admin/users/${id}/status`, { active });
    return response.data;
  },

  async updateUser(id: number, data: UpdateUserPayload): Promise<AdminUserDetail> {
    const response = await api.put<AdminUserDetail>(`/admin/users/${id}`, data);
    return response.data;
  },

  async deleteUser(id: number, hard = false): Promise<void> {
    await api.delete(`/admin/users/${id}`, { params: { hard } });
  },

  async resetPassword(id: number): Promise<{ message: string }> {
    const response = await api.post<{ message: string }>(`/admin/users/${id}/reset-password`);
    return response.data;
  },

  async updateKyc(id: number, approved: boolean): Promise<AdminUserDetail> {
    const response = await api.patch<AdminUserDetail>(`/admin/users/${id}/kyc`, { approved });
    return response.data;
  },

};
