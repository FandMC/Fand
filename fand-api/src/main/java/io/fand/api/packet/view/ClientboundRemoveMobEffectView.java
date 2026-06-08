package io.fand.api.packet.view;

import io.fand.api.packet.PacketView;
import java.util.UUID;

/** Typed view of {@link ClientboundRemoveMobEffectPacket}. */
public interface ClientboundRemoveMobEffectView extends PacketView {

    default int entityId() {
        return require("entityId", int.class);
    }
    default Object effect() {
        return require("effect", Object.class);
    }

    /** Returns a copy with {@code entityId} replaced. */
    default ClientboundRemoveMobEffectView withEntityId(int entityId) {
        return (ClientboundRemoveMobEffectView) with("entityId", entityId);
    }
    /** Returns a copy with {@code effect} replaced. */
    default ClientboundRemoveMobEffectView withEffect(Object effect) {
        return (ClientboundRemoveMobEffectView) with("effect", effect);
    }
}
