package io.fand.api.packet.view;

import io.fand.api.packet.PacketView;
import java.util.UUID;

/** Typed view of {@link ClientboundSectionBlocksUpdatePacket}. */
public interface ClientboundSectionBlocksUpdateView extends PacketView {

    default Object sectionPos() {
        return require("sectionPos", Object.class);
    }
    default Object positions() {
        return require("positions", Object.class);
    }
    default Object states() {
        return require("states", Object.class);
    }

    /** Returns a copy with {@code sectionPos} replaced. */
    default ClientboundSectionBlocksUpdateView withSectionPos(Object sectionPos) {
        return (ClientboundSectionBlocksUpdateView) with("sectionPos", sectionPos);
    }
    /** Returns a copy with {@code positions} replaced. */
    default ClientboundSectionBlocksUpdateView withPositions(Object positions) {
        return (ClientboundSectionBlocksUpdateView) with("positions", positions);
    }
    /** Returns a copy with {@code states} replaced. */
    default ClientboundSectionBlocksUpdateView withStates(Object states) {
        return (ClientboundSectionBlocksUpdateView) with("states", states);
    }
}
