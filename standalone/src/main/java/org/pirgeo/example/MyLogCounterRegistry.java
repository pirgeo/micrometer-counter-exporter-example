package org.pirgeo.example;

import io.micrometer.core.instrument.Clock;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.step.StepCounter;
import io.micrometer.core.instrument.step.StepMeterRegistry;
import io.micrometer.core.instrument.step.StepRegistryConfig;
import io.micrometer.core.instrument.util.NamedThreadFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.Comparator;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

public class MyLogCounterRegistry extends StepMeterRegistry {
    private static final ThreadFactory DEFAULT_THREAD_FACTORY = new NamedThreadFactory("my-counter-logs-publisher");

    private final Logger logger;

    private final StepRegistryConfig config;

    private final AtomicLong exportNumber = new AtomicLong(0);

    public MyLogCounterRegistry(Duration step) {
        this(
                new StepRegistryConfig() {
                    @Override
                    public String prefix() {
                        return null;
                    }

                    @Override
                    public String get(String key) {
                        return null;
                    }

                    @Override
                    public Duration step() {
                        return step;
                    }
                },
                LoggerFactory.getLogger(MyLogCounterRegistry.class)
        );
    }

    public MyLogCounterRegistry(StepRegistryConfig config, Logger logger) {
        super(config, Clock.SYSTEM);

        this.config = config;
        this.logger = logger;

        start(DEFAULT_THREAD_FACTORY);
    }

    @Override
    protected void publish() {
        logger.info("Export #{}", exportNumber.get());
        getMeters().stream()
                // sort by name
                .sorted(Comparator.comparing(m -> m.getId().getName()))
                .forEach(m ->
                        m.use(
                                gauge -> {
                                },
                                // only counters are exported, everything else is ignored.
                                counter -> logger.info(formatCounter(counter)),
                                timer -> {
                                },
                                summary -> {
                                },
                                longTaskTimer -> {
                                },
                                timeGauge -> {
                                },
                                counter -> {
                                },
                                timer -> {
                                },
                                meter -> {
                                }
                        )
                );
        exportNumber.incrementAndGet();
    }

    /**
     * MyLogCounter just calls the Methods on StepCounter, but also logs what is happening
     */
    class MyLogCounter extends StepCounter {
        public MyLogCounter(Id id, Clock clock, long stepMillis) {
            super(id, clock, stepMillis);
        }

        @Override
        public void increment(double amount) {
            logger.info("{} incremented by {}", this.getId().getName(), amount);
            super.increment(amount);
        }
    }

    @Override
    protected Counter newCounter(Meter.Id id) {
        logger.info("{} created", id.getName());
        // return a MyLogCounter. It does exactly the same as what the StepCounter does, but also logs it.
        return new MyLogCounter(id, this.clock, this.config.step().toMillis());
    }

    private String formatCounter(Counter counter) {
        String tagsAsString = counter.getId().getTags().stream()
                .sorted(Comparator.comparing(Tag::getKey))
                .map(t ->
                        String.format("%s=%s", t.getKey(), t.getValue())
                )
                .collect(
                        Collectors.joining(",")
                );
        return String.format("%s | %s | %2.2f", counter.getId().getName(), tagsAsString, counter.count());
    }

    @Override
    public void close() {
        logger.info("Shutdown request received, exporting once more, then shutting down...");
        super.close();
    }

    @Override
    protected TimeUnit getBaseTimeUnit() {
        return TimeUnit.MILLISECONDS;
    }
}
