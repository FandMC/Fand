package io.fand.api.entity;

import java.util.Optional;

/**
 * Lingering area effect cloud entity.
 */
public interface AreaEffectCloud extends Entity {

    double radius();

    void setRadius(double radius);

    int duration();

    void setDuration(int ticks);

    int waitTime();

    void setWaitTime(int ticks);

    double radiusOnUse();

    void setRadiusOnUse(double radius);

    double radiusPerTick();

    void setRadiusPerTick(double radius);

    int durationOnUse();

    void setDurationOnUse(int ticks);

    boolean waiting();

    Optional<? extends LivingEntity> owner();

    void setOwner(LivingEntity owner);
}
