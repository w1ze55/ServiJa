import { defineStore } from 'pinia';
import { ref, computed } from 'vue';
import reviewService, { type ReviewData, type ReviewResponse, type PendingReview } from '@/api/services/reviewService';

export const useReviewStore = defineStore('review', () => {
  const reviews = ref<ReviewResponse[]>([]);
  const pendingReviews = ref<PendingReview[]>([]);
  const isLoading = ref(false);
  const isSubmitting = ref(false);
  const pendingLoaded = ref(false);

  const hasPendingReviews = computed(() => pendingReviews.value.length > 0);
  const pendingCount = computed(() => pendingReviews.value.length);

  const submitReview = async (
    serviceRequestId: string | number,
    data: ReviewData
  ): Promise<ReviewResponse | null> => {
    isSubmitting.value = true;
    try {
      const response = await reviewService.createReview(serviceRequestId, data);
      return response;
    } catch (err: any) {
      console.error('Erro ao enviar avaliação:', err);
      throw err;
    } finally {
      isSubmitting.value = false;
    }
  };

  const loadReviews = async (userId: string | number): Promise<void> => {
    isLoading.value = true;
    try {
      reviews.value = await reviewService.getReviewsForUser(userId);
    } catch (err) {
      console.error('Erro ao carregar avaliações:', err);
      reviews.value = [];
    } finally {
      isLoading.value = false;
    }
  };

  const checkCanReview = async (serviceRequestId: string | number): Promise<boolean> => {
    try {
      return await reviewService.canReview(serviceRequestId);
    } catch (err) {
      console.error('Erro ao verificar elegibilidade de avaliação:', err);
      return false;
    }
  };

  const getMyReview = async (serviceRequestId: string | number): Promise<ReviewResponse | null> => {
    try {
      return await reviewService.getMyReview(serviceRequestId);
    } catch (err) {
      console.error('Erro ao buscar avaliação:', err);
      return null;
    }
  };

  const loadPendingReviews = async (): Promise<void> => {
    try {
      pendingReviews.value = await reviewService.getPendingReviews();
      pendingLoaded.value = true;
    } catch (err) {
      console.error('Erro ao carregar avaliações pendentes:', err);
      pendingReviews.value = [];
    }
  };

  const removePendingReview = (serviceRequestId: number) => {
    pendingReviews.value = pendingReviews.value.filter(
      (p) => p.serviceRequestId !== serviceRequestId
    );
  };

  return {
    reviews,
    pendingReviews,
    isLoading,
    isSubmitting,
    pendingLoaded,
    hasPendingReviews,
    pendingCount,
    submitReview,
    loadReviews,
    checkCanReview,
    getMyReview,
    loadPendingReviews,
    removePendingReview,
  };
});
