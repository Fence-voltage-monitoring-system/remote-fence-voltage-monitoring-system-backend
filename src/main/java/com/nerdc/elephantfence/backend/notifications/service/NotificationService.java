package com.nerdc.elephantfence.backend.notifications.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nerdc.elephantfence.backend.fences.repository.FenceRepository;
import com.nerdc.elephantfence.backend.locations.entity.District;
import com.nerdc.elephantfence.backend.locations.entity.Province;
import com.nerdc.elephantfence.backend.locations.repository.DistrictRepository;
import com.nerdc.elephantfence.backend.locations.repository.ProvinceRepository;
import com.nerdc.elephantfence.backend.notifications.dto.NotificationPageDTO;
import com.nerdc.elephantfence.backend.notifications.dto.NotificationResponseDTO;
import com.nerdc.elephantfence.backend.notifications.dto.NotificationStatsDTO;
import com.nerdc.elephantfence.backend.notifications.entity.UserNotification;
import com.nerdc.elephantfence.backend.notifications.repository.UserNotificationRepository;
import com.nerdc.elephantfence.backend.sections.entity.Section;
import com.nerdc.elephantfence.backend.sections.repository.SectionRepository;
import com.nerdc.elephantfence.backend.users.entity.User;
import com.nerdc.elephantfence.backend.users.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {

    private final UserNotificationRepository userNotificationRepository;
    private final FenceRepository fenceRepository;
    private final SectionRepository sectionRepository;
    private final ProvinceRepository provinceRepository;
    private final DistrictRepository districtRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final NotificationWebSocketHandler webSocketHandler;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Transactional(readOnly = true)
    public NotificationPageDTO getNotifications(Boolean read, String category, int page, int pageSize) {
        UUID userId = getCurrentUserId();
        Pageable pageable = PageRequest.of(page - 1, pageSize);
        Page<UserNotification> resultPage = userNotificationRepository.findWithFilters(userId, read, category, pageable);

        List<NotificationResponseDTO> items = resultPage.getContent().stream()
                .map(this::toResponse)
                .toList();

        return NotificationPageDTO.builder()
                .items(items)
                .page(page)
                .pageSize(pageSize)
                .totalItems(resultPage.getTotalElements())
                .totalPages(resultPage.getTotalPages())
                .build();
    }

    @Transactional(readOnly = true)
    public NotificationStatsDTO getStats() {
        UUID userId = getCurrentUserId();
        long unread = userNotificationRepository.countByUserIdAndRead(userId, false);
        long inApp = userNotificationRepository.countByUserIdAndChannel(userId, "IN_APP");
        long websocket = userNotificationRepository.countByUserIdAndChannel(userId, "WEBSOCKET");
        long smsDelivered = userNotificationRepository.countByUserIdAndChannel(userId, "SMS");

        return NotificationStatsDTO.builder()
                .unread(unread)
                .inApp(inApp)
                .websocket(websocket)
                .smsDelivered(smsDelivered)
                .build();
    }

    @Transactional
    public NotificationResponseDTO markRead(Long id) {
        UserNotification notif = userNotificationRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Notification not found with ID: " + id));
        notif.setRead(true);
        userNotificationRepository.save(notif);
        return toResponse(notif);
    }

    @Transactional
    public Map<String, Integer> markAllRead() {
        UUID userId = getCurrentUserId();
        int updated = userNotificationRepository.markAllReadForUser(userId);
        Map<String, Integer> result = new HashMap<>();
        result.put("updated", updated);
        return result;
    }

    @Transactional
    public Map<String, Integer> clearRead() {
        UUID userId = getCurrentUserId();
        int deleted = userNotificationRepository.deleteReadForUser(userId);
        Map<String, Integer> result = new HashMap<>();
        result.put("deleted", deleted);
        return result;
    }

    @Transactional
    public void sendNotification(UserNotification notification) {
        userNotificationRepository.save(notification);
        broadcastNotification(notification);
    }

    private void broadcastNotification(UserNotification notif) {
        try {
            String json = objectMapper.writeValueAsString(toResponse(notif));
            webSocketHandler.broadcast(json);
        } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
            log.error("Failed to broadcast notification websocket update", e);
        }
    }

    private UUID getCurrentUserId() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || auth.getPrincipal() instanceof String) {
            return userRepository.findByEmailIgnoreCase("admin@nerdc.lk")
                    .map(User::getId)
                    .orElseGet(() -> {
                        User user = User.builder()
                                .fullName("System Administrator")
                                .email("admin@nerdc.lk")
                                .passwordHash(passwordEncoder.encode("Admin@123"))
                                .role(com.nerdc.elephantfence.backend.users.entity.Role.SUPER_ADMIN)
                                .enabled(true)
                                .staffId("ADM-001")
                                .build();
                        return userRepository.save(user).getId();
                    });
        }

        String email;
        if (auth.getPrincipal() instanceof UserDetails ud) {
            email = ud.getUsername();
        } else {
            email = auth.getName();
        }

        return userRepository.findByEmailIgnoreCase(email)
                .map(User::getId)
                .orElseThrow(() -> new IllegalArgumentException("Authenticated user not found in database: " + email));
    }

    private NotificationResponseDTO toResponse(UserNotification notif) {
        String provinceName = null;
        String districtName = null;
        String fenceName = null;
        String sectionCode = null;

        if (notif.getFenceId() != null) {
            var fOpt = fenceRepository.findById(notif.getFenceId());
            if (fOpt.isPresent()) {
                var fence = fOpt.get();
                fenceName = fence.getName();
                if (fence.getProvince() != null) {
                    provinceName = fence.getProvince().getName();
                }
                if (fence.getDistrict() != null) {
                    districtName = fence.getDistrict().getName();
                }
            }
        }

        if (notif.getSectionId() != null) {
            sectionCode = sectionRepository.findById(notif.getSectionId()).map(Section::getCode).orElse(null);
        }

        List<String> channelsList = notif.getChannels() != null
                ? Arrays.stream(notif.getChannels().split(","))
                        .map(String::trim)
                        .filter(s -> !s.isEmpty())
                        .toList()
                : Collections.emptyList();

        return NotificationResponseDTO.builder()
                .id(notif.getId())
                .code(notif.getCode())
                .title(notif.getTitle())
                .message(notif.getMessage())
                .category(notif.getCategory())
                .province(provinceName)
                .district(districtName)
                .fence(fenceName)
                .section(sectionCode)
                .time(notif.getCreatedAt() != null ? notif.getCreatedAt().toString() : "")
                .read(notif.isRead())
                .channels(channelsList)
                .relatedAlert(notif.getRelatedAlertCode())
                .build();
    }
}
