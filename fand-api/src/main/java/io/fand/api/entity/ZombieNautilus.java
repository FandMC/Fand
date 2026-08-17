package io.fand.api.entity;

import net.kyori.adventure.key.Key;

/** Zombie Nautilus variant controls. */
public interface ZombieNautilus extends Nautilus {

    Key variant();
    void setVariant(Key variant);
}
