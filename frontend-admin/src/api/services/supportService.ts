import api from '@/api/config/axios';
import type { SupportAttachment, SupportCategoryValue, SupportStatusValue } from '@/constants/support';

export interface SupportRequest {
  id: number;
  userId: number;
  userName: string;
  userEmail: string;
  category: SupportCategoryValue;
  description: string;
  attachments: SupportAttachment[];
  status: SupportStatusValue;
  adminResponse?: string | null;
  adminUserId?: number | null;
  adminName?: string | null;
  createdAt: string;
  updatedAt: string;
  answeredAt?: string | null;
}

export const supportService = {
  async getSupportRequests(): Promise<SupportRequest[]> {
    const response = await api.get<SupportRequest[]>('/admin/support-requests');
    return response.data || [];
  },

  async getSupportRequestById(id: number | string): Promise<SupportRequest> {
    const response = await api.get<SupportRequest>(`/admin/support-requests/${id}`);
    return response.data;
  },

  async approveSupportRequest(id: number | string, responseMessage: string): Promise<SupportRequest> {
    const response = await api.patch<SupportRequest>(`/admin/support-requests/${id}/approve`, {
      responseMessage,
    });
    return response.data;
  },

  async rejectSupportRequest(id: number | string, responseMessage: string): Promise<SupportRequest> {
    const response = await api.patch<SupportRequest>(`/admin/support-requests/${id}/reject`, {
      responseMessage,
    });
    return response.data;
  },
};
