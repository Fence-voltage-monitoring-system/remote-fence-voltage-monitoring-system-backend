package com.nerdc.elephantfence.backend.gateways.dto;

import lombok.*;

import java.time.OffsetDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GatewayResponseDTO {
    private String id;
    private String name;
    private String serial;
    private String imei;
    private List<String> fences;
    private String status;
    private Integer signal;
    private Integer power;
    private Integer devices;
    private OffsetDateTime lastSeen;
    private String firmware;
    private boolean enabled;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
}
