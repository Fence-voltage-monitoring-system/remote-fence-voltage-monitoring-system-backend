package com.nerdc.elephantfence.backend.devices.service;

import com.nerdc.elephantfence.backend.common.security.UserPrincipal;
import com.nerdc.elephantfence.backend.configuration.service.ConfigurationService;
import com.nerdc.elephantfence.backend.devices.dto.AssignDeviceRequestDTO;
import com.nerdc.elephantfence.backend.devices.dto.CreateDeviceRequestDTO;
import com.nerdc.elephantfence.backend.devices.dto.DeviceResponseDTO;
import com.nerdc.elephantfence.backend.devices.dto.UpdateDeviceRequestDTO;
import com.nerdc.elephantfence.backend.devices.entity.Device;
import com.nerdc.elephantfence.backend.devices.entity.DeviceStatus;
import com.nerdc.elephantfence.backend.devices.repository.DeviceRepository;
import com.nerdc.elephantfence.backend.fences.entity.Fence;
import com.nerdc.elephantfence.backend.fences.repository.FenceRepository;
import com.nerdc.elephantfence.backend.locations.entity.District;
import com.nerdc.elephantfence.backend.locations.entity.Province;
import com.nerdc.elephantfence.backend.users.entity.Role;
import com.nerdc.elephantfence.backend.users.entity.User;
import com.nerdc.elephantfence.backend.users.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DeviceService {

    private final DeviceRepository deviceRepository;
    private final UserRepository userRepository;
    private final FenceRepository fenceRepository;

    @Autowired(required = false)
    private ConfigurationService configurationService;

    @Transactional(readOnly = true)
    public List<DeviceResponseDTO> getAllDevices() {
        User user = getCurrentUser();
        List<Device> all = deviceRepository.findAll();

        if (user == null || user.getRole() == Role.SUPER_ADMIN) {
            return all.stream()
                    .map(this::convertToResponseDTO)
                    .toList();
        }

        Map<Long, Fence> fenceMap = fenceRepository.findAll().stream()
                .collect(Collectors.toMap(Fence::getId, f -> f, (a, b) -> a));

        return all.stream()
                .filter(device -> isDeviceVisibleToUser(device, user, fenceMap))
                .map(this::convertToResponseDTO)
                .toList();
    }

    @Transactional(readOnly = true)
    public DeviceResponseDTO getDeviceById(Long id) {
        Device device = deviceRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Device not found with ID: " + id));
        validateDeviceAccess(device, "view");
        return convertToResponseDTO(device);
    }

    @Transactional
    public DeviceResponseDTO createDevice(CreateDeviceRequestDTO dto) {
        User user = getCurrentUser();
        if (user != null && user.getRole() == Role.MAINTENANCE) {
            throw new AccessDeniedException("Access denied: Maintenance technicians cannot register devices.");
        }

        if (deviceRepository.existsBySerialIgnoreCase(dto.getSerial())) {
            throw new IllegalArgumentException("Device already exists with serial: " + dto.getSerial());
        }

        String type = dto.getType() != null && !dto.getType().isBlank() ? dto.getType() : "Voltage Monitor";

        Device.DeviceBuilder deviceBuilder = Device.builder()
                .name(dto.getName())
                .serial(dto.getSerial().trim())
                .type(type)
                .status(DeviceStatus.offline)
                .signal(0)
                .battery(0)
                .enabled(true);

        if (dto.getFence() != null && !dto.getFence().isBlank()) {
            Long fenceId = deviceRepository.findFenceIdByName(dto.getFence().trim());
            if (fenceId != null) {
                Fence targetFence = fenceRepository.findById(fenceId).orElse(null);
                if (targetFence != null && user != null) {
                    validateFenceScope(user, targetFence, "register devices in");
                }
                deviceBuilder.fenceId(fenceId);
                if (dto.getSection() != null && !dto.getSection().isBlank()) {
                    Long sectionId = deviceRepository.findSectionIdByCodeAndFenceId(dto.getSection().trim(), fenceId);
                    if (sectionId != null) {
                        deviceBuilder.sectionId(sectionId);
                        deviceBuilder.status(DeviceStatus.online);
                        deviceBuilder.voltage(6.0);
                        deviceBuilder.signal(100);
                    }
                }
            }
        }

        Device saved = deviceRepository.save(deviceBuilder.build());
        return convertToResponseDTO(saved);
    }

    @Transactional
    public DeviceResponseDTO updateDevice(Long id, UpdateDeviceRequestDTO dto) {
        Device device = deviceRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Device not found with ID: " + id));

        validateDeviceAccess(device, "edit");

        if (dto.getName() != null) device.setName(dto.getName());
        if (dto.getSerial() != null && !dto.getSerial().isBlank()) device.setSerial(dto.getSerial().trim());
        if (dto.getType() != null) device.setType(dto.getType());
        if (dto.getEnabled() != null) device.setEnabled(dto.getEnabled());

        if (dto.getFence() != null) {
            if (dto.getFence().isBlank()) {
                device.setFenceId(null);
                device.setSectionId(null);
            } else {
                Long fenceId = deviceRepository.findFenceIdByName(dto.getFence().trim());
                if (fenceId == null) {
                    throw new IllegalArgumentException("Fence not found with name: " + dto.getFence());
                }
                Fence targetFence = fenceRepository.findById(fenceId).orElse(null);
                if (targetFence != null) {
                    User user = getCurrentUser();
                    if (user != null) {
                        validateFenceScope(user, targetFence, "assign devices to");
                    }
                }
                device.setFenceId(fenceId);

                if (dto.getSection() != null) {
                    if (dto.getSection().isBlank()) {
                        device.setSectionId(null);
                    } else {
                        Long sectionId = deviceRepository.findSectionIdByCodeAndFenceId(dto.getSection().trim(), fenceId);
                        if (sectionId == null) {
                            throw new IllegalArgumentException("Section not found with code: " + dto.getSection() + " on fence: " + dto.getFence());
                        }
                        device.setSectionId(sectionId);
                    }
                }
            }
        }

        Device updated = deviceRepository.save(device);
        return convertToResponseDTO(updated);
    }

    @Transactional
    public DeviceResponseDTO assignDevice(Long id, AssignDeviceRequestDTO dto) {
        Device device = deviceRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Device not found with ID: " + id));

        validateDeviceAccess(device, "assign");

        Long fenceId = deviceRepository.findFenceIdByName(dto.getFence().trim());
        if (fenceId == null) {
            throw new IllegalArgumentException("Fence not found with name: " + dto.getFence());
        }

        Fence targetFence = fenceRepository.findById(fenceId).orElse(null);
        if (targetFence != null) {
            User user = getCurrentUser();
            if (user != null) {
                validateFenceScope(user, targetFence, "assign devices to");
            }
        }

        Long sectionId = deviceRepository.findSectionIdByCodeAndFenceId(dto.getSection().trim(), fenceId);
        if (sectionId == null) {
            throw new IllegalArgumentException("Section not found with code: " + dto.getSection() + " on fence: " + dto.getFence());
        }

        device.setFenceId(fenceId);
        device.setSectionId(sectionId);
        device.setStatus(DeviceStatus.online);
        device.setVoltage(6.0);
        device.setSignal(100);

        Device updated = deviceRepository.save(device);
        return convertToResponseDTO(updated);
    }

    @Transactional
    public DeviceResponseDTO unassignDevice(Long id) {
        Device device = deviceRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Device not found with ID: " + id));

        validateDeviceAccess(device, "unassign");

        device.setFenceId(null);
        device.setSectionId(null);
        device.setStatus(DeviceStatus.offline);
        device.setVoltage(null);
        device.setSignal(0);

        Device updated = deviceRepository.save(device);
        return convertToResponseDTO(updated);
    }

    @Transactional
    public DeviceResponseDTO toggleEnabled(Long id, boolean enabled) {
        Device device = deviceRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Device not found with ID: " + id));

        validateDeviceAccess(device, "toggle status on");

        device.setEnabled(enabled);
        Device updated = deviceRepository.save(device);
        return convertToResponseDTO(updated);
    }

    @Transactional
    public void deleteDevice(Long id) {
        if (!deviceRepository.existsById(id)) {
            throw new IllegalArgumentException("Device not found with ID: " + id);
        }
        User user = getCurrentUser();
        if (user != null && user.getRole() != Role.SUPER_ADMIN) {
            throw new AccessDeniedException("Access denied: Only Super Admins can remove devices.");
        }
        deviceRepository.deleteById(id);
    }

    private User getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof UserPrincipal principal)) {
            return null;
        }
        return userRepository.findByIdWithProvincesAndDistricts(principal.getId()).orElse(null);
    }

    private boolean isDeviceVisibleToUser(Device device, User user, Map<Long, Fence> fenceMap) {
        if (device.getFenceId() == null) {
            // Unassigned inventory is visible to administrative staff, not to maintenance technicians
            return user.getRole() != Role.MAINTENANCE;
        }

        Fence fence = fenceMap.get(device.getFenceId());
        if (fence == null) {
            return false;
        }

        return switch (user.getRole()) {
            case SUPER_ADMIN -> true;
            case REGIONAL_ADMIN -> fence.getProvince() != null &&
                    user.getAssignedProvinces() != null &&
                    user.getAssignedProvinces().stream().anyMatch(p -> p.getId().equals(fence.getProvince().getId()));
            case FIELD_ADMIN -> fence.getDistrict() != null &&
                    user.getAssignedDistricts() != null &&
                    user.getAssignedDistricts().stream().anyMatch(d -> d.getId().equals(fence.getDistrict().getId()));
            case MAINTENANCE -> {
                boolean isPrimary = fence.getPrimaryMaintenanceUser() != null &&
                        fence.getPrimaryMaintenanceUser().getId().equals(user.getId());
                boolean isBackup = fence.getBackupMaintenanceUsers() != null &&
                        fence.getBackupMaintenanceUsers().stream().anyMatch(u -> u.getId().equals(user.getId()));
                boolean inProvince = fence.getProvince() != null && user.getAssignedProvinces() != null &&
                        user.getAssignedProvinces().stream().anyMatch(p -> p.getId().equals(fence.getProvince().getId()));
                boolean inDistrict = fence.getDistrict() != null && user.getAssignedDistricts() != null &&
                        user.getAssignedDistricts().stream().anyMatch(d -> d.getId().equals(fence.getDistrict().getId()));
                yield isPrimary || isBackup || inProvince || inDistrict;
            }
        };
    }

    private void validateDeviceAccess(Device device, String action) {
        User user = getCurrentUser();
        if (user == null || user.getRole() == Role.SUPER_ADMIN) {
            return;
        }

        if (user.getRole() == Role.MAINTENANCE && !"view".equalsIgnoreCase(action)) {
            throw new AccessDeniedException("Access denied: Maintenance technicians cannot " + action + " devices.");
        }

        if (device.getFenceId() != null) {
            Fence fence = fenceRepository.findById(device.getFenceId()).orElse(null);
            if (fence != null) {
                validateFenceScope(user, fence, action);
            }
        }
    }

    private void validateFenceScope(User user, Fence fence, String action) {
        if (user == null || user.getRole() == Role.SUPER_ADMIN) {
            return;
        }

        if (user.getRole() == Role.REGIONAL_ADMIN) {
            boolean inProvince = fence.getProvince() != null && user.getAssignedProvinces() != null &&
                    user.getAssignedProvinces().stream().anyMatch(p -> p.getId().equals(fence.getProvince().getId()));
            if (!inProvince) {
                String provName = fence.getProvince() != null ? fence.getProvince().getName() : "another";
                throw new AccessDeniedException("Access denied: You do not have permission to " + action + " devices in " + provName + " Province.");
            }
        } else if (user.getRole() == Role.FIELD_ADMIN) {
            boolean inDistrict = fence.getDistrict() != null && user.getAssignedDistricts() != null &&
                    user.getAssignedDistricts().stream().anyMatch(d -> d.getId().equals(fence.getDistrict().getId()));
            if (!inDistrict) {
                String distName = fence.getDistrict() != null ? fence.getDistrict().getName() : "another";
                throw new AccessDeniedException("Access denied: You do not have permission to " + action + " devices in " + distName + " District.");
            }
        } else if (user.getRole() == Role.MAINTENANCE) {
            throw new AccessDeniedException("Access denied: Maintenance technicians cannot " + action + " devices.");
        }
    }

    private DeviceResponseDTO convertToResponseDTO(Device device) {
        String fenceName = null;
        if (device.getFenceId() != null) {
            fenceName = deviceRepository.findFenceNameById(device.getFenceId());
        }

        String sectionCode = null;
        if (device.getSectionId() != null) {
            sectionCode = deviceRepository.findSectionCodeById(device.getSectionId());
        }

        return DeviceResponseDTO.builder()
                .id(device.getId().toString())
                .name(device.getName())
                .serial(device.getSerial())
                .type(device.getType())
                .status(computeDeviceStatus(device).name())
                .voltage(device.getVoltage())
                .signal(device.getSignal())
                .battery(device.getBattery())
                .enabled(device.isEnabled())
                .lastSeen(device.getLastSeen())
                .fence(fenceName)
                .section(sectionCode)
                .createdAt(device.getCreatedAt())
                .updatedAt(device.getUpdatedAt())
                .build();
    }

    private DeviceStatus computeDeviceStatus(Device device) {
        if (!device.isEnabled()) {
            return DeviceStatus.offline;
        }
        if (device.getLastSeen() == null) {
            return device.getStatus() != null ? device.getStatus() : DeviceStatus.offline;
        }
        int expected = 15;
        int grace = 5;
        int timeout = 30;
        if (configurationService != null) {
            ConfigurationService.GatewayTimingConfig timing = configurationService.getGatewayTimingConfig();
            expected = timing.expectedIntervalMinutes();
            grace = timing.graceMinutes();
            timeout = timing.offlineTimeoutMinutes();
        }

        Duration duration = Duration.between(device.getLastSeen(), OffsetDateTime.now());
        long minutes = duration.toMinutes();
        if (minutes <= (expected + grace)) {
            return DeviceStatus.online;
        } else if (minutes <= (expected + grace + timeout)) {
            return DeviceStatus.warning;
        } else {
            return DeviceStatus.offline;
        }
    }
}
