package com.sample.webserver.service;

import com.lmax.disruptor.dsl.Disruptor;
import com.sample.webserver.model.EventHeader;
import com.sample.webserver.model.MutableEvent;
import com.sample.webserver.model.Topic;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.socket.WebSocketHandler;
import org.springframework.web.reactive.socket.WebSocketSession;
import reactor.core.publisher.Mono;

import java.util.Map;

@Component
@AllArgsConstructor
public class DefaultWebSocketHandler implements WebSocketHandler {
    private final WebSocketSessionRegistry sessionRegistry;
    private final Disruptor<MutableEvent> disruptor;

    @Override
    public Mono<Void> handle(WebSocketSession session) {
        sessionRegistry.add(session);
        disruptorPublish(Topic.WEBSOCKET_CONNECT, session.getId());
        Mono<Void> closeMono = session.closeStatus().map(status -> {
            sessionRegistry.remove(session);
            disruptorPublish(Topic.WEBSOCKET_CLOSE, session.getId());
            return null;
        });
        return closeMono;
    }

    protected void disruptorPublish(String topic, String sessionId) {
        disruptor.publishEvent((event, sequence) -> {
            Map<String, String> headers = Map.of(
                    EventHeader.TOPIC, topic,
                    EventHeader.SESSION_ID, sessionId
            );
            event.init(headers, null);
        });
    }
}