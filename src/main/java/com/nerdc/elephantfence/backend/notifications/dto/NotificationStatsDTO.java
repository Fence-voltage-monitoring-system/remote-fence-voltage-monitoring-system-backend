package com.nerdc.elephantfence.backend.notifications.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationStatsDTO {
    private long inApp;
    private long websocket;
    private long smsDelivered;
    private long unread;
}
