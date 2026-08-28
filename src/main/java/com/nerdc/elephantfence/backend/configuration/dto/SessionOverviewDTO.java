package com.nerdc.elephantfence.backend.configuration.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SessionOverviewDTO {
    private int totalActiveSessions;
    private int activeUsers;
    private int administratorSessions;
    private int expiringSoon;
    private int suspiciousSessions;
    private List<ActiveSessionRecordDTO> sessions;
}
