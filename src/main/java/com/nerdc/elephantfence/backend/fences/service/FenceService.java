package com.nerdc.elephantfence.backend.fences.service;

import com.nerdc.elephantfence.backend.common.security.UserPrincipal;
import com.nerdc.elephantfence.backend.fences.dto.FenceCreateRequestDTO;
import com.nerdc.elephantfence.backend.fences.dto.FenceResponseDTO;
import com.nerdc.elephantfence.backend.fences.dto.FenceUpdateRequestDTO;
import com.nerdc.elephantfence.backend.fences.dto.MaintenanceTeamRequestDTO;
import com.nerdc.elephantfence.backend.fences.entity.Fence;
import com.nerdc.elephantfence.backend.fences.entity.FenceHealth;
import com.nerdc.elephantfence.backend.fences.repository.FenceRepository;
import com.nerdc.elephantfence.backend.locations.entity.District;
import com.nerdc.elephantfence.backend.locations.entity.Province;
import com.nerdc.elephantfence.backend.locations.repository.DistrictRepository;
import com.nerdc.elephantfence.backend.locations.repository.ProvinceRepository;
import com.nerdc.elephantfence.backend.sections.repository.SectionRepository;
import com.nerdc.elephantfence.backend.users.dto.UserResponseDTO;
import com.nerdc.elephantfence.backend.users.entity.Role;
import com.nerdc.elephantfence.backend.users.entity.User;
import com.nerdc.elephantfence.backend.users.repository.UserRepository;
import com.nerdc.elephantfence.backend.users.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
@RequiredArgsConstructor
public class FenceService {

    private final FenceRepository fenceRepository;
    private final ProvinceRepository provinceRepository;
    private final DistrictRepository districtRepository;
    private final UserRepository userRepository;
    private final UserService userService;
    private final SectionRepository sectionRepository;

    @Transactional(readOnly = true)
    public List<FenceResponseDTO> getAllFences(Long provinceId, Long districtId) {
        List<Fence> fences;
        if (districtId != null) {
            fences = fenceRepository.findByDistrictId(districtId);
        } else if (provinceId != null) {
            fences = fenceRepository.findByProvinceId(provinceId);
        } else {
            fences = fenceRepository.findAll();
        }
        return fences.stream().map(this::toFenceResponseDTO).toList();
    }

    @Transactional(readOnly = true)
    public FenceResponseDTO getFenceById(Long id) {
        Fence fence = fenceRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Fence not found with ID: " + id));
        return toFenceResponseDTO(fence);
    }

    @Transactional
    public FenceResponseDTO createFence(FenceCreateRequestDTO dto, UserPrincipal principal) {
        validateWriteAccess(principal, dto.getProvinceId(), dto.getDistrictId());

        if (fenceRepository.existsByCodeIgnoreCase(dto.getCode())) {
            throw new IllegalArgumentException("Fence code already exists: " + dto.getCode());
        }

        Province province = provinceRepository.findById(dto.getProvinceId())
                .orElseThrow(() -> new IllegalArgumentException("Province not found with ID: " + dto.getProvinceId()));

        District district = districtRepository.findById(dto.getDistrictId())
                .orElseThrow(() -> new IllegalArgumentException("District not found with ID: " + dto.getDistrictId()));

        validateLocationConsistency(province, district);

        FenceHealth health = FenceHealth.OFFLINE;
        if (dto.getHealth() != null) {
            try {
                health = FenceHealth.valueOf(dto.getHealth().toUpperCase());
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("Invalid fence health status: " + dto.getHealth());
            }
        }

        Fence fence = Fence.builder()
                .code(dto.getCode().trim())
                .name(dto.getName().trim())
                .province(province)
                .district(district)
                .lengthKm(dto.getLengthKm())
                .health(health)
                .build();

        Fence saved = fenceRepository.save(fence);
        return toFenceResponseDTO(saved);
    }

    @Transactional
    public FenceResponseDTO saveDraft(FenceCreateRequestDTO dto) {
        throw new UnsupportedOperationException("Database schema does not support fence configuration drafts natively. Please register a fence using standard creation.");
    }

