package com.nerdc.elephantfence.backend.sections.repository;

import com.nerdc.elephantfence.backend.sections.entity.Section;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SectionRepository extends JpaRepository<Section, Long> {

    List<Section> findByFenceIdOrderByCodeAsc(Long fenceId);

    long countByFenceId(Long fenceId);

    boolean existsByFenceIdAndCodeIgnoreCase(Long fenceId, String code);

    boolean existsByFenceIdAndCodeIgnoreCaseAndIdNot(Long fenceId, String code, Long id);

    @Query(value = "SELECT COUNT(*) FROM alerts WHERE section_id = :sectionId", nativeQuery = true)
    long countAlertsBySectionId(@Param("sectionId") Long sectionId);

    @Query(value = "SELECT t.id as id, t.device_id as deviceId, d.name as deviceName, d.serial as deviceSerial, " +
                   "t.voltage_kv as voltageKv, t.battery as battery, t.signal as signal, t.recorded_at as recordedAt " +
                   "FROM telemetry_readings t " +
                   "JOIN devices d ON t.device_id = d.id " +
                   "WHERE d.section_id = :sectionId " +
                   "ORDER BY t.recorded_at DESC " +
                   "LIMIT :limit", nativeQuery = true)
    List<SectionTelemetryProjection> findTelemetryBySectionId(@Param("sectionId") Long sectionId, @Param("limit") int limit);
}
