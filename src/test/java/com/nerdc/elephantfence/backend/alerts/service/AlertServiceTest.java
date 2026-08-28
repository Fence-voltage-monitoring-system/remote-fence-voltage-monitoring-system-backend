package com.nerdc.elephantfence.backend.alerts.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nerdc.elephantfence.backend.alerts.dto.*;
import com.nerdc.elephantfence.backend.alerts.entity.Alert;
import com.nerdc.elephantfence.backend.alerts.entity.AlertComment;
import com.nerdc.elephantfence.backend.alerts.entity.AlertEvent;
import com.nerdc.elephantfence.backend.alerts.repository.AlertCommentRepository;
import com.nerdc.elephantfence.backend.alerts.repository.AlertEventRepository;
import com.nerdc.elephantfence.backend.alerts.repository.AlertRepository;
import com.nerdc.elephantfence.backend.fences.entity.Fence;
import com.nerdc.elephantfence.backend.fences.repository.FenceRepository;
import com.nerdc.elephantfence.backend.locations.entity.District;
import com.nerdc.elephantfence.backend.locations.entity.Province;
import com.nerdc.elephantfence.backend.locations.repository.DistrictRepository;
import com.nerdc.elephantfence.backend.locations.repository.ProvinceRepository;
import com.nerdc.elephantfence.backend.sections.entity.Section;
import com.nerdc.elephantfence.backend.sections.repository.SectionRepository;
import com.nerdc.elephantfence.backend.users.entity.User;
import com.nerdc.elephantfence.backend.users.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AlertServiceTest {

    @Mock
    private AlertRepository alertRepository;
    @Mock
    private AlertEventRepository alertEventRepository;
    @Mock
    private AlertCommentRepository alertCommentRepository;
    @Mock
    private FenceRepository fenceRepository;
    @Mock
    private SectionRepository sectionRepository;
    @Mock
    private ProvinceRepository provinceRepository;
    @Mock
    private DistrictRepository districtRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private AlertWebSocketHandler webSocketHandler;

    @InjectMocks
    private AlertService alertService;

    private Alert alert;
    private User admin;

    @BeforeEach
    void setUp() {
        admin = User.builder()
                .id(UUID.randomUUID())
                .fullName("System Administrator")
                .email("admin@nerdc.lk")
                .enabled(true)
                .build();

        alert = Alert.builder()
                .id(1L)
                .code("ALT-001")
                .title("Voltage Drop")
                .type("VOLTAGE_DROP")
                .severity("CRITICAL")
                .status("UNACKNOWLEDGED")
                .assignmentStatus("UNASSIGNED")
                .detectedVoltageKv(BigDecimal.valueOf(1.5))
                .thresholdVoltageKv(BigDecimal.valueOf(3.0))
                .createdAt(OffsetDateTime.now())
                .build();
    }

    @Test
    void getStats_shouldReturnCorrectStatistics() {
        when(alertRepository.countBySeverityAndStatusNot("CRITICAL", "RESOLVED")).thenReturn(5L);
        when(alertRepository.countBySeverityAndStatusNot("WARNING", "RESOLVED")).thenReturn(10L);
        when(alertRepository.countByStatus("UNACKNOWLEDGED")).thenReturn(3L);
        when(alertRepository.countUnderMaintenance()).thenReturn(4L);
        when(alertRepository.countResolvedSince(any())).thenReturn(2L);

        AlertStatsDTO stats = alertService.getStats();

        assertThat(stats.getActiveCritical()).isEqualTo(5);
        assertThat(stats.getActiveWarnings()).isEqualTo(10);
        assertThat(stats.getUnacknowledged()).isEqualTo(3);
        assertThat(stats.getUnderMaintenance()).isEqualTo(4);
        assertThat(stats.getResolvedToday()).isEqualTo(2);
    }

    @Test
    void acknowledge_shouldSetStatusToAcknowledged() {
        when(alertRepository.findById(1L)).thenReturn(Optional.of(alert));
        when(userRepository.findByEmailIgnoreCase("admin@nerdc.lk")).thenReturn(Optional.of(admin));
        when(alertRepository.save(any(Alert.class))).thenReturn(alert);

        AlertResponseDTO response = alertService.acknowledge(1L);

        assertThat(response.getStatus()).isEqualTo("ACKNOWLEDGED");
        verify(alertRepository).save(alert);
        verify(alertEventRepository).save(any(AlertEvent.class));
    }

    @Test
    void acceptAssignment_shouldSetStatusToAccepted() {
        when(alertRepository.findById(1L)).thenReturn(Optional.of(alert));
        when(userRepository.findByEmailIgnoreCase("admin@nerdc.lk")).thenReturn(Optional.of(admin));
        when(alertRepository.save(any(Alert.class))).thenReturn(alert);

        AlertResponseDTO response = alertService.acceptAssignment(1L);

        assertThat(response.getAssignmentStatus()).isEqualTo("ACCEPTED");
        verify(alertRepository).save(alert);
        verify(alertEventRepository).save(any(AlertEvent.class));
    }

    @Test
    void declineAssignment_shouldSetStatusToDeclined() {
        DeclineAssignmentRequestDTO request = DeclineAssignmentRequestDTO.builder()
                .reason("Too busy")
                .build();

        when(alertRepository.findById(1L)).thenReturn(Optional.of(alert));
        when(userRepository.findByEmailIgnoreCase("admin@nerdc.lk")).thenReturn(Optional.of(admin));
        when(alertRepository.save(any(Alert.class))).thenReturn(alert);

        AlertResponseDTO response = alertService.declineAssignment(1L, request);

        assertThat(response.getAssignmentStatus()).isEqualTo("DECLINED");
        verify(alertRepository).save(alert);
        verify(alertEventRepository).save(any(AlertEvent.class));
    }

    @Test
    void startWork_shouldSetStatusToInProgress() {
        when(alertRepository.findById(1L)).thenReturn(Optional.of(alert));
        when(userRepository.findByEmailIgnoreCase("admin@nerdc.lk")).thenReturn(Optional.of(admin));
        when(alertRepository.save(any(Alert.class))).thenReturn(alert);

        AlertResponseDTO response = alertService.startWork(1L);

        assertThat(response.getStatus()).isEqualTo("IN_PROGRESS");
        verify(alertRepository).save(alert);
        verify(alertEventRepository).save(any(AlertEvent.class));
    }

    @Test
    void resolveManually_shouldSetStatusToResolved() {
        ResolveManuallyRequestDTO request = ResolveManuallyRequestDTO.builder()
                .reason("Fixed physical break")
                .build();

        when(alertRepository.findById(1L)).thenReturn(Optional.of(alert));
        when(userRepository.findByEmailIgnoreCase("admin@nerdc.lk")).thenReturn(Optional.of(admin));
        when(alertRepository.save(any(Alert.class))).thenReturn(alert);

        AlertResponseDTO response = alertService.resolveManually(1L, request);

        assertThat(response.getStatus()).isEqualTo("RESOLVED");
        assertThat(response.getResolutionType()).isEqualTo("MANUAL");
        verify(alertRepository).save(alert);
        verify(alertEventRepository).save(any(AlertEvent.class));
    }

    @Test
    void addComment_shouldSaveComment() {
        AddCommentRequestDTO request = AddCommentRequestDTO.builder()
                .comment("Checking batteries now")
                .build();

        when(alertRepository.findById(1L)).thenReturn(Optional.of(alert));
        when(userRepository.findByEmailIgnoreCase("admin@nerdc.lk")).thenReturn(Optional.of(admin));
        when(alertCommentRepository.save(any(AlertComment.class))).thenAnswer(i -> i.getArgument(0));

        AlertResponseDTO response = alertService.addComment(1L, request);

        verify(alertCommentRepository).save(any(AlertComment.class));
        verify(alertEventRepository).save(any(AlertEvent.class));
    }
}
