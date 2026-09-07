package com.nerdc.elephantfence.backend.users.service;

import com.nerdc.elephantfence.backend.locations.entity.District;
import com.nerdc.elephantfence.backend.locations.entity.Province;
import com.nerdc.elephantfence.backend.locations.repository.DistrictRepository;
import com.nerdc.elephantfence.backend.locations.repository.ProvinceRepository;
import com.nerdc.elephantfence.backend.users.dto.UserCreateRequestDTO;
import com.nerdc.elephantfence.backend.users.dto.UserResponseDTO;
import com.nerdc.elephantfence.backend.users.dto.UserUpdateRequestDTO;
import com.nerdc.elephantfence.backend.users.entity.Role;
import com.nerdc.elephantfence.backend.users.entity.User;
import com.nerdc.elephantfence.backend.users.repository.UserRepository;
import com.nerdc.elephantfence.backend.common.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final ProvinceRepository provinceRepository;
    private final DistrictRepository districtRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional(readOnly = true)
    public List<UserResponseDTO> getAllUsers() {
        return userRepository.findAll().stream()
                .map(this::toUserResponseDTO)
                .toList();
    }

    @Transactional(readOnly = true)
    public UserResponseDTO getUserById(UUID id) {
        User user = userRepository.findByIdWithProvincesAndDistricts(id)
                .orElseThrow(() -> new IllegalArgumentException("User not found with id: " + id));
        return toUserResponseDTO(user);
    }

    @Transactional
    public UserResponseDTO createUser(UserCreateRequestDTO dto) {
        validateUserCreationAccess(dto);

        if (userRepository.existsByEmailIgnoreCase(dto.getEmail())) {
            throw new IllegalArgumentException("User already exists with email: " + dto.getEmail());
        }

        User user = User.builder()
                .fullName(dto.getFullName())
                .email(dto.getEmail().toLowerCase().trim())
                .passwordHash(passwordEncoder.encode(dto.getPassword()))
                .role(dto.getRole())
                .enabled(true)
                .passwordChangeRequired(true)
                .staffId(dto.getStaffId())
                .contactNumber(dto.getContactNumber())
                .assignedProvinces(new HashSet<>())
                .assignedDistricts(new HashSet<>())
                .build();

        if (dto.getProvinceIds() != null && !dto.getProvinceIds().isEmpty()) {
            List<Province> provinces = provinceRepository.findAllById(dto.getProvinceIds());
            user.getAssignedProvinces().addAll(provinces);
        }

        if (dto.getDistrictIds() != null && !dto.getDistrictIds().isEmpty()) {
            List<District> districts = districtRepository.findAllById(dto.getDistrictIds());
            user.getAssignedDistricts().addAll(districts);
        }

        User saved = userRepository.save(user);
        return toUserResponseDTO(saved);
    }

    @Transactional
    public UserResponseDTO updateUser(UUID id, UserUpdateRequestDTO dto) {
        User user = userRepository.findByIdWithProvincesAndDistricts(id)
                .orElseThrow(() -> new IllegalArgumentException("User not found with id: " + id));

        validateTargetUserManagementAccess(user);

        if (dto.getFullName() != null) user.setFullName(dto.getFullName());
        if (dto.getEmail() != null && !dto.getEmail().trim().equalsIgnoreCase(user.getEmail())) {
            String newEmail = dto.getEmail().trim().toLowerCase();
            if (userRepository.existsByEmailIgnoreCase(newEmail)) {
                throw new IllegalArgumentException("Email address " + newEmail + " is already registered to another account.");
            }
            user.setEmail(newEmail);
        }
        if (dto.getRole() != null) user.setRole(dto.getRole());
        if (dto.getEnabled() != null) user.setEnabled(dto.getEnabled());
        if (dto.getStaffId() != null) user.setStaffId(dto.getStaffId());
        if (dto.getContactNumber() != null) user.setContactNumber(dto.getContactNumber());

        if (dto.getProvinceIds() != null) {
            user.getAssignedProvinces().clear();
            if (!dto.getProvinceIds().isEmpty()) {
                user.getAssignedProvinces().addAll(provinceRepository.findAllById(dto.getProvinceIds()));
            }
        }

        if (dto.getDistrictIds() != null) {
            user.getAssignedDistricts().clear();
            if (!dto.getDistrictIds().isEmpty()) {
                user.getAssignedDistricts().addAll(districtRepository.findAllById(dto.getDistrictIds()));
            }
        }

        User updated = userRepository.save(user);
        return toUserResponseDTO(updated);
    }

    @Transactional
    public void deleteUser(UUID id) {
        User user = userRepository.findByIdWithProvincesAndDistricts(id)
                .orElseThrow(() -> new IllegalArgumentException("User not found with id: " + id));
        validateTargetUserManagementAccess(user);
        userRepository.deleteById(id);
    }

    @Transactional
    public UserResponseDTO updateUserStatus(UUID id, boolean enabled) {
        User user = userRepository.findByIdWithProvincesAndDistricts(id)
                .orElseThrow(() -> new IllegalArgumentException("User not found with id: " + id));
        validateTargetUserManagementAccess(user);
        user.setEnabled(enabled);
        User updated = userRepository.save(user);
        return toUserResponseDTO(updated);
    }

    @Transactional
    public Map<String, String> resetPassword(UUID id) {
        User user = userRepository.findByIdWithProvincesAndDistricts(id)
                .orElseThrow(() -> new IllegalArgumentException("User not found with id: " + id));
        validateTargetUserManagementAccess(user);
        user.setPasswordHash(passwordEncoder.encode("Password@123456"));
        user.setPasswordChangeRequired(true);
        userRepository.save(user);
        return Map.of("message", "Password reset successfully for " + user.getFullName() + ". Temporary password is: Password@123456");
    }

    private void validateTargetUserManagementAccess(User targetUser) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof UserPrincipal principal)) {
            return;
        }

        User actor = userRepository.findByIdWithProvincesAndDistricts(principal.getId())
                .orElseThrow(() -> new AccessDeniedException("Authenticated user not found"));

        if (actor.getRole() == Role.SUPER_ADMIN) {
            return;
        }

        if (actor.getRole() == Role.REGIONAL_ADMIN) {
            if (targetUser.getRole() == Role.SUPER_ADMIN) {
                throw new AccessDeniedException("Regional admins cannot manage Super Admin accounts.");
            }

            Set<Province> actorProvinces = actor.getAssignedProvinces();
            Set<Province> targetProvinces = targetUser.getAssignedProvinces();

            if (actorProvinces != null && !actorProvinces.isEmpty() && targetProvinces != null && !targetProvinces.isEmpty()) {
                boolean hasOverlap = actorProvinces.stream().anyMatch(targetProvinces::contains);
                if (!hasOverlap) {
                    throw new AccessDeniedException("Access denied: Target user belongs to a region outside your authority.");
                }
            }
            return;
        }

        if (actor.getRole() == Role.FIELD_ADMIN) {
            if (targetUser.getRole() != Role.MAINTENANCE) {
                throw new AccessDeniedException("Field Admins can only manage Maintenance staff accounts.");
            }

            Set<District> actorDistricts = actor.getAssignedDistricts();
            Set<District> targetDistricts = targetUser.getAssignedDistricts();

            if (actorDistricts != null && !actorDistricts.isEmpty() && targetDistricts != null && !targetDistricts.isEmpty()) {
                boolean hasOverlap = actorDistricts.stream().anyMatch(targetDistricts::contains);
                if (!hasOverlap) {
                    throw new AccessDeniedException("Access denied: Target user belongs to a district outside your authority.");
                }
            }
            return;
        }

        throw new AccessDeniedException("Access denied: You do not have permission to manage users.");
    }

    private void validateUserCreationAccess(UserCreateRequestDTO dto) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof UserPrincipal principal)) {
            return;
        }

        User actor = userRepository.findByIdWithProvincesAndDistricts(principal.getId())
                .orElseThrow(() -> new AccessDeniedException("Authenticated user not found"));

        if (actor.getRole() == Role.SUPER_ADMIN) {
            return;
        }

        if (actor.getRole() == Role.REGIONAL_ADMIN) {
            if (dto.getRole() == Role.SUPER_ADMIN || dto.getRole() == Role.REGIONAL_ADMIN) {
                throw new AccessDeniedException("Access denied: Regional admins can only create users below their level (Field Admin or Maintenance).");
            }

            Set<Province> actorProvinces = actor.getAssignedProvinces();
            if (actorProvinces != null && !actorProvinces.isEmpty()) {
                List<Long> actorProvinceIds = actorProvinces.stream().map(Province::getId).toList();
                if (dto.getProvinceIds() != null && !dto.getProvinceIds().isEmpty()) {
                    boolean allMatch = dto.getProvinceIds().stream().allMatch(actorProvinceIds::contains);
                    if (!allMatch) {
                        throw new AccessDeniedException("Access denied: Cannot assign users to a region outside your authority.");
                    }
                }
            }
            return;
        }

        if (actor.getRole() == Role.FIELD_ADMIN) {
            if (dto.getRole() != Role.MAINTENANCE) {
                throw new AccessDeniedException("Access denied: Field Admins can only create Maintenance staff accounts.");
            }

            Set<District> actorDistricts = actor.getAssignedDistricts();
            if (actorDistricts != null && !actorDistricts.isEmpty()) {
                List<Long> actorDistrictIds = actorDistricts.stream().map(District::getId).toList();
                if (dto.getDistrictIds() != null && !dto.getDistrictIds().isEmpty()) {
                    boolean allMatch = dto.getDistrictIds().stream().allMatch(actorDistrictIds::contains);
                    if (!allMatch) {
                        throw new AccessDeniedException("Access denied: Cannot assign users to a district outside your authority.");
                    }
                }
            }
            return;
        }

        throw new AccessDeniedException("Access denied: You do not have permission to create users.");
    }

    public UserResponseDTO toUserResponseDTO(User user) {
        List<Long> provinceIds = user.getAssignedProvinces() != null
                ? user.getAssignedProvinces().stream().map(Province::getId).toList()
                : Collections.emptyList();

        List<String> provinceNames = user.getAssignedProvinces() != null
                ? user.getAssignedProvinces().stream().map(Province::getName).toList()
                : Collections.emptyList();

        List<Long> districtIds = user.getAssignedDistricts() != null
                ? user.getAssignedDistricts().stream().map(District::getId).toList()
                : Collections.emptyList();

        List<String> districtNames = user.getAssignedDistricts() != null
                ? user.getAssignedDistricts().stream().map(District::getName).toList()
                : Collections.emptyList();

        return UserResponseDTO.builder()
                .id(user.getId())
                .fullName(user.getFullName())
                .email(user.getEmail())
                .role(user.getRole())
                .enabled(user.isEnabled())
                .passwordChangeRequired(user.isPasswordChangeRequired())
                .staffId(user.getStaffId())
                .contactNumber(user.getContactNumber())
                .lastLoginAt(user.getLastLoginAt())
                .passwordChangedAt(user.getPasswordChangedAt())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .provinceIds(provinceIds)
                .provinceNames(provinceNames)
                .districtIds(districtIds)
                .districtNames(districtNames)
                .build();
    }
}
