package com.nerdc.elephantfence.backend.alerts.service;
import com.nerdc.elephantfence.backend.config.LiveTicketService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.*;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
@Component @RequiredArgsConstructor
public class AlertWebSocketHandler extends TextWebSocketHandler {
    private final LiveTicketService tickets;
    private final Set<WebSocketSession> sessions=ConcurrentHashMap.newKeySet();
    @Override public void afterConnectionEstablished(WebSocketSession session){sessions.add(session);}
    @Override public void afterConnectionClosed(WebSocketSession session,CloseStatus status){sessions.remove(session);}
    // Send only an invalidation. Incident data is fetched through the scoped, authenticated REST API.
    public void broadcast(String ignored){
        for(var session:sessions)try{
            var ticket=(LiveTicketService.Ticket)session.getAttributes().get("liveTicket");
            if(ticket==null||!tickets.valid(ticket)){session.close(CloseStatus.POLICY_VIOLATION);sessions.remove(session);continue;}
            synchronized(session){if(session.isOpen())session.sendMessage(new TextMessage("{\"type\":\"ALERTS_CHANGED\"}"));}
        }catch(Exception ex){sessions.remove(session);try{session.close();}catch(Exception ignoredClose){}}
    }
}

