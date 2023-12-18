package com.sample.webserver.model;

import com.fasterxml.jackson.annotation.JsonIncludeProperties;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

import java.util.Map;

@ToString
@EqualsAndHashCode
@JsonIncludeProperties({"headers", "payload"})
@Getter
public class Event {
    private Map<String, String> headers;
    private Object payload;

    public void init(Map<String, String> headers, Object payload) {
        this.headers = headers;
        this.payload = payload;
    }

    public String getHeader(String name) {
        return headers.get(name);
    }

    public String getRequiredHeader(String name) {
        String value = headers.get(name);
        if (value == null) {
            throw new RuntimeException(String.format("header '%s' not provided", name));
        }
        return value;
    }

    public Topic getTopic() {
        return Topic.parse(getRequiredHeader(EventHeader.TOPIC));
    }

    public <T> T getPayload(Class<T> type) {
        return type.cast(payload);
    }

    public void clear() {
        this.headers = null;
        this.payload = null;
    }
}
