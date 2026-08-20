package com.nerdc.elephantfence.backend.locations.service;

import com.nerdc.elephantfence.backend.locations.dto.DistrictResponseDTO;
import com.nerdc.elephantfence.backend.locations.dto.ProvinceResponseDTO;
import com.nerdc.elephantfence.backend.locations.entity.District;
import com.nerdc.elephantfence.backend.locations.entity.Province;
import com.nerdc.elephantfence.backend.locations.repository.DistrictRepository;
import com.nerdc.elephantfence.backend.locations.repository.ProvinceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Sort;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LocationServiceTest {

    @Mock
    private ProvinceRepository provinceRepository;

    @Mock
    private DistrictRepository districtRepository;

    @InjectMocks
    private LocationService locationService;

    private Province province;
    private District district;

    @BeforeEach
    void setUp() {
        province = Province.builder().id(1L).name("Western").build();
        district = District.builder().id(10L).province(province).name("Colombo").build();
    }

    @Test
    void getAllProvinces_shouldReturnProvinceList() {
        when(provinceRepository.findAll(any(Sort.class))).thenReturn(List.of(province));

        List<ProvinceResponseDTO> result = locationService.getAllProvinces();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getName()).isEqualTo("Western");
    }

    @Test
    void getDistrictsByProvince_shouldReturnDistrictList() {
        when(districtRepository.findByProvinceIdOrderByNameAsc(1L)).thenReturn(List.of(district));

        List<DistrictResponseDTO> result = locationService.getDistrictsByProvince(1L);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getName()).isEqualTo("Colombo");
        assertThat(result.get(0).getProvinceName()).isEqualTo("Western");
    }
}
