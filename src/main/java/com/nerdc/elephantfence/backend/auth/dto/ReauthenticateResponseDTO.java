package com.nerdc.elephantfence.backend.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReauthenticateResponseDTO {

    private boolean authenticated;
    private int validityMinutes;
    private OffsetDateTime expiresAt;
    private String message;
}
