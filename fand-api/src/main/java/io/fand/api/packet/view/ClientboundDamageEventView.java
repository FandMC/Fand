package io.fand.api.packet.view;

import io.fand.api.packet.PacketView;
import java.util.UUID;

/** Typed view of {@link ClientboundDamageEventPacket}. */
public interface ClientboundDamageEventView extends PacketView {

    default int entityId() {
        return require("entityId", int.class);
    }
    default Object sourceType() {
        return require("sourceType", Object.class);
    }
    default int sourceCauseId() {
        return require("sourceCauseId", int.class);
    }
    default int sourceDirectId() {
        return require("sourceDirectId", int.class);
    }
    default Object sourcePosition() {
        return require("sourcePosition", Object.class);
    }

    /** Returns a copy with {@code entityId} replaced. */
    default ClientboundDamageEventView withEntityId(int entityId) {
        return (ClientboundDamageEventView) with("entityId", entityId);
    }
    /** Returns a copy with {@code sourceType} replaced. */
    default ClientboundDamageEventView withSourceType(Object sourceType) {
        return (ClientboundDamageEventView) with("sourceType", sourceType);
    }
    /** Returns a copy with {@code sourceCauseId} replaced. */
    default ClientboundDamageEventView withSourceCauseId(int sourceCauseId) {
        return (ClientboundDamageEventView) with("sourceCauseId", sourceCauseId);
    }
    /** Returns a copy with {@code sourceDirectId} replaced. */
    default ClientboundDamageEventView withSourceDirectId(int sourceDirectId) {
        return (ClientboundDamageEventView) with("sourceDirectId", sourceDirectId);
    }
    /** Returns a copy with {@code sourcePosition} replaced. */
    default ClientboundDamageEventView withSourcePosition(Object sourcePosition) {
        return (ClientboundDamageEventView) with("sourcePosition", sourcePosition);
    }
}
