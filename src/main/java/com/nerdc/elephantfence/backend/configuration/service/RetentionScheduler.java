package com.nerdc.elephantfence.backend.configuration.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.nerdc.elephantfence.backend.configuration.entity.SystemConfiguration;
import com.nerdc.elephantfence.backend.configuration.repository.SystemConfigurationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.concurrent.atomic.AtomicReference;

@Component
@RequiredArgsConstructor
@Slf4j
public class RetentionScheduler {

    private final DataRetentionService retentionService;
    private final SystemConfigurationRepository configRepository;
    private final AtomicReference<LocalDate> lastRunDate = new AtomicReference<>(null);

    private ZoneId getZone() {
        return configRepository.findBySection("general")
                .map(c -> {
                    String tz = c.getConfigData().path("timezone").asText("Asia/Colombo");
                    try { return ZoneId.of(tz); } catch (Exception ignored) { return ZoneId.of("Asia/Colombo"); }
                })
                .orElse(ZoneId.of("Asia/Colombo"));
    }

    @Scheduled(fixedDelay = 60000)
    public void checkAndRunScheduledCleanup() {
        try {
            var configOpt = configRepository.findBySection("retention");
            if (configOpt.isEmpty() || configOpt.get().getConfigData() == null) {
                return;
            }

            JsonNode config = configOpt.get().getConfigData();
            boolean enabled = config.path("automaticCleanupEnabled").asBoolean(true);
            if (!enabled) {
                return;
            }

            String timeStr = config.path("cleanupTime").asText("02:00");
            LocalTime targetTime;
            try {
                targetTime = LocalTime.parse(timeStr);
            } catch (Exception e) {
                targetTime = LocalTime.of(2, 0);
            }

            ZonedDateTime now = ZonedDateTime.now(getZone());
            LocalDate today = now.toLocalDate();
            LocalTime currentTime = now.toLocalTime();

            // Run if current time is within 1 minute of targetTime and hasn't already run today
            if (currentTime.getHour() == targetTime.getHour() && currentTime.getMinute() == targetTime.getMinute()) {
                if (!today.equals(lastRunDate.get())) {
                    lastRunDate.set(today);
                    log.info("Triggering scheduled retention cleanup for date {}", today);
                    retentionService.executeCleanup(false, "Automated scheduled retention cleanup");
                }
            }
        } catch (Exception e) {
            log.error("Error in retention cleanup scheduler check", e);
        }
    }
}
