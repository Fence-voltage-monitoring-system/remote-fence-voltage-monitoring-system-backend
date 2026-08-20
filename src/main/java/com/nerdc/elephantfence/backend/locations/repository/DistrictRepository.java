package com.nerdc.elephantfence.backend.locations.repository;

import com.nerdc.elephantfence.backend.locations.entity.District;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DistrictRepository extends JpaRepository<District, Long> {
    List<District> findByProvinceIdOrderByNameAsc(Long provinceId);
    Optional<District> findByProvinceIdAndNameIgnoreCase(Long provinceId, String name);
}
