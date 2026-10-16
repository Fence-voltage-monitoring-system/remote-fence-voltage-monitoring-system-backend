package com.nerdc.elephantfence.backend.auth.service;

import com.nerdc.elephantfence.backend.configuration.service.ConfigurationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
public class LoginRateLimiterService {

    private final ConfigurationService configurationService;

    private static class AttemptInfo {
        int count;
        long windowStartEpoch;
        long lockedAtEpoch;

        AttemptInfo(long nowEpoch) {
            this.count = 1;
            this.windowStartEpoch = nowEpoch;
            this.lockedAtEpoch = 0;
        }
    }

    private final Map<String, AttemptInfo> attempts = new ConcurrentHashMap<>();

    private ConfigurationService.SecurityPolicyConfig getSecurityConfig() {
        if (configurationService != null) {
            try {
                return configurationService.getSecurityPolicyConfig();
            } catch (Exception ignored) {}
        }
        return new ConfigurationService.SecurityPolicyConfig(12, 24, true, 5, 15, 30, true, true, true, true);
    }

    public void checkRateLimit(String key) {
        long now = Instant.now().getEpochSecond();
        AttemptInfo info = attempts.get(key);

        if (info == null) {
            return;
        }

        ConfigurationService.SecurityPolicyConfig config = getSecurityConfig();
        int maxAttempts = config.failedLoginAttempts();
        long windowSeconds = config.failedAttemptWindowMinutes() * 60L;
        long lockDurationSeconds = config.accountLockMinutes() * 60L;

        if (info.lockedAtEpoch > 0) {
            if (now - info.lockedAtEpoch > lockDurationSeconds) {
                attempts.remove(key);
            } else {
                long remainingSeconds = lockDurationSeconds - (now - info.lockedAtEpoch);
                throw new ResponseStatusException(
                        HttpStatus.TOO_MANY_REQUESTS,
                        "Account temporarily locked due to failed login attempts. Please try again in " + (remainingSeconds / 60 + 1) + " minutes."
                );
            }
        } else if (now - info.windowStartEpoch > windowSeconds) {
            attempts.remove(key);
        } else if (info.count >= maxAttempts) {
            info.lockedAtEpoch = now;
            throw new ResponseStatusException(
                    HttpStatus.TOO_MANY_REQUESTS,
                    "Account temporarily locked due to failed login attempts. Please try again in " + config.accountLockMinutes() + " minutes."
            );
        }
    }

    public void recordFailedAttempt(String key) {
        long now = Instant.now().getEpochSecond();
        ConfigurationService.SecurityPolicyConfig config = getSecurityConfig();
        int maxAttempts = config.failedLoginAttempts();
        long windowSeconds = config.failedAttemptWindowMinutes() * 60L;

        attempts.compute(key, (k, info) -> {
            if (info == null || (info.lockedAtEpoch == 0 && now - info.windowStartEpoch > windowSeconds)) {
                AttemptInfo fresh = new AttemptInfo(now);
                if (fresh.count >= maxAttempts) {
                    fresh.lockedAtEpoch = now;
                }
                return fresh;
            }
            info.count++;
            if (info.count >= maxAttempts && info.lockedAtEpoch == 0) {
                info.lockedAtEpoch = now;
            }
            return info;
        });
    }

    public void resetAttempts(String key) {
        attempts.remove(key);
    }
}
