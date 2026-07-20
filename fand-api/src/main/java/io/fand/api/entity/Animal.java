package io.fand.api.entity;

/**
 * A vanilla animal entity.
 */
public interface Animal extends Ageable {

    /** Whether this adult animal can currently enter love mode. */
    boolean canBreed();

    /** Whether this animal is currently in love mode. */
    boolean inLove();

    /** Remaining love-mode duration in ticks. */
    int loveTicks();

    /** Sets the remaining love-mode duration in ticks. Marshals to the server thread. */
    void setLoveTicks(int ticks);
}
