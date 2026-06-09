package io.fand.api.entity;

import java.util.Optional;
import org.jspecify.annotations.Nullable;

/** Horse and horse-like entity controls. */
public interface Horse extends Animal {

    boolean tamed();

    void setTamed(boolean tamed);

    Optional<? extends LivingEntity> owner();

    void setOwner(@Nullable LivingEntity owner);

    int temper();

    void setTemper(int temper);

    int maxTemper();

    boolean bred();

    void setBred(boolean bred);

    boolean eating();

    void setEating(boolean eating);

    boolean standing();

    void stand();
}
