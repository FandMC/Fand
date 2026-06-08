package io.fand.api.packet.view;

import io.fand.api.packet.PacketView;
import java.util.UUID;

/** Typed view of {@link ClientboundPlayerCombatKillPacket}. */
public interface ClientboundPlayerCombatKillView extends PacketView {

    default int playerId() {
        return require("playerId", int.class);
    }
    default Object message() {
        return require("message", Object.class);
    }

    /** Returns a copy with {@code playerId} replaced. */
    default ClientboundPlayerCombatKillView withPlayerId(int playerId) {
        return (ClientboundPlayerCombatKillView) with("playerId", playerId);
    }
    /** Returns a copy with {@code message} replaced. */
    default ClientboundPlayerCombatKillView withMessage(Object message) {
        return (ClientboundPlayerCombatKillView) with("message", message);
    }
}
