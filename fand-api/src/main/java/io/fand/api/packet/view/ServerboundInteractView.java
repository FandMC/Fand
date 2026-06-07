package io.fand.api.packet.view;

import io.fand.api.packet.PacketView;
import io.fand.api.packet.Vec3d;

/** Typed view of a player interacting with an entity (attack or right-click). Replaceable. */
public interface ServerboundInteractView extends PacketView {

    /** The id of the entity being interacted with. */
    default int entityId() {
        return require("entityId", Integer.class);
    }

    /** The hand used: {@code "MAIN_HAND"} or {@code "OFF_HAND"}. */
    default String hand() {
        return require("hand", String.class);
    }

    /** The precise interaction point relative to the entity, if present. */
    default Vec3d location() {
        return require("location", Vec3d.class);
    }

    /** Whether the player was sneaking (secondary action). */
    default boolean sneaking() {
        return require("usingSecondaryAction", Boolean.class);
    }
}
