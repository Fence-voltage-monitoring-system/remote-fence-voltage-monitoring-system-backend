package com.nerdc.elephantfence.backend.configuration.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ActiveSessionRecordDTO {
    private String id;
    private UUID userId;
    private String userName;
    private String role;
    private String device;
    private String browser;
    private String ipAddress;
    private String approximateLocation;
    private OffsetDateTime signedInAt;
    private OffsetDateTime lastActivityAt;
    private OffsetDateTime expiresAt;
    private boolean current;
    private boolean suspicious;
}
