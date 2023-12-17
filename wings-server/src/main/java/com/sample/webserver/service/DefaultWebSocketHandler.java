package com.sample.webserver.service;

import com.sample.webserver.model.Topic;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.socket.WebSocketHandler;
import org.springframework.web.reactive.socket.WebSocketSession;
import reactor.core.publisher.Mono;

@Component
@AllArgsConstructor
public class DefaultWebSocketHandler implements WebSocketHandler {
    private static final String JSON_TEMPLATE = "{\"headers\":{\"topic\":\"%s\"},\"payload\":{\"sessionId\":\"%s\"}}";
    private final WebSocketSessionRegistry sessionRegistry;

    @Override
    public Mono<Void> handle(WebSocketSession session) {
        sessionRegistry.add(session);
        String json = String.format(JSON_TEMPLATE, Topic.WEBSOCKET_INIT, session.getId());
        Mono<Void> sendMono = session.send(Mono.just(session.textMessage(json)));
        Mono<Void> closeMono = session.closeStatus().map(status -> {
            sessionRegistry.remove(session);
            return null;
        });
        return sendMono.and(closeMono);
    }
}
