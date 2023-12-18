package com.sample.webserver.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

@AllArgsConstructor
@Getter
@Builder
@ToString
public class Car {
    private Long carId;
    private String make;
    private String model;
    private String trim;
    private Integer year;
}
