package io.fand.api.packet.view;

import io.fand.api.packet.PacketView;
import java.util.UUID;

/** Typed view of {@link ServerboundPlayerInputPacket}. */
public interface ServerboundPlayerInputView extends PacketView {

    default Object input() {
        return require("input", Object.class);
    }

    /** Returns a copy with {@code input} replaced. */
    default ServerboundPlayerInputView withInput(Object input) {
        return (ServerboundPlayerInputView) with("input", input);
    }
}
