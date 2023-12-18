package com.sample.webserver.service;

import com.lmax.disruptor.dsl.Disruptor;
import com.sample.webserver.model.Car;
import com.sample.webserver.model.Event;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class CarSubscriptionDelegate extends AbstractSubscriptionDelegate<Car> {
    private static final Comparator<Car> COMPARATOR = Comparator
            .comparing(Car::getMake)
            .thenComparing(Car::getModel)
            .thenComparing(Car::getTrim)
            .thenComparing(Car::getYear);

    public CarSubscriptionDelegate(Disruptor<Event> disruptor) {
        super(Car.class, disruptor);
    }

    private final Map<Long, Car> carsById = new ConcurrentHashMap<>();

    @Override
    protected List<Car> getSnapshot(Event event) {
        List<Car> snapshot = new ArrayList<>(carsById.values());
        Collections.sort(snapshot, COMPARATOR);
        return snapshot;
    }

    @Override
    protected void onEntry(Event event) {
        Car car = event.getPayload(Car.class);
        carsById.put(car.getCarId(), car);
    }
}
