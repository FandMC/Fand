package io.fand.api.entity;

import java.util.Optional;
import org.jspecify.annotations.Nullable;

/**
 * An animal with vanilla tame/owner state.
 */
public interface Tameable extends Animal {

    /** Whether this animal is tamed. */
    boolean tamed();

    /** Sets tamed state. Marshals to the server thread. */
    void setTamed(boolean tamed);

    /** Current owner, if loaded and known. */
    Optional<? extends LivingEntity> owner();

    /** Sets or clears the owner reference. Marshals to the server thread. */
    void setOwner(@Nullable LivingEntity owner);

    /** Whether this animal is in a sitting pose. */
    boolean sitting();

    /** Sets sitting pose. Marshals to the server thread. */
    void setSitting(boolean sitting);
}
