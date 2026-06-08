package io.fand.api.packet.view;

import io.fand.api.packet.PacketView;
import java.util.UUID;

/** Typed view of {@link ServerboundChangeGameModePacket}. */
public interface ServerboundChangeGameModeView extends PacketView {

    default Object mode() {
        return require("mode", Object.class);
    }

    /** Returns a copy with {@code mode} replaced. */
    default ServerboundChangeGameModeView withMode(Object mode) {
        return (ServerboundChangeGameModeView) with("mode", mode);
    }
}
