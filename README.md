# micrometer-counter-exporter-example

After using Micrometer for a while, a little quirk surfaced. This example is to reproduce the problem, and is intended
to serve as a base for troubleshooting it.

## The problem

When running Micrometer (independently of whether it is running with Spring Boot or in standalone mode), Counters
incremented for the first time shortly before application exit are exported with an unexpected value.
Let's assume Micrometer is running in an AWS Lambda function, which are naturally short-lived environments.
Let's also assume that you want to count the number of successful runs of your function by incrementing a counter
every time your app reaches the end of its life.
Shortly after the counter is incremented, the function exits.

## The sample

The sample contains a custom `StepMeterRegistry`, which, instead of actually exporting, simply logs whenever a counter
would be exported.
The `MyLogCounterRegistry` also contains a `MyLogCounter` which does exactly what the `StepCounter` does, but also logs
when its created or incremented.

There are four Counters in the sample, created and incremented at different times in the application lifecycle.
Depending on when they are created and incremented, they are exported at different times.

## The output

When running the MicrometerStandalone sample, this output is generated (annotations added manually):

```text
[main] INFO io.micrometer.core.instrument.push.PushMeterRegistry - publishing metrics for MyLogCounterRegistry every 5s
# Count 1, 3, and 3 are created at the beginning of the app, and count 1 is incremented immediately
[main] INFO org.pirgeo.example.MyLogCounterRegistry - count1 created
[main] INFO org.pirgeo.example.MyLogCounterRegistry - count1 incremented by 1.0
[main] INFO org.pirgeo.example.MyLogCounterRegistry - count2 created
[main] INFO org.pirgeo.example.MyLogCounterRegistry - count3 created

# The main thread sleeps for the duration of the export interval, thus the first export cycle will have rolled around
[main] INFO org.pirgeo.example.MicrometerStandalone - sleeping for 5 seconds
[my-counter-logs-publisher] INFO org.pirgeo.example.MyLogCounterRegistry - Export #0

# Note that count 1 is immediately exported after being incremented in the first export cycle
[my-counter-logs-publisher] INFO org.pirgeo.example.MyLogCounterRegistry - count1 | created_at=0,incremented_at=0 | 1.00
[my-counter-logs-publisher] INFO org.pirgeo.example.MyLogCounterRegistry - count2 | created_at=0,incremented_at=1 | 0.00
[my-counter-logs-publisher] INFO org.pirgeo.example.MyLogCounterRegistry - count3 | created_at=0,incremented_at=2 | 0.00
[main] INFO org.pirgeo.example.MicrometerStandalone - sleep over, continuing main thread
[main] INFO org.pirgeo.example.MyLogCounterRegistry - count1 incremented by 1.0
[main] INFO org.pirgeo.example.MyLogCounterRegistry - count2 incremented by 1.0
[main] INFO org.pirgeo.example.MicrometerStandalone - sleeping for 5 seconds
[my-counter-logs-publisher] INFO org.pirgeo.example.MyLogCounterRegistry - Export #1

# Again, the two counters that have been incremented in the last interval are exported immediately
[my-counter-logs-publisher] INFO org.pirgeo.example.MyLogCounterRegistry - count1 | created_at=0,incremented_at=0 | 1.00
[my-counter-logs-publisher] INFO org.pirgeo.example.MyLogCounterRegistry - count2 | created_at=0,incremented_at=1 | 1.00
[my-counter-logs-publisher] INFO org.pirgeo.example.MyLogCounterRegistry - count3 | created_at=0,incremented_at=2 | 0.00
[main] INFO org.pirgeo.example.MicrometerStandalone - sleep over, continuing main thread

# This is the last export interval, it will *not* run until the export interval rolls around (there is no sleep here)
[main] INFO org.pirgeo.example.MyLogCounterRegistry - count4 created
# Increment by 2 here, to distinguish in the export
[main] INFO org.pirgeo.example.MyLogCounterRegistry - count0 incremented by 2.0
[main] INFO org.pirgeo.example.MyLogCounterRegistry - count1 incremented by 2.0
[main] INFO org.pirgeo.example.MyLogCounterRegistry - count2 incremented by 2.0
[main] INFO org.pirgeo.example.MyLogCounterRegistry - count3 incremented by 2.0

# On shutdown, all counters are exported once more
[main] INFO org.pirgeo.example.MyLogCounterRegistry - Shutdown request received, exporting once more, then shutting down...
[main] INFO org.pirgeo.example.MyLogCounterRegistry - Export #2

# In the last export interval, the results from the previous interval are exported, which is unexpected.
[main] INFO org.pirgeo.example.MyLogCounterRegistry - count1 | created_at=0,incremented_at=0 | 1.00
[main] INFO org.pirgeo.example.MyLogCounterRegistry - count2 | created_at=0,incremented_at=1 | 1.00
[main] INFO org.pirgeo.example.MyLogCounterRegistry - count3 | created_at=0,incremented_at=2 | 0.00
[main] INFO org.pirgeo.example.MyLogCounterRegistry - count4 | created_at=2,incremented_at=2 | 0.00
```

## Expectations

The expectation is that on shutdown, the change since the last export would be exported.

