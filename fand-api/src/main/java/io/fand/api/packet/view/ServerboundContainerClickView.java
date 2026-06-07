package io.fand.api.packet.view;

import io.fand.api.packet.PacketView;

/**
 * Typed view of a click inside an open container. The changed-slot map and
 * carried item are exposed via the dynamic {@code get(...)} as opaque values;
 * the scalar fields below are replaceable (other fields pass through unchanged).
 */
public interface ServerboundContainerClickView extends PacketView {

    /** The window id of the open container. */
    default int containerId() {
        return require("containerId", Integer.class);
    }

    /** The container state id used for desync detection. */
    default int stateId() {
        return require("stateId", Integer.class);
    }

    /** The clicked slot, or {@code -999} for outside-the-window clicks. */
    default short slot() {
        return require("slotNum", Short.class);
    }

    /** The mouse button / key encoded for the click. */
    default byte button() {
        return require("buttonNum", Byte.class);
    }
}
