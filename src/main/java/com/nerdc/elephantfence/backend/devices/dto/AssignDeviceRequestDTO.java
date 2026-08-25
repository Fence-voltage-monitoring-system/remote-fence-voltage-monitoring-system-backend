package com.nerdc.elephantfence.backend.devices.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AssignDeviceRequestDTO {

    @NotBlank(message = "Fence name is required for assignment")
    private String fence;

    @NotBlank(message = "Section code is required for assignment")
    private String section;
}
