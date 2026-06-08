package io.fand.api.packet.view;

import io.fand.api.packet.PacketView;
import java.util.UUID;

/** Typed view of {@link ClientboundSetObjectivePacket}. */
public interface ClientboundSetObjectiveView extends PacketView {

    default String objectiveName() {
        return require("objectiveName", String.class);
    }
    default Object displayName() {
        return require("displayName", Object.class);
    }
    default Object renderType() {
        return require("renderType", Object.class);
    }
    default Object numberFormat() {
        return require("numberFormat", Object.class);
    }
    default int method() {
        return require("method", int.class);
    }

    /** Returns a copy with {@code objectiveName} replaced. */
    default ClientboundSetObjectiveView withObjectiveName(String objectiveName) {
        return (ClientboundSetObjectiveView) with("objectiveName", objectiveName);
    }
    /** Returns a copy with {@code displayName} replaced. */
    default ClientboundSetObjectiveView withDisplayName(Object displayName) {
        return (ClientboundSetObjectiveView) with("displayName", displayName);
    }
    /** Returns a copy with {@code renderType} replaced. */
    default ClientboundSetObjectiveView withRenderType(Object renderType) {
        return (ClientboundSetObjectiveView) with("renderType", renderType);
    }
    /** Returns a copy with {@code numberFormat} replaced. */
    default ClientboundSetObjectiveView withNumberFormat(Object numberFormat) {
        return (ClientboundSetObjectiveView) with("numberFormat", numberFormat);
    }
    /** Returns a copy with {@code method} replaced. */
    default ClientboundSetObjectiveView withMethod(int method) {
        return (ClientboundSetObjectiveView) with("method", method);
    }
}
