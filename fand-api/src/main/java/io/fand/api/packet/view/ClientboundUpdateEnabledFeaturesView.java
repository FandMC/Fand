package io.fand.api.packet.view;

import io.fand.api.packet.PacketView;
import java.util.UUID;

/** Typed view of {@link ClientboundUpdateEnabledFeaturesPacket}. */
public interface ClientboundUpdateEnabledFeaturesView extends PacketView {

    default Object features() {
        return require("features", Object.class);
    }

    /** Returns a copy with {@code features} replaced. */
    default ClientboundUpdateEnabledFeaturesView withFeatures(Object features) {
        return (ClientboundUpdateEnabledFeaturesView) with("features", features);
    }
}
