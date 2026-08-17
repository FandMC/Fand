package io.fand.api.entity;

/** Parched combat characteristics. */
public interface Parched extends Mob {

    default boolean weaknessImmune() {
        return true;
    }

    default int weaknessArrowDurationTicks() {
        return 600;
    }
}
