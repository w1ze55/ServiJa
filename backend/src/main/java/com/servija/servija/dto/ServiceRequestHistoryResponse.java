package com.servija.servija.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ServiceRequestHistoryResponse {

    private Long id;
    private String operationType;
    private String operationKey;
    private String userName;
    private String formattedDateTime;
    private String details;
}
