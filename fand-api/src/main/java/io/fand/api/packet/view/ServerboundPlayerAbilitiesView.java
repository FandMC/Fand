package io.fand.api.packet.view;

import io.fand.api.packet.PacketView;
import java.util.UUID;

/** Typed view of {@link ServerboundPlayerAbilitiesPacket}. */
public interface ServerboundPlayerAbilitiesView extends PacketView {

    default boolean isFlying() {
        return require("isFlying", boolean.class);
    }

    /** Returns a copy with {@code isFlying} replaced. */
    default ServerboundPlayerAbilitiesView withIsFlying(boolean isFlying) {
        return (ServerboundPlayerAbilitiesView) with("isFlying", isFlying);
    }
}
