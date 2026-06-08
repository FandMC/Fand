package io.fand.api.entity;

/**
 * A mob with vanilla age state.
 */
public interface Ageable extends Mob {

    /** Vanilla age in ticks. Negative values are baby age, {@code 0} is adult. */
    int age();

    /** Sets vanilla age in ticks. Marshals to the server thread. */
    void setAge(int age);

    /** Whether this entity is currently a baby. */
    boolean baby();

    /** Sets baby/adult state using vanilla age values. Marshals to the server thread. */
    void setBaby(boolean baby);
}
