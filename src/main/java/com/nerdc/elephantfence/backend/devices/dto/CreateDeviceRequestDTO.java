package com.nerdc.elephantfence.backend.devices.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateDeviceRequestDTO {

    @NotBlank(message = "Device name is required")
    @Size(max = 100, message = "Device name must not exceed 100 characters")
    private String name;

    @NotBlank(message = "Hardware serial number is required")
    @Size(max = 100, message = "Serial number must not exceed 100 characters")
    private String serial;

    @Size(max = 50, message = "Device type must not exceed 50 characters")
    private String type;

    private String fence;   // optional fence name
    private String section; // optional section code
}
