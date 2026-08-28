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
import com.nerdc.elephantfence.backend.sections.dto.SectionBulkImportRequestDTO;
import com.nerdc.elephantfence.backend.sections.dto.SectionBulkRowDTO;
import com.nerdc.elephantfence.backend.sections.entity.Section;
import com.nerdc.elephantfence.backend.sections.repository.SectionRepository;
import com.nerdc.elephantfence.backend.users.entity.Role;
import com.nerdc.elephantfence.backend.users.entity.User;
import com.nerdc.elephantfence.backend.users.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SectionService {

    private final SectionRepository sectionRepository;
    private final FenceRepository fenceRepository;
    private final ProvinceRepository provinceRepository;
    private final DistrictRepository districtRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public List<SectionResponseDTO> getByFenceId(Long fenceId) {
        if (!fenceRepository.existsById(fenceId)) {
            throw new IllegalArgumentException("Fence not found with ID: " + fenceId);
        }
        return sectionRepository.findByFenceIdOrderByCodeAsc(fenceId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public SectionResponseDTO getById(Long id) {
        return toResponse(findSection(id));
    }

    @Transactional
    public SectionResponseDTO create(SectionCreateRequestDTO dto, UserPrincipal principal) {
        Fence fence = fenceRepository.findById(dto.getFenceId())
                .orElseThrow(() -> new IllegalArgumentException("Fence not found with ID: " + dto.getFenceId()));

        String code = normalizeCode(dto.getCode());
        if (sectionRepository.existsByFenceIdAndCodeIgnoreCase(dto.getFenceId(), code)) {
            throw new IllegalArgumentException("Section code already exists for this fence: " + code);
        }

        Long provinceId = dto.getProvinceId();
        Long districtId = dto.getDistrictId();
        if (provinceId == null && districtId == null) {
            provinceId = fence.getProvince().getId();
            districtId = fence.getDistrict().getId();
        }

        validateWriteAccess(principal, provinceId, districtId);
        validateLocationOverride(dto.getProvinceId(), dto.getDistrictId());

        Section section = Section.builder()
                .fenceId(dto.getFenceId())
                .code(code)
                .startGps(dto.getStartGps())
                .endGps(dto.getEndGps())
                .lengthKm(dto.getLengthKm())
                .provinceId(provinceId)
                .districtId(districtId)
                .build();

        return toResponse(sectionRepository.save(section));
    }

    @Transactional
    public SectionResponseDTO update(Long id, SectionUpdateRequestDTO dto, UserPrincipal principal) {
        Section section = findSection(id);

        validateWriteAccess(principal, section.getProvinceId(), section.getDistrictId());

        if (dto.getCode() != null) {
            String code = normalizeCode(dto.getCode());
            if (sectionRepository.existsByFenceIdAndCodeIgnoreCaseAndIdNot(section.getFenceId(), code, id)) {
                throw new IllegalArgumentException("Section code already exists for this fence: " + code);
            }
            section.setCode(code);
        }
        if (dto.getStartGps() != null) {
            section.setStartGps(dto.getStartGps());
        }
        if (dto.getEndGps() != null) {
            section.setEndGps(dto.getEndGps());
        }
        if (dto.getLengthKm() != null) {
            section.setLengthKm(dto.getLengthKm());
        }
        if (dto.getProvinceId() != null || dto.getDistrictId() != null) {
            Long provId = dto.getProvinceId() != null ? dto.getProvinceId() : section.getProvinceId();
            Long distId = dto.getDistrictId() != null ? dto.getDistrictId() : section.getDistrictId();

            validateLocationOverride(provId, distId);
            validateWriteAccess(principal, provId, distId);

            section.setProvinceId(provId);
            section.setDistrictId(distId);
        }

        return toResponse(sectionRepository.save(section));
    }

    @Transactional
    public void delete(Long id, UserPrincipal principal) {
        Section section = findSection(id);

        validateWriteAccess(principal, section.getProvinceId(), section.getDistrictId());

        long alertCount = sectionRepository.countAlertsBySectionId(id);
        if (alertCount > 0) {
            throw new IllegalArgumentException(
                    "Cannot delete section: it has " + alertCount + " associated alert record(s) in the system.");
        }
        sectionRepository.delete(section);
    }

    @Transactional(readOnly = true)
    public List<SectionTelemetryResponseDTO> getSectionTelemetry(Long sectionId, int limit) {
        if (!sectionRepository.existsById(sectionId)) {
            throw new IllegalArgumentException("Section not found with ID: " + sectionId);
        }
        return sectionRepository.findTelemetryBySectionId(sectionId, limit).stream()
                .map(proj -> SectionTelemetryResponseDTO.builder()
                        .id(proj.getId())
                        .deviceId(proj.getDeviceId())
                        .deviceName(proj.getDeviceName())
                        .deviceSerial(proj.getDeviceSerial())
                        .voltageKv(proj.getVoltageKv())
                        .battery(proj.getBattery())
                        .signal(proj.getSignal())
                        .recordedAt(proj.getRecordedAt())
                        .build())
                .toList();
    }

    private Section findSection(Long id) {
        return sectionRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Section not found with ID: " + id));
    }

    private String normalizeCode(String code) {
        String normalized = code == null ? "" : code.trim();
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException("Section code is required");
        }
        return normalized;
    }

    private void validateLocationOverride(Long provinceId, Long districtId) {
        if (provinceId == null && districtId == null) {
            return;
        }
        if (provinceId == null || districtId == null) {
            throw new IllegalArgumentException("Section location override requires both province ID and district ID.");
        }

        Province province = provinceRepository.findById(provinceId)
                .orElseThrow(() -> new IllegalArgumentException("Province not found with ID: " + provinceId));
        District district = districtRepository.findById(districtId)
                .orElseThrow(() -> new IllegalArgumentException("District not found with ID: " + districtId));
        if (!district.getProvince().getId().equals(province.getId())) {
            throw new IllegalArgumentException(
                    "District '" + district.getName() + "' does not belong to Province '" + province.getName() + "'");
        }
    }

    @Transactional
    public List<SectionResponseDTO> bulkCreate(SectionBulkImportRequestDTO dto, UserPrincipal principal) {
        Fence fence = fenceRepository.findByCodeIgnoreCase(dto.getFenceCode())
                .orElseThrow(() -> new IllegalArgumentException("Fence not found with code: " + dto.getFenceCode()));

        validateWriteAccess(principal, fence.getProvince().getId(), fence.getDistrict().getId());

        int nextIndex = getNextSectionIndex(fence.getId());
        List<Section> sectionsToSave = new ArrayList<>();

        for (SectionBulkRowDTO row : dto.getRows()) {
            String code = String.format("SEC-%03d", nextIndex++);
            String startGps = (row.getStartLatitude() != null && row.getStartLongitude() != null)
                    ? row.getStartLatitude() + "," + row.getStartLongitude()
                    : null;
            String endGps = (row.getEndLatitude() != null && row.getEndLongitude() != null)
                    ? row.getEndLatitude() + "," + row.getEndLongitude()
                    : null;

            Section section = Section.builder()
                    .fenceId(fence.getId())
                    .code(code)
                    .startGps(startGps)
                    .endGps(endGps)
                    .lengthKm(row.getLengthKm())
                    .provinceId(fence.getProvince().getId())
                    .districtId(fence.getDistrict().getId())
                    .build();

            sectionsToSave.add(section);
        }

        List<Section> saved = sectionRepository.saveAll(sectionsToSave);
        return saved.stream().map(this::toResponse).toList();
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

    private int getNextSectionIndex(Long fenceId) {
        List<Section> existing = sectionRepository.findByFenceIdOrderByCodeAsc(fenceId);
        int maxIndex = 0;
        for (Section s : existing) {
            String code = s.getCode();
            if (code != null && code.startsWith("SEC-")) {
                try {
                    int index = Integer.parseInt(code.substring(4));
                    if (index > maxIndex) {
                        maxIndex = index;
                    }
                } catch (NumberFormatException ignored) {
                }
            }
        }
        return maxIndex + 1;
    }

    private SectionResponseDTO toResponse(Section section) {
        return SectionResponseDTO.builder()
                .id(section.getId())
                .fenceId(section.getFenceId())
                .code(section.getCode())
                .startGps(section.getStartGps())
                .endGps(section.getEndGps())
                .lengthKm(section.getLengthKm())
                .voltageKv(section.getVoltageKv())
                .battery(section.getBattery())
                .status(section.getStatus().name())
                .provinceId(section.getProvinceId())
                .districtId(section.getDistrictId())
                .updatedAt(section.getUpdatedAt())
                .build();
    }
}
