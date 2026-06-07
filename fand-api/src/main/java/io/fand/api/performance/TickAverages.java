package io.fand.api.performance;

/**
 * Three standard rolling tick-performance windows.
 */
public record TickAverages(double oneMinute, double fiveMinutes, double fifteenMinutes) {
}
