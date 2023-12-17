package com.sample.webserver.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lmax.disruptor.EventHandler;
import com.sample.webserver.model.Event;
import com.sample.webserver.model.Topic;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.socket.WebSocketMessage;
import org.springframework.web.reactive.socket.WebSocketSession;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Component
@AllArgsConstructor
public class OutboundWebsocketEventHandler implements EventHandler<Event> {
    private final Set<String> TOPICS = Set.of(Topic.TRADE_SNAPSHOT, Topic.TRADE_ENTRY);

    private final WebSocketSessionRegistry sessionRegistry;
    private final ObjectMapper objectMapper;

    @Override
    public void onEvent(Event event, long sequence, boolean endOfBatch) throws Exception {
        if (TOPICS.contains(event.getTopic())) {
            String json = objectMapper.writeValueAsString(event);
            List<Mono<Void>> sendMonos = new ArrayList<>();
            for (WebSocketSession session : sessionRegistry.getAll()) {
                WebSocketMessage message = session.textMessage(json);
                sendMonos.add(session.send(Mono.just(message)));
            }
            Mono<Void> sendMono = Mono.when(sendMonos);
            sendMono.block();
        }
    }
}
