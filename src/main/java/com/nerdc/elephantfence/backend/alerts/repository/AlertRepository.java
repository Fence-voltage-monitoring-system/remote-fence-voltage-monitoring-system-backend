package com.nerdc.elephantfence.backend.alerts.repository;

import com.nerdc.elephantfence.backend.alerts.entity.Alert;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;

@Repository
public interface AlertRepository extends JpaRepository<Alert, Long> {

    @Query("SELECT a FROM Alert a WHERE " +
           "(:severity IS NULL OR :severity = '' OR a.severity = :severity) AND " +
           "(:provinceId IS NULL OR a.provinceId = :provinceId) AND " +
           "(:fenceId IS NULL OR a.fenceId = :fenceId) AND " +
           "(:type IS NULL OR :type = '' OR a.type = :type) AND " +
           "(:status IS NULL OR :status = '' OR a.status = :status) " +
           "ORDER BY a.createdAt DESC")
    Page<Alert> findWithFilters(
            @Param("severity") String severity,
            @Param("provinceId") Long provinceId,
            @Param("fenceId") Long fenceId,
            @Param("type") String type,
            @Param("status") String status,
            Pageable pageable
    );

    long countBySeverityAndStatusNot(String severity, String status);

    long countByStatus(String status);

    @Query("SELECT COUNT(a) FROM Alert a WHERE a.status IN ('ASSIGNED', 'IN_PROGRESS', 'UNDER_MAINTENANCE')")
    long countUnderMaintenance();

    @Query("SELECT COUNT(a) FROM Alert a WHERE a.status = 'RESOLVED' AND a.resolvedAt >= :startOfToday")
    long countResolvedSince(@Param("startOfToday") OffsetDateTime startOfToday);

    long countByFenceId(Long fenceId);
    
    long countBySectionId(Long sectionId);
}
