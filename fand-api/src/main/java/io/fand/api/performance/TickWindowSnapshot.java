package io.fand.api.performance;

/**
 * Performance data collected for a single rolling tick window.
 *
 * @param window represented rolling window
 * @param ticksPerSecond TPS statistics for the window
 * @param tickIntervalMilliseconds start-to-start tick interval statistics
 * @param millisecondsPerTick tick-work duration statistics
 * @param utilization average tick-work time divided by the target 50ms tick budget
 * @param sampleCount number of ticks observed in this window
 */
public record TickWindowSnapshot(
        TickWindow window,
        MetricStatistics ticksPerSecond,
        MetricStatistics tickIntervalMilliseconds,
        MetricStatistics millisecondsPerTick,
        double utilization,
        int sampleCount
) {
}
