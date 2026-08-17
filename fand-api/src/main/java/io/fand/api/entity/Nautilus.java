package io.fand.api.entity;

import io.fand.api.inventory.Inventory;

/** Shared controls for living and zombie Nautilus mounts. */
public interface Nautilus extends Tameable {

    boolean dashing();
    void setDashing(boolean dashing);
    int dashCooldownTicks();
    void setDashCooldownTicks(int ticks);
    Inventory inventory();
}
