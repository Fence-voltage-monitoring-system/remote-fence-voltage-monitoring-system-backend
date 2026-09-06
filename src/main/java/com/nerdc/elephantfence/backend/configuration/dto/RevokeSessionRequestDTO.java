package com.nerdc.elephantfence.backend.configuration.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RevokeSessionRequestDTO {

    @NotBlank(message = "Reason for session revocation is required")
    private String reason;
}
