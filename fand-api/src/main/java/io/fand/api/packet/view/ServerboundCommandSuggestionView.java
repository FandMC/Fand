package io.fand.api.packet.view;

import io.fand.api.packet.PacketView;
import java.util.UUID;

/** Typed view of {@link ServerboundCommandSuggestionPacket}. */
public interface ServerboundCommandSuggestionView extends PacketView {

    default int id() {
        return require("id", int.class);
    }
    default String command() {
        return require("command", String.class);
    }

    /** Returns a copy with {@code id} replaced. */
    default ServerboundCommandSuggestionView withId(int id) {
        return (ServerboundCommandSuggestionView) with("id", id);
    }
    /** Returns a copy with {@code command} replaced. */
    default ServerboundCommandSuggestionView withCommand(String command) {
        return (ServerboundCommandSuggestionView) with("command", command);
    }
}
