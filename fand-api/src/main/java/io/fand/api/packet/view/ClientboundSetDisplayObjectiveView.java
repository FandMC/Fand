package io.fand.api.packet.view;

import io.fand.api.packet.PacketView;
import java.util.UUID;

/** Typed view of {@link ClientboundSetDisplayObjectivePacket}. */
public interface ClientboundSetDisplayObjectiveView extends PacketView {

    default Object slot() {
        return require("slot", Object.class);
    }
    default String objectiveName() {
        return require("objectiveName", String.class);
    }

    /** Returns a copy with {@code slot} replaced. */
    default ClientboundSetDisplayObjectiveView withSlot(Object slot) {
        return (ClientboundSetDisplayObjectiveView) with("slot", slot);
    }
    /** Returns a copy with {@code objectiveName} replaced. */
    default ClientboundSetDisplayObjectiveView withObjectiveName(String objectiveName) {
        return (ClientboundSetDisplayObjectiveView) with("objectiveName", objectiveName);
    }
}
