package io.fand.api.packet.view;

import io.fand.api.packet.PacketView;
import net.kyori.adventure.text.Component;

/** Typed view of the title text shown to a player. Replaceable. */
public interface ClientboundSetTitleTextView extends PacketView {

    /** The title text. */
    default Component text() {
        return require("text", Component.class);
    }
}
