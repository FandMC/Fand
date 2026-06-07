package io.fand.api.performance;

/**
 * Summary statistics for a measured server metric.
 *
 * @param average arithmetic average, except TPS where it is ticks divided by elapsed interval
 * @param minimum lowest observed value in the window
 * @param maximum highest observed value in the window
 * @param median median observed value in the window
 */
public record MetricStatistics(double average, double minimum, double maximum, double median) {
}
