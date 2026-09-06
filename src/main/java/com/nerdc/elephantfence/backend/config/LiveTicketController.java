package com.nerdc.elephantfence.backend.config;
import com.nerdc.elephantfence.backend.common.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import java.util.Map;
@RestController @RequiredArgsConstructor @RequestMapping("/api/live")
public class LiveTicketController {
    private final LiveTicketService tickets;
    @PostMapping("/tickets/{channel}")
    public Map<String,String> issue(@AuthenticationPrincipal UserPrincipal principal,@PathVariable String channel){
        return Map.of("ticket",tickets.issue(principal,channel));
    }
}

