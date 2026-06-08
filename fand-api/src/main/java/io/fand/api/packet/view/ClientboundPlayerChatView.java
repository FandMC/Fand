package io.fand.api.packet.view;

import io.fand.api.packet.PacketView;
import java.util.UUID;

/** Typed view of {@link ClientboundPlayerChatPacket}. */
public interface ClientboundPlayerChatView extends PacketView {

    default int globalIndex() {
        return require("globalIndex", int.class);
    }
    default UUID sender() {
        return require("sender", UUID.class);
    }
    default int index() {
        return require("index", int.class);
    }
    default Object MessageSignature() {
        return require("MessageSignature", Object.class);
    }
    default Object body() {
        return require("body", Object.class);
    }
    default Object Component() {
        return require("Component", Object.class);
    }
    default Object filterMask() {
        return require("filterMask", Object.class);
    }
    default Object chatType() {
        return require("chatType", Object.class);
    }

    /** Returns a copy with {@code globalIndex} replaced. */
    default ClientboundPlayerChatView withGlobalIndex(int globalIndex) {
        return (ClientboundPlayerChatView) with("globalIndex", globalIndex);
    }
    /** Returns a copy with {@code sender} replaced. */
    default ClientboundPlayerChatView withSender(UUID sender) {
        return (ClientboundPlayerChatView) with("sender", sender);
    }
    /** Returns a copy with {@code index} replaced. */
    default ClientboundPlayerChatView withIndex(int index) {
        return (ClientboundPlayerChatView) with("index", index);
    }
    /** Returns a copy with {@code MessageSignature} replaced. */
    default ClientboundPlayerChatView withMessageSignature(Object MessageSignature) {
        return (ClientboundPlayerChatView) with("MessageSignature", MessageSignature);
    }
    /** Returns a copy with {@code body} replaced. */
    default ClientboundPlayerChatView withBody(Object body) {
        return (ClientboundPlayerChatView) with("body", body);
    }
    /** Returns a copy with {@code Component} replaced. */
    default ClientboundPlayerChatView withComponent(Object Component) {
        return (ClientboundPlayerChatView) with("Component", Component);
    }
    /** Returns a copy with {@code filterMask} replaced. */
    default ClientboundPlayerChatView withFilterMask(Object filterMask) {
        return (ClientboundPlayerChatView) with("filterMask", filterMask);
    }
    /** Returns a copy with {@code chatType} replaced. */
    default ClientboundPlayerChatView withChatType(Object chatType) {
        return (ClientboundPlayerChatView) with("chatType", chatType);
    }
}
