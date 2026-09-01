package com.nerdc.elephantfence.backend.auth.service;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class LoginRateLimiterService {

    private static final int MAX_ATTEMPTS = 5;
    private static final long BLOCK_DURATION_SECONDS = 900; // 15 minutes

    private static class AttemptInfo {
        int count;
        long firstAttemptEpoch;

        AttemptInfo(long nowEpoch) {
            this.count = 1;
            this.firstAttemptEpoch = nowEpoch;
        }
    }

    private final Map<String, AttemptInfo> attempts = new ConcurrentHashMap<>();

    public void checkRateLimit(String key) {
        long now = Instant.now().getEpochSecond();
        AttemptInfo info = attempts.get(key);

        if (info != null) {
            if (now - info.firstAttemptEpoch > BLOCK_DURATION_SECONDS) {
                attempts.remove(key);
            } else if (info.count >= MAX_ATTEMPTS) {
                long remainingSeconds = BLOCK_DURATION_SECONDS - (now - info.firstAttemptEpoch);
                throw new ResponseStatusException(
                        HttpStatus.TOO_MANY_REQUESTS,
                        "Too many failed login attempts. Please try again in " + (remainingSeconds / 60 + 1) + " minutes."
                );
            }
        }
    }

    public void recordFailedAttempt(String key) {
        long now = Instant.now().getEpochSecond();
        attempts.compute(key, (k, info) -> {
            if (info == null || (now - info.firstAttemptEpoch > BLOCK_DURATION_SECONDS)) {
                return new AttemptInfo(now);
            }
            info.count++;
            return info;
        });
    }

    public void resetAttempts(String key) {
        attempts.remove(key);
    }
}
