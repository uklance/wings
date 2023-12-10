package com.sample.webserver.configuration;

import com.lmax.disruptor.EventHandler;
import com.lmax.disruptor.dsl.Disruptor;
import com.lmax.disruptor.util.DaemonThreadFactory;
import com.sample.webserver.model.Event;
import com.sample.webserver.service.DefaultWebSocketHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.web.reactive.HandlerMapping;
import org.springframework.web.reactive.handler.SimpleUrlHandlerMapping;
import org.springframework.web.reactive.socket.WebSocketHandler;

import java.util.Map;

@Configuration
public class WebServerConfiguration {
    private static final Logger LOGGER = LoggerFactory.getLogger(WebServerConfiguration.class);

    @Bean
    public Disruptor<Event> disruptor(
            @Value("${disruptor.bufferSize}") int bufferSize,
            @Qualifier("eventHandler1") EventHandler<Event> eventHandler1,
            @Qualifier("eventHandler2") EventHandler<Event> eventHandler2,
            @Qualifier("closeEventHandler") EventHandler<Event> closeEventHandler
    ) {
        Disruptor<Event> disruptor = new Disruptor<>(Event::new, bufferSize, DaemonThreadFactory.INSTANCE);
        disruptor.handleEventsWith(eventHandler1, eventHandler2).then(closeEventHandler);
        return disruptor;
    }

    @Bean
    public HandlerMapping webSocketHandlerMapping(DefaultWebSocketHandler handler) {
        SimpleUrlHandlerMapping handlerMapping = new SimpleUrlHandlerMapping();
        handlerMapping.setOrder(1);
        handlerMapping.setUrlMap(Map.of("/websocket", handler));
        return handlerMapping;
    }

    @Bean
    public ApplicationListener<ContextRefreshedEvent> refreshListener(Disruptor<Event> disruptor, @Value("${disruptor.start}") boolean start) {
        return (ContextRefreshedEvent event) -> {
            if (start) {
                LOGGER.info("Starting the disruptor");
                disruptor.start();
            }
        };
    }

    @Bean
    public ApplicationListener<ContextClosedEvent> closeListener(Disruptor<Event> disruptor) {
        return (ContextClosedEvent event) -> {
            if (disruptor.hasStarted()) {
                LOGGER.info("Shutting down the disruptor");
                disruptor.shutdown();
            }
        };
    }

    @Bean
    public EventHandler<Event> eventHandler1() {
        return (Event event, long sequence, boolean endOfBatch) -> {
            LOGGER.info("eventHandler1: event={}, sequence={}, endOfBatch={}", event, sequence, endOfBatch);
        };
    }

    @Bean
    public EventHandler<Event> eventHandler2() {
        return (Event event, long sequence, boolean endOfBatch) -> {
            LOGGER.info("eventHandler2: event={}, sequence={}, endOfBatch={}", event, sequence, endOfBatch);
        };
    }

    @Bean
    public EventHandler<Event> closeEventHandler() {
        return (Event event, long sequence, boolean endOfBatch) -> {
            LOGGER.info("closeEventHandler: event={}, sequence={}, endOfBatch={}", event, sequence, endOfBatch);
            event.clear();
        };
    }
}
