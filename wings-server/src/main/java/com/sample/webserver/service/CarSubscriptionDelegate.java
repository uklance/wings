package com.sample.webserver.service;

import com.lmax.disruptor.dsl.Disruptor;
import com.sample.webserver.model.Car;
import com.sample.webserver.model.MutableEvent;
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

    public CarSubscriptionDelegate(Disruptor<MutableEvent> disruptor) {
        super(Car.class, disruptor);
    }

    private final Map<Long, Car> carsById = new ConcurrentHashMap<>();

    @Override
    protected List<Car> getSnapshot(MutableEvent event) {
        List<Car> snapshot = new ArrayList<>(carsById.values());
        Collections.sort(snapshot, COMPARATOR);
        return snapshot;
    }

    @Override
    protected void onEntry(MutableEvent event) {
        Car car = event.getPayload(Car.class);
        carsById.put(car.getCarId(), car);
    }
}
