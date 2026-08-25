package com.nerdc.elephantfence.backend.gateways.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.nerdc.elephantfence.backend.gateways.dto.CreateGatewayRequestDTO;
import com.nerdc.elephantfence.backend.gateways.dto.GatewayResponseDTO;
import com.nerdc.elephantfence.backend.gateways.dto.UpdateGatewayRequestDTO;
import com.nerdc.elephantfence.backend.gateways.entity.Gateway;
import com.nerdc.elephantfence.backend.gateways.entity.GatewayStatus;
import com.nerdc.elephantfence.backend.gateways.repository.GatewayRepository;

@ExtendWith(MockitoExtension.class)
class GatewayServiceTest {

    @Mock
    private GatewayRepository gatewayRepository;

    @InjectMocks
    private GatewayService gatewayService;

    private Gateway gateway;

    @BeforeEach
    void setUp() {
        gateway = Gateway.builder()
                .id(1L)
                .name("Monaragala Gateway")
                .serial("GW-2026-1004")
                .imei("356938035643809")
                .status(GatewayStatus.offline)
                .signal(0)
                .power(0)
                .firmware("v2.4.1")
                .enabled(true)
                .build();
    }

    @Test
    void getAllGateways_shouldReturnGatewayList() {
        when(gatewayRepository.findAll()).thenReturn(List.of(gateway));
        when(gatewayRepository.countDevicesByGatewayId(1L)).thenReturn(4);
        when(gatewayRepository.findFenceNamesByGatewayId(1L)).thenReturn(List.of("Fence A"));

        List<GatewayResponseDTO> result = gatewayService.getAllGateways();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getName()).isEqualTo("Monaragala Gateway");
        assertThat(result.get(0).getDevices()).isEqualTo(4);
        assertThat(result.get(0).getFences()).containsExactly("Fence A");
    }

    @Test
    void createGateway_shouldSaveAndReturnGateway() {
        CreateGatewayRequestDTO dto = CreateGatewayRequestDTO.builder()
                .name("Monaragala Gateway")
                .serial("GW-2026-1004")
                .imei("356938035643809")
                .firmware("v2.4.1")
                .build();

        when(gatewayRepository.existsBySerialIgnoreCase(dto.getSerial())).thenReturn(false);
        when(gatewayRepository.existsByImei(dto.getImei())).thenReturn(false);
        when(gatewayRepository.save(any(Gateway.class))).thenReturn(gateway);

        GatewayResponseDTO result = gatewayService.createGateway(dto);

        assertThat(result).isNotNull();
        assertThat(result.getSerial()).isEqualTo("GW-2026-1004");
        verify(gatewayRepository, times(1)).save(any(Gateway.class));
    }

    @Test
    void createGateway_shouldThrowExceptionWhenSerialExists() {
        CreateGatewayRequestDTO dto = CreateGatewayRequestDTO.builder()
                .name("Monaragala Gateway")
                .serial("GW-2026-1004")
                .imei("356938035643809")
                .build();

        when(gatewayRepository.existsBySerialIgnoreCase(dto.getSerial())).thenReturn(true);

        assertThatThrownBy(() -> gatewayService.createGateway(dto))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Gateway already exists with serial");
    }

    @Test
    void createGateway_shouldThrowExceptionWhenImeiExists() {
        CreateGatewayRequestDTO dto = CreateGatewayRequestDTO.builder()
                .name("Monaragala Gateway")
                .serial("GW-2026-1004")
                .imei("356938035643809")
                .build();

        when(gatewayRepository.existsBySerialIgnoreCase(dto.getSerial())).thenReturn(false);
        when(gatewayRepository.existsByImei(dto.getImei())).thenReturn(true);

        assertThatThrownBy(() -> gatewayService.createGateway(dto))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Gateway already exists with IMEI");
    }

    @Test
    void updateGateway_shouldUpdateFields() {
        UpdateGatewayRequestDTO dto = UpdateGatewayRequestDTO.builder()
                .name("Updated Gateway")
                .build();

        when(gatewayRepository.findById(1L)).thenReturn(Optional.of(gateway));
        when(gatewayRepository.save(any(Gateway.class))).thenReturn(gateway);

        GatewayResponseDTO result = gatewayService.updateGateway(1L, dto);

        assertThat(result).isNotNull();
        verify(gatewayRepository, times(1)).save(any(Gateway.class));
    }

    @Test
    void deleteGateway_shouldDelete() {
        when(gatewayRepository.existsById(1L)).thenReturn(true);

        gatewayService.deleteGateway(1L);

        verify(gatewayRepository, times(1)).deleteById(1L);
    }
}
