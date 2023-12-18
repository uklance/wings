package com.sample.webserver.disruptor;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lmax.disruptor.EventHandler;
import com.sample.webserver.model.Event;
import com.sample.webserver.model.Topic;
import com.sample.webserver.service.WebSocketSessionRegistry;
import lombok.AllArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.socket.WebSocketMessage;
import org.springframework.web.reactive.socket.WebSocketSession;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Predicate;

import static com.sample.webserver.model.EventHeader.CORRELATION_ID;
import static com.sample.webserver.model.EventHeader.SESSION_ID;
import static com.sample.webserver.model.Topic.*;

@Component
@AllArgsConstructor
public class OutboundWebsocketEventHandler implements EventHandler<Event> {
    private static final Logger LOGGER = LoggerFactory.getLogger(OutboundWebsocketEventHandler.class);
    private final Map<String, Consumer<Event>> handlersByEvent = Map.of(
            EVENT_SUBSCRIBE, this::onSubscribe,
            EVENT_UNSUBSCRIBE, this::onUnsubscribe,
            EVENT_ENTRY, this::onEntry,
            EVENT_SNAPSHOT, this::onSnapshot
    );

    private final WebSocketSessionRegistry sessionRegistry;
    private final ObjectMapper objectMapper;

    private final Map<String, List<Event>> subsByEntity = new LinkedHashMap<>();

    @Override
    public void onEvent(Event event, long sequence, boolean endOfBatch) throws Exception {
        Topic topic = event.getTopic();
        Consumer<Event> handler = handlersByEvent.get(topic.getEvent());
        if (handler != null) {
            handler.accept(event);
        }
    }

    protected void onSubscribe(Event event) {
        Topic topic = event.getTopic();
        List<Event> entitySubs = subsByEntity.computeIfAbsent(topic.getEntity(), key -> new ArrayList<>());
        Event eventClone = new Event();
        eventClone.init(event.getHeaders(), event.getPayload());
        entitySubs.add(eventClone);
    }

    protected void onUnsubscribe(Event event) {
        Topic topic = event.getTopic();
        String sessionId = event.getRequiredHeader(SESSION_ID);
        String correlationId = event.getRequiredHeader(CORRELATION_ID);

        Predicate<Event> predicate = e -> sessionId.equals(e.getRequiredHeader(SESSION_ID))
                && correlationId.equals(e.getRequiredHeader(CORRELATION_ID));

        List<Event> entitySubs = subsByEntity.computeIfAbsent(topic.getEntity(), key -> new ArrayList<>());
        entitySubs.removeIf(predicate);
    }

    protected void onSnapshot(Event event) {
        String sessionId = event.getRequiredHeader(SESSION_ID);
        WebSocketSession session = sessionRegistry.get(sessionId);
        try {
            WebSocketMessage wsMessage = session.textMessage(objectMapper.writeValueAsString(event));
            session.send(Mono.just(wsMessage)).block();
        } catch (JsonProcessingException e) {
            LOGGER.error("Error sending snapshot", e);
        }
    }

    protected void onEntry(Event event) {
        Topic topic = event.getTopic();
        List<Event> entitySubs = subsByEntity.computeIfAbsent(topic.getEntity(), key -> new ArrayList<>());
        List<Mono<Void>> sendMonos = new ArrayList<>();
        for (Event sub : entitySubs) {
            String sessionId = sub.getRequiredHeader(SESSION_ID);
            WebSocketSession session = sessionRegistry.get(sessionId);
            if (session != null) {
                String correlationId = sub.getRequiredHeader(CORRELATION_ID);
                Map<String, String> sendHeaders = new LinkedHashMap<>(event.getHeaders());
                sendHeaders.put(CORRELATION_ID, correlationId);
                Event sendEvent = new Event();
                sendEvent.init(sendHeaders, event.getPayload());
                try {
                    String json = objectMapper.writeValueAsString(event);
                    WebSocketMessage message = session.textMessage(json);
                    sendMonos.add(session.send(Mono.just(message)));
                } catch (JsonProcessingException e) {
                    LOGGER.error("Error sending entry", e);
                }
            }
        }
        Mono<Void> sendMono = Mono.when(sendMonos);
        sendMono.block();
    }
}
