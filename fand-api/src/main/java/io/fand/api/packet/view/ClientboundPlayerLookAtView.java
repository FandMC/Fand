package io.fand.api.packet.view;

import io.fand.api.packet.PacketView;
import java.util.UUID;

/** Typed view of {@link ClientboundPlayerLookAtPacket}. */
public interface ClientboundPlayerLookAtView extends PacketView {

    default double x() {
        return require("x", double.class);
    }
    default double y() {
        return require("y", double.class);
    }
    default double z() {
        return require("z", double.class);
    }
    default int entity() {
        return require("entity", int.class);
    }
    default Object fromAnchor() {
        return require("fromAnchor", Object.class);
    }
    default Object toAnchor() {
        return require("toAnchor", Object.class);
    }
    default boolean atEntity() {
        return require("atEntity", boolean.class);
    }

    /** Returns a copy with {@code x} replaced. */
    default ClientboundPlayerLookAtView withX(double x) {
        return (ClientboundPlayerLookAtView) with("x", x);
    }
    /** Returns a copy with {@code y} replaced. */
    default ClientboundPlayerLookAtView withY(double y) {
        return (ClientboundPlayerLookAtView) with("y", y);
    }
    /** Returns a copy with {@code z} replaced. */
    default ClientboundPlayerLookAtView withZ(double z) {
        return (ClientboundPlayerLookAtView) with("z", z);
    }
    /** Returns a copy with {@code entity} replaced. */
    default ClientboundPlayerLookAtView withEntity(int entity) {
        return (ClientboundPlayerLookAtView) with("entity", entity);
    }
    /** Returns a copy with {@code fromAnchor} replaced. */
    default ClientboundPlayerLookAtView withFromAnchor(Object fromAnchor) {
        return (ClientboundPlayerLookAtView) with("fromAnchor", fromAnchor);
    }
    /** Returns a copy with {@code toAnchor} replaced. */
    default ClientboundPlayerLookAtView withToAnchor(Object toAnchor) {
        return (ClientboundPlayerLookAtView) with("toAnchor", toAnchor);
    }
    /** Returns a copy with {@code atEntity} replaced. */
    default ClientboundPlayerLookAtView withAtEntity(boolean atEntity) {
        return (ClientboundPlayerLookAtView) with("atEntity", atEntity);
    }
}
