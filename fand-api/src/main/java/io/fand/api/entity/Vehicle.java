package io.fand.api.entity;

/**
 * Minecart, boat, and similar rideable vehicle entity.
 */
public interface Vehicle extends Entity {

    double damage();

    void setDamage(double damage);

    int hurtTime();

    void setHurtTime(int ticks);

    int hurtDirection();

    void setHurtDirection(int direction);
}
