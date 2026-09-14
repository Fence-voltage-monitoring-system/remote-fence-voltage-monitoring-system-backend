package com.nerdc.elephantfence.backend.fences.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FenceResponseDTO {
    private Long id;
    private String code;
    private String name;
    private Long provinceId;
    private String provinceName;
    private String province; // Mapped for frontend FenceRecord
    private Long districtId;
    private String districtName;
    private String district; // Mapped for frontend FenceRecord
    private BigDecimal lengthKm;
    private BigDecimal averageVoltageKv;
    private String health;
    private UUID primaryMaintenanceUserId;
    private String primaryMaintenanceUserName;
    private List<UUID> backupMaintenanceUserIds;
    private Integer sections; // Mapped for frontend FenceRecord
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
}
