package com.nerdc.elephantfence.backend.notifications.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class SmsService {

    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${app.sms.enabled:true}")
    private boolean smsEnabled;

    @Value("${app.sms.api-url:https://api.dialog.lk/sms/v1/send}")
    private String smsApiUrl;

    @Value("${app.sms.api-key:mock-dialog-sms-key}")
    private String smsApiKey;

    @Value("${app.sms.sender-id:DWC_FENCE}")
    private String senderId;

    @Async
    public void sendAlertSms(String phoneNumber, String alertTitle, String fenceName, String severity) {
        if (!smsEnabled || phoneNumber == null || phoneNumber.isBlank()) {
            log.info("SMS notifications disabled or phone number missing. Skipping SMS to {}", phoneNumber);
            return;
        }

        String formattedMessage = String.format(
                "[%s - %s] ALERT: %s on %s. Inspect system immediately.",
                senderId,
                severity.toUpperCase(),
                alertTitle,
                fenceName
        );

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", "Bearer " + smsApiKey);

            Map<String, Object> body = Map.of(
                    "recipient", phoneNumber,
                    "message", formattedMessage,
                    "sender", senderId
            );

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);

            log.info("Dispatching SMS alert to {} via Gateway API [{}]: {}", phoneNumber, smsApiUrl, formattedMessage);

            // Execute HTTP SMS Gateway call (with graceful fallback catching API connection failures in dev)
            try {
                restTemplate.postForEntity(smsApiUrl, request, String.class);
                log.info("SMS alert successfully delivered to gateway for {}", phoneNumber);
            } catch (Exception httpErr) {
                log.warn("SMS Gateway endpoint [{}] unreachable or mock environment active. Message logged: {}", smsApiUrl, formattedMessage);
            }

        } catch (Exception e) {
            log.error("Failed to construct or send SMS to {}: {}", phoneNumber, e.getMessage());
        }
    }
}
