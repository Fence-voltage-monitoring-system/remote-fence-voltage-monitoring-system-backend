package com.nerdc.elephantfence.backend.users.dto;

import com.nerdc.elephantfence.backend.users.entity.Role;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserResponseDTO {
    private UUID id;
    private String fullName;
    private String email;
    private Role role;
    private boolean enabled;
    private boolean passwordChangeRequired;
    private String staffId;
    private String contactNumber;
    private OffsetDateTime lastLoginAt;
    private OffsetDateTime passwordChangedAt;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
    private List<Long> provinceIds;
    private List<Long> districtIds;
}
