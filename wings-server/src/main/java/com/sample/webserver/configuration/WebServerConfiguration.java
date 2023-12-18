package com.sample.webserver.configuration;

import com.lmax.disruptor.EventHandler;
import com.lmax.disruptor.dsl.Disruptor;
import com.lmax.disruptor.util.DaemonThreadFactory;
import com.sample.webserver.model.Event;
import com.sample.webserver.service.DefaultWebSocketHandler;
import com.sample.webserver.service.DummyEventPublisher;
import com.sample.webserver.service.EventFilePoller;
import com.sample.webserver.disruptor.OutboundWebsocketEventHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

@Configuration
public class WebServerConfiguration {
    private static final Logger LOGGER = LoggerFactory.getLogger(WebServerConfiguration.class);

    @Bean
    public Disruptor<Event> disruptor(
            @Value("${disruptor.bufferSize}") int bufferSize,
            OutboundWebsocketEventHandler outboundWsEventHandler
    ) {
        EventHandler<Event> closeEventHandler = (event, sequence, endOfBatch) -> {
            LOGGER.info("closeEventHandler: event={}, sequence={}, endOfBatch={}", event, sequence, endOfBatch);
            event.clear();
        };
        Disruptor<Event> disruptor = new Disruptor<>(Event::new, bufferSize, DaemonThreadFactory.INSTANCE);
        disruptor.handleEventsWith(outboundWsEventHandler).then(closeEventHandler);
        return disruptor;
    }

    @Bean
    public HandlerMapping webSocketHandlerMapping(DefaultWebSocketHandler handler) {
        Map<String, WebSocketHandler> handlerMap = Map.of("/websocket", handler);
        return new SimpleUrlHandlerMapping(handlerMap, 1);
    }

    @Bean
    public ApplicationListener<ContextRefreshedEvent> refreshListener(
            @Value("${disruptor.start}") boolean startDisruptor,
            Disruptor<Event> disruptor,
            @Value("${eventFilePoller.start}") boolean startFilePoller,
            EventFilePoller filePoller,
            @Value("${dummyEventPublisher.start}") boolean startDummyPublisher,
            DummyEventPublisher dummyPublisher

    ) {
        return (ContextRefreshedEvent event) -> {
            if (startDisruptor) {
                LOGGER.info("Starting the disruptor");
                disruptor.start();
            }
            if (startFilePoller) {
                LOGGER.info("Starting the file poller");
                filePoller.start();
            }
            if (startDisruptor) {
                LOGGER.info("Starting the dummy event publisher");
                dummyPublisher.start();
            }
        };
    }

    @Bean
    public ApplicationListener<ContextClosedEvent> closeListener(
            Disruptor<Event> disruptor,
            EventFilePoller filePoller,
            DummyEventPublisher dummyPublisher) {
        return (ContextClosedEvent event) -> {
            if (filePoller.isRunning()) {
                LOGGER.info("Stopping the file poller");
                filePoller.stop();
            }
            if (dummyPublisher.isRunning()) {
                LOGGER.info("Stopping the dummy event publisher");
                dummyPublisher.stop();
            }
            if (disruptor.hasStarted()) {
                LOGGER.info("Shutting down the disruptor");
                disruptor.shutdown();
            }
        };
    }

    @Bean
    public ScheduledExecutorService scheduledExecutorService() {
        return Executors.newSingleThreadScheduledExecutor();
    }
}
