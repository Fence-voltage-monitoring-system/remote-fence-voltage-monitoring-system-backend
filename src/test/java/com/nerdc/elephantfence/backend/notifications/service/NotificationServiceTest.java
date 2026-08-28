package com.nerdc.elephantfence.backend.notifications.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nerdc.elephantfence.backend.fences.repository.FenceRepository;
import com.nerdc.elephantfence.backend.locations.repository.DistrictRepository;
import com.nerdc.elephantfence.backend.locations.repository.ProvinceRepository;
import com.nerdc.elephantfence.backend.notifications.dto.NotificationResponseDTO;
import com.nerdc.elephantfence.backend.notifications.dto.NotificationStatsDTO;
import com.nerdc.elephantfence.backend.notifications.entity.UserNotification;
import com.nerdc.elephantfence.backend.notifications.repository.UserNotificationRepository;
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

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock
    private UserNotificationRepository userNotificationRepository;
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
    private NotificationWebSocketHandler webSocketHandler;

    @InjectMocks
    private NotificationService notificationService;

    private UserNotification notification;
    private User admin;

    @BeforeEach
    void setUp() {
        admin = User.builder()
                .id(UUID.randomUUID())
                .fullName("System Administrator")
                .email("admin@nerdc.lk")
                .enabled(true)
                .build();

        notification = UserNotification.builder()
                .id(1L)
                .userId(admin.getId())
                .code("NTF-001")
                .title("Voltage Critical")
                .message("Fence section is down")
                .category("CRITICAL")
                .read(false)
                .channels("IN_APP,WEBSOCKET")
                .createdAt(OffsetDateTime.now())
                .build();
    }

    @Test
    void getStats_shouldReturnCorrectStats() {
        UUID userId = admin.getId();
        when(userRepository.findByEmailIgnoreCase("admin@nerdc.lk")).thenReturn(Optional.of(admin));
        when(userNotificationRepository.countByUserIdAndRead(userId, false)).thenReturn(5L);
        when(userNotificationRepository.countByUserIdAndChannel(userId, "IN_APP")).thenReturn(10L);
        when(userNotificationRepository.countByUserIdAndChannel(userId, "WEBSOCKET")).thenReturn(8L);
        when(userNotificationRepository.countByUserIdAndChannel(userId, "SMS")).thenReturn(2L);

        NotificationStatsDTO stats = notificationService.getStats();

        assertThat(stats.getUnread()).isEqualTo(5);
        assertThat(stats.getInApp()).isEqualTo(10);
        assertThat(stats.getWebsocket()).isEqualTo(8);
        assertThat(stats.getSmsDelivered()).isEqualTo(2);
    }

    @Test
    void markRead_shouldSetReadToTrue() {
        when(userNotificationRepository.findById(1L)).thenReturn(Optional.of(notification));
        when(userNotificationRepository.save(any(UserNotification.class))).thenReturn(notification);

        NotificationResponseDTO response = notificationService.markRead(1L);

        assertThat(response.isRead()).isTrue();
        verify(userNotificationRepository).save(notification);
    }

    @Test
    void markAllRead_shouldReturnUpdatedCount() {
        when(userRepository.findByEmailIgnoreCase("admin@nerdc.lk")).thenReturn(Optional.of(admin));
        when(userNotificationRepository.markAllReadForUser(admin.getId())).thenReturn(4);

        Map<String, Integer> result = notificationService.markAllRead();

        assertThat(result.get("updated")).isEqualTo(4);
        verify(userNotificationRepository).markAllReadForUser(admin.getId());
    }

    @Test
    void clearRead_shouldReturnDeletedCount() {
        when(userRepository.findByEmailIgnoreCase("admin@nerdc.lk")).thenReturn(Optional.of(admin));
        when(userNotificationRepository.deleteReadForUser(admin.getId())).thenReturn(3);

        Map<String, Integer> result = notificationService.clearRead();

        assertThat(result.get("deleted")).isEqualTo(3);
        verify(userNotificationRepository).deleteReadForUser(admin.getId());
    }

    @Test
    void sendNotification_shouldSaveAndBroadcast() {
        notificationService.sendNotification(notification);

        verify(userNotificationRepository).save(notification);
        verify(webSocketHandler).broadcast(any(String.class));
    }
}
