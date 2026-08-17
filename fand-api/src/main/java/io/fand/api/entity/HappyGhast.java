package io.fand.api.entity;

/** Happy Ghast harness and stable-flight state. */
public interface HappyGhast extends Animal {

    boolean leashHolder();
    boolean staysStill();
    boolean onStillTimeout();
    void setStillTimeoutTicks(int ticks);
}
