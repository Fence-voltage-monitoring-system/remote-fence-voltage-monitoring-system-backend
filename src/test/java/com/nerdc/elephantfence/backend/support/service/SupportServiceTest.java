package com.nerdc.elephantfence.backend.support.service;

import com.nerdc.elephantfence.backend.support.dto.FaqDTO;
import com.nerdc.elephantfence.backend.support.dto.SupportTicketPayloadDTO;
import com.nerdc.elephantfence.backend.support.dto.SupportTicketResponseDTO;
import com.nerdc.elephantfence.backend.support.dto.SystemStatusInfoDTO;
import com.nerdc.elephantfence.backend.support.entity.Faq;
import com.nerdc.elephantfence.backend.support.entity.SupportTicket;
import com.nerdc.elephantfence.backend.support.repository.FaqRepository;
import com.nerdc.elephantfence.backend.support.repository.SupportTicketRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SupportServiceTest {

    @Mock
    private FaqRepository faqRepository;

    @Mock
    private SupportTicketRepository supportTicketRepository;

    @InjectMocks
    private SupportService supportService;

    @Test
    void getFaqs_shouldReturnMappedList() {
        Faq faq = Faq.builder()
                .id(1L)
                .category("General")
                .question("How to login?")
                .answer("Use email.")
                .build();
        when(faqRepository.findAllByOrderByDisplayOrderAsc()).thenReturn(List.of(faq));

        List<FaqDTO> faqs = supportService.getFaqs();

        assertThat(faqs).hasSize(1);
        assertThat(faqs.get(0).getCategory()).isEqualTo("General");
    }

    @Test
    void submitTicket_shouldSaveAndReturnId() {
        SupportTicketPayloadDTO payload = SupportTicketPayloadDTO.builder()
                .name("John")
                .email("john@test.com")
                .category("Hardware")
                .subject("Device down")
                .description("Not working")
                .priority("HIGH")
                .build();

        SupportTicket savedTicket = SupportTicket.builder()
                .id(42L)
                .status("OPEN")
                .build();

        when(supportTicketRepository.save(any(SupportTicket.class))).thenReturn(savedTicket);

        SupportTicketResponseDTO response = supportService.submitTicket(payload);

        assertThat(response.getId()).isEqualTo("TKT-42");
        assertThat(response.getStatus()).isEqualTo("OPEN");
    }

    @Test
    void getSystemStatus_shouldReturnOperational() {
        SystemStatusInfoDTO status = supportService.getSystemStatus();

        assertThat(status.getOverallStatus()).isEqualTo("OPERATIONAL");
        assertThat(status.getVersion()).isEqualTo("1.0.0");
        assertThat(status.getLastUpdated()).isNotNull();
    }
}
