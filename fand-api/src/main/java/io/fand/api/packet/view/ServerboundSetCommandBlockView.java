package io.fand.api.packet.view;

import io.fand.api.packet.PacketView;
import java.util.UUID;

/** Typed view of {@link ServerboundSetCommandBlockPacket}. */
public interface ServerboundSetCommandBlockView extends PacketView {

    default Object pos() {
        return require("pos", Object.class);
    }
    default String command() {
        return require("command", String.class);
    }
    default boolean trackOutput() {
        return require("trackOutput", boolean.class);
    }
    default boolean conditional() {
        return require("conditional", boolean.class);
    }
    default boolean automatic() {
        return require("automatic", boolean.class);
    }
    default Object mode() {
        return require("mode", Object.class);
    }

    /** Returns a copy with {@code pos} replaced. */
    default ServerboundSetCommandBlockView withPos(Object pos) {
        return (ServerboundSetCommandBlockView) with("pos", pos);
    }
    /** Returns a copy with {@code command} replaced. */
    default ServerboundSetCommandBlockView withCommand(String command) {
        return (ServerboundSetCommandBlockView) with("command", command);
    }
    /** Returns a copy with {@code trackOutput} replaced. */
    default ServerboundSetCommandBlockView withTrackOutput(boolean trackOutput) {
        return (ServerboundSetCommandBlockView) with("trackOutput", trackOutput);
    }
    /** Returns a copy with {@code conditional} replaced. */
    default ServerboundSetCommandBlockView withConditional(boolean conditional) {
        return (ServerboundSetCommandBlockView) with("conditional", conditional);
    }
    /** Returns a copy with {@code automatic} replaced. */
    default ServerboundSetCommandBlockView withAutomatic(boolean automatic) {
        return (ServerboundSetCommandBlockView) with("automatic", automatic);
    }
    /** Returns a copy with {@code mode} replaced. */
    default ServerboundSetCommandBlockView withMode(Object mode) {
        return (ServerboundSetCommandBlockView) with("mode", mode);
    }
}
