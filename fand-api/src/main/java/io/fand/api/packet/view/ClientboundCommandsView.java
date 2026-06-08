package io.fand.api.packet.view;

import io.fand.api.packet.PacketView;
import java.util.UUID;

/** Typed view of {@link ClientboundCommandsPacket}. */
public interface ClientboundCommandsView extends PacketView {

    default int rootIndex() {
        return require("rootIndex", int.class);
    }
    default Object entries() {
        return require("entries", Object.class);
    }
    default Object context() {
        return require("context", Object.class);
    }
    default Object builder() {
        return require("builder", Object.class);
    }
    default Object nodes() {
        return require("nodes", Object.class);
    }

    /** Returns a copy with {@code rootIndex} replaced. */
    default ClientboundCommandsView withRootIndex(int rootIndex) {
        return (ClientboundCommandsView) with("rootIndex", rootIndex);
    }
    /** Returns a copy with {@code entries} replaced. */
    default ClientboundCommandsView withEntries(Object entries) {
        return (ClientboundCommandsView) with("entries", entries);
    }
    /** Returns a copy with {@code context} replaced. */
    default ClientboundCommandsView withContext(Object context) {
        return (ClientboundCommandsView) with("context", context);
    }
    /** Returns a copy with {@code builder} replaced. */
    default ClientboundCommandsView withBuilder(Object builder) {
        return (ClientboundCommandsView) with("builder", builder);
    }
    /** Returns a copy with {@code nodes} replaced. */
    default ClientboundCommandsView withNodes(Object nodes) {
        return (ClientboundCommandsView) with("nodes", nodes);
    }
}
