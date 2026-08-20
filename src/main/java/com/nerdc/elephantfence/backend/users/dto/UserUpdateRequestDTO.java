package com.nerdc.elephantfence.backend.users.dto;

import com.nerdc.elephantfence.backend.users.entity.Role;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserUpdateRequestDTO {

    @Size(max = 150)
    private String fullName;

    private Role role;
    private Boolean enabled;
    private String staffId;
    private String contactNumber;

    private List<Long> provinceIds;
    private List<Long> districtIds;
}
