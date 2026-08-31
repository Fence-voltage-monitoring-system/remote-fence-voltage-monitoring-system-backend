package com.nerdc.elephantfence.backend.support.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SupportTicketResponseDTO {
    private String id;
    private String status;
}
