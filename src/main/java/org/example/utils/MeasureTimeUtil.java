package org.example.utils;

import lombok.extern.slf4j.Slf4j;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.function.Supplier;

@Slf4j
public class MeasureTimeUtil {


    public static <T> T measureAndPrintTime(Supplier<T> func, String funcName) {
        Instant start = Instant.now();
        T result = func.get();
        Instant finish = Instant.now();
        System.out.printf("Method %s took %d ms\n", funcName, ChronoUnit.MILLIS.between(start, finish));

        return result;
    }

    public static <T> long measureTime(Supplier<T> func) {
        Instant startFirst = Instant.now();
        T result = func.get();
        Instant finish = Instant.now();

        return ChronoUnit.MILLIS.between(startFirst, finish);
    }

}
