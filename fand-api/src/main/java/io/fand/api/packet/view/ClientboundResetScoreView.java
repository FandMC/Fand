package io.fand.api.packet.view;

import io.fand.api.packet.PacketView;
import java.util.UUID;

/** Typed view of {@link ClientboundResetScorePacket}. */
public interface ClientboundResetScoreView extends PacketView {

    default String owner() {
        return require("owner", String.class);
    }
    default Object String() {
        return require("String", Object.class);
    }

    /** Returns a copy with {@code owner} replaced. */
    default ClientboundResetScoreView withOwner(String owner) {
        return (ClientboundResetScoreView) with("owner", owner);
    }
    /** Returns a copy with {@code String} replaced. */
    default ClientboundResetScoreView withString(Object String) {
        return (ClientboundResetScoreView) with("String", String);
    }
}
