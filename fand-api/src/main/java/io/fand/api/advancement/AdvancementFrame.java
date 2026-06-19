package io.fand.api.advancement;

public enum AdvancementFrame {
    TASK("task"),
    CHALLENGE("challenge"),
    GOAL("goal");

    private final String serializedName;

    AdvancementFrame(String serializedName) {
        this.serializedName = serializedName;
    }

    public String serializedName() {
        return serializedName;
    }
}
