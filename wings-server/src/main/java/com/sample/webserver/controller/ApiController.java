package com.sample.webserver.controller;

import com.lmax.disruptor.dsl.Disruptor;
import com.sample.webserver.model.Event;
import com.sample.webserver.model.JsonNodeEvent;
import lombok.AllArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@AllArgsConstructor
public class ApiController {
    private static final Logger LOGGER = LoggerFactory.getLogger(ApiController.class);
    private final Disruptor<Event> disruptor;

    @PostMapping(value = "/api/event", produces = MediaType.APPLICATION_JSON_VALUE)
    public String event(@RequestBody JsonNodeEvent jsonEvent) {
        LOGGER.info("Event received " + jsonEvent);
        if (jsonEvent.getPayload() != null && !jsonEvent.getPayload().isNull()) {
            throw new IllegalStateException("TODO: support event payload " + jsonEvent.getPayload());
        }
        disruptor.publishEvent((event, sequence) -> event.init(jsonEvent.getHeaders(), null));
        return null;
    }
}
