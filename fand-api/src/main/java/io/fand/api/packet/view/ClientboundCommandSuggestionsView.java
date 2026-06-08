package io.fand.api.packet.view;

import io.fand.api.packet.PacketView;
import java.util.UUID;

/** Typed view of {@link ClientboundCommandSuggestionsPacket}. */
public interface ClientboundCommandSuggestionsView extends PacketView {

    default int id() {
        return require("id", int.class);
    }
    default int start() {
        return require("start", int.class);
    }
    default int length() {
        return require("length", int.class);
    }
    default Object suggestions() {
        return require("suggestions", Object.class);
    }

    /** Returns a copy with {@code id} replaced. */
    default ClientboundCommandSuggestionsView withId(int id) {
        return (ClientboundCommandSuggestionsView) with("id", id);
    }
    /** Returns a copy with {@code start} replaced. */
    default ClientboundCommandSuggestionsView withStart(int start) {
        return (ClientboundCommandSuggestionsView) with("start", start);
    }
    /** Returns a copy with {@code length} replaced. */
    default ClientboundCommandSuggestionsView withLength(int length) {
        return (ClientboundCommandSuggestionsView) with("length", length);
    }
    /** Returns a copy with {@code suggestions} replaced. */
    default ClientboundCommandSuggestionsView withSuggestions(Object suggestions) {
        return (ClientboundCommandSuggestionsView) with("suggestions", suggestions);
    }
}
