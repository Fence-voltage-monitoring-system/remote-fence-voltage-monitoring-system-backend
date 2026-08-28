package com.nerdc.elephantfence.backend.notifications.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationPageDTO {
    private List<NotificationResponseDTO> items;
    private int page;
    private int pageSize;
    private long totalItems;
    private int totalPages;
}
