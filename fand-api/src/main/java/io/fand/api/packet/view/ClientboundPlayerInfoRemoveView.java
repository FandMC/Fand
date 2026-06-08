package io.fand.api.packet.view;

import io.fand.api.packet.PacketView;
import java.util.UUID;

/** Typed view of {@link ClientboundPlayerInfoRemovePacket}. */
public interface ClientboundPlayerInfoRemoveView extends PacketView {

    default Object profileIds() {
        return require("profileIds", Object.class);
    }

    /** Returns a copy with {@code profileIds} replaced. */
    default ClientboundPlayerInfoRemoveView withProfileIds(Object profileIds) {
        return (ClientboundPlayerInfoRemoveView) with("profileIds", profileIds);
    }
}
