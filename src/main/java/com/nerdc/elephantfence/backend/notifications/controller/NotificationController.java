package com.nerdc.elephantfence.backend.notifications.controller;

import com.nerdc.elephantfence.backend.notifications.dto.NotificationPageDTO;
import com.nerdc.elephantfence.backend.notifications.dto.NotificationResponseDTO;
import com.nerdc.elephantfence.backend.notifications.dto.NotificationStatsDTO;
import com.nerdc.elephantfence.backend.notifications.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping
    public ResponseEntity<NotificationPageDTO> getNotifications(
            @RequestParam(required = false) Boolean read,
            @RequestParam(required = false) String category,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize
    ) {
        return ResponseEntity.ok(notificationService.getNotifications(read, category, page, pageSize));
    }

    @GetMapping("/stats")
    public ResponseEntity<NotificationStatsDTO> getStats() {
        return ResponseEntity.ok(notificationService.getStats());
    }

    @PatchMapping("/{id}/read")
    public ResponseEntity<NotificationResponseDTO> markRead(@PathVariable Long id) {
        return ResponseEntity.ok(notificationService.markRead(id));
    }

    @PatchMapping("/read-all")
    public ResponseEntity<Map<String, Integer>> markAllRead() {
        return ResponseEntity.ok(notificationService.markAllRead());
    }

    @DeleteMapping("/read")
    public ResponseEntity<Map<String, Integer>> clearRead() {
        return ResponseEntity.ok(notificationService.clearRead());
    }
}
