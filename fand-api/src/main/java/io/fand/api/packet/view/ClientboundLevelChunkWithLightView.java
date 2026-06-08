package io.fand.api.packet.view;

import io.fand.api.packet.PacketView;
import java.util.UUID;

/** Typed view of {@link ClientboundLevelChunkWithLightPacket}. */
public interface ClientboundLevelChunkWithLightView extends PacketView {

    default int x() {
        return require("x", int.class);
    }
    default int z() {
        return require("z", int.class);
    }
    default Object chunkData() {
        return require("chunkData", Object.class);
    }
    default Object lightData() {
        return require("lightData", Object.class);
    }

    /** Returns a copy with {@code x} replaced. */
    default ClientboundLevelChunkWithLightView withX(int x) {
        return (ClientboundLevelChunkWithLightView) with("x", x);
    }
    /** Returns a copy with {@code z} replaced. */
    default ClientboundLevelChunkWithLightView withZ(int z) {
        return (ClientboundLevelChunkWithLightView) with("z", z);
    }
    /** Returns a copy with {@code chunkData} replaced. */
    default ClientboundLevelChunkWithLightView withChunkData(Object chunkData) {
        return (ClientboundLevelChunkWithLightView) with("chunkData", chunkData);
    }
    /** Returns a copy with {@code lightData} replaced. */
    default ClientboundLevelChunkWithLightView withLightData(Object lightData) {
        return (ClientboundLevelChunkWithLightView) with("lightData", lightData);
    }
}
