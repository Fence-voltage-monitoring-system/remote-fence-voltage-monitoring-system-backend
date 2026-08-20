package com.nerdc.elephantfence.backend.locations.controller;

import com.nerdc.elephantfence.backend.locations.dto.DistrictResponseDTO;
import com.nerdc.elephantfence.backend.locations.dto.ProvinceResponseDTO;
import com.nerdc.elephantfence.backend.locations.service.LocationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/locations")
@RequiredArgsConstructor
public class LocationController {

    private final LocationService locationService;

    @GetMapping("/provinces")
    public ResponseEntity<List<ProvinceResponseDTO>> getAllProvinces() {
        return ResponseEntity.ok(locationService.getAllProvinces());
    }

    @GetMapping("/districts")
    public ResponseEntity<List<DistrictResponseDTO>> getDistricts(@RequestParam(required = false) Long provinceId) {
        if (provinceId != null) {
            return ResponseEntity.ok(locationService.getDistrictsByProvince(provinceId));
        }
        return ResponseEntity.ok(locationService.getAllDistricts());
    }
}
