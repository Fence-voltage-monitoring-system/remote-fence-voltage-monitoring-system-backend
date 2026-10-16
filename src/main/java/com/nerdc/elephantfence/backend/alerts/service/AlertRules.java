package com.nerdc.elephantfence.backend.alerts.service;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.nerdc.elephantfence.backend.configuration.repository.SystemConfigurationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
@Component @RequiredArgsConstructor
public class AlertRules {
    private final SystemConfigurationRepository configurations;
    private JsonNode value(String section){return configurations.findBySection(section).map(c->c.getConfigData()).orElse(JsonNodeFactory.instance.objectNode());}
    public int number(String name,int fallback){return Math.max(1,Math.min(10080,value("alerts").path(name).asInt(fallback)));}
    public boolean enabled(String name,boolean fallback){return value("alerts").path(name).asBoolean(fallback);}
    public boolean notifications(){return inAppEnabled() || smsEnabled();}
    public double voltage(String name,double fallback){double v=value("voltage").path(name).asDouble(fallback);return Double.isFinite(v)&&v>=0?v:fallback;}
    public double criticalBatteryPercent(){double v=value("alerts").path("criticalBatteryPercent").asDouble(10.0);return Double.isFinite(v)&&v>=0?v:10.0;}
    public int healthyReadingsRequired(){return number("healthyReadingsRequired", 2);}
    public int abnormalReadingsRequired(){return number("abnormalReadingsRequired", 1);}
    public int cooldownMinutes(){return number("cooldownMinutes", 30);}
    public boolean autoResolve(){return enabled("autoResolve", true);}
    public boolean wireBreakEnabled(){return enabled("wireBreakEnabled", true);}
    public boolean criticalVoltageEnabled(){return enabled("criticalVoltageEnabled", true);}
    public boolean lowVoltageEnabled(){return enabled("lowVoltageEnabled", true);}
    public boolean lowBatteryEnabled(){return enabled("lowBatteryEnabled", true);}
    public boolean criticalBatteryEnabled(){return enabled("criticalBatteryEnabled", true);}
    public boolean solarFailureEnabled(){return enabled("solarFailureEnabled", true);}
    public boolean voltageFluctuationEnabled(){return enabled("voltageFluctuationEnabled", true);}
    public int fluctuationCount(){return number("fluctuationCount", 4);}
    public int fluctuationWindowMinutes(){return number("fluctuationWindowMinutes", 10);}
    public boolean gatewayOfflineEnabled(){return enabled("gatewayOfflineEnabled", true);}
    public boolean escalationEnabled(){return enabled("escalationEnabled", true);}
    public int acknowledgementTimeoutMinutes(){return number("acknowledgementTimeoutMinutes", 10);}
    public int maintenanceAcceptanceTimeoutMinutes(){return number("maintenanceAcceptanceTimeoutMinutes", 15);}
    public boolean notifySuperAdmins(){return enabled("notifySuperAdmins", true);}
    public boolean notifyRegionalAdmins(){return enabled("notifyRegionalAdmins", true);}
    public boolean notifyFieldAdmins(){return enabled("notifyFieldAdmins", true);}
    public boolean notifyMaintenance(){return enabled("notifyMaintenance", true);}
    public boolean websocketEnabled(){return value("notifications").path("websocketEnabled").asBoolean(true) && enabled("websocketEnabled", true);}
    public boolean smsEnabled(){return value("notifications").path("smsEnabled").asBoolean(true) && enabled("smsEnabled", true);}
    public boolean inAppEnabled(){return value("notifications").path("inAppEnabled").asBoolean(true) && enabled("inAppEnabled", true);}
    public boolean categoryEnabled(String category) {
        if (category == null) return true;
        return switch (category.toUpperCase()) {
            case "CRITICAL" -> value("notifications").path("criticalAlertsEnabled").asBoolean(true);
            case "WARNING" -> value("notifications").path("warningAlertsEnabled").asBoolean(true);
            case "MAINTENANCE" -> value("notifications").path("maintenanceUpdatesEnabled").asBoolean(true);
            case "SYSTEM" -> value("notifications").path("systemUpdatesEnabled").asBoolean(true);
            default -> true;
        };
    }
}

