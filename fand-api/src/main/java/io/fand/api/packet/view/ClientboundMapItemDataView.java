package io.fand.api.packet.view;

import io.fand.api.packet.PacketView;
import java.util.UUID;

/** Typed view of {@link ClientboundMapItemDataPacket}. */
public interface ClientboundMapItemDataView extends PacketView {

    default Object mapId() {
        return require("mapId", Object.class);
    }
    default byte scale() {
        return require("scale", byte.class);
    }
    default boolean locked() {
        return require("locked", boolean.class);
    }
    default Object decorations() {
        return require("decorations", Object.class);
    }
    default Object colorPatch() {
        return require("colorPatch", Object.class);
    }

    /** Returns a copy with {@code mapId} replaced. */
    default ClientboundMapItemDataView withMapId(Object mapId) {
        return (ClientboundMapItemDataView) with("mapId", mapId);
    }
    /** Returns a copy with {@code scale} replaced. */
    default ClientboundMapItemDataView withScale(byte scale) {
        return (ClientboundMapItemDataView) with("scale", scale);
    }
    /** Returns a copy with {@code locked} replaced. */
    default ClientboundMapItemDataView withLocked(boolean locked) {
        return (ClientboundMapItemDataView) with("locked", locked);
    }
    /** Returns a copy with {@code decorations} replaced. */
    default ClientboundMapItemDataView withDecorations(Object decorations) {
        return (ClientboundMapItemDataView) with("decorations", decorations);
    }
    /** Returns a copy with {@code colorPatch} replaced. */
    default ClientboundMapItemDataView withColorPatch(Object colorPatch) {
        return (ClientboundMapItemDataView) with("colorPatch", colorPatch);
    }
}
