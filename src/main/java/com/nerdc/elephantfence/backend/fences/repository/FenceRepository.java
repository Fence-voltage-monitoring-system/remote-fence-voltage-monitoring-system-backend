package com.nerdc.elephantfence.backend.fences.repository;

import com.nerdc.elephantfence.backend.fences.entity.Fence;
import com.nerdc.elephantfence.backend.users.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FenceRepository extends JpaRepository<Fence, Long> {
    boolean existsByCodeIgnoreCase(String code);
    boolean existsByCodeIgnoreCaseAndIdNot(String code, Long id);
    Optional<Fence> findByCodeIgnoreCase(String code);
    
    List<Fence> findByProvinceId(Long provinceId);
    List<Fence> findByDistrictId(Long districtId);

    @Query("SELECT DISTINCT u FROM User u " +
           "LEFT JOIN u.assignedProvinces p " +
           "LEFT JOIN u.assignedDistricts d " +
           "WHERE u.enabled = true " +
           "AND u.role IN (com.nerdc.elephantfence.backend.users.entity.Role.MAINTENANCE, com.nerdc.elephantfence.backend.users.entity.Role.FIELD_ADMIN) " +
           "AND (p.id = :provinceId OR d.id = :districtId)")
    List<User> findMaintenanceCandidates(@Param("provinceId") Long provinceId, @Param("districtId") Long districtId);

    @Query(value = "SELECT COUNT(*) FROM alerts WHERE fence_id = :fenceId", nativeQuery = true)
    long countAlertsByFenceId(@Param("fenceId") Long fenceId);
}
