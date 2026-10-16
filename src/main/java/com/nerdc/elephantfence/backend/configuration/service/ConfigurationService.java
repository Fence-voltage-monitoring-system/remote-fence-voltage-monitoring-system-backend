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
          "coordinateFormat": "DECIMAL_DEGREES",
          "expectedReportingIntervalMinutes": 15,
          "lateArrivalGraceMinutes": 5,
          "offlineTimeoutMinutes": 30,
          "staleDataMinutes": 60,
          "defaultHistoryPeriod": "24h",
          "pageSize": 10,
          "maintenanceMode": false,
          "allowLoginDuringMaintenance": true,
          "readOnlyMode": false,
          "maintenanceMessage": "System is undergoing scheduled maintenance."
        }
        """);

        DEFAULT_CONFIGS.put("voltage", """
        {
          "healthyKv": 5.0,
          "warningKv": 3.0,
          "criticalKv": 1.5,
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
          "fluctuationCount": 4,
          "fluctuationWindowMinutes": 10,
          "abnormalReadingsRequired": 1,
          "healthyReadingsRequired": 2,
          "cooldownMinutes": 30,
          "autoResolve": true,
          "inAppEnabled": true,
          "websocketEnabled": true,
          "smsEnabled": true,
          "escalationEnabled": true,
          "acknowledgementTimeoutMinutes": 10,
          "maintenanceAcceptanceTimeoutMinutes": 15,
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
          "smsEnabled": true,
          "criticalAlertsEnabled": true,
          "warningAlertsEnabled": true,
          "maintenanceUpdatesEnabled": true,
          "systemUpdatesEnabled": true
        }
        """);

        DEFAULT_CONFIGS.put("retention", """
        {
          "rawTelemetryDays": 90,
          "hourlySummaryDays": 730,
          "dailySummaryDays": 1825,
          "alertIncidentDays": 2555,
          "notificationDays": 30,
          "auditLogDays": 2555,
          "systemLogDays": 90,
          "generatedReportDays": 365,
          "archiveBeforeDeletion": true,
          "automaticCleanupEnabled": true,
          "cleanupSchedule": "DAILY",
          "cleanupTime": "02:00"
        }
        """);

        DEFAULT_CONFIGS.put("security", """
        {
          "minimumPasswordLength": 12,
          "passwordHistoryCount": 5,
          "temporaryPasswordExpiryHours": 24,
          "forceChangeAfterReset": true,
          "failedLoginAttempts": 5,
          "failedAttemptWindowMinutes": 15,
          "accountLockMinutes": 30,
          "requireMfaForSuperAdmins": true,
          "requireMfaForOtherAdmins": false,
          "inactiveAccountDays": 180,
          "notifyOnAccountLockout": true,
          "notifyOnPasswordChange": true,
          "notifyOnNewDeviceLogin": true,
          "notifyOnScopeChange": true
        }
        """);

        DEFAULT_CONFIGS.put("sessions", """
        {
          "maximumSessionHours": 12,
          "idleTimeoutMinutes": 30,
          "rememberMeDays": 7,
          "logoutWarningMinutes": 5,
          "maximumConcurrentSessions": 2,
          "newLoginBehaviour": "REVOKE_OLDEST",
          "requireReauthentication": true,
          "reauthenticationValidityMinutes": 5,
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
          "defaultZoom": 7,
          "healthyColor": "#4ADE80",
          "warningColor": "#FBBF24",
          "criticalColor": "#F87171",
          "offlineColor": "#9CA3AF",
          "unassignedColor": "#4B5563",
          "showGateways": true,
          "showMonitoringDevices": true,
          "showActiveAlerts": true,
          "showMaintenanceWork": true,
          "liveUpdatesEnabled": true,
          "highlightRecentChanges": true,
          "focusCriticalAlerts": true,
          "showStaleDataWarning": true,
          "showOfflineIndicators": true,
          "showProvinceBoundaries": false,
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

        newConfig = configRepository.saveAndFlush(newConfig);
        return toResponseDTO(newConfig);
    }

    public record GatewayTimingConfig(int expectedIntervalMinutes, int graceMinutes, int offlineTimeoutMinutes, int staleDataMinutes) {}

    public GatewayTimingConfig getGatewayTimingConfig() {
        try {
            ConfigurationSaveResponseDTO generalDTO = getSection("general");
            if (generalDTO != null && generalDTO.getValue() != null) {
                Map<String, Object> val = generalDTO.getValue();
                int expected = parseInteger(val.get("expectedReportingIntervalMinutes"), 15);
                int grace = parseInteger(val.get("lateArrivalGraceMinutes"), 5);
                int timeout = parseInteger(val.get("offlineTimeoutMinutes"), 30);
                int stale = parseInteger(val.get("staleDataMinutes"), 60);
                return new GatewayTimingConfig(expected, grace, timeout, stale);
            }
        } catch (Exception e) {
            log.warn("Failed to read gateway timing config, using defaults", e);
        }
        return new GatewayTimingConfig(15, 5, 30, 60);
    }

    public record SystemModeConfig(
            boolean maintenanceMode,
            boolean allowLoginDuringMaintenance,
            boolean readOnlyMode,
            String maintenanceMessage
    ) {}

    public SystemModeConfig getSystemModeConfig() {
        try {
            ConfigurationSaveResponseDTO generalDTO = getSection("general");
            if (generalDTO != null && generalDTO.getValue() != null) {
                Map<String, Object> val = generalDTO.getValue();
                boolean maintenance = parseBoolean(val.get("maintenanceMode"), false);
                boolean allowLogin = parseBoolean(val.get("allowLoginDuringMaintenance"), true);
                boolean readOnly = parseBoolean(val.get("readOnlyMode"), false);
                String message = val.get("maintenanceMessage") instanceof String s && !s.isBlank()
                        ? s
                        : "System is undergoing scheduled maintenance.";
                return new SystemModeConfig(maintenance, allowLogin, readOnly, message);
            }
        } catch (Exception e) {
            log.warn("Failed to read system mode config, using defaults", e);
        }
        return new SystemModeConfig(false, true, false, "System is undergoing scheduled maintenance.");
    }

    public boolean isMaintenanceMode() {
        return getSystemModeConfig().maintenanceMode();
    }

    public boolean isReadOnlyMode() {
        return getSystemModeConfig().readOnlyMode();
    }

    public record VoltageThresholdConfig(
            double healthyKv,
            double warningKv,
            double criticalKv,
            double lowBatteryPercent
    ) {}

    public record AlertRuleConfig(
            boolean lowVoltageEnabled,
            boolean criticalVoltageEnabled,
            boolean wireBreakEnabled,
            boolean gatewayOfflineEnabled,
            boolean lowBatteryEnabled,
            boolean criticalBatteryEnabled,
            double criticalBatteryPercent,
            boolean solarFailureEnabled,
            boolean voltageFluctuationEnabled,
            int fluctuationCount,
            int fluctuationWindowMinutes,
            int abnormalReadingsRequired,
            int healthyReadingsRequired,
            int cooldownMinutes,
            boolean autoResolve,
            boolean inAppEnabled,
            boolean websocketEnabled,
            boolean smsEnabled,
            boolean escalationEnabled,
            int acknowledgementTimeoutMinutes,
            int maintenanceAcceptanceTimeoutMinutes,
            boolean notifySuperAdmins,
            boolean notifyRegionalAdmins,
            boolean notifyFieldAdmins,
            boolean notifyMaintenance
    ) {}

    public AlertRuleConfig getAlertRuleConfig() {
        try {
            ConfigurationSaveResponseDTO alertsDTO = getSection("alerts");
            if (alertsDTO != null && alertsDTO.getValue() != null) {
                Map<String, Object> val = alertsDTO.getValue();
                return new AlertRuleConfig(
                        parseBoolean(val.get("lowVoltageEnabled"), true),
                        parseBoolean(val.get("criticalVoltageEnabled"), true),
                        parseBoolean(val.get("wireBreakEnabled"), true),
                        parseBoolean(val.get("gatewayOfflineEnabled"), true),
                        parseBoolean(val.get("lowBatteryEnabled"), true),
                        parseBoolean(val.get("criticalBatteryEnabled"), true),
                        parseDouble(val.get("criticalBatteryPercent"), 10.0),
                        parseBoolean(val.get("solarFailureEnabled"), true),
                        parseBoolean(val.get("voltageFluctuationEnabled"), true),
                        parseInteger(val.get("fluctuationCount"), 4),
                        parseInteger(val.get("fluctuationWindowMinutes"), 10),
                        parseInteger(val.get("abnormalReadingsRequired"), 1),
                        parseInteger(val.get("healthyReadingsRequired"), 2),
                        parseInteger(val.get("cooldownMinutes"), 30),
                        parseBoolean(val.get("autoResolve"), true),
                        parseBoolean(val.get("inAppEnabled"), true),
                        parseBoolean(val.get("websocketEnabled"), true),
                        parseBoolean(val.get("smsEnabled"), true),
                        parseBoolean(val.get("escalationEnabled"), true),
                        parseInteger(val.get("acknowledgementTimeoutMinutes"), 10),
                        parseInteger(val.get("maintenanceAcceptanceTimeoutMinutes"), 15),
                        parseBoolean(val.get("notifySuperAdmins"), true),
                        parseBoolean(val.get("notifyRegionalAdmins"), true),
                        parseBoolean(val.get("notifyFieldAdmins"), true),
                        parseBoolean(val.get("notifyMaintenance"), true)
                );
            }
        } catch (Exception e) {
            log.warn("Failed to read alert rules config, using defaults", e);
        }
        return new AlertRuleConfig(true, true, true, true, true, true, 10.0, true, true, 4, 10, 1, 2, 30, true, true, true, true, true, 10, 15, true, true, true, true);
    }

    public VoltageThresholdConfig getVoltageThresholdConfig() {
        try {
            ConfigurationSaveResponseDTO voltageDTO = getSection("voltage");
            if (voltageDTO != null && voltageDTO.getValue() != null) {
                Map<String, Object> val = voltageDTO.getValue();
                double healthy = parseDouble(val.get("healthyKv"), 5.0);
                double warning = parseDouble(val.get("warningKv"), 3.0);
                double critical = parseDouble(val.get("criticalKv"), 1.5);
                double lowBattery = parseDouble(val.get("lowBatteryPercent"), 20.0);
                return new VoltageThresholdConfig(healthy, warning, critical, lowBattery);
            }
        } catch (Exception e) {
            log.warn("Failed to read voltage threshold config, using defaults", e);
        }
        return new VoltageThresholdConfig(5.0, 3.0, 1.5, 20.0);
    }

    public record SecurityPolicyConfig(
            int minimumPasswordLength,
            int temporaryPasswordExpiryHours,
            boolean forceChangeAfterReset,
            int failedLoginAttempts,
            int failedAttemptWindowMinutes,
            int accountLockMinutes,
            boolean notifyOnAccountLockout,
            boolean notifyOnPasswordChange,
            boolean notifyOnNewDeviceLogin,
            boolean notifyOnScopeChange
    ) {}

    public SecurityPolicyConfig getSecurityPolicyConfig() {
        try {
            ConfigurationSaveResponseDTO dto = getSection("security");
            if (dto != null && dto.getValue() != null) {
                Map<String, Object> val = dto.getValue();
                return new SecurityPolicyConfig(
                        parseInteger(val.get("minimumPasswordLength"), 12),
                        parseInteger(val.get("temporaryPasswordExpiryHours"), 24),
                        parseBoolean(val.get("forceChangeAfterReset"), true),
                        parseInteger(val.get("failedLoginAttempts"), 5),
                        parseInteger(val.get("failedAttemptWindowMinutes"), 15),
                        parseInteger(val.get("accountLockMinutes"), 30),
                        parseBoolean(val.get("notifyOnAccountLockout"), true),
                        parseBoolean(val.get("notifyOnPasswordChange"), true),
                        parseBoolean(val.get("notifyOnNewDeviceLogin"), true),
                        parseBoolean(val.get("notifyOnScopeChange"), true)
                );
            }
        } catch (Exception e) {
            log.warn("Failed to read security policy config, using defaults", e);
        }
        return new SecurityPolicyConfig(12, 24, true, 5, 15, 30, true, true, true, true);
    }

    public record SessionManagementConfig(
            int maximumSessionHours,
            int idleTimeoutMinutes,
            int rememberMeDays,
            int logoutWarningMinutes,
            int maximumConcurrentSessions,
            String newLoginBehaviour,
            boolean requireReauthentication,
            int reauthenticationValidityMinutes,
            boolean revokeOnPasswordChange,
            boolean revokeOnPasswordReset,
            boolean revokeOnRoleOrScopeChange,
            boolean revokeOnAccountDeactivation,
            boolean revokeOnSuspiciousLogin
    ) {}

    public SessionManagementConfig getSessionManagementConfig() {
        try {
            ConfigurationSaveResponseDTO dto = getSection("sessions");
            if (dto != null && dto.getValue() != null) {
                Map<String, Object> val = dto.getValue();
                return new SessionManagementConfig(
                        parseInteger(val.get("maximumSessionHours"), 12),
                        parseInteger(val.get("idleTimeoutMinutes"), 30),
                        parseInteger(val.get("rememberMeDays"), 7),
                        parseInteger(val.get("logoutWarningMinutes"), 5),
                        parseInteger(val.get("maximumConcurrentSessions"), 2),
                        val.get("newLoginBehaviour") != null ? val.get("newLoginBehaviour").toString().trim() : "REVOKE_OLDEST",
                        parseBoolean(val.get("requireReauthentication"), true),
                        parseInteger(val.get("reauthenticationValidityMinutes"), 5),
                        parseBoolean(val.get("revokeOnPasswordChange"), true),
                        parseBoolean(val.get("revokeOnPasswordReset"), true),
                        parseBoolean(val.get("revokeOnRoleOrScopeChange"), true),
                        parseBoolean(val.get("revokeOnAccountDeactivation"), true),
                        parseBoolean(val.get("revokeOnSuspiciousLogin"), true)
                );
            }
        } catch (Exception e) {
            log.warn("Failed to read session management config, using defaults", e);
        }
        return new SessionManagementConfig(12, 30, 7, 5, 2, "REVOKE_OLDEST", true, 5, true, true, true, true, true);
    }

    private int parseInteger(Object val, int fallback) {
        if (val instanceof Number n) {
            return n.intValue();
        }
        if (val instanceof String s) {
            try {
                return Integer.parseInt(s.trim());
            } catch (NumberFormatException ignored) {}
        }
        return fallback;
    }

    private boolean parseBoolean(Object val, boolean fallback) {
        if (val instanceof Boolean b) {
            return b;
        }
        if (val instanceof String s) {
            return Boolean.parseBoolean(s.trim());
        }
        return fallback;
    }

    private double parseDouble(Object val, double fallback) {
        if (val instanceof Number n) {
            return n.doubleValue();
        }
        if (val instanceof String s) {
            try {
                return Double.parseDouble(s.trim());
            } catch (NumberFormatException ignored) {}
        }
        return fallback;
    }

    public record MapConfiguration(
            double defaultLatitude,
            double defaultLongitude,
            int defaultZoom,
            String healthyColor,
            String warningColor,
            String criticalColor,
            String offlineColor,
            String unassignedColor
    ) {}

    public MapConfiguration getMapConfiguration() {
        try {
            ConfigurationSaveResponseDTO dto = getSection("map");
            if (dto != null && dto.getValue() != null) {
                Map<String, Object> val = dto.getValue();
                return new MapConfiguration(
                        parseDouble(val.get("defaultLatitude"), 7.8731),
                        parseDouble(val.get("defaultLongitude"), 80.7718),
                        parseInteger(val.get("defaultZoom"), 7),
                        val.get("healthyColor") != null ? val.get("healthyColor").toString() : "#4ADE80",
                        val.get("warningColor") != null ? val.get("warningColor").toString() : "#FBBF24",
                        val.get("criticalColor") != null ? val.get("criticalColor").toString() : "#F87171",
                        val.get("offlineColor") != null ? val.get("offlineColor").toString() : "#9CA3AF",
                        val.get("unassignedColor") != null ? val.get("unassignedColor").toString() : "#4B5563"
                );
            }
        } catch (Exception e) {
            log.warn("Failed to read map config, using defaults", e);
        }
        return new MapConfiguration(7.8731, 80.7718, 7, "#4ADE80", "#FBBF24", "#F87171", "#9CA3AF", "#4B5563");
    }

    private static final java.util.regex.Pattern HEX_COLOR_PATTERN = java.util.regex.Pattern.compile("^#[0-9a-fA-F]{6}$");

    private void validateSection(String section, ConfigurationSaveRequestDTO request) {
        if (request == null || request.getValue() == null) {
            throw new IllegalArgumentException("Configuration request and value cannot be null");
        }
        if ("map".equalsIgnoreCase(section)) {
            validateMapConfig(request.getValue());
        }
    }

    private void validateMapConfig(Map<String, Object> map) {
        if (!map.containsKey("defaultLatitude") || map.get("defaultLatitude") == null) {
            throw new IllegalArgumentException("Default latitude is required");
        }
        double lat = parseDouble(map.get("defaultLatitude"), -999.0);
        if (lat < 5.8 || lat > 10.0) {
            throw new IllegalArgumentException("Default latitude must be within Sri Lanka territory (5.8 to 10.0)");
        }

        if (!map.containsKey("defaultLongitude") || map.get("defaultLongitude") == null) {
            throw new IllegalArgumentException("Default longitude is required");
        }
        double lng = parseDouble(map.get("defaultLongitude"), -999.0);
        if (lng < 79.5 || lng > 82.2) {
            throw new IllegalArgumentException("Default longitude must be within Sri Lanka territory (79.5 to 82.2)");
        }

        if (!map.containsKey("defaultZoom") || map.get("defaultZoom") == null) {
            throw new IllegalArgumentException("Default zoom level is required");
        }
        int zoom = parseInteger(map.get("defaultZoom"), -1);
        if (zoom < 5 || zoom > 18) {
            throw new IllegalArgumentException("Default zoom level must be between 5 and 18");
        }

        validateColorField(map, "healthyColor", true);
        validateColorField(map, "warningColor", true);
        validateColorField(map, "criticalColor", true);
        validateColorField(map, "offlineColor", false);
        validateColorField(map, "unassignedColor", false);

        String healthy = String.valueOf(map.get("healthyColor")).trim();
        String warning = String.valueOf(map.get("warningColor")).trim();
        String critical = String.valueOf(map.get("criticalColor")).trim();

        if (healthy.equalsIgnoreCase(warning)) {
            throw new IllegalArgumentException("Healthy and warning colors cannot be identical");
        }
        if (healthy.equalsIgnoreCase(critical)) {
            throw new IllegalArgumentException("Healthy and critical colors cannot be identical");
        }
        if (warning.equalsIgnoreCase(critical)) {
            throw new IllegalArgumentException("Warning and critical colors cannot be identical");
        }
    }

    private void validateColorField(Map<String, Object> map, String fieldName, boolean required) {
        Object val = map.get(fieldName);
        if (val == null) {
            if (required) {
                throw new IllegalArgumentException(fieldName + " is required");
            }
            return;
        }
        String color = val.toString().trim();
        if (!HEX_COLOR_PATTERN.matcher(color).matches()) {
            throw new IllegalArgumentException("Invalid hex color format for " + fieldName + ": " + color);
        }
    }

    @Transactional
    public ConfigurationSaveResponseDTO saveSection(String section, ConfigurationSaveRequestDTO request, UUID userId) {
        validateSection(section, request);

        SystemConfiguration config = configRepository.findBySection(section)
                .orElse(SystemConfiguration.builder()
                        .section(section)
                        .version(0)
                        .build());

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        config.setConfigData(objectMapper.valueToTree(request.getValue()));
        config.setUpdatedBy(user);
        config.setReason(request.getReason());
        config.setVersion(config.getVersion() + 1);

        config = configRepository.saveAndFlush(config);
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
    public void revokeSession(String sessionId, String reason) {
        log.info("Revoking session {} with reason: {}", sessionId, reason);
        sessionRepository.revokeSession(sessionId);
    }

    @Transactional
    public int revokeUserSessions(UUID userId, String reason) {
        log.info("Revoking all active sessions for user {} with reason: {}", userId, reason);
        return sessionRepository.revokeAllUserSessions(userId);
    }

    private ConfigurationSaveResponseDTO toResponseDTO(SystemConfiguration config) {
        String updatedByEmail = null;
        if (config.getUpdatedBy() != null) {
            updatedByEmail = config.getUpdatedBy().getEmail();
        }

        return ConfigurationSaveResponseDTO.builder()
                .section(config.getSection())
                .value(objectMapper.convertValue(config.getConfigData(), new com.fasterxml.jackson.core.type.TypeReference<Map<String,Object>>() {}))
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
