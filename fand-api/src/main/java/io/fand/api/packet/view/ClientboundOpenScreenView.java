package io.fand.api.packet.view;

import io.fand.api.packet.PacketView;
import net.kyori.adventure.text.Component;

/**
 * Typed view of a container-open request. Read-only. The menu type is exposed
 * via the dynamic {@code get("type", ...)} as an opaque value.
 */
public interface ClientboundOpenScreenView extends PacketView {

    default int containerId() {
        return require("containerId", Integer.class);
    }

    /** The window title. */
    default Component title() {
        return require("title", Component.class);
    }
}
