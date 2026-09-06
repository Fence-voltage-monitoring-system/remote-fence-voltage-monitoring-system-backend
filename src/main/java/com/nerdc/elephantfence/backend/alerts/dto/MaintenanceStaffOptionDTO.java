package com.nerdc.elephantfence.backend.alerts.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MaintenanceStaffOptionDTO {
    private java.util.UUID id;
    private String name;
    private String email;
    private String responsibility; // 'PRIMARY', 'BACKUP', 'DISTRICT'
    private boolean available;
}
