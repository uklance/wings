package com.sample.webserver.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sample.webserver.configuration.WebServerConfiguration;
import com.sample.webserver.model.EventHeader;
import com.sample.webserver.model.EventType;
import com.sample.webserver.model.JsonNodeEvent;
import lombok.AllArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.socket.WebSocketHandler;
import org.springframework.web.reactive.socket.WebSocketSession;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Map;

@Component
@AllArgsConstructor
public class DefaultWebSocketHandler implements WebSocketHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(WebServerConfiguration.class);
    private final WebSocketSessionRegistry sessionRegistry;
    private final ObjectMapper objectMapper;

    @Override
    public Mono<Void> handle(WebSocketSession session) {
        sessionRegistry.add(session);
        Map<String, String> headers = Map.of(EventHeader.EVENT_TYPE, EventType.WEBSOCKET_INIT);
        JsonNode payload = objectMapper.valueToTree(Map.of("sessionId", session.getId()));
        String json;
        try {
            json = objectMapper.writeValueAsString(new JsonNodeEvent(headers, payload));
        } catch (Exception e) {
            LOGGER.error(session.getId(), e);
            return Mono.empty();
        }
        Mono<Void> sendMono = session.send(Mono.just(session.textMessage(json)));
        Mono<Void> closeMono = session.closeStatus().map(status -> {
            sessionRegistry.remove(session);
            return null;
        });
        return sendMono.and(closeMono);
    }
}
