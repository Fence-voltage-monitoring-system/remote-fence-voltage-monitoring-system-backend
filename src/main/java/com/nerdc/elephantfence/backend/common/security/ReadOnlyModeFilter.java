package com.nerdc.elephantfence.backend.common.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nerdc.elephantfence.backend.configuration.service.ConfigurationService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Map;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class ReadOnlyModeFilter extends OncePerRequestFilter {

    private final ConfigurationService configurationService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final Set<String> MUTATING_METHODS = Set.of("POST", "PUT", "PATCH", "DELETE");

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        if (MUTATING_METHODS.contains(request.getMethod())) {
            String uri = request.getRequestURI();

            if (isExemptUri(uri)) {
                filterChain.doFilter(request, response);
                return;
            }

            if (configurationService.isReadOnlyMode()) {
                Authentication auth = SecurityContextHolder.getContext().getAuthentication();
                boolean isSuperAdmin = auth != null && auth.getAuthorities() != null && auth.getAuthorities().stream()
                        .anyMatch(a -> "ROLE_SUPER_ADMIN".equals(a.getAuthority()));

                if (!isSuperAdmin) {
                    response.setStatus(HttpStatus.FORBIDDEN.value());
                    response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                    Map<String, Object> errorBody = Map.of(
                            "code", "READ_ONLY_MODE",
                            "message", "System is currently in Read-Only Mode. Data modifications are restricted."
                    );
                    objectMapper.writeValue(response.getOutputStream(), errorBody);
                    return;
                }
            }
        }

        filterChain.doFilter(request, response);
    }

    private boolean isExemptUri(String uri) {
        return uri.startsWith("/api/auth")
                || uri.startsWith("/api/telemetry")
                || uri.startsWith("/actuator")
                || uri.startsWith("/api/health");
    }
}
