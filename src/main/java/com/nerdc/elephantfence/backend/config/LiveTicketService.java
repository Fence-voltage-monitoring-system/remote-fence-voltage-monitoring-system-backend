package com.nerdc.elephantfence.backend.config;
import com.nerdc.elephantfence.backend.common.security.UserPrincipal;
import com.nerdc.elephantfence.backend.configuration.repository.UserSessionRepository;
import com.nerdc.elephantfence.backend.users.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import java.time.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
@Service @RequiredArgsConstructor
public class LiveTicketService {
    private final UserSessionRepository sessions;
    private final UserRepository users;
    public record Ticket(UUID userId,String sessionId,String channel,Instant expires){}
    private final Map<String,Ticket> tickets=new ConcurrentHashMap<>();
    public String issue(UserPrincipal principal,String channel){
        if(principal==null||principal.getSessionId()==null)throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        if(!Set.of("alerts","notifications").contains(channel))throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
        tickets.entrySet().removeIf(e->e.getValue().expires().isBefore(Instant.now()));
        if(tickets.size()>=10000)throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS);
        String key=UUID.randomUUID().toString();
        Ticket ticket=new Ticket(principal.getId(),principal.getSessionId(),channel,Instant.now().plusSeconds(30));
        if(!valid(ticket))throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        tickets.put(key,ticket);return key;
    }
    public Ticket consume(String key,String channel){
        Ticket ticket=key==null?null:tickets.remove(key);
        return ticket!=null&&ticket.channel().equals(channel)&&ticket.expires().isAfter(Instant.now())&&valid(ticket)?ticket:null;
    }
    public boolean valid(Ticket ticket){
        return users.findById(ticket.userId()).filter(u->u.isEnabled()).isPresent()&&
            sessions.findById(ticket.sessionId()).filter(s->s.getExpiresAt().isAfter(OffsetDateTime.now())&&s.getUser().getId().equals(ticket.userId())).isPresent();
    }
}

