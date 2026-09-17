package com.nerdc.elephantfence.backend.notifications.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${app.mail.from:alerts@dwc.gov.lk}")
    private String fromEmail;

    @Value("${app.mail.enabled:true}")
    private boolean mailEnabled;

    @Async
    public void sendAlertEmail(String toEmail, String recipientName, String alertTitle, String alertMessage, String fenceName, String severity) {
        if (!mailEnabled || toEmail == null || toEmail.isBlank()) {
            log.info("Email notifications disabled or recipient email missing. Skipping alert email to {}", toEmail);
            return;
        }

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(toEmail);
            helper.setSubject("🚨 [" + severity.toUpperCase() + "] " + alertTitle + " - " + fenceName);

            String htmlContent = """
                <!DOCTYPE html>
                <html>
                <head>
                    <style>
                        body { font-family: Arial, sans-serif; background-color: #f4f6f4; margin: 0; padding: 20px; }
                        .container { max-width: 600px; background: #ffffff; padding: 24px; border-radius: 12px; border: 1px solid #e0e6e1; }
                        .header { background: #1b3020; color: #ffffff; padding: 16px; border-radius: 8px; text-align: center; }
                        .badge { display: inline-block; padding: 4px 10px; border-radius: 4px; font-weight: bold; font-size: 12px; color: #ffffff; }
                        .critical { background-color: #dc2626; }
                        .warning { background-color: #d97706; }
                        .content { margin-top: 20px; font-size: 14px; color: #334155; line-height: 1.6; }
                        .footer { margin-top: 30px; font-size: 11px; color: #64748b; border-top: 1px solid #e2e8f0; padding-top: 12px; }
                    </style>
                </head>
                <body>
                    <div class="container">
                        <div class="header">
                            <h2 style="margin:0;">Remote Elephant Fence Monitoring</h2>
                            <p style="margin:4px 0 0 0; font-size:12px;">Department of Wildlife Conservation</p>
                        </div>
                        <div class="content">
                            <p>Dear %s,</p>
                            <p>An automated operational alert has been triggered for <strong>%s</strong>:</p>
                            <p><span class="badge %s">%s ALERT</span></p>
                            <div style="background:#f8fafc; border-left: 4px solid #16a34a; padding:12px; margin:16px 0;">
                                <strong>%s</strong><br/>
                                <span>%s</span>
                            </div>
                            <p>Please log in to the Remote Fence Monitoring System to inspect live section telemetry and manage response work orders.</p>
                        </div>
                        <div class="footer">
                            Sent automatically by Remote Elephant Fence Monitoring System. Do not reply directly to this email.
                        </div>
                    </div>
                </body>
                </html>
                """.formatted(
                    recipientName != null ? recipientName : "Officer",
                    fenceName,
                    severity.toLowerCase(),
                    severity.toUpperCase(),
                    alertTitle,
                    alertMessage
            );

            helper.setText(htmlContent, true);
            mailSender.send(message);
            log.info("Alert email successfully dispatched to {} for alert [{}]", toEmail, alertTitle);

        } catch (MessagingException e) {
            log.error("Failed to send alert email to {}: {}", toEmail, e.getMessage());
        } catch (Exception e) {
            log.warn("SMTP mail server unavailable or unconfigured for email to {}. Alert logged.", toEmail);
        }
    }

    @Async
    public void sendSecurityEmail(String toEmail, String subject, String bodyText) {
        if (!mailEnabled || toEmail == null || toEmail.isBlank()) return;
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, false, "UTF-8");
            helper.setFrom(fromEmail);
            helper.setTo(toEmail);
            helper.setSubject("🔒 Security Notification: " + subject);
            helper.setText(bodyText, false);
            mailSender.send(message);
            log.info("Security email dispatched to {}", toEmail);
        } catch (Exception e) {
            log.warn("Failed to send security email to {}: {}", toEmail, e.getMessage());
        }
    }
}
