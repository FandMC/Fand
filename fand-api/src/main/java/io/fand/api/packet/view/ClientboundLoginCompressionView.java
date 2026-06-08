package io.fand.api.packet.view;

import io.fand.api.packet.PacketView;
import java.util.UUID;

/** Typed view of {@link ClientboundLoginCompressionPacket}. */
public interface ClientboundLoginCompressionView extends PacketView {

    default int compressionThreshold() {
        return require("compressionThreshold", int.class);
    }

    /** Returns a copy with {@code compressionThreshold} replaced. */
    default ClientboundLoginCompressionView withCompressionThreshold(int compressionThreshold) {
        return (ClientboundLoginCompressionView) with("compressionThreshold", compressionThreshold);
    }
}
