package com.nerdc.elephantfence.backend.notifications.entity;

import com.nerdc.elephantfence.backend.users.entity.User;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.domain.Persistable;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "user_notification_preferences")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserNotificationPreferences implements Persistable<UUID> {

    @Id
    @Column(name = "user_id")
    private UUID userId;

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "user_id")
    private User user;

    @Column(name = "sound_enabled")
    @Builder.Default
    private boolean soundEnabled = true;

    @Column(name = "desktop_notifications_enabled")
    @Builder.Default
    private boolean desktopNotificationsEnabled = true;

    @Column(name = "mark_as_read_on_open")
    @Builder.Default
    private boolean markAsReadOnOpen = true;

    @Column(name = "quiet_hours_enabled")
    @Builder.Default
    private boolean quietHoursEnabled = false;

    @Column(name = "quiet_hours_start")
    @Builder.Default
    private String quietHoursStart = "22:00";

    @Column(name = "quiet_hours_end")
    @Builder.Default
    private String quietHoursEnd = "06:00";

    @Column(name = "group_similar_notifications")
    @Builder.Default
    private boolean groupSimilarNotifications = true;

    @Column(name = "grouping_window_minutes")
    @Builder.Default
    private int groupingWindowMinutes = 15;

    @Column(name = "digest_enabled")
    @Builder.Default
    private boolean digestEnabled = false;

    @Column(name = "digest_interval_minutes")
    @Builder.Default
    private int digestIntervalMinutes = 60;

    @Column(name = "updated_at", nullable = false)
    @Builder.Default
    private OffsetDateTime updatedAt = OffsetDateTime.now();

    @Transient
    @Builder.Default
    private boolean isNewEntity = true;

    @Override
    public UUID getId() {
        return userId;
    }

    @Override
    public boolean isNew() {
        return isNewEntity;
    }

    @PostLoad
    @PostPersist
    void markNotNew() {
        this.isNewEntity = false;
    }
}
