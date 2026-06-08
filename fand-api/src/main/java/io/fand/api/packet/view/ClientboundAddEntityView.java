package io.fand.api.packet.view;

import io.fand.api.packet.PacketView;
import java.util.UUID;

/** Typed view of {@link ClientboundAddEntityPacket}. */
public interface ClientboundAddEntityView extends PacketView {

    default int id() {
        return require("id", int.class);
    }
    default UUID uuid() {
        return require("uuid", UUID.class);
    }
    default double x() {
        return require("x", double.class);
    }
    default double y() {
        return require("y", double.class);
    }
    default double z() {
        return require("z", double.class);
    }
    default Object movement() {
        return require("movement", Object.class);
    }
    default byte xRot() {
        return require("xRot", byte.class);
    }
    default byte yRot() {
        return require("yRot", byte.class);
    }
    default byte yHeadRot() {
        return require("yHeadRot", byte.class);
    }
    default int data() {
        return require("data", int.class);
    }

    /** Returns a copy with {@code id} replaced. */
    default ClientboundAddEntityView withId(int id) {
        return (ClientboundAddEntityView) with("id", id);
    }
    /** Returns a copy with {@code uuid} replaced. */
    default ClientboundAddEntityView withUuid(UUID uuid) {
        return (ClientboundAddEntityView) with("uuid", uuid);
    }
    /** Returns a copy with {@code x} replaced. */
    default ClientboundAddEntityView withX(double x) {
        return (ClientboundAddEntityView) with("x", x);
    }
    /** Returns a copy with {@code y} replaced. */
    default ClientboundAddEntityView withY(double y) {
        return (ClientboundAddEntityView) with("y", y);
    }
    /** Returns a copy with {@code z} replaced. */
    default ClientboundAddEntityView withZ(double z) {
        return (ClientboundAddEntityView) with("z", z);
    }
    /** Returns a copy with {@code movement} replaced. */
    default ClientboundAddEntityView withMovement(Object movement) {
        return (ClientboundAddEntityView) with("movement", movement);
    }
    /** Returns a copy with {@code xRot} replaced. */
    default ClientboundAddEntityView withXRot(byte xRot) {
        return (ClientboundAddEntityView) with("xRot", xRot);
    }
    /** Returns a copy with {@code yRot} replaced. */
    default ClientboundAddEntityView withYRot(byte yRot) {
        return (ClientboundAddEntityView) with("yRot", yRot);
    }
    /** Returns a copy with {@code yHeadRot} replaced. */
    default ClientboundAddEntityView withYHeadRot(byte yHeadRot) {
        return (ClientboundAddEntityView) with("yHeadRot", yHeadRot);
    }
    /** Returns a copy with {@code data} replaced. */
    default ClientboundAddEntityView withData(int data) {
        return (ClientboundAddEntityView) with("data", data);
    }
}
