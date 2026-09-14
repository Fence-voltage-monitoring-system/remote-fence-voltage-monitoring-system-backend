package com.nerdc.elephantfence.backend.config;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.http.*;
import org.springframework.http.server.*;
import org.springframework.web.socket.*;
import org.springframework.web.socket.server.HandshakeInterceptor;
import org.springframework.web.util.UriComponentsBuilder;
import java.util.Map;
@Component @RequiredArgsConstructor
public class LiveHandshakeInterceptor implements HandshakeInterceptor {
    private final LiveTicketService tickets;
    public boolean beforeHandshake(ServerHttpRequest request,ServerHttpResponse response,WebSocketHandler handler,Map<String,Object> attributes){
        String channel=request.getURI().getPath().contains("/alerts/")?"alerts":"notifications";
        String key=UriComponentsBuilder.fromUri(request.getURI()).build().getQueryParams().getFirst("ticket");
        var ticket=tickets.consume(key,channel);
        if(ticket==null){response.setStatusCode(HttpStatus.UNAUTHORIZED);return false;}
        attributes.put("liveTicket",ticket);return true;
    }
    public void afterHandshake(ServerHttpRequest request,ServerHttpResponse response,WebSocketHandler handler,Exception exception){}
}

