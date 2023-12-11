package com.sample.webserver.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lmax.disruptor.dsl.Disruptor;
import com.sample.webserver.model.Event;
import com.sample.webserver.model.JsonNodeEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.File;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

@Component
public class EventFilePoller {
    private static final Logger LOGGER = LoggerFactory.getLogger(EventFilePoller.class);
    private final ScheduledExecutorService executorService;
    private final Disruptor<Event> disruptor;
    private final ObjectMapper objectMapper;
    private final String dir;
    private final int periodMillis;
    private ScheduledFuture<?> future;

    public EventFilePoller(
            ScheduledExecutorService executorService,
            Disruptor<Event> disruptor,
            ObjectMapper objectMapper,
            @Value("${eventFilePoller.dir}") String dir,
            @Value("${eventFilePoller.periodMillis}") int periodMillis
    ) {
        this.executorService = executorService;
        this.disruptor = disruptor;
        this.objectMapper = objectMapper;
        this.dir = dir;
        this.periodMillis = periodMillis;
    }

    public synchronized void start() {
        File dirFile = new File(dir);
        dirFile.mkdirs();
        Runnable command = () -> {
            try {
                File[] files = dirFile.listFiles();
                if (files != null) {
                    for (File file : files) {
                        if (!file.isDirectory()) {
                            LOGGER.info("Processing " + file);
                            JsonNodeEvent jsonNodeEvent = objectMapper.readValue(file, JsonNodeEvent.class);
                            disruptor.publishEvent((event, sequence) -> event.init(jsonNodeEvent.getHeaders(), jsonNodeEvent.getPayload()));
                            if (!file.delete()) {
                                LOGGER.warn("Could not delete " + file);
                            }
                        }
                    }
                }
            } catch (Exception e) {
                LOGGER.error(e.getMessage(), e);
            }
        };
        this.future = executorService.scheduleAtFixedRate(command, 0, periodMillis, TimeUnit.MILLISECONDS);
    }

    public synchronized void stop() {
        if (this.future != null) {
            this.future.cancel(false);
            this.future = null;
        }
    }

    public synchronized boolean isRunning() {
        return future != null;
    }
}
