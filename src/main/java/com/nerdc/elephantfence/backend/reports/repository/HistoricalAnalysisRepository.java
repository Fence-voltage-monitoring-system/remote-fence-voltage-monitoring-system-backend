package com.nerdc.elephantfence.backend.reports.repository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;

@Repository
public class HistoricalAnalysisRepository {

    @PersistenceContext
    private EntityManager em;

    @SuppressWarnings("unchecked")
    public List<Object[]> findTelemetryReadings(Long deviceId, OffsetDateTime since) {
        if (deviceId != null) {
            return em.createNativeQuery(
                    "SELECT recorded_at, voltage_kv, battery, signal FROM telemetry_readings " +
                    "WHERE device_id = :deviceId AND recorded_at >= :since " +
                    "ORDER BY recorded_at ASC")
                    .setParameter("deviceId", deviceId)
                    .setParameter("since", since)
                    .getResultList();
        } else {
            return em.createNativeQuery(
                    "SELECT recorded_at, voltage_kv, battery, signal FROM telemetry_readings " +
                    "WHERE recorded_at >= :since " +
                    "ORDER BY recorded_at ASC")
                    .setParameter("since", since)
                    .getResultList();
        }
    }

    public long countAlertsSince(OffsetDateTime since) {
        return ((Number) em.createNativeQuery(
                "SELECT COUNT(*) FROM alerts WHERE created_at >= :since")
                .setParameter("since", since)
                .getSingleResult()).longValue();
    }

    public long countCriticalAlertsSince(OffsetDateTime since) {
        return ((Number) em.createNativeQuery(
                "SELECT COUNT(*) FROM alerts WHERE severity = 'critical' AND created_at >= :since")
                .setParameter("since", since)
                .getSingleResult()).longValue();
    }

    @SuppressWarnings("unchecked")
    public List<Object[]> findAlertTimestampsSince(OffsetDateTime since) {
        return em.createNativeQuery(
                "SELECT created_at FROM alerts WHERE created_at >= :since")
                .setParameter("since", since)
                .getResultList();
    }
}
