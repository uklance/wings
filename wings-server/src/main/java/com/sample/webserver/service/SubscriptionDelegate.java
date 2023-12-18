package com.sample.webserver.service;

import com.sample.webserver.model.Event;

public interface SubscriptionDelegate {
    String getEntity();
    void onEvent(Event event, long sequence);
}
