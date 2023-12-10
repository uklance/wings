package com.sample.webserver.service;

import lombok.AllArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.socket.WebSocketHandler;
import org.springframework.web.reactive.socket.WebSocketSession;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Component
@AllArgsConstructor
public class DefaultWebSocketHandler implements WebSocketHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultWebSocketHandler.class);
    private final WebSocketSessionRegistry sessionRegistry;

    @Override
    public Mono<Void> handle(WebSocketSession session) {
        sessionRegistry.add(session);
        String json = String.format("{\"payload\":{\"sessionId\":\"%s\"}}", session.getId());
        Mono<Void> sendMono = session.send(Flux.just(session.textMessage(json)));
        Mono<Void> closeMono = session.closeStatus().map(status -> {
            sessionRegistry.remove(session);
            return null;
        });
        return Mono.zip(sendMono, closeMono).map(tuple -> null);
    }
}
