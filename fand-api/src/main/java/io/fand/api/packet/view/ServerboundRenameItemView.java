package io.fand.api.packet.view;

import io.fand.api.packet.PacketView;
import java.util.UUID;

/** Typed view of {@link ServerboundRenameItemPacket}. */
public interface ServerboundRenameItemView extends PacketView {

    default String name() {
        return require("name", String.class);
    }

    /** Returns a copy with {@code name} replaced. */
    default ServerboundRenameItemView withName(String name) {
        return (ServerboundRenameItemView) with("name", name);
    }
}
