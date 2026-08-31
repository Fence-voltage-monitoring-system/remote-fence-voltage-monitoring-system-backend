package com.nerdc.elephantfence.backend.systemhealth.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class JobRetryResponseDTO {
    private String message;
    private String executionId;
}
