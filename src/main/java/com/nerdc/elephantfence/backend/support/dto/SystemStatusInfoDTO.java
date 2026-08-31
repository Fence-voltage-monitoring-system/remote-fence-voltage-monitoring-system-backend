package com.nerdc.elephantfence.backend.support.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SystemStatusInfoDTO {
    private String overallStatus;
    private String lastUpdated;
    private String environment;
    private String version;
}
