package com.sample.webserver.service;

import com.sample.webserver.model.MutableEvent;

public interface SubscriptionDelegate {
    String getEntity();
    void onEvent(MutableEvent event, long sequence);
}
