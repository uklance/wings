package com.sample.webserver.disruptor;

import com.lmax.disruptor.EventHandler;
import com.sample.webserver.model.MutableEvent;
import com.sample.webserver.model.Topic;
import com.sample.webserver.service.SubscriptionDelegate;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toMap;

@Component
public class SubscriptionEventHandler implements EventHandler<MutableEvent> {
    private final Map<String, SubscriptionDelegate> delegatesByEntity;

    public SubscriptionEventHandler(List<SubscriptionDelegate> delegates) {
        this.delegatesByEntity = delegates.stream().collect(toMap(SubscriptionDelegate::getEntity, identity()));
    }

    @Override
    public void onEvent(MutableEvent event, long sequence, boolean endOfBatch) throws Exception {
        Topic topic = event.getTopic();
        SubscriptionDelegate delegate = delegatesByEntity.get(topic.getEntity());
        if (delegate != null) {
            delegate.onEvent(event, sequence);
        }
    }
}
