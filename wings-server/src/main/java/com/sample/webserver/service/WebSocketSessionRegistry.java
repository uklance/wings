package com.sample.webserver.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.socket.WebSocketSession;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Component
public class WebSocketSessionRegistry {
    private static final Logger LOGGER = LoggerFactory.getLogger(WebSocketSessionRegistry.class);

    private final ConcurrentMap<String, WebSocketSession> sessionMap = new ConcurrentHashMap<>();

    public void add(WebSocketSession session) {
        LOGGER.info("Adding session {} to registry", session.getId());
        sessionMap.put(session.getId(), session);
    }

    public void remove(WebSocketSession session) {
        LOGGER.info("Removing session {} from registry", session.getId());
        sessionMap.remove(session.getId());
    }
}
