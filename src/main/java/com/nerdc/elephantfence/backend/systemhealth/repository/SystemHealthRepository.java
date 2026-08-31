package com.nerdc.elephantfence.backend.systemhealth.repository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class SystemHealthRepository {

    @PersistenceContext
    private EntityManager em;

    public long countTotalGateways() {
        return ((Number) em.createNativeQuery("SELECT COUNT(*) FROM gateways").getSingleResult()).longValue();
    }

    public long countOnlineGateways() {
        return ((Number) em.createNativeQuery("SELECT COUNT(*) FROM gateways WHERE status = 'online'").getSingleResult()).longValue();
    }

    public long countOfflineGateways() {
        return ((Number) em.createNativeQuery("SELECT COUNT(*) FROM gateways WHERE status = 'offline'").getSingleResult()).longValue();
    }

    public long countLateReportingGateways() {
        return ((Number) em.createNativeQuery("SELECT COUNT(*) FROM gateways WHERE status = 'warning'").getSingleResult()).longValue();
    }

    @SuppressWarnings("unchecked")
    public List<Object[]> findUnhealthyGateways() {
        return em.createNativeQuery(
                "SELECT g.id, g.serial, 'N/A' as fence_code, g.status, g.last_seen " +
                "FROM gateways g " +
                "WHERE g.status IN ('offline', 'warning') " +
                "ORDER BY g.last_seen ASC LIMIT 10")
                .getResultList();
    }

    public Object getLatestTelemetryAt() {
        try {
            return em.createNativeQuery("SELECT MAX(recorded_at) FROM telemetry_readings").getSingleResult();
        } catch (Exception e) {
            return null;
        }
    }
}
