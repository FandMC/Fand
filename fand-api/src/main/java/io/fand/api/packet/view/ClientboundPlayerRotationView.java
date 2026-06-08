package io.fand.api.packet.view;

import io.fand.api.packet.PacketView;
import java.util.UUID;

/** Typed view of {@link ClientboundPlayerRotationPacket}. */
public interface ClientboundPlayerRotationView extends PacketView {

    default float yRot() {
        return require("yRot", float.class);
    }
    default boolean relativeY() {
        return require("relativeY", boolean.class);
    }
    default float xRot() {
        return require("xRot", float.class);
    }
    default boolean relativeX() {
        return require("relativeX", boolean.class);
    }

    /** Returns a copy with {@code yRot} replaced. */
    default ClientboundPlayerRotationView withYRot(float yRot) {
        return (ClientboundPlayerRotationView) with("yRot", yRot);
    }
    /** Returns a copy with {@code relativeY} replaced. */
    default ClientboundPlayerRotationView withRelativeY(boolean relativeY) {
        return (ClientboundPlayerRotationView) with("relativeY", relativeY);
    }
    /** Returns a copy with {@code xRot} replaced. */
    default ClientboundPlayerRotationView withXRot(float xRot) {
        return (ClientboundPlayerRotationView) with("xRot", xRot);
    }
    /** Returns a copy with {@code relativeX} replaced. */
    default ClientboundPlayerRotationView withRelativeX(boolean relativeX) {
        return (ClientboundPlayerRotationView) with("relativeX", relativeX);
    }
}
