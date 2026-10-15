package com.nerdc.elephantfence.backend.auth.service;

import com.nerdc.elephantfence.backend.auth.dto.LoginRequestDTO;
import com.nerdc.elephantfence.backend.auth.dto.LoginResponseDTO;
import com.nerdc.elephantfence.backend.common.security.JwtTokenProvider;
import com.nerdc.elephantfence.backend.common.security.UserPrincipal;
import com.nerdc.elephantfence.backend.configuration.repository.UserSessionRepository;
import com.nerdc.elephantfence.backend.configuration.service.ConfigurationService;
import com.nerdc.elephantfence.backend.users.dto.UserResponseDTO;
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
}
