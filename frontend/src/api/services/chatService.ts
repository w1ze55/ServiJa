import api from '../config/axios';

export interface ConversationDTO {
  id: string;
  serviceRequestId: number;
  contractorEmail: string;
  providerEmail: string;
  locked: boolean;
  lastMessage: { content: string; senderId: string; sentAt: string } | null;
  unreadCount: Record<string, number>;
  createdAt: string;
  updatedAt: string;
}

export interface MessageDTO {
  id: string;
  conversationId: string;
  senderId: string;
  senderType: 'contractor' | 'provider';
  type: 'text' | 'image' | 'file' | 'system';
  content: string;
  attachmentUrl: string | null;
  status: string;
  sentAt: string;
}

export interface PageResponse<T> {
  content: T[];
  totalPages: number;
  totalElements: number;
  size: number;
  number: number;
  last: boolean;
}

export const chatService = {
  async listConversations(): Promise<ConversationDTO[]> {
    const response = await api.get<ConversationDTO[]>('/chat/conversations');
    return response.data;
  },

  async getOrCreateConversation(serviceRequestId: number): Promise<ConversationDTO> {
    const response = await api.get<ConversationDTO>(`/chat/service-request/${serviceRequestId}`);
    return response.data;
  },

  async getMessages(conversationId: string, page = 0, size = 50): Promise<PageResponse<MessageDTO>> {
    const response = await api.get<PageResponse<MessageDTO>>(
      `/chat/conversations/${conversationId}/messages`,
      { params: { page, size } }
    );
    return response.data;
  },

  async sendMessage(
    conversationId: string,
    content: string,
    type: 'text' | 'image' | 'file' = 'text',
    attachmentUrl?: string
  ): Promise<MessageDTO> {
    const response = await api.post<MessageDTO>(`/chat/conversations/${conversationId}/messages`, {
      content,
      type,
      attachmentUrl: attachmentUrl ?? null,
    });
    return response.data;
  },

  async markAsRead(conversationId: string): Promise<void> {
    await api.put(`/chat/conversations/${conversationId}/read`);
  },
};
