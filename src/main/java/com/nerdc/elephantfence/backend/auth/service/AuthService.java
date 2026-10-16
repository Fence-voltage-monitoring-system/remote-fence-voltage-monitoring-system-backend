package com.nerdc.elephantfence.backend.auth.service;

import com.nerdc.elephantfence.backend.auth.dto.ChangePasswordRequestDTO;
import com.nerdc.elephantfence.backend.auth.dto.LoginRequestDTO;
import com.nerdc.elephantfence.backend.auth.dto.LoginResponseDTO;
import com.nerdc.elephantfence.backend.auth.dto.RefreshTokenRequestDTO;
import com.nerdc.elephantfence.backend.common.security.JwtTokenProvider;
import com.nerdc.elephantfence.backend.common.security.UserPrincipal;
import com.nerdc.elephantfence.backend.configuration.entity.UserSession;
import com.nerdc.elephantfence.backend.configuration.repository.UserSessionRepository;
import com.nerdc.elephantfence.backend.configuration.service.ConfigurationService;
import com.nerdc.elephantfence.backend.users.entity.Role;
import com.nerdc.elephantfence.backend.users.entity.User;
import com.nerdc.elephantfence.backend.users.repository.UserRepository;
import com.nerdc.elephantfence.backend.auth.dto.ReauthenticateResponseDTO;
import com.nerdc.elephantfence.backend.users.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.Comparator;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider tokenProvider;
    private final UserRepository userRepository;
    private final UserService userService;
    private final PasswordEncoder passwordEncoder;
    private final LoginRateLimiterService rateLimiterService;
    private final UserSessionRepository sessionRepository;
    private final ConfigurationService configurationService;

    @Transactional
    public LoginResponseDTO login(LoginRequestDTO request, jakarta.servlet.http.HttpServletRequest servletRequest) {
        String clientKey = request.getEmail().toLowerCase().trim();
        rateLimiterService.checkRateLimit(clientKey);

        Authentication authentication;
        try {
            authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            clientKey,
                            request.getPassword()
                    )
            );
        } catch (org.springframework.security.core.AuthenticationException ex) {
            rateLimiterService.recordFailedAttempt(clientKey);
            throw ex;
        }

        rateLimiterService.resetAttempts(clientKey);

        ConfigurationService.SystemModeConfig systemMode = configurationService.getSystemModeConfig();
        if (systemMode.maintenanceMode() && !systemMode.allowLoginDuringMaintenance()) {
            boolean isSuperAdmin = authentication.getAuthorities().stream()
                    .anyMatch(a -> "ROLE_SUPER_ADMIN".equals(a.getAuthority()));
            if (!isSuperAdmin) {
                throw new DisabledException(systemMode.maintenanceMessage());
            }
        }

        UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();
        
        String sessionId = UUID.randomUUID().toString();
        
        String userAgent = servletRequest.getHeader("User-Agent");
        String device = parseDevice(userAgent);
        String browser = parseBrowser(userAgent);
        String ipAddress = getClientIp(servletRequest);
        String location = resolveLocation(ipAddress);

        String accessToken = tokenProvider.generateToken(authentication, sessionId);
        String refreshToken = tokenProvider.generateRefreshToken(userPrincipal.getId(), sessionId);

        User user = userRepository.findByIdWithProvincesAndDistricts(userPrincipal.getId())
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        user.setLastLoginAt(OffsetDateTime.now());
        userRepository.save(user);

        ConfigurationService.SessionManagementConfig sessionConfig = null;
        try {
            if (configurationService != null) {
                sessionConfig = configurationService.getSessionManagementConfig();
            }
        } catch (Exception ignored) {}

        if (sessionConfig != null && sessionRepository != null) {
            int maxConcurrent = sessionConfig.maximumConcurrentSessions();
            if (maxConcurrent > 0) {
                var activeSessions = sessionRepository.findActiveByUserId(userPrincipal.getId(), OffsetDateTime.now());
                if (activeSessions != null && activeSessions.size() >= maxConcurrent) {
                    if ("REJECT_NEW".equalsIgnoreCase(sessionConfig.newLoginBehaviour())) {
                        throw new org.springframework.web.server.ResponseStatusException(
                                org.springframework.http.HttpStatus.CONFLICT,
                                "Maximum concurrent active sessions reached (" + maxConcurrent + "). Please sign out from another device."
                        );
                    } else {
                        // REVOKE_OLDEST (default)
                        activeSessions.sort(Comparator.comparing(
                                s -> s.getSignedInAt() != null ? s.getSignedInAt() : OffsetDateTime.MIN
                        ));
                        int toRemove = activeSessions.size() - maxConcurrent + 1;
                        for (int i = 0; i < toRemove && i < activeSessions.size(); i++) {
                            sessionRepository.delete(activeSessions.get(i));
                        }
                    }
                }
            }
        }

        OffsetDateTime expiresAt;
        if (Boolean.TRUE.equals(request.getRememberMe())) {
            int rememberDays = (sessionConfig != null && sessionConfig.rememberMeDays() > 0)
                    ? sessionConfig.rememberMeDays() : 7;
            expiresAt = OffsetDateTime.now().plusDays(rememberDays);
        } else {
            int maxHours = (sessionConfig != null && sessionConfig.maximumSessionHours() > 0)
                    ? sessionConfig.maximumSessionHours() : 12;
            expiresAt = OffsetDateTime.now().plusHours(maxHours);
        }

        UserSession session = UserSession.builder()
                .id(sessionId)
                .user(user)
                .device(device)
                .browser(browser)
                .ipAddress(ipAddress)
                .approximateLocation(location)
                .expiresAt(expiresAt)
                .build();
        sessionRepository.save(session);

        return LoginResponseDTO.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .user(userService.toUserResponseDTO(user))
                .build();
    }

    @Transactional
    public LoginResponseDTO refreshToken(RefreshTokenRequestDTO request) {
        String token = request.getRefreshToken();
        if (!tokenProvider.validateToken(token)) {
            throw new IllegalArgumentException("Invalid or expired refresh token");
        }

        UUID userId = tokenProvider.getUserIdFromJWT(token);
        String sessionId = tokenProvider.getSessionIdFromJWT(token);

        if (sessionId != null) {
            UserSession session = sessionRepository.findById(sessionId)
                    .orElseThrow(() -> new IllegalArgumentException("Session has been revoked or expired"));
            if (session.getExpiresAt().isBefore(OffsetDateTime.now())) {
                sessionRepository.delete(session);
                throw new IllegalArgumentException("Session has been revoked or expired");
            }
            int maxHours = 12;
            try {
                if (configurationService != null) {
                    var sc = configurationService.getSessionManagementConfig();
                    if (sc != null && sc.maximumSessionHours() > 0) {
                        maxHours = sc.maximumSessionHours();
                    }
                }
            } catch (Exception ignored) {}
            session.setExpiresAt(OffsetDateTime.now().plusHours(maxHours));
            sessionRepository.save(session);
        }

        User user = userRepository.findByIdWithProvincesAndDistricts(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        if (!user.isEnabled()) {
            throw new IllegalStateException("User account is disabled");
        }

        ConfigurationService.SystemModeConfig systemMode = configurationService.getSystemModeConfig();
        if (systemMode.maintenanceMode() && !systemMode.allowLoginDuringMaintenance()) {
            if (user.getRole() != Role.SUPER_ADMIN) {
                throw new DisabledException(systemMode.maintenanceMessage());
            }
        }

        String newAccessToken = tokenProvider.generateTokenFromUserId(user.getId(), 86400000L, sessionId);
        String newRefreshToken = tokenProvider.generateRefreshToken(user.getId(), sessionId);

        return LoginResponseDTO.builder()
                .accessToken(newAccessToken)
                .refreshToken(newRefreshToken)
                .tokenType("Bearer")
                .user(userService.toUserResponseDTO(user))
                .build();
    }

    @Transactional
    public void changePassword(UUID userId, ChangePasswordRequestDTO request) {
        changePassword(userId, request, null);
    }

    @Transactional
    public void changePassword(UUID userId, ChangePasswordRequestDTO request, String currentSessionId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPasswordHash())) {
            throw new IllegalArgumentException("Current password does not match");
        }

        int minLength = 12;
        try {
            if (configurationService != null && configurationService.getSecurityPolicyConfig() != null) {
                minLength = configurationService.getSecurityPolicyConfig().minimumPasswordLength();
            }
        } catch (Exception ignored) {}

        if (request.getNewPassword() == null || request.getNewPassword().length() < minLength) {
            throw new IllegalArgumentException("New password must be at least " + minLength + " characters long");
        }

        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        user.setPasswordChangeRequired(false);
        user.setPasswordChangedAt(OffsetDateTime.now());
        userRepository.save(user);

        try {
            if (configurationService != null && sessionRepository != null) {
                var sessionConfig = configurationService.getSessionManagementConfig();
                if (sessionConfig != null && sessionConfig.revokeOnPasswordChange()) {
                    String activeSessionId = currentSessionId;
                    if (activeSessionId == null) {
                        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
                        if (auth != null && auth.getPrincipal() instanceof UserPrincipal p) {
                            activeSessionId = p.getSessionId();
                        }
                    }
                    if (activeSessionId != null) {
                        sessionRepository.revokeOtherUserSessions(userId, activeSessionId);
                    }
                }
            }
        } catch (Exception ex) {
            log.warn("Failed to revoke other sessions after password change for user {}", userId, ex);
        }
    }

    @Transactional(readOnly = true)
    public ReauthenticateResponseDTO reauthenticate(UUID userId, String password) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        if (!passwordEncoder.matches(password, user.getPasswordHash())) {
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.UNAUTHORIZED, "Invalid password"
            );
        }

        int validityMinutes = 5;
        try {
            if (configurationService != null) {
                var sessionConfig = configurationService.getSessionManagementConfig();
                if (sessionConfig != null && sessionConfig.reauthenticationValidityMinutes() > 0) {
                    validityMinutes = sessionConfig.reauthenticationValidityMinutes();
                }
            }
        } catch (Exception ignored) {}

        OffsetDateTime expiresAt = OffsetDateTime.now().plusMinutes(validityMinutes);

        return ReauthenticateResponseDTO.builder()
                .authenticated(true)
                .validityMinutes(validityMinutes)
                .expiresAt(expiresAt)
                .message("Re-authentication successful")
                .build();
    }

    private String parseDevice(String userAgent) {
        if (userAgent == null) return "Unknown";
        String ua = userAgent.toLowerCase();
        if (ua.contains("mobi") || ua.contains("android") || ua.contains("iphone") || ua.contains("ipad")) {
            if (ua.contains("iphone")) return "iPhone";
            if (ua.contains("ipad")) return "iPad";
            if (ua.contains("android")) return "Android Mobile";
            return "Mobile Device";
        }
        if (ua.contains("windows")) return "Windows Desktop";
        if (ua.contains("macintosh") || ua.contains("mac os")) return "macOS Desktop";
        if (ua.contains("linux")) return "Linux Desktop";
        return "Desktop";
    }

    private String parseBrowser(String userAgent) {
        if (userAgent == null) return "Unknown";
        String ua = userAgent.toLowerCase();
        if (ua.contains("edg/")) return "Edge";
        if (ua.contains("chrome/") && !ua.contains("chromium")) return "Chrome";
        if (ua.contains("safari/") && !ua.contains("chrome")) return "Safari";
        if (ua.contains("firefox/")) return "Firefox";
        if (ua.contains("opr/") || ua.contains("opera/")) return "Opera";
        return "Unknown Browser";
    }

    private String getClientIp(jakarta.servlet.http.HttpServletRequest request) {
        String xfHeader = request.getHeader("X-Forwarded-For");
        if (xfHeader == null || xfHeader.isEmpty() || "unknown".equalsIgnoreCase(xfHeader)) {
            return request.getRemoteAddr();
        }
        return xfHeader.split(",")[0].trim();
    }

    private String resolveLocation(String ipAddress) {
        if (ipAddress == null) return "Unknown";
        if (ipAddress.equals("127.0.0.1") || ipAddress.equals("0:0:0:0:0:0:0:1")) {
            return "Localhost";
        }
        return "Colombo, Sri Lanka";
    }

    @Transactional
    public java.util.Map<String, Object> revokeOtherSessions(UserPrincipal userPrincipal) {
        UUID userId = (userPrincipal != null) ? userPrincipal.getId() : userService.getAllUsers().get(0).getId();
        String currentSessionId = (userPrincipal != null) ? userPrincipal.getSessionId() : null;

        var activeSessions = sessionRepository.findActiveByUserId(userId, OffsetDateTime.now());
        int count = 0;
        for (UserSession session : activeSessions) {
            if (currentSessionId == null || !session.getId().equals(currentSessionId)) {
                sessionRepository.delete(session);
                count++;
            }
        }
        return java.util.Map.of("message", count > 0 ? count + " other session(s) were signed out successfully." : "No other active sessions found.", "revokedSessions", count);
    }
}
