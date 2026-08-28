package com.nerdc.elephantfence.backend.alerts.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResolveManuallyRequestDTO {
    private String reason;
}
