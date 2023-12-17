package com.sample.webserver.model;

import com.fasterxml.jackson.annotation.JsonIncludeProperties;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

@ToString
@EqualsAndHashCode
@JsonIncludeProperties({"headers", "payload"})
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

    public String getTopic() {
        return getHeader(EventHeader.TOPIC);
    }

    public Set<String> getHeaderNames() {
        return Collections.unmodifiableSet(headers.keySet());
    }

    public <T> T getPayload(Class<T> type) {
        return type.cast(payload);
    }

    public void clear() {
        this.headers = null;
        this.payload = null;
    }

    public Map<String, String> getHeaders() {
        return headers;
    }

    public Object getPayload() {
        return payload;
    }
}
