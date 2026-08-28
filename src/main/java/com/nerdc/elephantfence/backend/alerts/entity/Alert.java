package com.nerdc.elephantfence.backend.alerts.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "alerts")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Alert {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String code;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(nullable = false, length = 50)
    private String type;

    @Column(nullable = false, length = 20)
    private String severity; // 'CRITICAL', 'WARNING'

    @Column(name = "province_id")
    private Long provinceId;

    @Column(name = "district_id")
    private Long districtId;

    @Column(name = "fence_id")
    private Long fenceId;

    @Column(name = "section_id")
    private Long sectionId;

    @Column(name = "device_id")
    private Long deviceId;

    @Column(name = "detected_voltage_kv", precision = 5, scale = 2)
    private BigDecimal detectedVoltageKv;

    @Column(name = "threshold_voltage_kv", precision = 5, scale = 2)
    private BigDecimal thresholdVoltageKv;

    @Column(nullable = false, length = 30)
    @Builder.Default
    private String status = "UNACKNOWLEDGED"; // 'UNACKNOWLEDGED', 'ACKNOWLEDGED', 'ASSIGNED', 'IN_PROGRESS', 'UNDER_MAINTENANCE', 'RESOLVED'

    @Column(name = "assignment_status", nullable = false, length = 30)
    @Builder.Default
    private String assignmentStatus = "UNASSIGNED"; // 'UNASSIGNED', 'AWAITING_ACCEPTANCE', 'ACCEPTED', 'DECLINED', 'ESCALATED', 'REASSIGNED', 'COMPLETED'

    @Column(name = "assignment_source", nullable = false, length = 30)
    @Builder.Default
    private String assignmentSource = "NONE"; // 'AUTO_PRIMARY', 'ADMIN_ASSIGNMENT', 'BACKUP_CLAIM', 'NONE'

    @Column(name = "assignee_user_id")
    private UUID assigneeUserId;

    @Column(name = "acknowledged_by_user_id")
    private UUID acknowledgedByUserId;

    @Column(name = "assigned_at")
    private OffsetDateTime assignedAt;

    @Column(name = "acceptance_deadline")
    private OffsetDateTime acceptanceDeadline;

    @Column(name = "acknowledged_at")
    private OffsetDateTime acknowledgedAt;

    @Column(name = "resolved_at")
    private OffsetDateTime resolvedAt;

    @Column(name = "resolution_type", length = 30)
    private String resolutionType; // 'AUTO_RECOVERY', 'MANUAL'

    @Column(name = "resolution_summary", columnDefinition = "TEXT")
    private String resolutionSummary;

    @Column(name = "resolution_cause", columnDefinition = "TEXT")
    private String resolutionCause;

    @Column(name = "resolution_actions", columnDefinition = "TEXT")
    private String resolutionActions;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;
}
