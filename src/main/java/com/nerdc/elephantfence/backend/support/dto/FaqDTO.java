package com.nerdc.elephantfence.backend.support.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FaqDTO {
    private String id;
    private String category;
    private String question;
    private String answer;
}
