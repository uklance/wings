package com.sample.webserver.service;

import com.lmax.disruptor.dsl.Disruptor;
import com.sample.webserver.model.Car;
import com.sample.webserver.model.MutableEvent;
import com.sample.webserver.model.EventHeader;
import com.sample.webserver.model.House;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

@Component
@RequiredArgsConstructor
public class DummyEventPublisher {
    private final Disruptor<MutableEvent> disruptor;
    private final ScheduledExecutorService executor;
    private ScheduledFuture<?> future;

    public synchronized void start() {
        AtomicLong nextId = new AtomicLong(0);
        Runnable command = () -> {
            long id = nextId.getAndIncrement();

            disruptor.publishEvent((event, sequence) -> {
                House house = House.builder()
                        .houseId(id)
                        .address(id + " Smith Street")
                        .build();
                Map<String, String> headers = Map.of(EventHeader.TOPIC, "House:entry");
                event.init(headers, house);
            });
            disruptor.publishEvent((event, sequence) -> {
                Car car = Car.builder()
                        .carId(id)
                        .make("BMW")
                        .model("M3")
                        .trim("Sports")
                        .year(2020 + (int) id)
                        .build();
                Map<String, String> headers = Map.of(EventHeader.TOPIC, "Car:entry");
                event.init(headers, car);
            });
        };
        this.future = executor.scheduleAtFixedRate(command, 0, 20, TimeUnit.SECONDS);
    }

    public synchronized void stop() {
        future.cancel(false);
        future = null;
    }

    public synchronized boolean isRunning() {
        return future != null;
    }
}
