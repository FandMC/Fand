package io.fand.api.packet.view;

import io.fand.api.packet.PacketView;
import java.util.UUID;

/** Typed view of {@link ClientboundPlayerInfoUpdatePacket}. */
public interface ClientboundPlayerInfoUpdateView extends PacketView {

    default Object actions() {
        return require("actions", Object.class);
    }
    default Object entries() {
        return require("entries", Object.class);
    }
    default Object reader() {
        return require("reader", Object.class);
    }
    default Object writer() {
        return require("writer", Object.class);
    }
    default UUID profileId() {
        return require("profileId", UUID.class);
    }

    /** Returns a copy with {@code actions} replaced. */
    default ClientboundPlayerInfoUpdateView withActions(Object actions) {
        return (ClientboundPlayerInfoUpdateView) with("actions", actions);
    }
    /** Returns a copy with {@code entries} replaced. */
    default ClientboundPlayerInfoUpdateView withEntries(Object entries) {
        return (ClientboundPlayerInfoUpdateView) with("entries", entries);
    }
    /** Returns a copy with {@code reader} replaced. */
    default ClientboundPlayerInfoUpdateView withReader(Object reader) {
        return (ClientboundPlayerInfoUpdateView) with("reader", reader);
    }
    /** Returns a copy with {@code writer} replaced. */
    default ClientboundPlayerInfoUpdateView withWriter(Object writer) {
        return (ClientboundPlayerInfoUpdateView) with("writer", writer);
    }
    /** Returns a copy with {@code profileId} replaced. */
    default ClientboundPlayerInfoUpdateView withProfileId(UUID profileId) {
        return (ClientboundPlayerInfoUpdateView) with("profileId", profileId);
    }
}
