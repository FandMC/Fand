package io.fand.api.packet.view;

import io.fand.api.packet.PacketView;
import java.util.UUID;

/** Typed view of {@link ClientboundCooldownPacket}. */
public interface ClientboundCooldownView extends PacketView {

    default Object cooldownGroup() {
        return require("cooldownGroup", Object.class);
    }
    default int duration() {
        return require("duration", int.class);
    }

    /** Returns a copy with {@code cooldownGroup} replaced. */
    default ClientboundCooldownView withCooldownGroup(Object cooldownGroup) {
        return (ClientboundCooldownView) with("cooldownGroup", cooldownGroup);
    }
    /** Returns a copy with {@code duration} replaced. */
    default ClientboundCooldownView withDuration(int duration) {
        return (ClientboundCooldownView) with("duration", duration);
    }
}
