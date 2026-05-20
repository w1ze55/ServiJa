package com.servija.servija.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class AuditLogPageResponse {
    private List<AuditLogDto> content;
    private long totalElements;
    private int totalPages;
    private int currentPage;
    private int pageSize;
}
