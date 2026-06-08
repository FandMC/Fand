package io.fand.api.packet.view;

import io.fand.api.packet.PacketView;
import java.util.UUID;

/** Typed view of {@link ServerboundClientInformationPacket}. */
public interface ServerboundClientInformationView extends PacketView {

    default Object information() {
        return require("information", Object.class);
    }

    /** Returns a copy with {@code information} replaced. */
    default ServerboundClientInformationView withInformation(Object information) {
        return (ServerboundClientInformationView) with("information", information);
    }
}
