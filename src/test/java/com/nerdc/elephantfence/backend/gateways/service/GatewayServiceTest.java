package com.nerdc.elephantfence.backend.gateways.service;

import com.nerdc.elephantfence.backend.common.security.UserPrincipal;
import com.nerdc.elephantfence.backend.fences.entity.Fence;
import com.nerdc.elephantfence.backend.fences.repository.FenceRepository;
import com.nerdc.elephantfence.backend.gateways.dto.GatewayCreateRequestDTO;
import com.nerdc.elephantfence.backend.gateways.dto.GatewayResponseDTO;
import com.nerdc.elephantfence.backend.gateways.dto.GatewayUpdateRequestDTO;
import com.nerdc.elephantfence.backend.gateways.entity.Gateway;
import com.nerdc.elephantfence.backend.gateways.repository.GatewayRepository;
import com.nerdc.elephantfence.backend.locations.entity.District;
import com.nerdc.elephantfence.backend.locations.entity.Province;
import com.nerdc.elephantfence.backend.users.entity.Role;
import com.nerdc.elephantfence.backend.users.entity.User;
import com.nerdc.elephantfence.backend.users.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GatewayServiceTest {

    @Mock
    private GatewayRepository gatewayRepository;

    @Mock
    private FenceRepository fenceRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private GatewayService gatewayService;

    private UserPrincipal superAdminPrincipal;
    private UserPrincipal regionalAdminPrincipal;
    private UserPrincipal fieldAdminPrincipal;

    private User superAdmin;
    private User regionalAdmin;
    private User fieldAdmin;

    private Province westernProvince;
    private Province centralProvince;

    private District colomboDistrict;
    private District kandyDistrict;

    private Fence westernFence;
    private Fence centralFence;

    @BeforeEach
    void setUp() {
        UUID superId = UUID.randomUUID();
        UUID regId = UUID.randomUUID();
        UUID fieldId = UUID.randomUUID();

        superAdminPrincipal = new UserPrincipal(superId, "Super", "super@nerdc.lk", "", true, List.of());
        regionalAdminPrincipal = new UserPrincipal(regId, "Reg", "reg@nerdc.lk", "", true, List.of());
        fieldAdminPrincipal = new UserPrincipal(fieldId, "Field", "field@nerdc.lk", "", true, List.of());

        westernProvince = Province.builder().id(1L).name("Western").build();
        centralProvince = Province.builder().id(2L).name("Central").build();

        colomboDistrict = District.builder().id(10L).name("Colombo").province(westernProvince).build();
        kandyDistrict = District.builder().id(20L).name("Kandy").province(centralProvince).build();

        superAdmin = User.builder()
                .id(superId)
                .role(Role.SUPER_ADMIN)
                .enabled(true)
                .build();

        regionalAdmin = User.builder()
                .id(regId)
                .role(Role.REGIONAL_ADMIN)
                .enabled(true)
                .assignedProvinces(Set.of(westernProvince))
                .build();

        fieldAdmin = User.builder()
                .id(fieldId)
                .role(Role.FIELD_ADMIN)
                .enabled(true)
                .assignedDistricts(Set.of(colomboDistrict))
                .build();

        westernFence = Fence.builder()
                .id(100L)
                .code("FENCE-W")
                .name("Western Fence")
                .province(westernProvince)
                .district(colomboDistrict)
                .build();

        centralFence = Fence.builder()
                .id(200L)
                .code("FENCE-C")
                .name("Central Fence")
                .province(centralProvince)
                .district(kandyDistrict)
                .build();
    }

    @Test
    void createGateway_superAdmin_shouldSucceed() {
        GatewayCreateRequestDTO dto = GatewayCreateRequestDTO.builder()
                .name("Test Gateway")
                .serial("GW-001")
                .imei("123456789")
                .fences(List.of("FENCE-C"))
                .build();

        when(gatewayRepository.existsBySerialIgnoreCase(any())).thenReturn(false);
        when(gatewayRepository.existsByImeiIgnoreCase(any())).thenReturn(false);
        when(fenceRepository.findByCodeIgnoreCase("FENCE-C")).thenReturn(Optional.of(centralFence));
        when(userRepository.findById(superAdminPrincipal.getId())).thenReturn(Optional.of(superAdmin));

        Gateway saved = Gateway.builder()
                .id(1L)
                .name(dto.getName())
                .serial(dto.getSerial())
                .imei(dto.getImei())
                .fences(Set.of(centralFence))
                .build();

        when(gatewayRepository.save(any(Gateway.class))).thenReturn(saved);

        GatewayResponseDTO response = gatewayService.createGateway(dto, superAdminPrincipal);

        assertThat(response.getName()).isEqualTo("Test Gateway");
        assertThat(response.getFences()).contains("Central Fence");
        verify(gatewayRepository).save(any(Gateway.class));
    }

    @Test
    void createGateway_regionalAdmin_withAssignedProvince_shouldSucceed() {
        GatewayCreateRequestDTO dto = GatewayCreateRequestDTO.builder()
                .name("Western Gateway")
                .serial("GW-002")
                .imei("2222222")
                .fences(List.of("FENCE-W"))
                .build();

        when(gatewayRepository.existsBySerialIgnoreCase(any())).thenReturn(false);
        when(gatewayRepository.existsByImeiIgnoreCase(any())).thenReturn(false);
        when(fenceRepository.findByCodeIgnoreCase("FENCE-W")).thenReturn(Optional.of(westernFence));
        when(userRepository.findById(regionalAdminPrincipal.getId())).thenReturn(Optional.of(regionalAdmin));

        Gateway saved = Gateway.builder()
                .id(2L)
                .name(dto.getName())
                .serial(dto.getSerial())
                .imei(dto.getImei())
                .fences(Set.of(westernFence))
                .build();

        when(gatewayRepository.save(any(Gateway.class))).thenReturn(saved);

        GatewayResponseDTO response = gatewayService.createGateway(dto, regionalAdminPrincipal);

        assertThat(response.getName()).isEqualTo("Western Gateway");
        assertThat(response.getFences()).contains("Western Fence");
    }

    @Test
    void createGateway_regionalAdmin_withUnassignedProvince_shouldThrowAccessDenied() {
        GatewayCreateRequestDTO dto = GatewayCreateRequestDTO.builder()
                .name("Central Gateway")
                .serial("GW-003")
                .imei("3333333")
                .fences(List.of("FENCE-C"))
                .build();

        when(gatewayRepository.existsBySerialIgnoreCase(any())).thenReturn(false);
        when(gatewayRepository.existsByImeiIgnoreCase(any())).thenReturn(false);
        when(fenceRepository.findByCodeIgnoreCase("FENCE-C")).thenReturn(Optional.of(centralFence));
        when(userRepository.findById(regionalAdminPrincipal.getId())).thenReturn(Optional.of(regionalAdmin));

        assertThatThrownBy(() -> gatewayService.createGateway(dto, regionalAdminPrincipal))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("You are not assigned to province");
    }

    @Test
    void createGateway_fieldAdmin_withAssignedDistrict_shouldSucceed() {
        GatewayCreateRequestDTO dto = GatewayCreateRequestDTO.builder()
                .name("Colombo Gateway")
                .serial("GW-004")
                .imei("4444444")
                .fences(List.of("FENCE-W"))
                .build();

        when(gatewayRepository.existsBySerialIgnoreCase(any())).thenReturn(false);
        when(gatewayRepository.existsByImeiIgnoreCase(any())).thenReturn(false);
        when(fenceRepository.findByCodeIgnoreCase("FENCE-W")).thenReturn(Optional.of(westernFence));
        when(userRepository.findById(fieldAdminPrincipal.getId())).thenReturn(Optional.of(fieldAdmin));

        Gateway saved = Gateway.builder()
                .id(4L)
                .name(dto.getName())
                .serial(dto.getSerial())
                .imei(dto.getImei())
                .fences(Set.of(westernFence))
                .build();

        when(gatewayRepository.save(any(Gateway.class))).thenReturn(saved);

        GatewayResponseDTO response = gatewayService.createGateway(dto, fieldAdminPrincipal);

        assertThat(response.getName()).isEqualTo("Colombo Gateway");
    }

    @Test
    void createGateway_fieldAdmin_withUnassignedDistrict_shouldThrowAccessDenied() {
        GatewayCreateRequestDTO dto = GatewayCreateRequestDTO.builder()
                .name("Kandy Gateway")
                .serial("GW-005")
                .imei("5555555")
                .fences(List.of("FENCE-C"))
                .build();

        when(gatewayRepository.existsBySerialIgnoreCase(any())).thenReturn(false);
        when(gatewayRepository.existsByImeiIgnoreCase(any())).thenReturn(false);
        when(fenceRepository.findByCodeIgnoreCase("FENCE-C")).thenReturn(Optional.of(centralFence));
        when(userRepository.findById(fieldAdminPrincipal.getId())).thenReturn(Optional.of(fieldAdmin));

        assertThatThrownBy(() -> gatewayService.createGateway(dto, fieldAdminPrincipal))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("You are not assigned to district");
    }
}
