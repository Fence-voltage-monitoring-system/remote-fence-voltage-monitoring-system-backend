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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FenceServiceTest {

    @Mock
    private FenceRepository fenceRepository;

    @Mock
    private ProvinceRepository provinceRepository;

    @Mock
    private DistrictRepository districtRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserService userService;

    @Mock
    private SectionRepository sectionRepository;

    @InjectMocks
    private FenceService fenceService;

    private Province province;
    private District district;
    private Fence fence;
    private User maintenanceUser;
    private UserPrincipal adminPrincipal;
    private User adminUser;

    @BeforeEach
    void setUp() {
        province = Province.builder().id(1L).name("Western").build();
        district = District.builder().id(10L).province(province).name("Colombo").build();
        
        fence = Fence.builder()
                .id(100L)
                .code("F-001")
                .name("Colombo Fence")
                .province(province)
                .district(district)
                .lengthKm(BigDecimal.valueOf(12.5))
                .health(FenceHealth.OFFLINE)
                .backupMaintenanceUsers(new HashSet<>())
                .build();

        maintenanceUser = User.builder()
                .id(UUID.randomUUID())
                .fullName("Maintenance Worker")
                .email("maint@nerdc.lk")
                .role(Role.MAINTENANCE)
                .enabled(true)
                .assignedProvinces(Set.of(province))
                .assignedDistricts(Set.of(district))
                .build();

        UUID adminId = UUID.randomUUID();
        adminPrincipal = new UserPrincipal(adminId, "Admin", "admin@nerdc.lk", "", true, List.of());
        adminUser = User.builder()
                .id(adminId)
                .role(Role.SUPER_ADMIN)
                .enabled(true)
                .build();
    }

    @Test
    void getAllFences_shouldReturnAllFences() {
        when(fenceRepository.findAll()).thenReturn(List.of(fence));
        List<FenceResponseDTO> responses = fenceService.getAllFences(null, null);
        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).getCode()).isEqualTo("F-001");
    }

    @Test
    void createFence_shouldCreateFenceSuccessfully() {
        FenceCreateRequestDTO request = FenceCreateRequestDTO.builder()
                .code("F-001")
                .name("Colombo Fence")
                .provinceId(1L)
                .districtId(10L)
                .lengthKm(BigDecimal.valueOf(12.5))
                .build();

        when(userRepository.findById(adminPrincipal.getId())).thenReturn(Optional.of(adminUser));
        when(fenceRepository.existsByCodeIgnoreCase("F-001")).thenReturn(false);
        when(provinceRepository.findById(1L)).thenReturn(Optional.of(province));
        when(districtRepository.findById(10L)).thenReturn(Optional.of(district));
        when(fenceRepository.save(any(Fence.class))).thenReturn(fence);

        FenceResponseDTO response = fenceService.createFence(request, adminPrincipal);
        assertThat(response).isNotNull();
        assertThat(response.getCode()).isEqualTo("F-001");
    }

    @Test
    void createFence_shouldThrowExceptionForMismatchedLocation() {
        Province central = Province.builder().id(2L).name("Central").build();
        District matale = District.builder().id(20L).province(central).name("Matale").build();

        FenceCreateRequestDTO request = FenceCreateRequestDTO.builder()
                .code("F-002")
                .name("Mismatched Fence")
                .provinceId(1L)
                .districtId(20L)
                .lengthKm(BigDecimal.valueOf(5.0))
                .build();

        when(userRepository.findById(adminPrincipal.getId())).thenReturn(Optional.of(adminUser));
        when(fenceRepository.existsByCodeIgnoreCase("F-002")).thenReturn(false);
        when(provinceRepository.findById(1L)).thenReturn(Optional.of(province));
        when(districtRepository.findById(20L)).thenReturn(Optional.of(matale));

        assertThatThrownBy(() -> fenceService.createFence(request, adminPrincipal))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("does not belong to Province");
    }

    @Test
    void updateFence_shouldUpdateSuccessfully() {
        FenceUpdateRequestDTO request = FenceUpdateRequestDTO.builder()
                .name("Updated Colombo Fence")
                .lengthKm(BigDecimal.valueOf(15.0))
                .build();

        when(userRepository.findById(adminPrincipal.getId())).thenReturn(Optional.of(adminUser));
        when(fenceRepository.findById(100L)).thenReturn(Optional.of(fence));
        when(fenceRepository.save(any(Fence.class))).thenAnswer(invocation -> invocation.getArgument(0));

        FenceResponseDTO response = fenceService.updateFence(100L, request, adminPrincipal);
        assertThat(response.getName()).isEqualTo("Updated Colombo Fence");
        assertThat(response.getLengthKm()).isEqualTo(BigDecimal.valueOf(15.0));
    }

    @Test
    void updateFence_shouldThrowNotFoundForInvalidId() {
        FenceUpdateRequestDTO request = FenceUpdateRequestDTO.builder().name("Updated Name").build();
        when(fenceRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> fenceService.updateFence(999L, request, adminPrincipal))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Fence not found");
    }

    @Test
    void getMaintenanceCandidates_shouldReturnCandidates() {
        when(fenceRepository.findMaintenanceCandidates(1L, 10L)).thenReturn(List.of(maintenanceUser));
        
        UserResponseDTO userResponse = UserResponseDTO.builder()
                .id(maintenanceUser.getId())
                .fullName("Maintenance Worker")
                .role(Role.MAINTENANCE)
                .build();
        when(userService.toUserResponseDTO(maintenanceUser)).thenReturn(userResponse);

        List<UserResponseDTO> candidates = fenceService.getMaintenanceCandidates(null, 1L, 10L);
        assertThat(candidates).hasSize(1);
        assertThat(candidates.get(0).getFullName()).isEqualTo("Maintenance Worker");
    }

    @Test
    void assignMaintenanceTeam_shouldAssignSuccessfully() {
        MaintenanceTeamRequestDTO request = MaintenanceTeamRequestDTO.builder()
                .primaryMaintenanceUserId(maintenanceUser.getId())
                .backupMaintenanceUserIds(Set.of())
                .build();

        when(userRepository.findById(adminPrincipal.getId())).thenReturn(Optional.of(adminUser));
        when(fenceRepository.findById(100L)).thenReturn(Optional.of(fence));
        when(userRepository.findById(maintenanceUser.getId())).thenReturn(Optional.of(maintenanceUser));
        when(fenceRepository.save(any(Fence.class))).thenAnswer(invocation -> invocation.getArgument(0));

        FenceResponseDTO response = fenceService.assignMaintenanceTeam(100L, request, adminPrincipal);
        assertThat(response.getPrimaryMaintenanceUserId()).isEqualTo(maintenanceUser.getId());
    }

    @Test
    void assignMaintenanceTeam_shouldRejectDuplicatePrimaryAndBackup() {
        MaintenanceTeamRequestDTO request = MaintenanceTeamRequestDTO.builder()
                .primaryMaintenanceUserId(maintenanceUser.getId())
                .backupMaintenanceUserIds(Set.of(maintenanceUser.getId()))
                .build();

        assertThatThrownBy(() -> fenceService.assignMaintenanceTeam(100L, request, adminPrincipal))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Primary maintenance user cannot also be assigned as a backup");
    }

    @Test
    void deleteFence_shouldDeleteSuccessfully() {
        when(userRepository.findById(adminPrincipal.getId())).thenReturn(Optional.of(adminUser));
        when(fenceRepository.findById(100L)).thenReturn(Optional.of(fence));
        when(fenceRepository.countAlertsByFenceId(100L)).thenReturn(0L);

        fenceService.deleteFence(100L, adminPrincipal);
        verify(fenceRepository, times(1)).delete(fence);
    }

    @Test
    void deleteFence_shouldBlockDeletionIfAlertsExist() {
        when(userRepository.findById(adminPrincipal.getId())).thenReturn(Optional.of(adminUser));
        when(fenceRepository.findById(100L)).thenReturn(Optional.of(fence));
        when(fenceRepository.countAlertsByFenceId(100L)).thenReturn(3L);

        assertThatThrownBy(() -> fenceService.deleteFence(100L, adminPrincipal))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Cannot delete fence: it has 3 associated alert record(s)");
    }
}
