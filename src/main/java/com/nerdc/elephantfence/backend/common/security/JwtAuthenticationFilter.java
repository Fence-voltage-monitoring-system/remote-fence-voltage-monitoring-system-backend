package com.nerdc.elephantfence.backend.common.security;

import com.nerdc.elephantfence.backend.configuration.entity.UserSession;
import com.nerdc.elephantfence.backend.configuration.repository.UserSessionRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.OffsetDateTime;
import java.util.UUID;

@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider tokenProvider;
    private final CustomUserDetailsService customUserDetailsService;
    private final UserSessionRepository sessionRepository;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        try {
            String jwt = getJwtFromRequest(request);

            if (StringUtils.hasText(jwt) && tokenProvider.validateToken(jwt)) {
                UUID userId = tokenProvider.getUserIdFromJWT(jwt);
                String sessionId = tokenProvider.getSessionIdFromJWT(jwt);

                boolean validSession = true;
                if (sessionId != null) {
                    var sessionOpt = sessionRepository.findById(sessionId);
                    if (sessionOpt.isEmpty() || sessionOpt.get().getExpiresAt().isBefore(OffsetDateTime.now())) {
                        validSession = false;
                        if (sessionOpt.isPresent()) {
                            sessionRepository.delete(sessionOpt.get());
                        }
                    } else {
                        // Periodic heartbeat: update last activity at if it's been > 1 min since last update
                        UserSession session = sessionOpt.get();
                        if (session.getLastActivityAt().plusMinutes(1).isBefore(OffsetDateTime.now())) {
                            session.setLastActivityAt(OffsetDateTime.now());
                            sessionRepository.save(session);
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
            return bearerToken.substring(7);
        }
        return null;
    }
}
