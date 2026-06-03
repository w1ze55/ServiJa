package com.servija.servija.service;

import com.servija.servija.model.ServiceSearchWeeklyMetric;
import com.servija.servija.repository.ServiceRepository;
import com.servija.servija.repository.ServiceSearchWeeklyMetricRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.springframework.data.domain.PageRequest.of;

@Service
@RequiredArgsConstructor
public class ServiceSearchWeeklyMetricService {

    private static final int TRACKED_RESULTS_LIMIT = 8;

    private final ServiceSearchWeeklyMetricRepository metricRepository;
    private final ServiceRepository serviceRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void trackSearchResults(List<Long> orderedServiceIds) {
        List<Long> trackedServiceIds = orderedServiceIds == null
                ? List.of()
                : orderedServiceIds.stream()
                        .filter(Objects::nonNull)
                        .collect(Collectors.collectingAndThen(
                                Collectors.toCollection(LinkedHashSet::new),
                                ids -> ids.stream().limit(TRACKED_RESULTS_LIMIT).toList()));

        if (trackedServiceIds.isEmpty()) {
            return;
        }

        LocalDate weekStartDate = getCurrentWeekStartDate();
        Map<Long, ServiceSearchWeeklyMetric> existingMetricsByServiceId = metricRepository
                .findByService_IdInAndWeekStartDate(trackedServiceIds, weekStartDate)
                .stream()
                .collect(Collectors.toMap(metric -> metric.getService().getId(), Function.identity()));

        List<ServiceSearchWeeklyMetric> metricsToPersist = new ArrayList<>();

        for (Long serviceId : trackedServiceIds) {
            ServiceSearchWeeklyMetric metric = existingMetricsByServiceId.get(serviceId);
            if (metric == null) {
                metric = ServiceSearchWeeklyMetric.builder()
                        .service(serviceRepository.getReferenceById(serviceId))
                        .weekStartDate(weekStartDate)
                        .searchCount(0L)
                        .build();
            }

            metric.setSearchCount((metric.getSearchCount() == null ? 0L : metric.getSearchCount()) + 1L);
            metricsToPersist.add(metric);
        }

        metricRepository.saveAll(metricsToPersist);
    }

    @Transactional(readOnly = true)
    public List<Long> getTopServiceIdsForCurrentWeek(int limit) {
        if (limit <= 0) {
            return List.of();
        }

        return metricRepository.findTopServiceIdsByWeekStartDate(getCurrentWeekStartDate(), of(0, limit));
    }

    protected LocalDate getCurrentWeekStartDate() {
        return LocalDate.now().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
    }
}
