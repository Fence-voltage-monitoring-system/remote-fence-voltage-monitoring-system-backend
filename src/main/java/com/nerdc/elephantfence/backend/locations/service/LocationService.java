package com.nerdc.elephantfence.backend.locations.service;

import com.nerdc.elephantfence.backend.locations.dto.DistrictResponseDTO;
import com.nerdc.elephantfence.backend.locations.dto.ProvinceResponseDTO;
import com.nerdc.elephantfence.backend.locations.entity.District;
import com.nerdc.elephantfence.backend.locations.entity.Province;
import com.nerdc.elephantfence.backend.locations.repository.DistrictRepository;
import com.nerdc.elephantfence.backend.locations.repository.ProvinceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class LocationService {

    private final ProvinceRepository provinceRepository;
    private final DistrictRepository districtRepository;

    @Transactional(readOnly = true)
    public List<ProvinceResponseDTO> getAllProvinces() {
        return provinceRepository.findAll(Sort.by(Sort.Direction.ASC, "name"))
                .stream()
                .map(this::toProvinceDTO)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<DistrictResponseDTO> getAllDistricts() {
        return districtRepository.findAll(Sort.by(Sort.Direction.ASC, "name"))
                .stream()
                .map(this::toDistrictDTO)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<DistrictResponseDTO> getDistrictsByProvince(Long provinceId) {
        return districtRepository.findByProvinceIdOrderByNameAsc(provinceId)
                .stream()
                .map(this::toDistrictDTO)
                .toList();
    }

    private ProvinceResponseDTO toProvinceDTO(Province province) {
        return ProvinceResponseDTO.builder()
                .id(province.getId())
                .name(province.getName())
                .build();
    }

    private DistrictResponseDTO toDistrictDTO(District district) {
        return DistrictResponseDTO.builder()
                .id(district.getId())
                .provinceId(district.getProvince().getId())
                .provinceName(district.getProvince().getName())
                .name(district.getName())
                .build();
    }
}
