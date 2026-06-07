package io.fand.api.world;

import net.kyori.adventure.key.Key;

/**
 * Vanilla generation templates used when creating a dynamic world.
 *
 * <p>The template selects the dimension type and chunk generator copied from
 * the server's loaded vanilla dimension registry. Dynamic worlds created from
 * these templates are stored under their own dimension key inside the active
 * save and share the server's global game time, matching vanilla non-overworld
 * dimensions.
 */
public enum WorldTemplate {
    OVERWORLD("minecraft:overworld"),
    NETHER("minecraft:the_nether"),
    END("minecraft:the_end");

    private final Key key;

    WorldTemplate(String key) {
        this.key = Key.key(key);
    }

    public Key key() {
        return key;
    }
}
