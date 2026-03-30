export interface ServiceReviewSummary {
  serviceRating?: number | null;
  serviceReviewCount?: number | null;
}

export const hasServiceRating = (service?: Partial<ServiceReviewSummary> | null): boolean =>
  Boolean(service && (service.serviceReviewCount ?? 0) > 0 && service.serviceRating != null);

export const formatServiceRatingDisplay = (service?: Partial<ServiceReviewSummary> | null): string => {
  if (!hasServiceRating(service)) {
    return 'Sem avaliações';
  }

  return `${service!.serviceRating!.toFixed(1)} (${service!.serviceReviewCount})`;
};
