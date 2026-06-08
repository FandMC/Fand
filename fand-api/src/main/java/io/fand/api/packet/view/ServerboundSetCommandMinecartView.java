package io.fand.api.packet.view;

import io.fand.api.packet.PacketView;
import java.util.UUID;

/** Typed view of {@link ServerboundSetCommandMinecartPacket}. */
public interface ServerboundSetCommandMinecartView extends PacketView {

    default int entity() {
        return require("entity", int.class);
    }
    default String command() {
        return require("command", String.class);
    }
    default boolean trackOutput() {
        return require("trackOutput", boolean.class);
    }

    /** Returns a copy with {@code entity} replaced. */
    default ServerboundSetCommandMinecartView withEntity(int entity) {
        return (ServerboundSetCommandMinecartView) with("entity", entity);
    }
    /** Returns a copy with {@code command} replaced. */
    default ServerboundSetCommandMinecartView withCommand(String command) {
        return (ServerboundSetCommandMinecartView) with("command", command);
    }
    /** Returns a copy with {@code trackOutput} replaced. */
    default ServerboundSetCommandMinecartView withTrackOutput(boolean trackOutput) {
        return (ServerboundSetCommandMinecartView) with("trackOutput", trackOutput);
    }
}
