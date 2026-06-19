package io.fand.api.map;

public enum MapScale {
    CLOSEST(0),
    CLOSE(1),
    NORMAL(2),
    FAR(3),
    FARTHEST(4);

    private final int value;

    MapScale(int value) {
        this.value = value;
    }

    public int value() {
        return value;
    }

    public static MapScale of(int value) {
        return switch (value) {
            case 0 -> CLOSEST;
            case 1 -> CLOSE;
            case 2 -> NORMAL;
            case 3 -> FAR;
            case 4 -> FARTHEST;
            default -> throw new IllegalArgumentException("Map scale must be between 0 and 4: " + value);
        };
    }
}
