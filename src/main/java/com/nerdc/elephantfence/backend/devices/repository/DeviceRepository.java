package com.nerdc.elephantfence.backend.devices.repository;

import com.nerdc.elephantfence.backend.devices.entity.Device;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface DeviceRepository extends JpaRepository<Device, Long> {
    Optional<Device> findBySerialIgnoreCase(String serial);
    boolean existsBySerialIgnoreCase(String serial);

    @Query(value = "SELECT name FROM fences WHERE id = :fenceId", nativeQuery = true)
    String findFenceNameById(@Param("fenceId") Long fenceId);

    @Query(value = "SELECT code FROM sections WHERE id = :sectionId", nativeQuery = true)
    String findSectionCodeById(@Param("sectionId") Long sectionId);

    @Query(value = "SELECT id FROM fences WHERE LOWER(name) = LOWER(:name)", nativeQuery = true)
    Long findFenceIdByName(@Param("name") String name);

    @Query(value = "SELECT id FROM sections WHERE LOWER(code) = LOWER(:code) AND fence_id = :fenceId", nativeQuery = true)
    Long findSectionIdByCodeAndFenceId(@Param("code") String code, @Param("fenceId") Long fenceId);
}
