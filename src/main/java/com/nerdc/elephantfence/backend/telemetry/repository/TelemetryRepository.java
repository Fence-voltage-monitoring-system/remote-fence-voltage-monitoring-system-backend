package com.nerdc.elephantfence.backend.telemetry.repository;

import com.nerdc.elephantfence.backend.telemetry.entity.TelemetryReading;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TelemetryRepository extends JpaRepository<TelemetryReading, Long> {
    List<TelemetryReading> findByDeviceIdOrderByRecordedAtDesc(Long deviceId);
    List<TelemetryReading> findByDeviceIdOrderByRecordedAtDesc(Long deviceId, Pageable pageable);
}
