package io.fand.api.packet.view;

import io.fand.api.packet.PacketView;
import net.kyori.adventure.text.Component;

/** Typed view of the action-bar text shown to a player. Replaceable. */
public interface ClientboundSetActionBarTextView extends PacketView {

    /** The action-bar text. */
    default Component text() {
        return require("text", Component.class);
    }
}
