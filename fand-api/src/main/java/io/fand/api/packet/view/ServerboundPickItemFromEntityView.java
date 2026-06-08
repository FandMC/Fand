package io.fand.api.packet.view;

import io.fand.api.packet.PacketView;
import java.util.UUID;

/** Typed view of {@link ServerboundPickItemFromEntityPacket}. */
public interface ServerboundPickItemFromEntityView extends PacketView {

    default int id() {
        return require("id", int.class);
    }
    default boolean includeData() {
        return require("includeData", boolean.class);
    }

    /** Returns a copy with {@code id} replaced. */
    default ServerboundPickItemFromEntityView withId(int id) {
        return (ServerboundPickItemFromEntityView) with("id", id);
    }
    /** Returns a copy with {@code includeData} replaced. */
    default ServerboundPickItemFromEntityView withIncludeData(boolean includeData) {
        return (ServerboundPickItemFromEntityView) with("includeData", includeData);
    }
}
