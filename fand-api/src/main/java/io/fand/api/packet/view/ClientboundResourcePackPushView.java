package io.fand.api.packet.view;

import io.fand.api.packet.PacketView;
import java.util.UUID;

/** Typed view of {@link ClientboundResourcePackPushPacket}. */
public interface ClientboundResourcePackPushView extends PacketView {

    default UUID id() {
        return require("id", UUID.class);
    }
    default String url() {
        return require("url", String.class);
    }
    default String hash() {
        return require("hash", String.class);
    }
    default boolean required() {
        return require("required", boolean.class);
    }
    default Object prompt() {
        return require("prompt", Object.class);
    }

    /** Returns a copy with {@code id} replaced. */
    default ClientboundResourcePackPushView withId(UUID id) {
        return (ClientboundResourcePackPushView) with("id", id);
    }
    /** Returns a copy with {@code url} replaced. */
    default ClientboundResourcePackPushView withUrl(String url) {
        return (ClientboundResourcePackPushView) with("url", url);
    }
    /** Returns a copy with {@code hash} replaced. */
    default ClientboundResourcePackPushView withHash(String hash) {
        return (ClientboundResourcePackPushView) with("hash", hash);
    }
    /** Returns a copy with {@code required} replaced. */
    default ClientboundResourcePackPushView withRequired(boolean required) {
        return (ClientboundResourcePackPushView) with("required", required);
    }
    /** Returns a copy with {@code prompt} replaced. */
    default ClientboundResourcePackPushView withPrompt(Object prompt) {
        return (ClientboundResourcePackPushView) with("prompt", prompt);
    }
}
