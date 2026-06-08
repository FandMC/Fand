package io.fand.api.packet.view;

import io.fand.api.packet.PacketView;
import java.util.UUID;

/** Typed view of {@link ClientboundRecipeBookAddPacket}. */
public interface ClientboundRecipeBookAddView extends PacketView {

    default Object entries() {
        return require("entries", Object.class);
    }
    default boolean replace() {
        return require("replace", boolean.class);
    }

    /** Returns a copy with {@code entries} replaced. */
    default ClientboundRecipeBookAddView withEntries(Object entries) {
        return (ClientboundRecipeBookAddView) with("entries", entries);
    }
    /** Returns a copy with {@code replace} replaced. */
    default ClientboundRecipeBookAddView withReplace(boolean replace) {
        return (ClientboundRecipeBookAddView) with("replace", replace);
    }
}
