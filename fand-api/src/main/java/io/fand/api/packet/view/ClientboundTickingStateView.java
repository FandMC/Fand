package io.fand.api.packet.view;

import io.fand.api.packet.PacketView;
import java.util.UUID;

/** Typed view of {@link ClientboundTickingStatePacket}. */
public interface ClientboundTickingStateView extends PacketView {

    default float tickRate() {
        return require("tickRate", float.class);
    }
    default boolean isFrozen() {
        return require("isFrozen", boolean.class);
    }

    /** Returns a copy with {@code tickRate} replaced. */
    default ClientboundTickingStateView withTickRate(float tickRate) {
        return (ClientboundTickingStateView) with("tickRate", tickRate);
    }
    /** Returns a copy with {@code isFrozen} replaced. */
    default ClientboundTickingStateView withIsFrozen(boolean isFrozen) {
        return (ClientboundTickingStateView) with("isFrozen", isFrozen);
    }
}
