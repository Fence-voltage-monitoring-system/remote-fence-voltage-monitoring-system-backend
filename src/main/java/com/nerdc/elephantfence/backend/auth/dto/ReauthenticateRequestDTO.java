package com.nerdc.elephantfence.backend.auth.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReauthenticateRequestDTO {

    @NotBlank(message = "Password is required")
    private String password;
}
