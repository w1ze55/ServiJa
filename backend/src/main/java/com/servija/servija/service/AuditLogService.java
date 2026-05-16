package com.servija.servija.service;

import com.servija.servija.dto.AuditLogDto;
import com.servija.servija.dto.AuditLogPageResponse;
import com.servija.servija.model.AuditLog;
import com.servija.servija.repository.AuditLogRepository;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AuditLogService {

    private final AuditLogRepository auditLogRepository;

    public AuditLog save(AuditLog log) {
        return auditLogRepository.save(log);
    }

    public AuditLogPageResponse listLogs(
            String search,
            String action,
            String entityType,
            AuditLog.ActorType actorType,
            AuditLog.Result result,
            LocalDate dateFrom,
            LocalDate dateTo,
            int page,
            int size) {

        Specification<AuditLog> spec = buildSpec(search, action, entityType, actorType, result, dateFrom, dateTo);
        PageRequest pageable = PageRequest.of(page, size, Sort.by("timestamp").descending());
        Page<AuditLog> pageResult = auditLogRepository.findAll(spec, pageable);

        List<AuditLogDto> content = pageResult.getContent().stream().map(this::toDto).toList();

        return AuditLogPageResponse.builder()
                .content(content)
                .totalElements(pageResult.getTotalElements())
                .totalPages(pageResult.getTotalPages())
                .currentPage(pageResult.getNumber())
                .pageSize(pageResult.getSize())
                .build();
    }

    private Specification<AuditLog> buildSpec(
            String search,
            String action,
            String entityType,
            AuditLog.ActorType actorType,
            AuditLog.Result result,
            LocalDate dateFrom,
            LocalDate dateTo) {

        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (search != null && !search.isBlank()) {
                String pattern = "%" + search.toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("actorName")), pattern),
                        cb.like(cb.lower(root.get("actorEmail")), pattern),
                        cb.like(cb.lower(root.get("entityLabel")), pattern),
                        cb.like(cb.lower(root.get("entityId")), pattern),
                        cb.like(cb.lower(root.get("action")), pattern)));
            }

            if (action != null && !action.isBlank()) {
                predicates.add(cb.equal(root.get("action"), action));
            }

            if (entityType != null && !entityType.isBlank()) {
                predicates.add(cb.equal(root.get("entityType"), entityType));
            }

            if (actorType != null) {
                predicates.add(cb.equal(root.get("actorType"), actorType));
            }

            if (result != null) {
                predicates.add(cb.equal(root.get("result"), result));
            }

            if (dateFrom != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("timestamp"), dateFrom.atStartOfDay()));
            }

            if (dateTo != null) {
                predicates.add(cb.lessThan(root.get("timestamp"), dateTo.plusDays(1).atStartOfDay()));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    private AuditLogDto toDto(AuditLog log) {
        return AuditLogDto.builder()
                .id(log.getId())
                .timestamp(log.getTimestamp())
                .actorId(log.getActorId())
                .actorName(log.getActorName())
                .actorEmail(log.getActorEmail())
                .actorType(log.getActorType())
                .action(log.getAction())
                .entityType(log.getEntityType())
                .entityId(log.getEntityId())
                .entityLabel(log.getEntityLabel())
                .httpMethod(log.getHttpMethod())
                .httpPath(log.getHttpPath())
                .httpStatus(log.getHttpStatus())
                .result(log.getResult())
                .ipAddress(log.getIpAddress())
                .userAgent(log.getUserAgent())
                .payload(log.getPayload())
                .build();
    }
}
