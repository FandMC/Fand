package io.fand.api.packet.view;

import io.fand.api.packet.PacketView;
import java.util.UUID;

/** Typed view of {@link ClientboundRecipeBookSettingsPacket}. */
public interface ClientboundRecipeBookSettingsView extends PacketView {

    default Object bookSettings() {
        return require("bookSettings", Object.class);
    }

    /** Returns a copy with {@code bookSettings} replaced. */
    default ClientboundRecipeBookSettingsView withBookSettings(Object bookSettings) {
        return (ClientboundRecipeBookSettingsView) with("bookSettings", bookSettings);
    }
}
