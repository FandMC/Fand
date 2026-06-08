package io.fand.api.packet.view;

import io.fand.api.packet.PacketView;
import java.util.UUID;

/** Typed view of {@link ClientboundInitializeBorderPacket}. */
public interface ClientboundInitializeBorderView extends PacketView {

    default double newCenterX() {
        return require("newCenterX", double.class);
    }
    default double newCenterZ() {
        return require("newCenterZ", double.class);
    }
    default double oldSize() {
        return require("oldSize", double.class);
    }
    default double newSize() {
        return require("newSize", double.class);
    }
    default long lerpTime() {
        return require("lerpTime", long.class);
    }
    default int newAbsoluteMaxSize() {
        return require("newAbsoluteMaxSize", int.class);
    }
    default int warningBlocks() {
        return require("warningBlocks", int.class);
    }
    default int warningTime() {
        return require("warningTime", int.class);
    }

    /** Returns a copy with {@code newCenterX} replaced. */
    default ClientboundInitializeBorderView withNewCenterX(double newCenterX) {
        return (ClientboundInitializeBorderView) with("newCenterX", newCenterX);
    }
    /** Returns a copy with {@code newCenterZ} replaced. */
    default ClientboundInitializeBorderView withNewCenterZ(double newCenterZ) {
        return (ClientboundInitializeBorderView) with("newCenterZ", newCenterZ);
    }
    /** Returns a copy with {@code oldSize} replaced. */
    default ClientboundInitializeBorderView withOldSize(double oldSize) {
        return (ClientboundInitializeBorderView) with("oldSize", oldSize);
    }
    /** Returns a copy with {@code newSize} replaced. */
    default ClientboundInitializeBorderView withNewSize(double newSize) {
        return (ClientboundInitializeBorderView) with("newSize", newSize);
    }
    /** Returns a copy with {@code lerpTime} replaced. */
    default ClientboundInitializeBorderView withLerpTime(long lerpTime) {
        return (ClientboundInitializeBorderView) with("lerpTime", lerpTime);
    }
    /** Returns a copy with {@code newAbsoluteMaxSize} replaced. */
    default ClientboundInitializeBorderView withNewAbsoluteMaxSize(int newAbsoluteMaxSize) {
        return (ClientboundInitializeBorderView) with("newAbsoluteMaxSize", newAbsoluteMaxSize);
    }
    /** Returns a copy with {@code warningBlocks} replaced. */
    default ClientboundInitializeBorderView withWarningBlocks(int warningBlocks) {
        return (ClientboundInitializeBorderView) with("warningBlocks", warningBlocks);
    }
    /** Returns a copy with {@code warningTime} replaced. */
    default ClientboundInitializeBorderView withWarningTime(int warningTime) {
        return (ClientboundInitializeBorderView) with("warningTime", warningTime);
    }
}
