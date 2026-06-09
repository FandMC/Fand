package io.fand.api.entity;

import java.util.Optional;

/**
 * Primed explosive entity.
 */
public interface Explosive extends Entity {

    int fuseTicks();

    void setFuseTicks(int ticks);

    Optional<? extends LivingEntity> owner();
}
