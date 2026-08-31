package com.nerdc.elephantfence.backend.dashboard.repository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class DashboardRepository {

    @PersistenceContext
    private EntityManager em;

    // ─── Summary Counts ───────────────────────────────────────────────────────

    public long countTotalFences() {
        return ((Number) em.createNativeQuery("SELECT COUNT(*) FROM fences")
                .getSingleResult()).longValue();
    }

    public long countTotalDevices() {
        return ((Number) em.createNativeQuery("SELECT COUNT(*) FROM devices")
                .getSingleResult()).longValue();
    }

    public long countActiveDevices() {
        return ((Number) em.createNativeQuery(
                "SELECT COUNT(*) FROM devices WHERE status = 'online'")
                .getSingleResult()).longValue();
    }

    public long countCriticalAlerts() {
        return ((Number) em.createNativeQuery(
                "SELECT COUNT(*) FROM alerts WHERE severity = 'critical' AND status IN ('open', 'acknowledged')")
                .getSingleResult()).longValue();
    }

    public long countLowVoltageFences() {
        return ((Number) em.createNativeQuery(
                "SELECT COUNT(DISTINCT f.id) FROM fences f " +
                "JOIN sections s ON s.fence_id = f.id " +
                "JOIN devices d ON d.section_id = s.id " +
                "WHERE d.status = 'warning'")
                .getSingleResult()).longValue();
    }

    // ─── Selected Device Context ──────────────────────────────────────────────

    @SuppressWarnings("unchecked")
    public List<Object[]> findFirstActiveDeviceContext() {
        return em.createNativeQuery(
                "SELECT d.id, d.voltage, d.battery, d.status, " +
                "f.id AS fence_id, f.name AS fence_name, " +
                "s.id AS section_id " +
                "FROM devices d " +
                "JOIN sections s ON s.id = d.section_id " +
                "JOIN fences f ON f.id = s.fence_id " +
                "WHERE d.status IN ('online', 'warning') " +
                "ORDER BY d.last_seen DESC NULLS LAST " +
                "LIMIT 1")
                .getResultList();
    }

    // ─── Device Analytics ─────────────────────────────────────────────────────

    @SuppressWarnings("unchecked")
    public List<Object[]> findDeviceContext(Long deviceId) {
        return em.createNativeQuery(
                "SELECT d.id, d.voltage, d.battery, d.status, " +
                "f.id AS fence_id, f.name AS fence_name, " +
                "s.id AS section_id " +
                "FROM devices d " +
                "JOIN sections s ON s.id = d.section_id " +
                "JOIN fences f ON f.id = s.fence_id " +
                "WHERE d.id = :deviceId")
                .setParameter("deviceId", deviceId)
                .getResultList();
    }

    @SuppressWarnings("unchecked")
    public List<Object[]> findVoltageHistory(Long deviceId, int limit) {
        return em.createNativeQuery(
                "SELECT recorded_at, voltage_kv FROM telemetry_readings " +
                "WHERE device_id = :deviceId " +
                "ORDER BY recorded_at DESC " +
                "LIMIT :limit")
                .setParameter("deviceId", deviceId)
                .setParameter("limit", limit)
                .getResultList();
    }

    public long countAlertsByDeviceAndSeverity(Long deviceId, String severity) {
        return ((Number) em.createNativeQuery(
                "SELECT COUNT(*) FROM alerts WHERE device_id = :deviceId AND severity = :severity " +
                "AND status IN ('open', 'acknowledged')")
                .setParameter("deviceId", deviceId)
                .setParameter("severity", severity)
                .getSingleResult()).longValue();
    }

    public long countResolvedAlertsByDevice(Long deviceId) {
        return ((Number) em.createNativeQuery(
                "SELECT COUNT(*) FROM alerts WHERE device_id = :deviceId AND status = 'resolved'")
                .setParameter("deviceId", deviceId)
                .getSingleResult()).longValue();
    }

    public long countOfflineAlertsByDevice(Long deviceId) {
        return ((Number) em.createNativeQuery(
                "SELECT COUNT(*) FROM alerts WHERE device_id = :deviceId AND severity = 'offline'")
                .setParameter("deviceId", deviceId)
                .getSingleResult()).longValue();
    }
}
