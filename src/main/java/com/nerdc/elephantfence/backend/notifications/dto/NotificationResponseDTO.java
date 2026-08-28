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
public class NotificationResponseDTO {
    private Long id;
    private String code;
    private String title;
    private String message;
    private String category;
    private String province;
    private String district;
    private String fence;
    private String section;
    private String time;
    private boolean read;
    private List<String> channels;
    private String relatedAlert;
}
