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
public class GatewayUpdateRequestDTO {
    private String name;
    private String serial;
    private String imei;
    private List<String> fences;
    private String firmware;
    private Boolean enabled;
}
