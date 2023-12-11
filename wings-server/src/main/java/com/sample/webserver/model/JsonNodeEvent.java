package com.sample.webserver.model;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Map;

@AllArgsConstructor
@Getter
public class JsonNodeEvent {
    private Map<String, String> headers;
    private JsonNode payload;
}