    @Transactional
    public FenceResponseDTO updateFence(Long id, FenceUpdateRequestDTO dto, UserPrincipal principal) {
        Fence fence = fenceRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Fence not found with ID: " + id));

        validateWriteAccess(principal, fence.getProvince().getId(), fence.getDistrict().getId());

        if (dto.getCode() != null && !dto.getCode().equalsIgnoreCase(fence.getCode())) {
            if (fenceRepository.existsByCodeIgnoreCaseAndIdNot(dto.getCode(), id)) {
                throw new IllegalArgumentException("Fence code already exists: " + dto.getCode());
            }
            fence.setCode(dto.getCode().trim());
        }

        if (dto.getName() != null) {
            fence.setName(dto.getName().trim());
        }

        if (dto.getProvinceId() != null || dto.getDistrictId() != null) {
            Long provinceId = dto.getProvinceId() != null ? dto.getProvinceId() : fence.getProvince().getId();
            Long districtId = dto.getDistrictId() != null ? dto.getDistrictId() : fence.getDistrict().getId();

            Province province = provinceRepository.findById(provinceId)
                    .orElseThrow(() -> new IllegalArgumentException("Province not found with ID: " + provinceId));
            District district = districtRepository.findById(districtId)
                    .orElseThrow(() -> new IllegalArgumentException("District not found with ID: " + districtId));

            validateLocationConsistency(province, district);
            validateWriteAccess(principal, provinceId, districtId);

            fence.setProvince(province);
            fence.setDistrict(district);
        }

        if (dto.getLengthKm() != null) {
            fence.setLengthKm(dto.getLengthKm());
        }

        if (dto.getHealth() != null) {
            try {
                fence.setHealth(FenceHealth.valueOf(dto.getHealth().toUpperCase()));
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("Invalid fence health status: " + dto.getHealth());
            }
        }

        Fence updated = fenceRepository.save(fence);
        return toFenceResponseDTO(updated);
    }

    @Transactional(readOnly = true)
    public List<UserResponseDTO> getMaintenanceCandidates(Long fenceId, Long provinceId, Long districtId) {
        Long targetProvinceId = provinceId;
        Long targetDistrictId = districtId;

        if (fenceId != null) {
            Fence fence = fenceRepository.findById(fenceId)
                    .orElseThrow(() -> new IllegalArgumentException("Fence not found with ID: " + fenceId));
            targetProvinceId = fence.getProvince().getId();
            targetDistrictId = fence.getDistrict().getId();
        }

        if (targetProvinceId == null && targetDistrictId == null) {
            throw new IllegalArgumentException("Either Fence ID or Province/District IDs must be provided.");
        }

        List<User> candidates = fenceRepository.findMaintenanceCandidates(targetProvinceId, targetDistrictId);
        return candidates.stream()
                .map(userService::toUserResponseDTO)
                .toList();
    }

    @Transactional
    public FenceResponseDTO assignMaintenanceTeam(Long id, MaintenanceTeamRequestDTO dto, UserPrincipal principal) {
        if (dto.getPrimaryMaintenanceUserId() != null && dto.getBackupMaintenanceUserIds() != null
                && dto.getBackupMaintenanceUserIds().contains(dto.getPrimaryMaintenanceUserId())) {
            throw new IllegalArgumentException("Primary maintenance user cannot also be assigned as a backup maintenance user.");
        }

        Fence fence = fenceRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Fence not found with ID: " + id));

        validateWriteAccess(principal, fence.getProvince().getId(), fence.getDistrict().getId());

        User primaryUser = null;
        if (dto.getPrimaryMaintenanceUserId() != null) {
            primaryUser = userRepository.findById(dto.getPrimaryMaintenanceUserId())
                    .orElseThrow(() -> new IllegalArgumentException("Primary maintenance user not found: " + dto.getPrimaryMaintenanceUserId()));
            
            validateMaintenanceRole(primaryUser);
            validateUserRegion(primaryUser, fence.getProvince().getId(), fence.getDistrict().getId());
        }

        Set<User> backupUsers = new HashSet<>();
        if (dto.getBackupMaintenanceUserIds() != null && !dto.getBackupMaintenanceUserIds().isEmpty()) {
            for (UUID backupId : dto.getBackupMaintenanceUserIds()) {
                User backupUser = userRepository.findById(backupId)
                        .orElseThrow(() -> new IllegalArgumentException("Backup maintenance user not found: " + backupId));
                
                validateMaintenanceRole(backupUser);
                validateUserRegion(backupUser, fence.getProvince().getId(), fence.getDistrict().getId());
                backupUsers.add(backupUser);
            }
        }

        fence.setPrimaryMaintenanceUser(primaryUser);
        fence.getBackupMaintenanceUsers().clear();
        fence.getBackupMaintenanceUsers().addAll(backupUsers);

        Fence updated = fenceRepository.save(fence);
        return toFenceResponseDTO(updated);
    }

