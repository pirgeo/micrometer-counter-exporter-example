package org.pirgeo.example;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Tags;

import java.time.Duration;

public class MicrometerStandalone {
    private static final String CREATED_AT_KEY = "created_at";
    private static final String INCREMENTED_AT_KEY = "incremented_at";

    public static void main(String[] args) throws InterruptedException {
        // The time between subsequent exports
        Duration exportTimeout = Duration.ofSeconds(5);

        // Create a registry that only prints counters to the console
        // Happens every 'exportTimeout' time units (e.g. every 5 seconds)
        MyLogCounterRegistry registry = new MyLogCounterRegistry(exportTimeout);

        // create counters that exist for more than one export interval
        // counter1 increments immediately (in the first interval)
        Counter count1 = registry.counter("count1", Tags.of(CREATED_AT_KEY, "0", INCREMENTED_AT_KEY, "0"));
        count1.increment();

        // counter2 is created in the first interval, but only incremented in the second interval
        Counter count2 = registry.counter("count2", Tags.of(CREATED_AT_KEY, "0", INCREMENTED_AT_KEY, "1"));

        // counter3 is created in the first interval, but only incremented in the third interval
        Counter count3 = registry.counter("count3", Tags.of(CREATED_AT_KEY, "0", INCREMENTED_AT_KEY, "2"));

        // wait for one export interval
        Thread.sleep(exportTimeout.toMillis());

        // increase some counters that live longer
        count1.increment();
        count2.increment();

        // wait for another export interval to be over
        Thread.sleep(exportTimeout.toMillis());

        // Create a counter that only lives for part of an interval
        Counter count4 = registry.counter("count4", Tags.of(CREATED_AT_KEY, "2", INCREMENTED_AT_KEY, "2"));

        // increment all counters
        count1.increment();
        count2.increment();
        // these two are only incremented in the last interval
        count3.increment();
        count4.increment();

        // close the registry and force an export for all of them
        registry.close();
    }
}