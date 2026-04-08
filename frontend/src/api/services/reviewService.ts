import api from '../config/axios';

export interface ReviewData {
  rating: number;
  comment?: string;
  tags?: string[];
}

export interface ReviewResponse {
  id: number;
  serviceRequestId: number;
  serviceName: string;
  reviewerName: string;
  reviewerAvatar?: string;
  reviewerType: 'CONTRACTOR' | 'FREELANCER';
  rating: number;
  comment?: string;
  tags: string[];
  createdAt: string;
}

export interface CanReviewResponse {
  canReview: boolean;
}

export interface PendingReview {
  serviceRequestId: number;
  serviceName: string;
  targetName: string;
  targetAvatar?: string;
  targetType: 'FREELANCER' | 'CONTRACTOR';
  completedAt: string;
}

export interface AiCommentRequest {
  rating: number;
  tags: string[];
  serviceName?: string;
  freelancerName?: string;
  existingComment?: string | null;
}

export interface AiCommentResponse {
  suggestion: string;
}

export interface AiSummaryResponse {
  available: boolean;
  strengths: string[];
  attentionPoints: string[];
  overall: string;
  generatedAt?: string;
}

const reviewService = {
  async createReview(serviceRequestId: string | number, data: ReviewData): Promise<ReviewResponse> {
    const response = await api.post<ReviewResponse>(`/reviews/${serviceRequestId}`, data);
    return response.data;
  },

  async getReviewsForUser(userId: string | number): Promise<ReviewResponse[]> {
    const response = await api.get<ReviewResponse[]>(`/reviews/user/${userId}`);
    return response.data;
  },

  async canReview(serviceRequestId: string | number): Promise<boolean> {
    const response = await api.get<CanReviewResponse>(`/reviews/can-review/${serviceRequestId}`);
    return response.data.canReview;
  },

  async getPendingReviews(): Promise<PendingReview[]> {
    const response = await api.get<PendingReview[]>('/reviews/pending');
    return response.data;
  },

  async getMyReview(serviceRequestId: string | number): Promise<ReviewResponse | null> {
    try {
      const response = await api.get<ReviewResponse>(`/reviews/my-review/${serviceRequestId}`);
      return response.data || null;
    } catch (error: any) {
      if (error.response?.status === 204) {
        return null;
      }
      throw error;
    }
  },

  async suggestComment(data: AiCommentRequest): Promise<AiCommentResponse> {
    const response = await api.post<AiCommentResponse>('/reviews/ai/suggest-comment', data);
    return response.data;
  },

  async getAiSummary(freelancerId: string | number): Promise<AiSummaryResponse> {
    const response = await api.get<AiSummaryResponse>(`/reviews/ai/summary/${freelancerId}`);
    return response.data;
  },
};

export default reviewService;
