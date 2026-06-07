package io.fand.api.performance;

/**
 * Standard rolling server-performance windows.
 */
public enum TickWindow {
    ONE_SECOND("1s", 20),
    FIVE_SECONDS("5s", 100),
    TEN_SECONDS("10s", 200),
    FIFTEEN_SECONDS("15s", 300),
    ONE_MINUTE("1m", 20 * 60),
    FIVE_MINUTES("5m", 20 * 60 * 5),
    FIFTEEN_MINUTES("15m", 20 * 60 * 15);

    private final String label;
    private final int ticks;

    TickWindow(String label, int ticks) {
        this.label = label;
        this.ticks = ticks;
    }

    public String label() {
        return label;
    }

    public int ticks() {
        return ticks;
    }
}
