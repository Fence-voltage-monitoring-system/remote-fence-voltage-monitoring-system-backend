package com.nerdc.elephantfence.backend.locations.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DistrictResponseDTO {
    private Long id;
    private Long provinceId;
    private String provinceName;
    private String name;
}
