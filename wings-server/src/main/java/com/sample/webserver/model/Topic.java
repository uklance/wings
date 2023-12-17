package com.sample.webserver.model;

public interface Topic {
    String WEBSOCKET_INIT = "Websocket:init";
    String TRADE_SUBSCRIBE = "Trade:subscribe";
    String TRADE_UNSUBSCRIBE = "Trade:unsubscribe";
    String TRADE_SNAPSHOT = "Trade:snapshot";
    String TRADE_ENTRY = "Trade:entry";
}
