package io.fand.api.player;

/**
 * Vanilla operator command permission levels.
 */
public enum OperatorLevel {
    ALL(0),
    MODERATORS(1),
    GAMEMASTERS(2),
    ADMINS(3),
    OWNERS(4);

    private final int id;

    OperatorLevel(int id) {
        this.id = id;
    }

    public int id() {
        return id;
    }

    public static OperatorLevel byId(int id) {
        if (id <= 0) return ALL;
        if (id == 1) return MODERATORS;
        if (id == 2) return GAMEMASTERS;
        if (id == 3) return ADMINS;
        return OWNERS;
    }
}
