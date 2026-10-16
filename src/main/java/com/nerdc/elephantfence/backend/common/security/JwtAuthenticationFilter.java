package com.nerdc.elephantfence.backend.common.security;

import com.nerdc.elephantfence.backend.configuration.entity.UserSession;
import com.nerdc.elephantfence.backend.configuration.repository.UserSessionRepository;
import com.nerdc.elephantfence.backend.configuration.service.ConfigurationService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.OffsetDateTime;
import java.util.UUID;

public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider tokenProvider;
    private final CustomUserDetailsService customUserDetailsService;
    private final UserSessionRepository sessionRepository;
    private final ConfigurationService configurationService;

    public JwtAuthenticationFilter(JwtTokenProvider tokenProvider,
                                   CustomUserDetailsService customUserDetailsService,
                                   UserSessionRepository sessionRepository) {
        this(tokenProvider, customUserDetailsService, sessionRepository, null);
    }

    public JwtAuthenticationFilter(JwtTokenProvider tokenProvider,
                                   CustomUserDetailsService customUserDetailsService,
                                   UserSessionRepository sessionRepository,
                                   ConfigurationService configurationService) {
        this.tokenProvider = tokenProvider;
        this.customUserDetailsService = customUserDetailsService;
        this.sessionRepository = sessionRepository;
        this.configurationService = configurationService;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        try {
            String jwt = getJwtFromRequest(request);

            if (StringUtils.hasText(jwt)) {
                UUID userId = tokenProvider.getUserIdFromJWT(jwt);
                String sessionId = tokenProvider.getSessionIdFromJWT(jwt);

                boolean validSession = true;
                if (sessionId != null) {
                    var sessionOpt = sessionRepository.findById(sessionId);
                    OffsetDateTime now = OffsetDateTime.now();
                    if (sessionOpt.isEmpty()) {
                        validSession = false;
                    } else {
                        UserSession session = sessionOpt.get();
                        int idleTimeoutMinutes = 30;
                        try {
                            if (configurationService != null) {
                                var sessionConfig = configurationService.getSessionManagementConfig();
                                if (sessionConfig != null && sessionConfig.idleTimeoutMinutes() > 0) {
                                    idleTimeoutMinutes = sessionConfig.idleTimeoutMinutes();
                                }
                            }
                        } catch (Exception ignored) {}

                        boolean expired = session.getExpiresAt().isBefore(now);
                        boolean idleTimedOut = session.getLastActivityAt() != null
                                && session.getLastActivityAt().plusMinutes(idleTimeoutMinutes).isBefore(now);

                        if (expired || idleTimedOut) {
                            validSession = false;
                            sessionRepository.delete(session);
                        } else {
                            // Periodic heartbeat: update last activity at if it's been > 1 min since last update
                            if (session.getLastActivityAt() == null || session.getLastActivityAt().plusMinutes(1).isBefore(now)) {
                                session.setLastActivityAt(now);
                                sessionRepository.save(session);
                            }
                        }
                    }
                }

                if (validSession) {
                    UserDetails userDetails = customUserDetailsService.loadUserById(userId);

                    if (userDetails.isEnabled()) {
                        UserPrincipal principal = (UserPrincipal) userDetails;
                        if (sessionId != null) {
                            principal = principal.withSessionId(sessionId);
                        }

                        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                                principal, null, principal.getAuthorities()
                        );
                        authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                        SecurityContextHolder.getContext().setAuthentication(authentication);
                    }
                }
            }
        } catch (Exception ex) {
            logger.error("Could not set user authentication in security context", ex);
        }

        filterChain.doFilter(request, response);
    }

    private String getJwtFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            String token = bearerToken.substring(7);
            if (StringUtils.hasText(token) && tokenProvider.validateToken(token)) {
                return token;
            }
        }

        if (request.getCookies() != null) {
            for (Cookie cookie : request.getCookies()) {
                if ("access_token".equals(cookie.getName())) {
                    String token = cookie.getValue();
                    if (StringUtils.hasText(token) && tokenProvider.validateToken(token)) {
                        return token;
                    }
                }
            }
        }

        return null;
    }
}

