package com.nerdc.elephantfence.backend.common.security;

import com.nerdc.elephantfence.backend.configuration.entity.UserSession;
import com.nerdc.elephantfence.backend.configuration.repository.UserSessionRepository;
import com.nerdc.elephantfence.backend.configuration.service.ConfigurationService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.io.IOException;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterTest {

    @Mock
    private JwtTokenProvider tokenProvider;

    @Mock
    private CustomUserDetailsService customUserDetailsService;

    @Mock
    private UserSessionRepository sessionRepository;

    @Mock
    private ConfigurationService configurationService;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private FilterChain filterChain;

    private JwtAuthenticationFilter filter;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();
        filter = new JwtAuthenticationFilter(tokenProvider, customUserDetailsService, sessionRepository, configurationService);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void doFilterInternal_shouldAuthenticateUser_whenSessionIsValidAndActive() throws ServletException, IOException {
        String token = "valid-token";
        UUID userId = UUID.randomUUID();
        String sessionId = "session-123";

        when(request.getHeader("Authorization")).thenReturn("Bearer " + token);
        when(tokenProvider.validateToken(token)).thenReturn(true);
        when(tokenProvider.getUserIdFromJWT(token)).thenReturn(userId);
        when(tokenProvider.getSessionIdFromJWT(token)).thenReturn(sessionId);

        UserSession activeSession = UserSession.builder()
                .id(sessionId)
                .expiresAt(OffsetDateTime.now().plusHours(4))
                .lastActivityAt(OffsetDateTime.now().minusMinutes(5))
                .build();
        when(sessionRepository.findById(sessionId)).thenReturn(Optional.of(activeSession));

        when(configurationService.getSessionManagementConfig()).thenReturn(
                new ConfigurationService.SessionManagementConfig(12, 30, 7, 5, 2, "REVOKE_OLDEST", true, 5, true, true, true, true, true)
        );

        UserPrincipal principal = new UserPrincipal(
                userId, "Test User", "test@nerdc.lk", "hash", true,
                List.of(new SimpleGrantedAuthority("ROLE_FIELD_ADMIN"))
        );
        when(customUserDetailsService.loadUserById(userId)).thenReturn(principal);

        filter.doFilter(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNotNull();
        UserPrincipal authenticatedPrincipal = (UserPrincipal) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        assertThat(authenticatedPrincipal.getId()).isEqualTo(userId);
        assertThat(authenticatedPrincipal.getSessionId()).isEqualTo(sessionId);
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void doFilterInternal_shouldRejectAndRevokeSession_whenIdleTimeoutExceeded() throws ServletException, IOException {
        String token = "valid-token";
        UUID userId = UUID.randomUUID();
        String sessionId = "session-idle";

        when(request.getHeader("Authorization")).thenReturn("Bearer " + token);
        when(tokenProvider.validateToken(token)).thenReturn(true);
        when(tokenProvider.getUserIdFromJWT(token)).thenReturn(userId);
        when(tokenProvider.getSessionIdFromJWT(token)).thenReturn(sessionId);

        // Session was last active 40 minutes ago with 30 min idle timeout
        UserSession idleSession = UserSession.builder()
                .id(sessionId)
                .expiresAt(OffsetDateTime.now().plusHours(4))
                .lastActivityAt(OffsetDateTime.now().minusMinutes(40))
                .build();
        when(sessionRepository.findById(sessionId)).thenReturn(Optional.of(idleSession));

        when(configurationService.getSessionManagementConfig()).thenReturn(
                new ConfigurationService.SessionManagementConfig(12, 30, 7, 5, 2, "REVOKE_OLDEST", true, 5, true, true, true, true, true)
        );

        filter.doFilter(request, response, filterChain);

        // Should revoke the idle session and not authenticate
        verify(sessionRepository).delete(idleSession);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(filterChain).doFilter(request, response);
    }
}
