package com.nerdc.elephantfence.backend.common.security;

import com.nerdc.elephantfence.backend.configuration.service.ConfigurationService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.WriteListener;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReadOnlyModeFilterTest {

    @Mock
    private ConfigurationService configurationService;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private FilterChain filterChain;

    @InjectMocks
    private ReadOnlyModeFilter readOnlyModeFilter;

    private ByteArrayOutputStream outputStream;

    @BeforeEach
    void setUp() throws IOException {
        outputStream = new ByteArrayOutputStream();
        ServletOutputStream servletOutputStream = new ServletOutputStream() {
            @Override
            public boolean isReady() {
                return true;
            }

            @Override
            public void setWriteListener(WriteListener writeListener) {}

            @Override
            public void write(int b) {
                outputStream.write(b);
            }
        };
        lenient().when(response.getOutputStream()).thenReturn(servletOutputStream);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void doFilter_shouldAllowGetRequests_evenWhenReadOnly() throws ServletException, IOException {
        when(request.getMethod()).thenReturn("GET");

        readOnlyModeFilter.doFilter(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        verify(configurationService, never()).isReadOnlyMode();
    }

    @Test
    void doFilter_shouldAllowMutatingRequests_whenReadOnlyIsFalse() throws ServletException, IOException {
        when(request.getMethod()).thenReturn("POST");
        when(request.getRequestURI()).thenReturn("/api/fences");
        when(configurationService.isReadOnlyMode()).thenReturn(false);

        readOnlyModeFilter.doFilter(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
    }

    @Test
    void doFilter_shouldAllowMutatingRequests_forExemptUris() throws ServletException, IOException {
        when(request.getMethod()).thenReturn("POST");
        when(request.getRequestURI()).thenReturn("/api/telemetry/ingest");

        readOnlyModeFilter.doFilter(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        verify(configurationService, never()).isReadOnlyMode();
    }

    @Test
    void doFilter_shouldBlockNonSuperAdmin_whenReadOnlyIsTrue() throws ServletException, IOException {
        when(request.getMethod()).thenReturn("POST");
        when(request.getRequestURI()).thenReturn("/api/fences");
        when(configurationService.isReadOnlyMode()).thenReturn(true);

        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(new UsernamePasswordAuthenticationToken(
                "user@nerdc.lk", "password", List.of(new SimpleGrantedAuthority("ROLE_REGIONAL_ADMIN"))
        ));
        SecurityContextHolder.setContext(context);

        readOnlyModeFilter.doFilter(request, response, filterChain);

        verify(response).setStatus(HttpServletResponse.SC_FORBIDDEN);
        verify(filterChain, never()).doFilter(request, response);
        assertThat(outputStream.toString()).contains("READ_ONLY_MODE");
    }

    @Test
    void doFilter_shouldAllowSuperAdmin_whenReadOnlyIsTrue() throws ServletException, IOException {
        when(request.getMethod()).thenReturn("PUT");
        when(request.getRequestURI()).thenReturn("/api/configuration/general");
        when(configurationService.isReadOnlyMode()).thenReturn(true);

        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(new UsernamePasswordAuthenticationToken(
                "admin@nerdc.lk", "password", List.of(new SimpleGrantedAuthority("ROLE_SUPER_ADMIN"))
        ));
        SecurityContextHolder.setContext(context);

        readOnlyModeFilter.doFilter(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
    }
}
