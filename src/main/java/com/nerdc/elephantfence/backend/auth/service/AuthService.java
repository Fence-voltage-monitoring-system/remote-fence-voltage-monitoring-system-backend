package com.nerdc.elephantfence.backend.auth.service;

import com.nerdc.elephantfence.backend.auth.dto.ChangePasswordRequestDTO;
import com.nerdc.elephantfence.backend.auth.dto.LoginRequestDTO;
import com.nerdc.elephantfence.backend.auth.dto.LoginResponseDTO;
import com.nerdc.elephantfence.backend.auth.dto.RefreshTokenRequestDTO;
import com.nerdc.elephantfence.backend.common.security.JwtTokenProvider;
import com.nerdc.elephantfence.backend.common.security.UserPrincipal;
import com.nerdc.elephantfence.backend.configuration.entity.UserSession;
import com.nerdc.elephantfence.backend.configuration.repository.UserSessionRepository;
import com.nerdc.elephantfence.backend.users.entity.User;
import com.nerdc.elephantfence.backend.users.repository.UserRepository;
import com.nerdc.elephantfence.backend.users.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider tokenProvider;
    private final UserRepository userRepository;
    private final UserService userService;
    private final PasswordEncoder passwordEncoder;
    private final UserSessionRepository sessionRepository;

    @Transactional
    public LoginResponseDTO login(LoginRequestDTO request, jakarta.servlet.http.HttpServletRequest servletRequest) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getEmail().toLowerCase().trim(),
                        request.getPassword()
                )
        );

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

        OffsetDateTime expiresAt = OffsetDateTime.now().plusHours(24);
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
            session.setExpiresAt(OffsetDateTime.now().plusHours(24));
            sessionRepository.save(session);
        }

        User user = userRepository.findByIdWithProvincesAndDistricts(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        if (!user.isEnabled()) {
            throw new IllegalStateException("User account is disabled");
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
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPasswordHash())) {
            throw new IllegalArgumentException("Current password does not match");
        }

        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        user.setPasswordChangeRequired(false);
        user.setPasswordChangedAt(OffsetDateTime.now());
        userRepository.save(user);
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
}
