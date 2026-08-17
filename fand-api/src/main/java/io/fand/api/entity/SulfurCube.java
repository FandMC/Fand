package io.fand.api.entity;

/** Sulfur Cube fuse, size, and bucket state. */
public interface SulfurCube extends Ageable {

    int size();
    void setSize(int size, boolean updateHealth);
    boolean fromBucket();
    void setFromBucket(boolean fromBucket);
    int fuseTicks();
    boolean primed();
    boolean canExplode();
    boolean prime(boolean imminent);
    boolean bodyItemEquipped();
}
