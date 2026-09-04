package com.nerdc.elephantfence.backend.users.service;

import com.nerdc.elephantfence.backend.locations.entity.District;
import com.nerdc.elephantfence.backend.locations.entity.Province;
import com.nerdc.elephantfence.backend.locations.repository.DistrictRepository;
import com.nerdc.elephantfence.backend.locations.repository.ProvinceRepository;
import com.nerdc.elephantfence.backend.users.dto.UserCreateRequestDTO;
import com.nerdc.elephantfence.backend.users.dto.UserResponseDTO;
import com.nerdc.elephantfence.backend.users.dto.UserUpdateRequestDTO;
import com.nerdc.elephantfence.backend.users.entity.User;
import com.nerdc.elephantfence.backend.users.repository.UserRepository;
import lombok.RequiredArgsConstructor;
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

        if (dto.getFullName() != null) user.setFullName(dto.getFullName());
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
        if (!userRepository.existsById(id)) {
            throw new IllegalArgumentException("User not found with id: " + id);
        }
        userRepository.deleteById(id);
    }

    @Transactional
    public UserResponseDTO updateUserStatus(UUID id, boolean enabled) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("User not found with id: " + id));
        user.setEnabled(enabled);
        User updated = userRepository.save(user);
        return toUserResponseDTO(updated);
    }

    @Transactional
    public Map<String, String> resetPassword(UUID id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("User not found with id: " + id));
        user.setPasswordHash(passwordEncoder.encode("Password@123456"));
        user.setPasswordChangeRequired(true);
        userRepository.save(user);
        return Map.of("message", "Password reset successfully for " + user.getFullName() + ". Temporary password is: Password@123456");
    }

    public UserResponseDTO toUserResponseDTO(User user) {
        List<Long> provinceIds = user.getAssignedProvinces() != null
                ? user.getAssignedProvinces().stream().map(Province::getId).toList()
                : Collections.emptyList();

        List<Long> districtIds = user.getAssignedDistricts() != null
                ? user.getAssignedDistricts().stream().map(District::getId).toList()
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
                .districtIds(districtIds)
                .build();
    }
}
