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
        sessionMap.put(session.getId(), session);
        LOGGER.info("Added session {} to registry (count = {})", session.getId(), sessionMap.size());
    }

    public void remove(WebSocketSession session) {
        sessionMap.remove(session.getId());
        LOGGER.info("Removed session {} from registry (count = {})", session.getId(), sessionMap.size());
    }
}
