package com.nerdc.elephantfence.backend.configuration.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nerdc.elephantfence.backend.common.security.UserPrincipal;
import com.nerdc.elephantfence.backend.configuration.dto.*;
import com.nerdc.elephantfence.backend.configuration.entity.SystemConfiguration;
import com.nerdc.elephantfence.backend.configuration.entity.UserSession;
import com.nerdc.elephantfence.backend.configuration.repository.SystemConfigurationRepository;
import com.nerdc.elephantfence.backend.configuration.repository.UserSessionRepository;
import com.nerdc.elephantfence.backend.users.entity.User;
import com.nerdc.elephantfence.backend.users.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ConfigurationService {

    private final SystemConfigurationRepository configRepository;
    private final UserSessionRepository sessionRepository;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final Map<String, String> DEFAULT_CONFIGS = new HashMap<>();

    static {
        DEFAULT_CONFIGS.put("general", """
        {
          "systemName": "Remote Elephant Fence Monitoring System",
          "organizationName": "NERDC",
          "systemCode": "EF-MON",
          "supportEmail": "support@nerdc.lk",
          "supportPhone": "+94112223344",
          "timezone": "Asia/Colombo",
          "dateFormat": "YYYY-MM-DD",
          "timeFormat": "24_HOUR",
          "language": "en",
          "voltageUnit": "kV",
          "distanceUnit": "km",
          "coordinateFormat": "DD",
          "expectedReportingIntervalMinutes": 15,
          "lateArrivalGraceMinutes": 5,
          "offlineTimeoutMinutes": 30,
          "staleDataMinutes": 60,
          "defaultHistoryPeriod": "30_DAYS",
          "pageSize": 10,
          "maintenanceMode": false,
          "allowLoginDuringMaintenance": true,
          "readOnlyMode": false,
          "maintenanceMessage": "System is undergoing scheduled maintenance."
        }
        """);

        DEFAULT_CONFIGS.put("voltage", """
        {
          "healthyKv": 8.0,
          "warningKv": 5.0,
          "criticalKv": 3.0,
          "lowBatteryPercent": 20.0
        }
        """);

        DEFAULT_CONFIGS.put("alerts", """
        {
          "lowVoltageEnabled": true,
          "criticalVoltageEnabled": true,
          "wireBreakEnabled": true,
          "gatewayOfflineEnabled": true,
          "lowBatteryEnabled": true,
          "criticalBatteryEnabled": true,
          "criticalBatteryPercent": 10,
          "solarFailureEnabled": true,
          "voltageFluctuationEnabled": true,
          "fluctuationCount": 3,
          "fluctuationWindowMinutes": 10,
          "abnormalReadingsRequired": 2,
          "healthyReadingsRequired": 3,
          "cooldownMinutes": 15,
          "autoResolve": true,
          "inAppEnabled": true,
          "websocketEnabled": true,
          "smsEnabled": false,
          "escalationEnabled": false,
          "acknowledgementTimeoutMinutes": 30,
          "maintenanceAcceptanceTimeoutMinutes": 120,
          "notifySuperAdmins": true,
          "notifyRegionalAdmins": true,
          "notifyFieldAdmins": true,
          "notifyMaintenance": true
        }
        """);

        DEFAULT_CONFIGS.put("notifications", """
        {
          "inAppEnabled": true,
          "websocketEnabled": true,
          "smsEnabled": false,
          "criticalAlertsEnabled": true,
          "warningAlertsEnabled": true,
          "maintenanceUpdatesEnabled": true,
          "systemUpdatesEnabled": false
        }
        """);

        DEFAULT_CONFIGS.put("retention", """
        {
          "rawTelemetryDays": 30,
          "hourlySummaryDays": 90,
          "dailySummaryDays": 365,
          "alertIncidentDays": 365,
          "notificationDays": 90,
          "auditLogDays": 180,
          "systemLogDays": 90,
          "generatedReportDays": 30,
          "archiveBeforeDeletion": true,
          "automaticCleanupEnabled": true,
          "cleanupSchedule": "WEEKLY",
          "cleanupTime": "02:00"
        }
        """);

        DEFAULT_CONFIGS.put("security", """
        {
          "minimumPasswordLength": 8,
          "passwordHistoryCount": 5,
          "temporaryPasswordExpiryHours": 24,
          "forceChangeAfterReset": true,
          "failedLoginAttempts": 5,
          "failedAttemptWindowMinutes": 15,
          "accountLockMinutes": 30,
          "requireMfaForSuperAdmins": false,
          "requireMfaForOtherAdmins": false,
          "inactiveAccountDays": 90,
          "notifyOnAccountLockout": true,
          "notifyOnPasswordChange": true,
          "notifyOnNewDeviceLogin": true,
          "notifyOnScopeChange": true
        }
        """);

        DEFAULT_CONFIGS.put("sessions", """
        {
          "maximumSessionHours": 24,
          "idleTimeoutMinutes": 30,
          "rememberMeDays": 7,
          "logoutWarningMinutes": 5,
          "maximumConcurrentSessions": 3,
          "newLoginBehaviour": "REVOKE_OLDEST",
          "requireReauthentication": false,
          "reauthenticationValidityMinutes": 15,
          "revokeOnPasswordChange": true,
          "revokeOnPasswordReset": true,
          "revokeOnRoleOrScopeChange": true,
          "revokeOnAccountDeactivation": true,
          "revokeOnSuspiciousLogin": true
        }
        """);

        DEFAULT_CONFIGS.put("map", """
        {
          "defaultLatitude": 7.8731,
          "defaultLongitude": 80.7718,
          "defaultZoom": 8,
          "healthyColor": "#10B981",
          "warningColor": "#F59E0B",
          "criticalColor": "#EF4444",
          "offlineColor": "#6B7280",
          "unassignedColor": "#374151",
          "showGateways": true,
          "showMonitoringDevices": true,
          "showActiveAlerts": true,
          "showMaintenanceWork": true,
          "liveUpdatesEnabled": true,
          "highlightRecentChanges": true,
          "focusCriticalAlerts": true,
          "showStaleDataWarning": true,
          "showOfflineIndicators": true,
          "showProvinceBoundaries": true,
          "showDistrictBoundaries": true,
          "showFenceCoverage": true,
          "showAlertOverlay": true
        }
        """);
    }

    @Transactional
    public ConfigurationSaveResponseDTO getSection(String section) {
        Optional<SystemConfiguration> configOpt = configRepository.findBySection(section);
        if (configOpt.isPresent()) {
            return toResponseDTO(configOpt.get());
        }

        JsonNode defaultData;
        String defaultJson = DEFAULT_CONFIGS.get(section.toLowerCase());
        if (defaultJson != null) {
            try {
                defaultData = objectMapper.readTree(defaultJson);
            } catch (Exception e) {
                log.error("Failed to parse default config for section {}", section, e);
                defaultData = objectMapper.createObjectNode();
            }
        } else {
            defaultData = objectMapper.createObjectNode();
        }

        SystemConfiguration newConfig = SystemConfiguration.builder()
                .section(section)
                .configData(defaultData)
                .version(1)
                .build();

        newConfig = configRepository.save(newConfig);
        return toResponseDTO(newConfig);
    }

    @Transactional
    public ConfigurationSaveResponseDTO saveSection(String section, ConfigurationSaveRequestDTO request, UUID userId) {
        SystemConfiguration config = configRepository.findBySection(section)
                .orElse(SystemConfiguration.builder()
                        .section(section)
                        .version(0)
                        .build());

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        config.setConfigData(request.getValue());
        config.setUpdatedBy(user);
        config.setReason(request.getReason());
        config.setVersion(config.getVersion() + 1);

        config = configRepository.save(config);
        return toResponseDTO(config);
    }

    @Transactional(readOnly = true)
    public SessionOverviewDTO getSessionOverview() {
        OffsetDateTime now = OffsetDateTime.now();
        List<UserSession> activeSessions = sessionRepository.findAllActive(now);

        String currentSessionId = getCurrentSessionId();

        List<ActiveSessionRecordDTO> records = activeSessions.stream()
                .map(s -> ActiveSessionRecordDTO.builder()
                        .id(s.getId())
                        .userId(s.getUser().getId())
                        .userName(s.getUser().getFullName())
                        .role(s.getUser().getRole().name())
                        .device(s.getDevice())
                        .browser(s.getBrowser())
                        .ipAddress(s.getIpAddress())
                        .approximateLocation(s.getApproximateLocation())
                        .signedInAt(s.getSignedInAt())
                        .lastActivityAt(s.getLastActivityAt())
                        .expiresAt(s.getExpiresAt())
                        .current(s.getId().equals(currentSessionId))
                        .suspicious(s.isSuspicious())
                        .build())
                .collect(Collectors.toList());

        int totalSessions = records.size();
        int activeUsers = (int) records.stream().map(ActiveSessionRecordDTO::getUserId).distinct().count();
        int adminSessions = (int) records.stream()
                .filter(r -> r.getRole().contains("ADMIN"))
                .count();
        int expiringSoon = (int) records.stream()
                .filter(r -> r.getExpiresAt().isBefore(now.plusHours(1)))
                .count();
        int suspiciousSessions = (int) records.stream()
                .filter(ActiveSessionRecordDTO::isSuspicious)
                .count();

        return SessionOverviewDTO.builder()
                .totalActiveSessions(totalSessions)
                .activeUsers(activeUsers)
                .administratorSessions(adminSessions)
                .expiringSoon(expiringSoon)
                .suspiciousSessions(suspiciousSessions)
                .sessions(records)
                .build();
    }

    @Transactional
    public void revokeSession(String sessionId) {
        sessionRepository.revokeSession(sessionId);
    }

    @Transactional
    public int revokeUserSessions(UUID userId) {
        return sessionRepository.revokeAllUserSessions(userId);
    }

    private ConfigurationSaveResponseDTO toResponseDTO(SystemConfiguration config) {
        String updatedByEmail = null;
        if (config.getUpdatedBy() != null) {
            updatedByEmail = config.getUpdatedBy().getEmail();
        }

        return ConfigurationSaveResponseDTO.builder()
                .section(config.getSection())
                .value(config.getConfigData())
                .updatedBy(updatedByEmail)
                .updatedAt(config.getUpdatedAt() != null ? config.getUpdatedAt() : OffsetDateTime.now())
                .version(config.getVersion())
                .build();
    }

    private String getCurrentSessionId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof UserPrincipal) {
            return ((UserPrincipal) auth.getPrincipal()).getSessionId();
        }
        return null;
    }
}
