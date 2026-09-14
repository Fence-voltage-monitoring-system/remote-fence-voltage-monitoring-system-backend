package com.nerdc.elephantfence.backend.notifications.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserNotificationPreferencesDTO {
    private boolean soundEnabled;
    private boolean desktopNotificationsEnabled;
    private boolean markAsReadOnOpen;
    private boolean quietHoursEnabled;
    private String quietHoursStart;
    private String quietHoursEnd;
    private boolean groupSimilarNotifications;
    private int groupingWindowMinutes;
    private boolean digestEnabled;
    private int digestIntervalMinutes;
}
