package com.nerdc.elephantfence.backend.support.controller;

import com.nerdc.elephantfence.backend.support.dto.FaqDTO;
import com.nerdc.elephantfence.backend.support.dto.SupportTicketPayloadDTO;
import com.nerdc.elephantfence.backend.support.dto.SupportTicketResponseDTO;
import com.nerdc.elephantfence.backend.support.dto.SystemStatusInfoDTO;
import com.nerdc.elephantfence.backend.support.service.SupportService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/support")
@RequiredArgsConstructor
public class SupportController {

    private final SupportService supportService;

    @GetMapping("/faqs")
    public ResponseEntity<List<FaqDTO>> getFaqs() {
        return ResponseEntity.ok(supportService.getFaqs());
    }

    @PostMapping("/tickets")
    public ResponseEntity<SupportTicketResponseDTO> submitTicket(
            @Valid @RequestBody SupportTicketPayloadDTO payload) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(supportService.submitTicket(payload));
    }

    @GetMapping("/status")
    public ResponseEntity<SystemStatusInfoDTO> getSystemStatus() {
        return ResponseEntity.ok(supportService.getSystemStatus());
    }
}
