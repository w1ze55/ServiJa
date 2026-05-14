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

export interface CreateSupportRequestPayload {
  userId?: number;
  userName?: string;
  userEmail?: string;
  supportCategory: SupportCategoryValue;
  description: string;
  attachments?: SupportAttachment[];
}

export const supportService = {
  async createSupportRequest(payload: CreateSupportRequestPayload): Promise<SupportRequest> {
    const response = await api.post<SupportRequest>('/support-requests', payload);
    return response.data;
  },

  async getMySupportRequests(): Promise<SupportRequest[]> {
    const response = await api.get<SupportRequest[]>('/support-requests/my');
    return response.data || [];
  },

  async getSupportRequestById(id: number | string): Promise<SupportRequest> {
    const response = await api.get<SupportRequest>(`/support-requests/${id}`);
    return response.data;
  },
};
