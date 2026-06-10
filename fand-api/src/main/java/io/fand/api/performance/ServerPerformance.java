package io.fand.api.performance;

/**
 * Current server tick performance snapshot.
 *
 * @param oneSecond 1 second rolling window
 * @param fiveSeconds 5 second rolling window
 * @param tenSeconds 10 second rolling window
 * @param fifteenSeconds 15 second rolling window
 * @param oneMinute 1 minute rolling window
 * @param fiveMinutes 5 minute rolling window
 * @param fifteenMinutes 15 minute rolling window
 * @param tickCount number of measured server ticks
 * @param chunkTrackingQueueDepth pending chunk tracking diff jobs
 */
public record ServerPerformance(
        TickWindowSnapshot oneSecond,
        TickWindowSnapshot fiveSeconds,
        TickWindowSnapshot tenSeconds,
        TickWindowSnapshot fifteenSeconds,
        TickWindowSnapshot oneMinute,
        TickWindowSnapshot fiveMinutes,
        TickWindowSnapshot fifteenMinutes,
        long tickCount,
        long chunkTrackingQueueDepth
) {
    public TickWindowSnapshot window(TickWindow window) {
        return switch (window) {
            case ONE_SECOND -> oneSecond;
            case FIVE_SECONDS -> fiveSeconds;
            case TEN_SECONDS -> tenSeconds;
            case FIFTEEN_SECONDS -> fifteenSeconds;
            case ONE_MINUTE -> oneMinute;
            case FIVE_MINUTES -> fiveMinutes;
            case FIFTEEN_MINUTES -> fifteenMinutes;
        };
    }

    /** Recent TPS averages in the traditional 1m, 5m, and 15m windows. */
    public TickAverages ticksPerSecond() {
        return new TickAverages(
                oneMinute.ticksPerSecond().average(),
                fiveMinutes.ticksPerSecond().average(),
                fifteenMinutes.ticksPerSecond().average());
    }

    /** Recent MSPT averages in the traditional 1m, 5m, and 15m windows. */
    public TickAverages millisecondsPerTick() {
        return new TickAverages(
                oneMinute.millisecondsPerTick().average(),
                fiveMinutes.millisecondsPerTick().average(),
                fifteenMinutes.millisecondsPerTick().average());
    }

    /** Average MSPT over the most recent short window. */
    public double currentMillisecondsPerTick() {
        return fiveSeconds.millisecondsPerTick().average();
    }
}
