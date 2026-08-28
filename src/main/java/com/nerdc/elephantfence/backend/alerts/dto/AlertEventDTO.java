package com.nerdc.elephantfence.backend.alerts.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AlertEventDTO {
    private Long id;
    private String type;
    private String label;
    private String timestamp;
    private String actor;
    private String details;
}
