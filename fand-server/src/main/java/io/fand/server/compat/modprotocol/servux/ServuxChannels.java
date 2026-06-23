package io.fand.server.compat.modprotocol.servux;

import net.kyori.adventure.key.Key;

final class ServuxChannels {

    static final Key HUD = Key.key("servux:hud_metadata");
    static final Key ENTITIES = Key.key("servux:entity_data");
    static final Key STRUCTURES = Key.key("servux:structures");
    static final Key LITEMATICA = Key.key("servux:litematics");
    static final Key TWEAKS = Key.key("servux:tweaks");

    private ServuxChannels() {
    }
}
