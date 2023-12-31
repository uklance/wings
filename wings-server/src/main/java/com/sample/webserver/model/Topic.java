package com.sample.webserver.model;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public class Topic {
    public static String EVENT_SUBSCRIBE = "subscribe";
    public static String EVENT_UNSUBSCRIBE = "unsubscribe";
    public static String EVENT_ENTRY = "entry";
    public static String EVENT_SNAPSHOT = "snapshot";

    public static final String WEBSOCKET_CONNECT = "Websocket:connect";
    public static final String WEBSOCKET_CLOSE = "Websocket:close";

    private String topic;
    private String entity;
    private String event;

    public static Topic parse(String topicString) {
        int colonIndex = topicString.indexOf(':');
        if (colonIndex < 0) throw new RuntimeException("Invalid topic " + topicString);

        String entity = topicString.substring(0, colonIndex);
        String event = topicString.substring(colonIndex + 1);
        return new Topic(topicString, entity, event);
    }

    public String toString() {
        return topic;
    }
}
