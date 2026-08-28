package com.nerdc.elephantfence.backend.alerts.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AlertResponseDTO {
    private Long id;
    private String code;
    private String title;
    private String type;
    private String severity; // 'CRITICAL', 'WARNING'
    private String province;
    private String district;
    private String fence;
    private String section;
    private String value;
    private String threshold;
    private String detected;
    private String status; // 'UNACKNOWLEDGED', 'ACKNOWLEDGED', 'ASSIGNED', 'IN_PROGRESS', 'UNDER_MAINTENANCE', 'RESOLVED'
    private String assignee;
    private Object assigneeId; // Long mock ID or UUID string
    private String assignmentStatus; // 'UNASSIGNED', 'AWAITING_ACCEPTANCE', 'ACCEPTED', 'DECLINED', 'ESCALATED', 'REASSIGNED', 'COMPLETED'
    private String assignmentSource; // 'AUTO_PRIMARY', 'ADMIN_ASSIGNMENT', 'BACKUP_CLAIM', 'NONE'
    private String assignedAt;
    private String acceptanceDeadline;
    private List<MaintenanceStaffOptionDTO> backupUsers;
    private List<MaintenanceStaffOptionDTO> eligibleMaintenanceStaff;
    private String device;
    private List<String> comments;
    private List<AlertEventDTO> timeline;
    private String acknowledgedBy;
    private String acknowledgedAt;
    private String resolutionType; // 'AUTO_RECOVERY', 'MANUAL'
    private Integer healthyReadingsReceived;
    private Integer healthyReadingsRequired;
}
