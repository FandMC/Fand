package io.fand.api.packet.view;

import io.fand.api.packet.PacketView;
import java.util.UUID;

/** Typed view of {@link ClientboundSetChunkCacheCenterPacket}. */
public interface ClientboundSetChunkCacheCenterView extends PacketView {

    default int x() {
        return require("x", int.class);
    }
    default int z() {
        return require("z", int.class);
    }

    /** Returns a copy with {@code x} replaced. */
    default ClientboundSetChunkCacheCenterView withX(int x) {
        return (ClientboundSetChunkCacheCenterView) with("x", x);
    }
    /** Returns a copy with {@code z} replaced. */
    default ClientboundSetChunkCacheCenterView withZ(int z) {
        return (ClientboundSetChunkCacheCenterView) with("z", z);
    }
}
