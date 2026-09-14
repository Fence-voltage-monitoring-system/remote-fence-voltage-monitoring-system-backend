package com.nerdc.elephantfence.backend.sections.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SectionBulkImportRequestDTO {
    @NotBlank(message = "Fence code is required")
    private String fenceCode;

    @NotEmpty(message = "Rows cannot be empty")
    @Valid
    private List<SectionBulkRowDTO> rows;
}
