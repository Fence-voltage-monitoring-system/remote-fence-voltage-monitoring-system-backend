package com.nerdc.elephantfence.backend.auth.service;

import com.nerdc.elephantfence.backend.auth.dto.ChangePasswordRequestDTO;
import com.nerdc.elephantfence.backend.auth.dto.LoginRequestDTO;
import com.nerdc.elephantfence.backend.auth.dto.LoginResponseDTO;
import com.nerdc.elephantfence.backend.common.security.JwtTokenProvider;
import com.nerdc.elephantfence.backend.common.security.UserPrincipal;
import com.nerdc.elephantfence.backend.configuration.entity.UserSession;
import com.nerdc.elephantfence.backend.configuration.repository.UserSessionRepository;
import com.nerdc.elephantfence.backend.configuration.service.ConfigurationService;
import com.nerdc.elephantfence.backend.users.dto.UserResponseDTO;
import java.time.OffsetDateTime;
import com.nerdc.elephantfence.backend.users.entity.Role;
import com.nerdc.elephantfence.backend.users.entity.User;
import com.nerdc.elephantfence.backend.users.repository.UserRepository;
import com.nerdc.elephantfence.backend.users.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private JwtTokenProvider tokenProvider;

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserService userService;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private LoginRateLimiterService rateLimiterService;

    @Mock
    private UserSessionRepository sessionRepository;

    @Mock
    private ConfigurationService configurationService;

    @Mock
    private HttpServletRequest servletRequest;

    @InjectMocks
    private AuthService authService;

    private UUID userId;
    private User normalUser;
    private User superAdminUser;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        normalUser = User.builder()
                .id(userId)
                .email("user@nerdc.lk")
                .fullName("Normal User")
                .role(Role.REGIONAL_ADMIN)
                .enabled(true)
                .build();

        superAdminUser = User.builder()
                .id(UUID.randomUUID())
                .email("admin@nerdc.lk")
                .fullName("Super Admin")
                .role(Role.SUPER_ADMIN)
                .enabled(true)
                .build();
    }

    @Test
    void login_shouldSucceed_whenNormalConditions() {
        LoginRequestDTO request = LoginRequestDTO.builder()
                .email("user@nerdc.lk")
                .password("password123")
                .build();

        UserPrincipal principal = new UserPrincipal(
                userId, "Normal User", "user@nerdc.lk", "hash", true,
                List.of(new SimpleGrantedAuthority("ROLE_REGIONAL_ADMIN"))
        );
        Authentication auth = new UsernamePasswordAuthenticationToken(principal, "password123", principal.getAuthorities());

        when(authenticationManager.authenticate(any())).thenReturn(auth);
        when(configurationService.getSystemModeConfig()).thenReturn(
                new ConfigurationService.SystemModeConfig(false, true, false, "Maintenance")
        );
        when(tokenProvider.generateToken(any(), any())).thenReturn("mock-access-token");
        when(tokenProvider.generateRefreshToken(any(), any())).thenReturn("mock-refresh-token");
        when(userRepository.findByIdWithProvincesAndDistricts(userId)).thenReturn(Optional.of(normalUser));
        when(userService.toUserResponseDTO(normalUser)).thenReturn(
                UserResponseDTO.builder().id(userId).email("user@nerdc.lk").build()
        );

        LoginResponseDTO response = authService.login(request, servletRequest);

        assertThat(response).isNotNull();
        assertThat(response.getAccessToken()).isEqualTo("mock-access-token");
        verify(userRepository).save(normalUser);
    }

    @Test
    void login_shouldBlockNonSuperAdmin_whenMaintenanceModeAndDisallowLogin() {
        LoginRequestDTO request = LoginRequestDTO.builder()
                .email("user@nerdc.lk")
                .password("password123")
                .build();

        UserPrincipal principal = new UserPrincipal(
                userId, "Normal User", "user@nerdc.lk", "hash", true,
                List.of(new SimpleGrantedAuthority("ROLE_REGIONAL_ADMIN"))
        );
        Authentication auth = new UsernamePasswordAuthenticationToken(principal, "password123", principal.getAuthorities());

        when(authenticationManager.authenticate(any())).thenReturn(auth);
        when(configurationService.getSystemModeConfig()).thenReturn(
                new ConfigurationService.SystemModeConfig(true, false, false, "System under emergency maintenance")
        );

        assertThatThrownBy(() -> authService.login(request, servletRequest))
                .isInstanceOf(DisabledException.class)
                .hasMessage("System under emergency maintenance");

        verify(tokenProvider, never()).generateToken(any(), any());
    }

    @Test
    void login_shouldAllowSuperAdmin_evenWhenMaintenanceModeAndDisallowLogin() {
        LoginRequestDTO request = LoginRequestDTO.builder()
                .email("admin@nerdc.lk")
                .password("password123")
                .build();

        UserPrincipal principal = new UserPrincipal(
                superAdminUser.getId(), "Super Admin", "admin@nerdc.lk", "hash", true,
                List.of(new SimpleGrantedAuthority("ROLE_SUPER_ADMIN"))
        );
        Authentication auth = new UsernamePasswordAuthenticationToken(principal, "password123", principal.getAuthorities());

        when(authenticationManager.authenticate(any())).thenReturn(auth);
        when(configurationService.getSystemModeConfig()).thenReturn(
                new ConfigurationService.SystemModeConfig(true, false, false, "System under emergency maintenance")
        );
        when(tokenProvider.generateToken(any(), any())).thenReturn("super-access-token");
        when(tokenProvider.generateRefreshToken(any(), any())).thenReturn("super-refresh-token");
        when(userRepository.findByIdWithProvincesAndDistricts(superAdminUser.getId())).thenReturn(Optional.of(superAdminUser));
        when(userService.toUserResponseDTO(superAdminUser)).thenReturn(
                UserResponseDTO.builder().id(superAdminUser.getId()).email("admin@nerdc.lk").build()
        );

        LoginResponseDTO response = authService.login(request, servletRequest);

        assertThat(response).isNotNull();
        assertThat(response.getAccessToken()).isEqualTo("super-access-token");
    }

    @Test
    void login_shouldRejectNew_whenMaxConcurrentReachedAndRejectNewConfigured() {
        LoginRequestDTO request = LoginRequestDTO.builder()
                .email("user@nerdc.lk")
                .password("password123")
                .build();

        UserPrincipal principal = new UserPrincipal(
                userId, "Normal User", "user@nerdc.lk", "hash", true,
                List.of(new SimpleGrantedAuthority("ROLE_REGIONAL_ADMIN"))
        );
        Authentication auth = new UsernamePasswordAuthenticationToken(principal, "password123", principal.getAuthorities());

        when(authenticationManager.authenticate(any())).thenReturn(auth);
        when(configurationService.getSystemModeConfig()).thenReturn(
                new ConfigurationService.SystemModeConfig(false, true, false, "Maintenance")
        );
        when(userRepository.findByIdWithProvincesAndDistricts(userId)).thenReturn(Optional.of(normalUser));
        when(configurationService.getSessionManagementConfig()).thenReturn(
                new ConfigurationService.SessionManagementConfig(12, 30, 7, 5, 2, "REJECT_NEW", true, 5, true, true, true, true, true)
        );
        when(sessionRepository.findActiveByUserId(eq(userId), any())).thenReturn(List.of(
                UserSession.builder().id("sess-1").build(),
                UserSession.builder().id("sess-2").build()
        ));

        assertThatThrownBy(() -> authService.login(request, servletRequest))
                .isInstanceOf(org.springframework.web.server.ResponseStatusException.class)
                .hasMessageContaining("Maximum concurrent active sessions reached");

        verify(sessionRepository, never()).save(any());
    }

    @Test
    void login_shouldRevokeOldest_whenMaxConcurrentReachedAndRevokeOldestConfigured() {
        LoginRequestDTO request = LoginRequestDTO.builder()
                .email("user@nerdc.lk")
                .password("password123")
                .build();

        UserPrincipal principal = new UserPrincipal(
                userId, "Normal User", "user@nerdc.lk", "hash", true,
                List.of(new SimpleGrantedAuthority("ROLE_REGIONAL_ADMIN"))
        );
        Authentication auth = new UsernamePasswordAuthenticationToken(principal, "password123", principal.getAuthorities());

        UserSession oldSession = UserSession.builder()
                .id("sess-old")
                .signedInAt(OffsetDateTime.now().minusHours(5))
                .build();
        UserSession newSession = UserSession.builder()
                .id("sess-new")
                .signedInAt(OffsetDateTime.now().minusHours(1))
                .build();

        List<UserSession> activeSessions = new java.util.ArrayList<>(List.of(oldSession, newSession));

        when(authenticationManager.authenticate(any())).thenReturn(auth);
        when(configurationService.getSystemModeConfig()).thenReturn(
                new ConfigurationService.SystemModeConfig(false, true, false, "Maintenance")
        );
        when(configurationService.getSessionManagementConfig()).thenReturn(
                new ConfigurationService.SessionManagementConfig(12, 30, 7, 5, 2, "REVOKE_OLDEST", true, 5, true, true, true, true, true)
        );
        when(sessionRepository.findActiveByUserId(eq(userId), any())).thenReturn(activeSessions);
        when(tokenProvider.generateToken(any(), any())).thenReturn("mock-access-token");
        when(tokenProvider.generateRefreshToken(any(), any())).thenReturn("mock-refresh-token");
        when(userRepository.findByIdWithProvincesAndDistricts(userId)).thenReturn(Optional.of(normalUser));
        when(userService.toUserResponseDTO(normalUser)).thenReturn(
                UserResponseDTO.builder().id(userId).email("user@nerdc.lk").build()
        );

        LoginResponseDTO response = authService.login(request, servletRequest);

        assertThat(response).isNotNull();
        verify(sessionRepository).delete(oldSession);
        verify(sessionRepository).save(any(UserSession.class));
    }

    @Test
    void changePassword_shouldRevokeOtherSessions_whenRevokeOnPasswordChangeEnabled() {
        ChangePasswordRequestDTO request = ChangePasswordRequestDTO.builder()
                .currentPassword("oldPassword")
                .newPassword("newSecurePassword123!")
                .build();

        normalUser.setPasswordHash("encodedOldPassword");
        when(userRepository.findById(userId)).thenReturn(Optional.of(normalUser));
        when(passwordEncoder.matches("oldPassword", "encodedOldPassword")).thenReturn(true);
        when(passwordEncoder.encode("newSecurePassword123!")).thenReturn("encodedNewPassword");
        when(configurationService.getSessionManagementConfig()).thenReturn(
                new ConfigurationService.SessionManagementConfig(12, 30, 7, 5, 2, "REVOKE_OLDEST", true, 5, true, true, true, true, true)
        );

        authService.changePassword(userId, request, "current-session-id");

        verify(sessionRepository).revokeOtherUserSessions(userId, "current-session-id");
        verify(userRepository).save(normalUser);
    }

    @Test
    void reauthenticate_shouldSucceed_whenPasswordMatches() {
        normalUser.setPasswordHash("encodedPass");
        when(userRepository.findById(userId)).thenReturn(Optional.of(normalUser));
        when(passwordEncoder.matches("secretPass", "encodedPass")).thenReturn(true);
        when(configurationService.getSessionManagementConfig()).thenReturn(
                new ConfigurationService.SessionManagementConfig(12, 30, 7, 5, 2, "REVOKE_OLDEST", true, 10, true, true, true, true, true)
        );

        var response = authService.reauthenticate(userId, "secretPass");

        assertThat(response.isAuthenticated()).isTrue();
        assertThat(response.getValidityMinutes()).isEqualTo(10);
        assertThat(response.getExpiresAt()).isAfter(OffsetDateTime.now());
    }

    @Test
    void reauthenticate_shouldThrowUnauthorized_whenPasswordWrong() {
        normalUser.setPasswordHash("encodedPass");
        when(userRepository.findById(userId)).thenReturn(Optional.of(normalUser));
        when(passwordEncoder.matches("wrongPass", "encodedPass")).thenReturn(false);

        assertThatThrownBy(() -> authService.reauthenticate(userId, "wrongPass"))
                .isInstanceOf(org.springframework.web.server.ResponseStatusException.class);
    }
}
