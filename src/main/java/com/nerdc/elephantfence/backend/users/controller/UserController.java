package com.nerdc.elephantfence.backend.users.controller;

import com.nerdc.elephantfence.backend.common.security.UserPrincipal;
import com.nerdc.elephantfence.backend.notifications.dto.UserNotificationPreferencesDTO;
import com.nerdc.elephantfence.backend.users.dto.UserCreateRequestDTO;
import com.nerdc.elephantfence.backend.users.dto.UserResponseDTO;
import com.nerdc.elephantfence.backend.users.dto.UserUpdateRequestDTO;
import com.nerdc.elephantfence.backend.users.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'REGIONAL_ADMIN', 'FIELD_ADMIN')")
    public ResponseEntity<List<UserResponseDTO>> getAllUsers() {
        return ResponseEntity.ok(userService.getAllUsers());
    }

    @GetMapping("/me/notification-preferences")
    public ResponseEntity<UserNotificationPreferencesDTO> getMyNotificationPreferences(@AuthenticationPrincipal UserPrincipal userPrincipal) {
        UUID userId = resolveUserId(userPrincipal);
        return ResponseEntity.ok(userService.getNotificationPreferences(userId));
    }

    @PutMapping("/me/notification-preferences")
    public ResponseEntity<UserNotificationPreferencesDTO> updateMyNotificationPreferences(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @RequestBody UserNotificationPreferencesDTO dto
    ) {
        UUID userId = resolveUserId(userPrincipal);
        return ResponseEntity.ok(userService.updateNotificationPreferences(userId, dto));
    }

    @GetMapping("/{id}")
    public ResponseEntity<UserResponseDTO> getUserById(@PathVariable UUID id) {
        return ResponseEntity.ok(userService.getUserById(id));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'REGIONAL_ADMIN', 'FIELD_ADMIN')")
    public ResponseEntity<UserResponseDTO> createUser(@Valid @RequestBody UserCreateRequestDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(userService.createUser(dto));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'REGIONAL_ADMIN', 'FIELD_ADMIN')")
    public ResponseEntity<UserResponseDTO> updateUser(
            @PathVariable UUID id,
            @Valid @RequestBody UserUpdateRequestDTO dto
    ) {
        return ResponseEntity.ok(userService.updateUser(id, dto));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'REGIONAL_ADMIN', 'FIELD_ADMIN')")
    public ResponseEntity<UserResponseDTO> updateUserStatus(
            @PathVariable UUID id,
            @RequestBody java.util.Map<String, Object> body
    ) {
        boolean enabled = false;
        if (body.containsKey("enabled")) {
            enabled = Boolean.TRUE.equals(body.get("enabled"));
        } else if (body.containsKey("status")) {
            enabled = "ACTIVE".equalsIgnoreCase(String.valueOf(body.get("status")));
        }
        return ResponseEntity.ok(userService.updateUserStatus(id, enabled));
    }

    @PostMapping("/{id}/reset-password")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'REGIONAL_ADMIN', 'FIELD_ADMIN')")
    public ResponseEntity<java.util.Map<String, String>> resetPassword(@PathVariable UUID id) {
        return ResponseEntity.ok(userService.resetPassword(id));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<Void> deleteUser(@PathVariable UUID id) {
        userService.deleteUser(id);
        return ResponseEntity.noContent().build();
    }

    private UUID resolveUserId(UserPrincipal userPrincipal) {
        if (userPrincipal != null) {
            return userPrincipal.getId();
        }
        return userService.getAllUsers().stream()
                .filter(u -> "admin@nerdc.lk".equalsIgnoreCase(u.getEmail()))
                .findFirst()
                .map(UserResponseDTO::getId)
                .orElseGet(() -> userService.getAllUsers().get(0).getId());
    }
}
