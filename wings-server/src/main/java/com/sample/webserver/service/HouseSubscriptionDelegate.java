package com.sample.webserver.service;

import com.lmax.disruptor.dsl.Disruptor;
import com.sample.webserver.model.Event;
import com.sample.webserver.model.House;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class HouseSubscriptionDelegate extends AbstractSubscriptionDelegate<House> {
    private static final Comparator<House> COMPARATOR = Comparator.comparing(House::getAddress);

    public HouseSubscriptionDelegate(Disruptor<Event> disruptor) {
        super(House.class, disruptor);
    }

    private final Map<Long, House> housesById = new ConcurrentHashMap<>();

    @Override
    protected List<House> getSnapshot(Event event) {
        List<House> snapshot = new ArrayList<>(housesById.values());
        Collections.sort(snapshot, COMPARATOR);
        return snapshot;
    }

    @Override
    protected void onEntry(Event event) {
        House house = event.getPayload(House.class);
        housesById.put(house.getHouseId(), house);
    }
}
