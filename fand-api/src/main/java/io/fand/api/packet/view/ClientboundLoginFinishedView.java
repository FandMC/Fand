package io.fand.api.packet.view;

import io.fand.api.packet.PacketView;
import java.util.UUID;

/** Typed view of {@link ClientboundLoginFinishedPacket}. */
public interface ClientboundLoginFinishedView extends PacketView {

    default Object gameProfile() {
        return require("gameProfile", Object.class);
    }

    /** Returns a copy with {@code gameProfile} replaced. */
    default ClientboundLoginFinishedView withGameProfile(Object gameProfile) {
        return (ClientboundLoginFinishedView) with("gameProfile", gameProfile);
    }
}
