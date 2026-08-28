package com.nerdc.elephantfence.backend.gateways.service;

import com.nerdc.elephantfence.backend.common.security.UserPrincipal;
import com.nerdc.elephantfence.backend.fences.entity.Fence;
import com.nerdc.elephantfence.backend.fences.repository.FenceRepository;
import com.nerdc.elephantfence.backend.gateways.dto.*;
import com.nerdc.elephantfence.backend.gateways.entity.Gateway;
import com.nerdc.elephantfence.backend.gateways.repository.GatewayRepository;
import com.nerdc.elephantfence.backend.users.entity.Role;
import com.nerdc.elephantfence.backend.users.entity.User;
import com.nerdc.elephantfence.backend.users.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class GatewayService {

    private final GatewayRepository gatewayRepository;
    private final FenceRepository fenceRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public List<GatewayResponseDTO> getAllGateways() {
        return gatewayRepository.findAll().stream()
                .map(this::toResponseDTO)
                .collect(Collectors.toList());
    }

    @Transactional
    public GatewayResponseDTO createGateway(GatewayCreateRequestDTO dto, UserPrincipal principal) {
        if (gatewayRepository.existsBySerialIgnoreCase(dto.getSerial())) {
            throw new IllegalArgumentException("Gateway serial already exists: " + dto.getSerial());
        }
        if (gatewayRepository.existsByImeiIgnoreCase(dto.getImei())) {
            throw new IllegalArgumentException("Gateway IMEI already exists: " + dto.getImei());
        }

        Set<Fence> resolvedFences = resolveFences(dto.getFences());
        validateRBAC(principal, resolvedFences);

        Gateway gateway = Gateway.builder()
                .name(dto.getName().trim())
                .serial(dto.getSerial().trim())
                .imei(dto.getImei().trim())
                .firmware(dto.getFirmware() != null ? dto.getFirmware().trim() : null)
                .fences(resolvedFences)
                .build();

        gateway = gatewayRepository.save(gateway);
        return toResponseDTO(gateway);
    }

    @Transactional
    public GatewayResponseDTO updateGateway(String idStr, GatewayUpdateRequestDTO dto, UserPrincipal principal) {
        Long id = parseGatewayId(idStr);
        Gateway gateway = gatewayRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Gateway not found: " + idStr));

        validateRBAC(principal, gateway.getFences());

        if (dto.getSerial() != null && !dto.getSerial().equalsIgnoreCase(gateway.getSerial())) {
            if (gatewayRepository.existsBySerialIgnoreCaseAndIdNot(dto.getSerial(), id)) {
                throw new IllegalArgumentException("Gateway serial already exists: " + dto.getSerial());
            }
            gateway.setSerial(dto.getSerial().trim());
        }

        if (dto.getImei() != null && !dto.getImei().equalsIgnoreCase(gateway.getImei())) {
            if (gatewayRepository.existsByImeiIgnoreCaseAndIdNot(dto.getImei(), id)) {
                throw new IllegalArgumentException("Gateway IMEI already exists: " + dto.getImei());
            }
            gateway.setImei(dto.getImei().trim());
        }

        if (dto.getName() != null) {
            gateway.setName(dto.getName().trim());
        }

        if (dto.getFirmware() != null) {
            gateway.setFirmware(dto.getFirmware().trim());
        }

        if (dto.getEnabled() != null) {
            gateway.setEnabled(dto.getEnabled());
        }

        if (dto.getFences() != null) {
            Set<Fence> resolvedFences = resolveFences(dto.getFences());
            validateRBAC(principal, resolvedFences);
            gateway.setFences(resolvedFences);
        }

        gateway = gatewayRepository.save(gateway);
        return toResponseDTO(gateway);
    }

    @Transactional
    public GatewayResponseDTO toggleEnabled(String idStr, boolean enabled, UserPrincipal principal) {
        Long id = parseGatewayId(idStr);
        Gateway gateway = gatewayRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Gateway not found: " + idStr));

        validateRBAC(principal, gateway.getFences());

        gateway.setEnabled(enabled);
        gateway = gatewayRepository.save(gateway);
        return toResponseDTO(gateway);
    }

    @Transactional
    public void deleteGateway(String idStr, UserPrincipal principal) {
        Long id = parseGatewayId(idStr);
        Gateway gateway = gatewayRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Gateway not found: " + idStr));

        validateRBAC(principal, gateway.getFences());

        gatewayRepository.delete(gateway);
    }

    private void validateRBAC(UserPrincipal principal, Set<Fence> targetFences) {
        if (principal == null) {
            throw new AccessDeniedException("User is not authenticated");
        }

        User user = userRepository.findById(principal.getId())
                .orElseThrow(() -> new AccessDeniedException("User not found"));

        if (user.getRole() == Role.SUPER_ADMIN) {
            return;
        }

        if (user.getRole() != Role.REGIONAL_ADMIN && user.getRole() != Role.FIELD_ADMIN) {
            throw new AccessDeniedException("Access denied: Insufficient privileges");
        }

        if (!targetFences.isEmpty()) {
            for (Fence fence : targetFences) {
                if (user.getRole() == Role.REGIONAL_ADMIN) {
                    boolean hasProvince = user.getAssignedProvinces().stream()
                            .anyMatch(p -> p.getId().equals(fence.getProvince().getId()));
                    if (!hasProvince) {
                        throw new AccessDeniedException("Access denied: You are not assigned to province of fence " + fence.getName());
                    }
                } else if (user.getRole() == Role.FIELD_ADMIN) {
                    boolean hasDistrict = user.getAssignedDistricts().stream()
                            .anyMatch(d -> d.getId().equals(fence.getDistrict().getId()));
                    if (!hasDistrict) {
                        throw new AccessDeniedException("Access denied: You are not assigned to district of fence " + fence.getName());
                    }
                }
            }
        }
    }

    private Long parseGatewayId(String idStr) {
        if (idStr == null) return null;
        if (idStr.startsWith("GTW-")) {
            try {
                return Long.parseLong(idStr.substring(4));
            } catch (NumberFormatException e) {
            }
        }
        try {
            return Long.parseLong(idStr);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid gateway ID format: " + idStr);
        }
    }

    private Set<Fence> resolveFences(List<String> fenceIdentifiers) {
        if (fenceIdentifiers == null || fenceIdentifiers.isEmpty()) {
            return new HashSet<>();
        }
        Set<Fence> resolved = new HashSet<>();
        for (String ident : fenceIdentifiers) {
            var fenceOpt = fenceRepository.findByCodeIgnoreCase(ident);
            if (fenceOpt.isEmpty()) {
                fenceOpt = fenceRepository.findAll().stream()
                        .filter(f -> f.getName().equalsIgnoreCase(ident))
                        .findFirst();
            }
            Fence fence = fenceOpt.orElseThrow(() -> new IllegalArgumentException("Fence not found: " + ident));
            resolved.add(fence);
        }
        return resolved;
    }

    private GatewayResponseDTO toResponseDTO(Gateway gateway) {
        List<String> fenceNames = gateway.getFences().stream()
                .map(Fence::getName)
                .collect(Collectors.toList());

        String lastSeenStr = "Not installed";
        if (gateway.getLastSeen() != null) {
            Duration duration = Duration.between(gateway.getLastSeen(), OffsetDateTime.now());
            long minutes = duration.toMinutes();
            if (minutes < 1) {
                lastSeenStr = "Just now";
            } else if (minutes < 60) {
                lastSeenStr = minutes + " min ago";
            } else if (minutes < 24 * 60) {
                lastSeenStr = (minutes / 60) + " hr ago";
            } else {
                lastSeenStr = (minutes / (24 * 60)) + " days ago";
            }
        }

        return GatewayResponseDTO.builder()
                .id("GTW-" + gateway.getId())
                .name(gateway.getName())
                .serial(gateway.getSerial())
                .imei(gateway.getImei())
                .fences(fenceNames)
                .status(gateway.getStatus())
                .signal(gateway.getSignal())
                .power(gateway.getPower())
                .devices(0)
                .lastSeen(lastSeenStr)
                .firmware(gateway.getFirmware())
                .enabled(gateway.isEnabled())
                .build();
    }
}
