package io.fand.api.entity;

/** Creeper-specific state and fuse controls. */
public interface Creeper extends Mob {

    boolean charged();

    void setCharged(boolean charged);

    boolean ignited();

    void ignite();

    int swellDirection();

    void setSwellDirection(int direction);

    double swelling();

    int fuseTicks();

    void setFuseTicks(int ticks);

    int explosionRadius();

    void setExplosionRadius(int radius);
}
