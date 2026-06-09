package io.fand.api.scoreboard;

/**
 * Vanilla score render modes.
 */
public enum ScoreRenderType {
    INTEGER("integer"),
    HEARTS("hearts");

    private final String id;

    ScoreRenderType(String id) {
        this.id = id;
    }

    public String id() {
        return id;
    }
}
