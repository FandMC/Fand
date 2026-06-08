package io.fand.api.packet.view;

import io.fand.api.packet.PacketView;
import java.util.UUID;

/** Typed view of {@link ClientboundSetChunkCacheRadiusPacket}. */
public interface ClientboundSetChunkCacheRadiusView extends PacketView {

    default int radius() {
        return require("radius", int.class);
    }

    /** Returns a copy with {@code radius} replaced. */
    default ClientboundSetChunkCacheRadiusView withRadius(int radius) {
        return (ClientboundSetChunkCacheRadiusView) with("radius", radius);
    }
}
