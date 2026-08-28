package com.nerdc.elephantfence.backend.gateways.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GatewayResponseDTO {
    private String id;
    private String name;
    private String serial;
    private String imei;
    private List<String> fences;
    private String status;
    private int signal;
    private int power;
    private int devices;
    private String lastSeen;
    private String firmware;
    private boolean enabled;
}
