package com.sample.webserver.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.ToString;

import java.util.Map;

@AllArgsConstructor
@Getter
@ToString
public class ImmutableEvent {
    private final Map<String, String> headers;
    private final Object payload;

    public ImmutableEvent(MutableEvent event) {
        this(event.getHeaders(), event.getPayload());
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
}
