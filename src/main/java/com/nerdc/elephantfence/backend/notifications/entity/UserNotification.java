package com.nerdc.elephantfence.backend.notifications.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "user_notifications")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserNotification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id")
    private UUID userId;

    @Column(nullable = false, length = 50)
    private String code;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String message;

    @Column(nullable = false, length = 30)
    private String category; // 'CRITICAL', 'WARNING', 'MAINTENANCE', 'SYSTEM'

    @Column(name = "fence_id")
    private Long fenceId;

    @Column(name = "section_id")
    private Long sectionId;

    @Column(nullable = false)
    @Builder.Default
    private boolean read = false;

    @Column(length = 100)
    private String channels; // Comma-separated (e.g. "IN_APP,WEBSOCKET,SMS")

    @Column(name = "related_alert_code", length = 50)
    private String relatedAlertCode;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;
}
