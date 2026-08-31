package com.nerdc.elephantfence.backend.support.service;

import com.nerdc.elephantfence.backend.support.dto.FaqDTO;
import com.nerdc.elephantfence.backend.support.dto.SupportTicketPayloadDTO;
import com.nerdc.elephantfence.backend.support.dto.SupportTicketResponseDTO;
import com.nerdc.elephantfence.backend.support.dto.SystemStatusInfoDTO;
import com.nerdc.elephantfence.backend.support.entity.Faq;
import com.nerdc.elephantfence.backend.support.entity.SupportTicket;
import com.nerdc.elephantfence.backend.support.repository.FaqRepository;
import com.nerdc.elephantfence.backend.support.repository.SupportTicketRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SupportService {

    private final FaqRepository faqRepository;
    private final SupportTicketRepository supportTicketRepository;

    @Transactional(readOnly = true)
    public List<FaqDTO> getFaqs() {
        return faqRepository.findAllByOrderByDisplayOrderAsc().stream()
                .map(faq -> FaqDTO.builder()
                        .id(faq.getId().toString())
                        .category(faq.getCategory())
                        .question(faq.getQuestion())
                        .answer(faq.getAnswer())
                        .build())
                .toList();
    }

    @Transactional
    public SupportTicketResponseDTO submitTicket(SupportTicketPayloadDTO payload) {
        SupportTicket ticket = SupportTicket.builder()
                .name(payload.getName())
                .email(payload.getEmail())
                .category(payload.getCategory())
                .subject(payload.getSubject())
                .description(payload.getDescription())
                .priority(payload.getPriority())
                .status("OPEN")
                .build();

        ticket = supportTicketRepository.save(ticket);

        return SupportTicketResponseDTO.builder()
                .id("TKT-" + ticket.getId())
                .status(ticket.getStatus())
                .build();
    }

    public SystemStatusInfoDTO getSystemStatus() {
        return SystemStatusInfoDTO.builder()
                .overallStatus("OPERATIONAL")
                .lastUpdated(OffsetDateTime.now().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME))
                .environment("PRODUCTION")
                .version("1.0.0")
                .build();
    }
}
