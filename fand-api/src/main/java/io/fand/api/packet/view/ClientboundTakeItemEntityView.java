package io.fand.api.packet.view;

import io.fand.api.packet.PacketView;
import java.util.UUID;

/** Typed view of {@link ClientboundTakeItemEntityPacket}. */
public interface ClientboundTakeItemEntityView extends PacketView {

    default int itemId() {
        return require("itemId", int.class);
    }
    default int playerId() {
        return require("playerId", int.class);
    }
    default int amount() {
        return require("amount", int.class);
    }

    /** Returns a copy with {@code itemId} replaced. */
    default ClientboundTakeItemEntityView withItemId(int itemId) {
        return (ClientboundTakeItemEntityView) with("itemId", itemId);
    }
    /** Returns a copy with {@code playerId} replaced. */
    default ClientboundTakeItemEntityView withPlayerId(int playerId) {
        return (ClientboundTakeItemEntityView) with("playerId", playerId);
    }
    /** Returns a copy with {@code amount} replaced. */
    default ClientboundTakeItemEntityView withAmount(int amount) {
        return (ClientboundTakeItemEntityView) with("amount", amount);
    }
}
