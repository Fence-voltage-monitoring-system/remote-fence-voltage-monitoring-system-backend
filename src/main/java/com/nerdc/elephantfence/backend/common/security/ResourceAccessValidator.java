package com.nerdc.elephantfence.backend.common.security;

import com.nerdc.elephantfence.backend.users.entity.Role;
import com.nerdc.elephantfence.backend.users.entity.User;
import com.nerdc.elephantfence.backend.users.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class ResourceAccessValidator {

    private final UserRepository userRepository;

    public void validateGeographicAccess(Long resourceProvinceId, Long resourceDistrictId) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof UserPrincipal principal)) {
            return; // System call or unauthenticated (caught by Spring Security filter)
        }

        User user = userRepository.findByIdWithProvincesAndDistricts(principal.getId())
                .orElseThrow(() -> new AccessDeniedException("User not found"));

        // Super Admins have global access across Sri Lanka
        if (user.getRole() == Role.SUPER_ADMIN) {
            return;
        }

        boolean hasProvinceAccess = false;
        if (resourceProvinceId != null && user.getAssignedProvinces() != null) {
            hasProvinceAccess = user.getAssignedProvinces().stream()
                    .anyMatch(p -> p.getId().equals(resourceProvinceId));
        }

        boolean hasDistrictAccess = false;
        if (resourceDistrictId != null && user.getAssignedDistricts() != null) {
            hasDistrictAccess = user.getAssignedDistricts().stream()
                    .anyMatch(d -> d.getId().equals(resourceDistrictId));
        }

        // If the user has assigned regions, enforce that at least one region matches
        boolean hasAssignedRegions = (user.getAssignedProvinces() != null && !user.getAssignedProvinces().isEmpty()) ||
                (user.getAssignedDistricts() != null && !user.getAssignedDistricts().isEmpty());

        if (hasAssignedRegions && !hasProvinceAccess && !hasDistrictAccess) {
            throw new AccessDeniedException("Access denied: Target resource is outside your assigned geographic region.");
        }
    }
}
