package com.sample.webserver.service;

import com.lmax.disruptor.dsl.Disruptor;
import com.sample.webserver.model.MutableEvent;
import com.sample.webserver.model.EventHeader;
import com.sample.webserver.model.Topic;
import lombok.AllArgsConstructor;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@AllArgsConstructor
public abstract class AbstractSubscriptionDelegate<T> implements SubscriptionDelegate {
    private final Class<T> entityType;
    private final Disruptor<MutableEvent> disruptor;

    @Override
    public String getEntity() {
        return entityType.getSimpleName();
    }

    @Override
    public void onEvent(MutableEvent event, long sequence) {
        Topic topic = event.getTopic();
        if (Topic.EVENT_SUBSCRIBE.equals(topic.getEvent())) {
            List<T> snapshot = getSnapshot(event);
            Map<String, String> outHeaders = new LinkedHashMap<>(event.getHeaders());
            String outTopic = topic.getEntity() + ":" + Topic.EVENT_SNAPSHOT;
            outHeaders.put(EventHeader.TOPIC, outTopic);
            disruptor.publishEvent((outEvent, outSequence) -> outEvent.init(outHeaders, snapshot));
        } else if (Topic.EVENT_ENTRY.equals(topic.getEvent())) {
            onEntry(event);
        }
    }

    protected abstract List<T> getSnapshot(MutableEvent event);

    protected abstract void onEntry(MutableEvent event);
}
