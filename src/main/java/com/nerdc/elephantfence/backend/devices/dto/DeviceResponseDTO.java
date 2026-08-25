package com.nerdc.elephantfence.backend.devices.dto;

import lombok.*;

import java.time.OffsetDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DeviceResponseDTO {
    private String id;
    private String name;
    private String serial;
    private String type;
    private String status;
    private Double voltage;
    private Integer signal;
    private Integer battery;
    private boolean enabled;
    private OffsetDateTime lastSeen;
    private String fence;   // maps to frontend 'fence' (name of fence)
    private String section; // maps to frontend 'section' (code of section)
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
}
