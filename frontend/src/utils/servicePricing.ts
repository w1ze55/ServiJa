export interface ServicePriceRangeData {
  price: number;
  priceType: string;
  completedServicesCount?: number | null;
  averageCompletedPrice?: number | null;
  averagePriceMin?: number | null;
  averagePriceMax?: number | null;
}

export const formatCurrency = (price: number) =>
  new Intl.NumberFormat('pt-BR', { style: 'currency', currency: 'BRL' }).format(price);

export const getPriceTypeSuffix = (priceType: string) =>
  priceType === 'HOURLY' ? '/hora' : priceType === 'DAILY' ? '/dia' : '';

export const formatBaseServicePrice = (price: number, priceType: string) =>
  `${formatCurrency(price)}${getPriceTypeSuffix(priceType)}`;

const roundAverageRangeValue = (value: number) => {
  const normalized = (value + 0.1) / 10;
  const lowerCandidate = (Math.floor(normalized) * 10) - 0.1;
  const upperCandidate = (Math.ceil(normalized) * 10) - 0.1;

  if (lowerCandidate <= 0) {
    return Number(upperCandidate.toFixed(2));
  }

  const nearestCandidate = Math.abs(value - lowerCandidate) <= Math.abs(upperCandidate - value)
    ? lowerCandidate
    : upperCandidate;

  return Number(nearestCandidate.toFixed(2));
};

export const formatAveragePriceRange = (min: number, max: number) => {
  const roundedMin = roundAverageRangeValue(min);
  const roundedMax = Math.max(roundAverageRangeValue(max), roundedMin);
  return `${formatCurrency(roundedMin)} - ${formatCurrency(roundedMax)}`;
};

export const hasAveragePriceRange = (service?: Partial<ServicePriceRangeData> | null): boolean =>
  Boolean(
    service
    && (service.completedServicesCount ?? 0) > 0
    && service.averagePriceMin != null
    && service.averagePriceMax != null
  );

export const getServicePriceDisplay = (service: ServicePriceRangeData): string => {
  if (!hasAveragePriceRange(service)) {
    return formatBaseServicePrice(service.price, service.priceType);
  }

  return formatAveragePriceRange(service.averagePriceMin!, service.averagePriceMax!);
};

export const getServicePriceSortValue = (service: ServicePriceRangeData): number =>
  service.averageCompletedPrice ?? service.price;
