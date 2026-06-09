package io.fand.api.entity;

import java.util.Optional;
import org.jspecify.annotations.Nullable;

/**
 * Primed explosive entity.
 */
public interface Explosive extends Entity {

    int fuseTicks();

    void setFuseTicks(int ticks);

    Optional<? extends LivingEntity> owner();

    void setOwner(@Nullable LivingEntity owner);
}
