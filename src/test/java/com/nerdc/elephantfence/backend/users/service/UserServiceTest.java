package com.nerdc.elephantfence.backend.users.service;

import com.nerdc.elephantfence.backend.configuration.repository.UserSessionRepository;
import com.nerdc.elephantfence.backend.configuration.service.ConfigurationService;
import com.nerdc.elephantfence.backend.users.dto.UserCreateRequestDTO;
import com.nerdc.elephantfence.backend.users.dto.UserResponseDTO;
import com.nerdc.elephantfence.backend.users.dto.UserUpdateRequestDTO;
import com.nerdc.elephantfence.backend.users.entity.Role;
import com.nerdc.elephantfence.backend.users.entity.User;
import com.nerdc.elephantfence.backend.users.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private UserSessionRepository sessionRepository;

    @Mock
    private ConfigurationService configurationService;

    @InjectMocks
    private UserService userService;

    @Test
    void createUser_shouldSaveAndReturnUser() {
        UserCreateRequestDTO dto = UserCreateRequestDTO.builder()
                .fullName("Field Admin")
                .email("field@nerdc.lk")
                .password("Secret123")
                .role(Role.FIELD_ADMIN)
                .build();

        when(userRepository.existsByEmailIgnoreCase("field@nerdc.lk")).thenReturn(false);
        when(passwordEncoder.encode("Secret123")).thenReturn("encodedPassword");

        User savedUser = User.builder()
                .id(UUID.randomUUID())
                .fullName(dto.getFullName())
                .email(dto.getEmail())
                .passwordHash("encodedPassword")
                .role(Role.FIELD_ADMIN)
                .enabled(true)
                .build();

        when(userRepository.save(any(User.class))).thenReturn(savedUser);

        UserResponseDTO response = userService.createUser(dto);

        assertThat(response.getEmail()).isEqualTo("field@nerdc.lk");
        assertThat(response.getRole()).isEqualTo(Role.FIELD_ADMIN);
    }

    @Test
    void createUser_shouldThrowExceptionWhenEmailExists() {
        UserCreateRequestDTO dto = UserCreateRequestDTO.builder()
                .email("existing@nerdc.lk")
                .build();

        when(userRepository.existsByEmailIgnoreCase("existing@nerdc.lk")).thenReturn(true);

        assertThatThrownBy(() -> userService.createUser(dto))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("User already exists");
    }

    @Test
    void resetPassword_shouldRevokeAllSessions() {
        UUID id = UUID.randomUUID();
        User user = User.builder().id(id).fullName("Test User").role(Role.FIELD_ADMIN).enabled(true).build();
        when(userRepository.findByIdWithProvincesAndDistricts(id)).thenReturn(Optional.of(user));
        when(passwordEncoder.encode(any())).thenReturn("encodedTempPass");
        when(configurationService.getSessionManagementConfig()).thenReturn(
                new ConfigurationService.SessionManagementConfig(12, 30, 7, 5, 2, "REVOKE_OLDEST", true, 5, true, true, true, true, true)
        );

        userService.resetPassword(id);

        verify(sessionRepository).revokeAllUserSessions(id);
        verify(userRepository).save(user);
    }

    @Test
    void updateUserStatus_whenDisabled_shouldRevokeAllSessions() {
        UUID id = UUID.randomUUID();
        User user = User.builder().id(id).fullName("Test User").role(Role.FIELD_ADMIN).enabled(true).build();
        when(userRepository.findByIdWithProvincesAndDistricts(id)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenReturn(user);
        when(configurationService.getSessionManagementConfig()).thenReturn(
                new ConfigurationService.SessionManagementConfig(12, 30, 7, 5, 2, "REVOKE_OLDEST", true, 5, true, true, true, true, true)
        );

        userService.updateUserStatus(id, false);

        verify(sessionRepository).revokeAllUserSessions(id);
    }

    @Test
    void updateUser_whenRoleChanged_shouldRevokeAllSessions() {
        UUID id = UUID.randomUUID();
        User user = User.builder().id(id).fullName("Test User").role(Role.FIELD_ADMIN).enabled(true).build();
        when(userRepository.findByIdWithProvincesAndDistricts(id)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenReturn(user);
        when(configurationService.getSessionManagementConfig()).thenReturn(
                new ConfigurationService.SessionManagementConfig(12, 30, 7, 5, 2, "REVOKE_OLDEST", true, 5, true, true, true, true, true)
        );

        UserUpdateRequestDTO updateDTO = UserUpdateRequestDTO.builder().role(Role.REGIONAL_ADMIN).build();
        userService.updateUser(id, updateDTO);

        verify(sessionRepository).revokeAllUserSessions(id);
    }
}
