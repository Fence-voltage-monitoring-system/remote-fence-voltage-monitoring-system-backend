package com.nerdc.elephantfence.backend.health.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RetryRequestDTO {

    @NotBlank(message = "Reason is required")
    private String reason;
}
