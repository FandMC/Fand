package io.fand.api.packet.view;

import io.fand.api.packet.PacketView;
import java.util.UUID;

/** Typed view of {@link ClientboundTickingStepPacket}. */
public interface ClientboundTickingStepView extends PacketView {

    default int tickSteps() {
        return require("tickSteps", int.class);
    }

    /** Returns a copy with {@code tickSteps} replaced. */
    default ClientboundTickingStepView withTickSteps(int tickSteps) {
        return (ClientboundTickingStepView) with("tickSteps", tickSteps);
    }
}
