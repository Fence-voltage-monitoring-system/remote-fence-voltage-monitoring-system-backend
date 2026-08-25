package com.nerdc.elephantfence.backend.devices.dto;

import jakarta.validation.constraints.Size;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateDeviceRequestDTO {

    @Size(max = 100, message = "Device name must not exceed 100 characters")
    private String name;

    @Size(max = 50, message = "Device type must not exceed 50 characters")
    private String type;

    private Boolean enabled;

    private String fence;   // optional fence name update
    private String section; // optional section code update
}
