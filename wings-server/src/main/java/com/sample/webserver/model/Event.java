package com.sample.webserver.model;

import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

@ToString
@EqualsAndHashCode
public class Event {
    private Map<String, Object> headers;
    private Object payload;

    public void init(Map<String, Object> headers, Object payload) {
        this.headers = headers;
        this.payload = payload;
    }

    public <T> T getHeader(String name, Class<T> type) {
        return type.cast(headers.get(name));
    }

    public String getStringHeader(String name) {
        return (String) headers.get(name);
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
}
