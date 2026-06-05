package io.fand.api.entity;

/**
 * The set of vanilla game modes a {@link Player} can be in. Names match the
 * vanilla constants so {@link #valueOf(String)} round-trips cleanly.
 */
public enum GameMode {
    SURVIVAL,
    CREATIVE,
    ADVENTURE,
    SPECTATOR
}
