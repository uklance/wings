package com.sample.webserver.model;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.util.LinkedHashMap;
import java.util.Map;

@ToString
@EqualsAndHashCode
public class Event {
    private Map<String, Object> headers = new LinkedHashMap<>();
    private Object payload;

    public <T> T getHeader(String name, Class<T> type) {
        return type.cast(headers.get(name));
    }

    public String getStringHeader(String name) {
        return (String) headers.get(name);
    }

    public <T> T getPayload(Class<T> type) {
        return type.cast(payload);
    }

    public void init(Map<String, Object> headers, Object payload) {
        this.headers.putAll(headers);
        this.payload = payload;
    }

    public void clear() {
        this.headers.clear();
        this.payload = null;
    }
}
