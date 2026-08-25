package io.fand.server;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

final class ShutdownFailuresTest {

    @Test
    void runsEveryStepAndAggregatesFailures() {
        var steps = new AtomicInteger();
        var failures = new ShutdownFailures(LoggerFactory.getLogger(getClass()));
        var first = new IllegalStateException("first");
        var second = new IllegalArgumentException("second");

        failures.run("first step", () -> {
            steps.incrementAndGet();
            throw first;
        });
        failures.run("second step", () -> {
            steps.incrementAndGet();
            throw second;
        });
        failures.run("final step", steps::incrementAndGet);

        assertThat(steps).hasValue(3);
        assertThatThrownBy(failures::throwIfPresent)
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Fand runtime shutdown failed")
                .satisfies(failure -> assertThat(failure.getSuppressed()).containsExactly(first, second));
    }
}
