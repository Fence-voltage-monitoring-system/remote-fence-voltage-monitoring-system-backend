package com.nerdc.elephantfence.backend.configuration.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RetentionStatusDTO {
    private String databaseSizeFormatted;
    private String databaseAllocatedFormatted;
    private int databaseUsagePercentage;
    private String archiveSizeFormatted;
    private String archiveAllocatedFormatted;
    private int archiveUsagePercentage;
    private String lastCleanupFormatted;
    private String lastCleanupSummary;
    private String nextCleanupFormatted;
    private String nextCleanupStatus;
    private boolean automaticCleanupEnabled;
}
