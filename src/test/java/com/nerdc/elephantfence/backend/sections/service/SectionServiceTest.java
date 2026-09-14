package com.nerdc.elephantfence.backend.sections.service;

import com.nerdc.elephantfence.backend.common.security.UserPrincipal;
import com.nerdc.elephantfence.backend.fences.entity.Fence;
import com.nerdc.elephantfence.backend.fences.repository.FenceRepository;
import com.nerdc.elephantfence.backend.locations.entity.District;
import com.nerdc.elephantfence.backend.locations.entity.Province;
import com.nerdc.elephantfence.backend.locations.repository.DistrictRepository;
import com.nerdc.elephantfence.backend.locations.repository.ProvinceRepository;
import com.nerdc.elephantfence.backend.sections.dto.SectionCreateRequestDTO;
import com.nerdc.elephantfence.backend.sections.dto.SectionResponseDTO;
import com.nerdc.elephantfence.backend.sections.dto.SectionUpdateRequestDTO;
import com.nerdc.elephantfence.backend.sections.dto.SectionTelemetryResponseDTO;
import com.nerdc.elephantfence.backend.sections.entity.Section;
import com.nerdc.elephantfence.backend.sections.entity.SectionStatus;
import com.nerdc.elephantfence.backend.sections.repository.SectionRepository;
import com.nerdc.elephantfence.backend.sections.repository.SectionTelemetryProjection;
import com.nerdc.elephantfence.backend.users.entity.Role;
import com.nerdc.elephantfence.backend.users.entity.User;
import com.nerdc.elephantfence.backend.users.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SectionServiceTest {

    @Mock
    private SectionRepository sectionRepository;

    @Mock
    private FenceRepository fenceRepository;

    @Mock
    private ProvinceRepository provinceRepository;

    @Mock
    private DistrictRepository districtRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private SectionService sectionService;

    private Province province;
    private District district;
    private Fence fence;
    private Section section;
    private UserPrincipal adminPrincipal;
    private User adminUser;

    @BeforeEach
    void setUp() {
        province = Province.builder().id(1L).name("Western").build();
        district = District.builder().id(10L).province(province).name("Colombo").build();

        fence = Fence.builder()
                .id(500L)
                .code("F-500")
                .name("Colombo Fence")
                .province(province)
                .district(district)
                .build();

        section = Section.builder()
                .id(100L)
                .fenceId(500L)
                .code("SEC-01")
                .startGps("6.9271,79.8612")
                .endGps("6.9275,79.8615")
                .lengthKm(BigDecimal.valueOf(1.5))
                .voltageKv(BigDecimal.valueOf(8.5))
                .battery(90)
                .status(SectionStatus.HEALTHY)
                .provinceId(1L)
                .districtId(10L)
                .updatedAt(OffsetDateTime.now())
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
    void getByFenceId_shouldReturnSections_whenFenceExists() {
        when(fenceRepository.existsById(500L)).thenReturn(true);
        when(sectionRepository.findByFenceIdOrderByCodeAsc(500L)).thenReturn(List.of(section));

        List<SectionResponseDTO> responses = sectionService.getByFenceId(500L);

        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).getCode()).isEqualTo("SEC-01");
        verify(sectionRepository).findByFenceIdOrderByCodeAsc(500L);
    }

    @Test
    void getByFenceId_shouldThrowException_whenFenceDoesNotExist() {
        when(fenceRepository.existsById(500L)).thenReturn(false);

        assertThatThrownBy(() -> sectionService.getByFenceId(500L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Fence not found with ID: 500");
    }

    @Test
    void getById_shouldReturnSection_whenExists() {
        when(sectionRepository.findById(100L)).thenReturn(Optional.of(section));

        SectionResponseDTO response = sectionService.getById(100L);

        assertThat(response).isNotNull();
        assertThat(response.getCode()).isEqualTo("SEC-01");
    }

    @Test
    void getById_shouldThrowException_whenDoesNotExist() {
        when(sectionRepository.findById(100L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> sectionService.getById(100L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Section not found with ID: 100");
    }

    @Test
    void create_shouldCreateSectionSuccessfully() {
        SectionCreateRequestDTO request = SectionCreateRequestDTO.builder()
                .fenceId(500L)
                .code("SEC-01")
                .startGps("6.9271,79.8612")
                .endGps("6.9275,79.8615")
                .lengthKm(BigDecimal.valueOf(1.5))
                .provinceId(1L)
                .districtId(10L)
                .build();

        when(userRepository.findById(adminPrincipal.getId())).thenReturn(Optional.of(adminUser));
        when(fenceRepository.findById(500L)).thenReturn(Optional.of(fence));
        when(sectionRepository.existsByFenceIdAndCodeIgnoreCase(500L, "SEC-01")).thenReturn(false);
        when(provinceRepository.findById(1L)).thenReturn(Optional.of(province));
        when(districtRepository.findById(10L)).thenReturn(Optional.of(district));
        when(sectionRepository.save(any(Section.class))).thenReturn(section);

        SectionResponseDTO response = sectionService.create(request, adminPrincipal);

        assertThat(response).isNotNull();
        assertThat(response.getCode()).isEqualTo("SEC-01");
        verify(sectionRepository).save(any(Section.class));
    }

    @Test
    void create_shouldThrowException_whenCodeAlreadyExists() {
        SectionCreateRequestDTO request = SectionCreateRequestDTO.builder()
                .fenceId(500L)
                .code("SEC-01")
                .lengthKm(BigDecimal.valueOf(1.5))
                .build();

        when(fenceRepository.findById(500L)).thenReturn(Optional.of(fence));
        when(sectionRepository.existsByFenceIdAndCodeIgnoreCase(500L, "SEC-01")).thenReturn(true);

        assertThatThrownBy(() -> sectionService.create(request, adminPrincipal))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Section code already exists for this fence: SEC-01");
    }

    @Test
    void create_shouldThrowException_whenProvinceAndDistrictAreInconsistent() {
        SectionCreateRequestDTO request = SectionCreateRequestDTO.builder()
                .fenceId(500L)
                .code("SEC-01")
                .lengthKm(BigDecimal.valueOf(1.5))
                .provinceId(1L)
                .districtId(10L)
                .build();

        Province anotherProvince = Province.builder().id(2L).name("Central").build();
        District inconsistentDistrict = District.builder().id(10L).province(anotherProvince).name("Kandy").build();

        when(userRepository.findById(adminPrincipal.getId())).thenReturn(Optional.of(adminUser));
        when(fenceRepository.findById(500L)).thenReturn(Optional.of(fence));
        when(provinceRepository.findById(1L)).thenReturn(Optional.of(province));
        when(districtRepository.findById(10L)).thenReturn(Optional.of(inconsistentDistrict));

        assertThatThrownBy(() -> sectionService.create(request, adminPrincipal))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("District 'Kandy' does not belong to Province 'Western'");
    }

    @Test
    void update_shouldUpdateSectionSuccessfully() {
        SectionUpdateRequestDTO request = SectionUpdateRequestDTO.builder()
                .code("SEC-01-NEW")
                .startGps("6.9000,79.8000")
                .build();

        when(userRepository.findById(adminPrincipal.getId())).thenReturn(Optional.of(adminUser));
        when(sectionRepository.findById(100L)).thenReturn(Optional.of(section));
        when(sectionRepository.existsByFenceIdAndCodeIgnoreCaseAndIdNot(500L, "SEC-01-NEW", 100L)).thenReturn(false);
        when(sectionRepository.save(any(Section.class))).thenAnswer(invocation -> invocation.getArgument(0));

        SectionResponseDTO response = sectionService.update(100L, request, adminPrincipal);

        assertThat(response).isNotNull();
        assertThat(response.getCode()).isEqualTo("SEC-01-NEW");
        assertThat(response.getStartGps()).isEqualTo("6.9000,79.8000");
    }

    @Test
    void delete_shouldDeleteSectionSuccessfully() {
        when(userRepository.findById(adminPrincipal.getId())).thenReturn(Optional.of(adminUser));
        when(sectionRepository.findById(100L)).thenReturn(Optional.of(section));
        when(sectionRepository.countAlertsBySectionId(100L)).thenReturn(0L);

        sectionService.delete(100L, adminPrincipal);

        verify(sectionRepository).delete(section);
    }

    @Test
    void delete_shouldThrowException_whenAssociatedAlertsExist() {
        when(userRepository.findById(adminPrincipal.getId())).thenReturn(Optional.of(adminUser));
        when(sectionRepository.findById(100L)).thenReturn(Optional.of(section));
        when(sectionRepository.countAlertsBySectionId(100L)).thenReturn(5L);

        assertThatThrownBy(() -> sectionService.delete(100L, adminPrincipal))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Cannot delete section: it has 5 associated alert record(s) in the system.");

        verify(sectionRepository, never()).delete(any(Section.class));
    }

    @Test
    void getSectionTelemetry_shouldReturnTelemetry_whenSectionExists() {
        SectionTelemetryProjection proj = mock(SectionTelemetryProjection.class);
        when(proj.getId()).thenReturn(1L);
        when(proj.getDeviceId()).thenReturn(200L);
        when(proj.getDeviceName()).thenReturn("Device 1");
        when(proj.getDeviceSerial()).thenReturn("DEV-123");
        when(proj.getVoltageKv()).thenReturn(BigDecimal.valueOf(9.2));
        when(proj.getBattery()).thenReturn(85);
        when(proj.getSignal()).thenReturn(4);
        when(proj.getRecordedAt()).thenReturn(OffsetDateTime.now());

        when(sectionRepository.existsById(100L)).thenReturn(true);
        when(sectionRepository.findTelemetryBySectionId(100L, 10)).thenReturn(List.of(proj));

        List<SectionTelemetryResponseDTO> telemetryList = sectionService.getSectionTelemetry(100L, 10);

        assertThat(telemetryList).hasSize(1);
        SectionTelemetryResponseDTO response = telemetryList.get(0);
        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getDeviceName()).isEqualTo("Device 1");
        assertThat(response.getVoltageKv()).isEqualTo(BigDecimal.valueOf(9.2));
    }

    @Test
    void getSectionTelemetry_shouldThrowException_whenSectionDoesNotExist() {
        when(sectionRepository.existsById(100L)).thenReturn(false);

        assertThatThrownBy(() -> sectionService.getSectionTelemetry(100L, 10))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Section not found with ID: 100");
    }
}
