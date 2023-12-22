package com.sample.webserver.disruptor;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.lmax.disruptor.RingBuffer;
import com.lmax.disruptor.dsl.Disruptor;
import com.lmax.disruptor.util.DaemonThreadFactory;
import com.sample.webserver.model.JsonEvent;
import com.sample.webserver.model.MutableEvent;
import com.sample.webserver.service.WebSocketSessionRegistry;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.reactive.socket.WebSocketSession;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static com.sample.webserver.model.EventHeader.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class OutboundWebsocketEventHandlerTest {
    private final ObjectMapper objectMapper = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);
    private Disruptor<MutableEvent> disruptor;
    private WebSocketSessionRegistry sessionRegistry;
    private AtomicInteger nextCorrelationId = new AtomicInteger();
    private Map<String, List<JsonEvent>> eventsBySessionId;

    @BeforeEach
    public void beforeEach() {
        eventsBySessionId = new LinkedHashMap<>();
        sessionRegistry = new WebSocketSessionRegistry();
        OutboundWebsocketEventHandler outboundHandler = new TestOutboundWebsocketEventHandler(sessionRegistry, objectMapper);
        disruptor = new Disruptor<>(MutableEvent::new, 1024, DaemonThreadFactory.INSTANCE);
        disruptor.handleEventsWith(outboundHandler);
        disruptor.start();
    }

    @AfterEach
    public void afterEach() {
        disruptor.shutdown();
    }

    @Test
    public void testSubscription() {
        // given
        WebSocketSession sessionA = mockWebSocketSession("sessionA");
        WebSocketSession sessionB = mockWebSocketSession("sessionB");

        // when
        sessionRegistry.add(sessionA);
        sessionRegistry.add(sessionB);

        String corrXA = subscribe("X", sessionA.getId());
        String corrYA = subscribe("Y", sessionA.getId());
        String corrXB = subscribe("X", sessionB.getId());
        String corrZB = subscribe("Z", sessionB.getId());

        snapshot("X", "x-snapshot-A", sessionA.getId(), corrXA);
        snapshot("X", "x-snapshot-B", sessionB.getId(), corrXB);
        snapshot("Y", "y-snapshot", sessionA.getId(), corrYA);
        snapshot("Z", "z-snapshot", sessionB.getId(), corrZB);

        entry("X", "x-entry-1");
        entry("X", "x-entry-2");
        entry("Y", "y-entry-1");
        entry("Z", "z-entry-1");

        unsubscribe("X", sessionA.getId(), corrXA);

        entry("X", "x-entry-3");

        awaitRingBuffer();

        // then
        List<JsonEvent> sessionAEvents = eventsBySessionId.get("sessionA");
        List<JsonEvent> sessionBEvents = eventsBySessionId.get("sessionB");

        assertThat(sessionAEvents).extracting(this::getTopicAndPayload).containsExactly(
                "X:snapshot(x-snapshot-A)",
                "Y:snapshot(y-snapshot)",
                "X:entry(x-entry-1)",
                "X:entry(x-entry-2)",
                "Y:entry(y-entry-1)"
        );
        assertThat(sessionAEvents).extracting(this::getCorrelationId)
                .containsExactly(corrXA, corrYA, corrXA, corrXA, corrYA);

        assertThat(sessionBEvents).extracting(this::getTopicAndPayload).containsExactly(
                "X:snapshot(x-snapshot-B)",
                "Z:snapshot(z-snapshot)",
                "X:entry(x-entry-1)",
                "X:entry(x-entry-2)",
                "Z:entry(z-entry-1)",
                "X:entry(x-entry-3)"
        );
        assertThat(sessionBEvents).extracting(this::getCorrelationId)
                .containsExactly(corrXB, corrZB, corrXB, corrXB, corrZB, corrXB);
    }

    private void awaitRingBuffer() {
        RingBuffer<MutableEvent> ringBuffer = disruptor.getRingBuffer();
        while (ringBuffer.remainingCapacity() != disruptor.getBufferSize()) {
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private String getTopicAndPayload(JsonEvent event) {
        String topic = event.getHeaders().get(TOPIC);
        String payload = event.getPayload().asText();
        return topic + "(" + payload + ")";
    }

    private String getCorrelationId(JsonEvent event) {
        return event.getHeaders().get(CORRELATION_ID);
    }

    private WebSocketSession mockWebSocketSession(String id) {
        WebSocketSession session = mock(WebSocketSession.class);
        when(session.getId()).thenReturn(id);
        return session;
    }

    private String subscribe(String entity, String sessionId) {
        String correlationId = String.format("%05d", nextCorrelationId.getAndIncrement());
        Map<String, String> headers = Map.of(
                TOPIC, entity + ":subscribe",
                SESSION_ID, sessionId,
                CORRELATION_ID, correlationId
        );
        publishEvent(headers);
        return correlationId;
    }

    private void snapshot(String entity, String payload, String sessionId, String correlationId) {
        Map<String, String> headers = Map.of(
                TOPIC, entity + ":snapshot",
                SESSION_ID, sessionId,
                CORRELATION_ID, correlationId
        );
        publishEvent(headers, payload);
    }


    private void entry(String entity, Object payload) {
        Map<String, String> headers = Map.of(TOPIC, entity + ":entry");
        publishEvent(headers, payload);
    }

    private void unsubscribe(String entity, String sessionId, String correlationId) {
        Map<String, String> headers = Map.of(
                TOPIC, entity + ":unsubscribe",
                SESSION_ID, sessionId,
                CORRELATION_ID, correlationId
        );
        publishEvent(headers);
    }

    private void publishEvent(Map<String, String> headers, Object payload) {
        disruptor.publishEvent((event, sequence) -> {
            event.init(headers, payload);
        });
    }

    private void publishEvent(Map<String, String> headers) {
        publishEvent(headers, null);
    }

    /**
     * Stores outbound messages in eventsBySessionId
     */
    private class TestOutboundWebsocketEventHandler extends OutboundWebsocketEventHandler {
        public TestOutboundWebsocketEventHandler(WebSocketSessionRegistry sessionRegistry, ObjectMapper objectMapper) {
            super(sessionRegistry, objectMapper);
        }

        @Override
        protected Mono<Void> sendTextMessage(WebSocketSession session, String json) {
            return Mono.<Void>empty().doOnTerminate(() -> {
                try {
                    JsonEvent event = objectMapper.readValue(json, JsonEvent.class);
                    List<JsonEvent> sessionEvents = eventsBySessionId.computeIfAbsent(session.getId(), key -> new ArrayList<>());
                    sessionEvents.add(event);
                } catch (JsonProcessingException e) {
                    throw new RuntimeException(e);
                }
            });
        }
    }
}