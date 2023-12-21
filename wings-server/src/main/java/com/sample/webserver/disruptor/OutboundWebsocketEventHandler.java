package com.sample.webserver.disruptor;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lmax.disruptor.EventHandler;
import com.sample.webserver.model.ImmutableEvent;
import com.sample.webserver.model.MutableEvent;
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
public class OutboundWebsocketEventHandler implements EventHandler<MutableEvent> {
    private static final Logger LOGGER = LoggerFactory.getLogger(OutboundWebsocketEventHandler.class);

    private final Map<String, Consumer<MutableEvent>> handlersByTopic = Map.of(
        WEBSOCKET_CONNECT, this::onWebsocketConnect,
        WEBSOCKET_CLOSE, this::onWebsocketClose
    );

    private final Map<String, Consumer<MutableEvent>> handlersByEvent = Map.of(
        EVENT_SUBSCRIBE, this::onSubscribe,
        EVENT_UNSUBSCRIBE, this::onUnsubscribe,
        EVENT_ENTRY, this::onEntry,
        EVENT_SNAPSHOT, this::onSnapshot
    );

    private final WebSocketSessionRegistry sessionRegistry;
    private final ObjectMapper objectMapper;
    private final Map<String, List<ImmutableEvent>> subsByEntity = new LinkedHashMap<>();

    @Override
    public void onEvent(MutableEvent event, long sequence, boolean endOfBatch) {
        Topic topic = event.getTopic();
        Consumer<MutableEvent> handler = handlersByTopic.containsKey(topic.getTopic())
            ? handlersByTopic.get(topic.getTopic())
            : handlersByEvent.get(topic.getEvent());
        if (handler != null) {
            handler.accept(event);
        }
    }

    /**
     * Send the sessionId to the client
     * @param event
     */
    protected void onWebsocketConnect(MutableEvent event) {
        String sessionId = event.getRequiredHeader(SESSION_ID);
        Mono<Void> sendMono = sendEvent(sessionId, event.getHeaders(), event.getPayload());
        sendMono.block();
    }

    /**
     * Tidy up any serverside state for the session
     * @param event
     */
    protected void onWebsocketClose(MutableEvent event) {
        String sessionId = event.getRequiredHeader(SESSION_ID);
        for (List<ImmutableEvent> entitySubs : subsByEntity.values()) {
            entitySubs.removeIf(e -> sessionId.equals(e.getRequiredHeader(SESSION_ID)));
        }
    }

    /**
     * Store the subscription messages so we can fan out future messages to clients
     * @param event
     */
    protected void onSubscribe(MutableEvent event) {
        Topic topic = event.getTopic();
        List<ImmutableEvent> entitySubs = subsByEntity.computeIfAbsent(topic.getEntity(), key -> new ArrayList<>());
        entitySubs.add(new ImmutableEvent(event));
    }

    /**
     * Remove serverside state for the subscription
     * @param event
     */
    protected void onUnsubscribe(MutableEvent event) {
        Topic topic = event.getTopic();
        String sessionId = event.getRequiredHeader(SESSION_ID);
        String correlationId = event.getRequiredHeader(CORRELATION_ID);

        Predicate<ImmutableEvent> predicate = e -> sessionId.equals(e.getRequiredHeader(SESSION_ID))
                && correlationId.equals(e.getRequiredHeader(CORRELATION_ID));

        List<ImmutableEvent> entitySubs = subsByEntity.computeIfAbsent(topic.getEntity(), key -> new ArrayList<>());
        entitySubs.removeIf(predicate);
    }

    /**
     * Send the snapshot message to the client
     * @param event
     */
    protected void onSnapshot(MutableEvent event) {
        String sessionId = event.getRequiredHeader(SESSION_ID);
        Mono<Void> sendMono = sendEvent(sessionId, event.getHeaders(), event.getPayload());
        sendMono.block();
    }

    /**
     * Fan out the message to all clients who are subscribing to it
     * @param event
     */
    protected void onEntry(MutableEvent event) {
        Topic topic = event.getTopic();
        List<ImmutableEvent> entitySubs = subsByEntity.computeIfAbsent(topic.getEntity(), key -> new ArrayList<>());
        List<Mono<Void>> sendMonos = new ArrayList<>();
        for (ImmutableEvent sub : entitySubs) {
            String sessionId = sub.getRequiredHeader(SESSION_ID);
            String correlationId = sub.getRequiredHeader(CORRELATION_ID);
            Map<String, String> sendHeaders = new LinkedHashMap<>(event.getHeaders());
            sendHeaders.put(CORRELATION_ID, correlationId);
            sendMonos.add(sendEvent(sessionId, sendHeaders, event.getPayload()));
        }
        Mono<Void> sendMono = Mono.when(sendMonos);
        sendMono.block();
    }

    /**
     * Send a websocket message to a single WebSocketSession
     * @param sessionId
     * @param headers
     * @param payload
     * @return
     */
    protected Mono<Void> sendEvent(String sessionId, Map<String, String> headers, Object payload) {
        WebSocketSession session = sessionRegistry.get(sessionId);
        if (session == null) {
            return Mono.empty();
        }
        try {
            ImmutableEvent event = new ImmutableEvent(headers, payload);
            String json = objectMapper.writeValueAsString(event);
            WebSocketMessage message = session.textMessage(json);
            return session.send(Mono.just(message));
        } catch (JsonProcessingException e) {
            LOGGER.error("Error sending entry", e);
            return Mono.empty();
        }
    }
}
