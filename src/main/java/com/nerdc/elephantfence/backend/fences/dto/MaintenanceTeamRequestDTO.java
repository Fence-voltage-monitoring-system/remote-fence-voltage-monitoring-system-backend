package com.nerdc.elephantfence.backend.fences.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Set;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MaintenanceTeamRequestDTO {
    private UUID primaryMaintenanceUserId;
    private Set<UUID> backupMaintenanceUserIds;
}
