package io.fand.api.packet.view;

import io.fand.api.packet.PacketView;
import java.util.UUID;

/** Typed view of {@link ServerboundHelloPacket}. */
public interface ServerboundHelloView extends PacketView {

    default String name() {
        return require("name", String.class);
    }
    default UUID profileId() {
        return require("profileId", UUID.class);
    }

    /** Returns a copy with {@code name} replaced. */
    default ServerboundHelloView withName(String name) {
        return (ServerboundHelloView) with("name", name);
    }
    /** Returns a copy with {@code profileId} replaced. */
    default ServerboundHelloView withProfileId(UUID profileId) {
        return (ServerboundHelloView) with("profileId", profileId);
    }
}
