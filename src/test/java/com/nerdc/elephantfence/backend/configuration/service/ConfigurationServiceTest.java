package com.nerdc.elephantfence.backend.configuration.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nerdc.elephantfence.backend.common.security.UserPrincipal;
import com.nerdc.elephantfence.backend.configuration.dto.*;
import com.nerdc.elephantfence.backend.configuration.entity.SystemConfiguration;
import com.nerdc.elephantfence.backend.configuration.entity.UserSession;
import com.nerdc.elephantfence.backend.configuration.repository.SystemConfigurationRepository;
import com.nerdc.elephantfence.backend.configuration.repository.UserSessionRepository;
import com.nerdc.elephantfence.backend.users.entity.Role;
import com.nerdc.elephantfence.backend.users.entity.User;
import com.nerdc.elephantfence.backend.users.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.OffsetDateTime;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ConfigurationServiceTest {

    @Mock
    private SystemConfigurationRepository configRepository;

    @Mock
    private UserSessionRepository sessionRepository;

    @Mock
    private UserRepository userRepository;

    @Spy
    private ObjectMapper objectMapper = new ObjectMapper();

    @InjectMocks
    private ConfigurationService configurationService;

    private User adminUser;

    @BeforeEach
    void setUp() {
        adminUser = User.builder()
                .id(UUID.randomUUID())
                .fullName("Admin User")
                .email("admin@nerdc.lk")
                .role(Role.SUPER_ADMIN)
                .enabled(true)
                .build();
    }

    @Test
    void getSection_shouldReturnFromDb_whenExists() {
        JsonNode data = objectMapper.createObjectNode().put("testKey", "testValue");
        SystemConfiguration config = SystemConfiguration.builder()
                .id(1L)
                .section("general")
                .configData(data)
                .updatedBy(adminUser)
                .updatedAt(OffsetDateTime.now())
                .version(2)
                .build();

        when(configRepository.findBySection("general")).thenReturn(Optional.of(config));

        ConfigurationSaveResponseDTO response = configurationService.getSection("general");

        assertThat(response.getSection()).isEqualTo("general");
        assertThat(response.getVersion()).isEqualTo(2);
        assertThat(response.getUpdatedBy()).isEqualTo("admin@nerdc.lk");
        assertThat(response.getValue().get("testKey").asText()).isEqualTo("testValue");
    }

    @Test
    void getSection_shouldSaveAndReturnDefault_whenNotExists() {
        when(configRepository.findBySection("voltage")).thenReturn(Optional.empty());
        when(configRepository.save(any(SystemConfiguration.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ConfigurationSaveResponseDTO response = configurationService.getSection("voltage");

        assertThat(response.getSection()).isEqualTo("voltage");
        assertThat(response.getVersion()).isEqualTo(1);
        assertThat(response.getValue().get("healthyKv").asDouble()).isEqualTo(8.0);
        verify(configRepository).save(any(SystemConfiguration.class));
    }

    @Test
    void saveSection_shouldUpdateExisting_andIncrementVersion() {
        JsonNode oldData = objectMapper.createObjectNode().put("key", "old");
        SystemConfiguration existing = SystemConfiguration.builder()
                .id(1L)
                .section("general")
                .configData(oldData)
                .version(1)
                .build();

        when(configRepository.findBySection("general")).thenReturn(Optional.of(existing));
        when(userRepository.findById(adminUser.getId())).thenReturn(Optional.of(adminUser));
        when(configRepository.save(any(SystemConfiguration.class))).thenAnswer(invocation -> invocation.getArgument(0));

        JsonNode newData = objectMapper.createObjectNode().put("key", "new");
        ConfigurationSaveRequestDTO request = new ConfigurationSaveRequestDTO(newData, "Change config");

        ConfigurationSaveResponseDTO response = configurationService.saveSection("general", request, adminUser.getId());

        assertThat(response.getSection()).isEqualTo("general");
        assertThat(response.getVersion()).isEqualTo(2);
        assertThat(response.getUpdatedBy()).isEqualTo("admin@nerdc.lk");
        assertThat(response.getValue().get("key").asText()).isEqualTo("new");
    }

    @Test
    void getSessionOverview_shouldReturnActiveSessionSummary() {
        String currentSessionId = "current-sess-123";
        UserPrincipal principal = new UserPrincipal(adminUser.getId(), adminUser.getFullName(), adminUser.getEmail(), "", true, List.of(), currentSessionId);
        Authentication auth = mock(Authentication.class);
        when(auth.getPrincipal()).thenReturn(principal);
        SecurityContext context = mock(SecurityContext.class);
        when(context.getAuthentication()).thenReturn(auth);
        SecurityContextHolder.setContext(context);

        User regularUser = User.builder()
                .id(UUID.randomUUID())
                .fullName("Field Staff")
                .email("staff@nerdc.lk")
                .role(Role.MAINTENANCE)
                .enabled(true)
                .build();

        UserSession currentSession = UserSession.builder()
                .id(currentSessionId)
                .user(adminUser)
                .device("Windows Desktop")
                .browser("Chrome")
                .ipAddress("127.0.0.1")
                .approximateLocation("Localhost")
                .signedInAt(OffsetDateTime.now().minusHours(2))
                .lastActivityAt(OffsetDateTime.now().minusMinutes(5))
                .expiresAt(OffsetDateTime.now().plusHours(22))
                .suspicious(false)
                .build();

        UserSession otherSession = UserSession.builder()
                .id("other-sess-456")
                .user(regularUser)
                .device("Android Mobile")
                .browser("Firefox")
                .ipAddress("192.168.1.100")
                .approximateLocation("Colombo, Sri Lanka")
                .signedInAt(OffsetDateTime.now().minusHours(10))
                .lastActivityAt(OffsetDateTime.now().minusHours(9))
                .expiresAt(OffsetDateTime.now().plusMinutes(30))
                .suspicious(true)
                .build();

        when(sessionRepository.findAllActive(any(OffsetDateTime.class))).thenReturn(List.of(currentSession, otherSession));

        SessionOverviewDTO overview = configurationService.getSessionOverview();

        assertThat(overview.getTotalActiveSessions()).isEqualTo(2);
        assertThat(overview.getActiveUsers()).isEqualTo(2);
        assertThat(overview.getAdministratorSessions()).isEqualTo(1);
        assertThat(overview.getExpiringSoon()).isEqualTo(1);
        assertThat(overview.getSuspiciousSessions()).isEqualTo(1);

        ActiveSessionRecordDTO record1 = overview.getSessions().stream().filter(s -> s.getId().equals(currentSessionId)).findFirst().orElseThrow();
        assertThat(record1.isCurrent()).isTrue();
        assertThat(record1.getUserName()).isEqualTo("Admin User");

        ActiveSessionRecordDTO record2 = overview.getSessions().stream().filter(s -> s.getId().equals("other-sess-456")).findFirst().orElseThrow();
        assertThat(record2.isCurrent()).isFalse();
        assertThat(record2.getUserName()).isEqualTo("Field Staff");

        SecurityContextHolder.clearContext();
    }

    @Test
    void revokeSession_shouldCallRepository() {
        configurationService.revokeSession("sess-123");
        verify(sessionRepository).revokeSession("sess-123");
    }

    @Test
    void revokeUserSessions_shouldCallRepositoryAndReturnCount() {
        UUID userId = UUID.randomUUID();
        when(sessionRepository.revokeAllUserSessions(userId)).thenReturn(3);

        int count = configurationService.revokeUserSessions(userId);

        assertThat(count).isEqualTo(3);
        verify(sessionRepository).revokeAllUserSessions(userId);
    }
}