    @Transactional
    public void deleteFence(Long id, UserPrincipal principal) {
        Fence fence = fenceRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Fence not found with ID: " + id));

        validateWriteAccess(principal, fence.getProvince().getId(), fence.getDistrict().getId());

        long alertsCount = fenceRepository.countAlertsByFenceId(id);
        if (alertsCount > 0) {
            throw new IllegalArgumentException("Cannot delete fence: it has " + alertsCount + " associated alert record(s) in the system.");
        }

        fenceRepository.delete(fence);
    }

    private void validateWriteAccess(UserPrincipal principal, Long provinceId, Long districtId) {
        if (principal == null) {
            throw new AccessDeniedException("User is not authenticated");
        }

        User user = userRepository.findById(principal.getId())
                .orElseThrow(() -> new AccessDeniedException("User not found"));

        if (user.getRole() == Role.SUPER_ADMIN) {
            return;
        }

        if (user.getRole() == Role.REGIONAL_ADMIN) {
            if (provinceId == null) {
                throw new AccessDeniedException("Province must be specified for regional admin check");
            }
            boolean hasProvince = user.getAssignedProvinces().stream()
                    .anyMatch(p -> p.getId().equals(provinceId));
            if (!hasProvince) {
                throw new AccessDeniedException("Access denied: You are not assigned to this province");
            }
            return;
        }

        if (user.getRole() == Role.FIELD_ADMIN) {
            if (districtId == null) {
                throw new AccessDeniedException("District must be specified for field admin check");
            }
            boolean hasDistrict = user.getAssignedDistricts().stream()
                    .anyMatch(d -> d.getId().equals(districtId));
            if (!hasDistrict) {
                throw new AccessDeniedException("Access denied: You are not assigned to this district");
            }
            return;
        }

        throw new AccessDeniedException("Access denied: Insufficient privileges");
    }

    private void validateLocationConsistency(Province province, District district) {
        if (!district.getProvince().getId().equals(province.getId())) {
            throw new IllegalArgumentException(String.format("District '%s' does not belong to Province '%s'", district.getName(), province.getName()));
        }
    }

    private void validateMaintenanceRole(User user) {
        if (user.getRole() != Role.MAINTENANCE && user.getRole() != Role.FIELD_ADMIN) {
            throw new IllegalArgumentException("User " + user.getFullName() + " does not have an eligible maintenance role.");
        }
        if (!user.isEnabled()) {
            throw new IllegalArgumentException("User " + user.getFullName() + " is disabled.");
        }
    }

    private void validateUserRegion(User user, Long provinceId, Long districtId) {
        boolean assignedToProvince = user.getAssignedProvinces().stream()
                .anyMatch(p -> p.getId().equals(provinceId));
        boolean assignedToDistrict = user.getAssignedDistricts().stream()
                .anyMatch(d -> d.getId().equals(districtId));

        if (!assignedToProvince && !assignedToDistrict) {
            throw new IllegalArgumentException("User " + user.getFullName() + " is not assigned to the fence's region (Province/District).");
        }
    }

    private FenceResponseDTO toFenceResponseDTO(Fence fence) {
        List<UUID> backupUserIds = fence.getBackupMaintenanceUsers() != null
                ? fence.getBackupMaintenanceUsers().stream().map(User::getId).toList()
                : Collections.emptyList();

        UUID primaryUserId = fence.getPrimaryMaintenanceUser() != null
                ? fence.getPrimaryMaintenanceUser().getId()
                : null;

        String primaryUserName = fence.getPrimaryMaintenanceUser() != null
                ? fence.getPrimaryMaintenanceUser().getFullName()
                : null;

        long sectionCount = sectionRepository.countByFenceId(fence.getId());

        return FenceResponseDTO.builder()
                .id(fence.getId())
                .code(fence.getCode())
                .name(fence.getName())
                .provinceId(fence.getProvince().getId())
                .provinceName(fence.getProvince().getName())
                .province(fence.getProvince().getName())
                .districtId(fence.getDistrict().getId())
                .districtName(fence.getDistrict().getName())
                .district(fence.getDistrict().getName())
                .lengthKm(fence.getLengthKm())
                .averageVoltageKv(fence.getAverageVoltageKv())
                .health(fence.getHealth().name())
                .primaryMaintenanceUserId(primaryUserId)
                .primaryMaintenanceUserName(primaryUserName)
                .backupMaintenanceUserIds(backupUserIds)
                .sections((int) sectionCount)
                .createdAt(fence.getCreatedAt())
                .updatedAt(fence.getUpdatedAt())
                .build();
    }
}
