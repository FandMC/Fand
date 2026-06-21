package io.fand.api.block;

import net.kyori.adventure.key.Key;

/** A Minecraft fluid type identified by its registry key. */
public interface FluidType {

    Key key();

    default boolean empty() {
        return key().asString().equals("minecraft:empty");
    }

    default boolean water() {
        var value = key().asString();
        return value.equals("minecraft:water") || value.equals("minecraft:flowing_water");
    }

    default boolean lava() {
        var value = key().asString();
        return value.equals("minecraft:lava") || value.equals("minecraft:flowing_lava");
    }

    default boolean source() {
        var value = key().asString();
        return value.equals("minecraft:water") || value.equals("minecraft:lava");
    }

    default boolean flowing() {
        return !empty() && !source();
    }
}
