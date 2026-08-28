package com.nerdc.elephantfence.backend.alerts.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AddCommentRequestDTO {
    @NotBlank(message = "Comment is required")
    private String comment;
}
