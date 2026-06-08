package io.fand.api.entity;

import java.util.Optional;
import org.jspecify.annotations.Nullable;

/**
 * A living entity with AI and targeting state.
 */
public interface Mob extends LivingEntity {

    Optional<? extends LivingEntity> target();

    void setTarget(@Nullable LivingEntity target);

    boolean noAi();

    void setNoAi(boolean noAi);

    boolean aggressive();

    void setAggressive(boolean aggressive);

    boolean persistent();

    void setPersistent();

    boolean canPickUpLoot();

    void setCanPickUpLoot(boolean canPickUpLoot);

    boolean leftHanded();

    void setLeftHanded(boolean leftHanded);
}
