package io.fand.api.packet.view;

import io.fand.api.packet.PacketView;
import java.util.UUID;

/** Typed view of {@link ClientboundPlayerCombatEndPacket}. */
public interface ClientboundPlayerCombatEndView extends PacketView {

    default int duration() {
        return require("duration", int.class);
    }

    /** Returns a copy with {@code duration} replaced. */
    default ClientboundPlayerCombatEndView withDuration(int duration) {
        return (ClientboundPlayerCombatEndView) with("duration", duration);
    }
}
