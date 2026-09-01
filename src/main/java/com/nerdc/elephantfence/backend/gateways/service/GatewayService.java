package com.nerdc.elephantfence.backend.gateways.service;

import com.nerdc.elephantfence.backend.gateways.dto.CreateGatewayRequestDTO;
import com.nerdc.elephantfence.backend.gateways.dto.GatewayResponseDTO;
import com.nerdc.elephantfence.backend.gateways.dto.UpdateGatewayRequestDTO;
import com.nerdc.elephantfence.backend.gateways.entity.Gateway;
import com.nerdc.elephantfence.backend.gateways.entity.GatewayStatus;
import com.nerdc.elephantfence.backend.gateways.repository.GatewayRepository;
import com.nerdc.elephantfence.backend.common.security.ResourceAccessValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class GatewayService {

    private final GatewayRepository gatewayRepository;
    private final ResourceAccessValidator resourceAccessValidator;

    @Transactional(readOnly = true)
    public List<GatewayResponseDTO> getAllGateways() {
        return gatewayRepository.findAll().stream()
                .map(this::convertToResponseDTO)
                .toList();
    }

    @Transactional(readOnly = true)
    public GatewayResponseDTO getGatewayById(Long id) {
        Gateway gateway = gatewayRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Gateway not found with ID: " + id));
        resourceAccessValidator.validateGeographicAccess(null, null);
        return convertToResponseDTO(gateway);
    }

    @Transactional
    public GatewayResponseDTO createGateway(CreateGatewayRequestDTO dto) {
        if (gatewayRepository.existsBySerialIgnoreCase(dto.getSerial())) {
            throw new IllegalArgumentException("Gateway already exists with serial: " + dto.getSerial());
        }
        if (gatewayRepository.existsByImei(dto.getImei())) {
            throw new IllegalArgumentException("Gateway already exists with IMEI: " + dto.getImei());
        }

        Gateway gateway = Gateway.builder()
                .name(dto.getName())
                .serial(dto.getSerial().trim())
                .imei(dto.getImei().trim())
                .status(GatewayStatus.offline)
                .signal(0)
                .power(0)
                .firmware(dto.getFirmware())
                .enabled(true)
                .build();

        Gateway saved = gatewayRepository.save(gateway);
        return convertToResponseDTO(saved);
    }

    @Transactional
    public GatewayResponseDTO updateGateway(Long id, UpdateGatewayRequestDTO dto) {
        Gateway gateway = gatewayRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Gateway not found with ID: " + id));

        if (dto.getSerial() != null && !dto.getSerial().equalsIgnoreCase(gateway.getSerial())) {
            if (gatewayRepository.existsBySerialIgnoreCase(dto.getSerial())) {
                throw new IllegalArgumentException("Gateway already exists with serial: " + dto.getSerial());
            }
            gateway.setSerial(dto.getSerial().trim());
        }

        if (dto.getImei() != null && !dto.getImei().equals(gateway.getImei())) {
            if (gatewayRepository.existsByImei(dto.getImei())) {
                throw new IllegalArgumentException("Gateway already exists with IMEI: " + dto.getImei());
            }
            gateway.setImei(dto.getImei().trim());
        }

        if (dto.getName() != null) gateway.setName(dto.getName());
        if (dto.getFirmware() != null) gateway.setFirmware(dto.getFirmware());
        if (dto.getEnabled() != null) gateway.setEnabled(dto.getEnabled());

        Gateway updated = gatewayRepository.save(gateway);
        return convertToResponseDTO(updated);
    }

    @Transactional
    public GatewayResponseDTO toggleEnabled(Long id, boolean enabled) {
        Gateway gateway = gatewayRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Gateway not found with ID: " + id));
        gateway.setEnabled(enabled);
        Gateway updated = gatewayRepository.save(gateway);
        return convertToResponseDTO(updated);
    }

    @Transactional
    public void deleteGateway(Long id) {
        if (!gatewayRepository.existsById(id)) {
            throw new IllegalArgumentException("Gateway not found with ID: " + id);
        }
        gatewayRepository.deleteById(id);
    }

    private GatewayResponseDTO convertToResponseDTO(Gateway gateway) {
        int deviceCount = gatewayRepository.countDevicesByGatewayId(gateway.getId());
        List<String> fenceNames = gatewayRepository.findFenceNamesByGatewayId(gateway.getId());

        return GatewayResponseDTO.builder()
                .id(gateway.getId().toString())
                .name(gateway.getName())
                .serial(gateway.getSerial())
                .imei(gateway.getImei())
                .status(gateway.getStatus().name())
                .signal(gateway.getSignal())
                .power(gateway.getPower())
                .firmware(gateway.getFirmware())
                .enabled(gateway.isEnabled())
                .lastSeen(gateway.getLastSeen())
                .createdAt(gateway.getCreatedAt())
                .updatedAt(gateway.getUpdatedAt())
                .devices(deviceCount)
                .fences(fenceNames)
                .build();
    }
}
