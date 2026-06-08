package io.fand.api.packet.view;

import io.fand.api.packet.PacketView;
import java.util.UUID;

/** Typed view of {@link ClientboundBossEventPacket}. */
public interface ClientboundBossEventView extends PacketView {

    default UUID id() {
        return require("id", UUID.class);
    }
    default Object operation() {
        return require("operation", Object.class);
    }
    default Object name() {
        return require("name", Object.class);
    }
    default float progress() {
        return require("progress", float.class);
    }
    default Object color() {
        return require("color", Object.class);
    }
    default Object overlay() {
        return require("overlay", Object.class);
    }
    default boolean darkenScreen() {
        return require("darkenScreen", boolean.class);
    }
    default boolean playMusic() {
        return require("playMusic", boolean.class);
    }
    default boolean createWorldFog() {
        return require("createWorldFog", boolean.class);
    }

    /** Returns a copy with {@code id} replaced. */
    default ClientboundBossEventView withId(UUID id) {
        return (ClientboundBossEventView) with("id", id);
    }
    /** Returns a copy with {@code operation} replaced. */
    default ClientboundBossEventView withOperation(Object operation) {
        return (ClientboundBossEventView) with("operation", operation);
    }
    /** Returns a copy with {@code name} replaced. */
    default ClientboundBossEventView withName(Object name) {
        return (ClientboundBossEventView) with("name", name);
    }
    /** Returns a copy with {@code progress} replaced. */
    default ClientboundBossEventView withProgress(float progress) {
        return (ClientboundBossEventView) with("progress", progress);
    }
    /** Returns a copy with {@code color} replaced. */
    default ClientboundBossEventView withColor(Object color) {
        return (ClientboundBossEventView) with("color", color);
    }
    /** Returns a copy with {@code overlay} replaced. */
    default ClientboundBossEventView withOverlay(Object overlay) {
        return (ClientboundBossEventView) with("overlay", overlay);
    }
    /** Returns a copy with {@code darkenScreen} replaced. */
    default ClientboundBossEventView withDarkenScreen(boolean darkenScreen) {
        return (ClientboundBossEventView) with("darkenScreen", darkenScreen);
    }
    /** Returns a copy with {@code playMusic} replaced. */
    default ClientboundBossEventView withPlayMusic(boolean playMusic) {
        return (ClientboundBossEventView) with("playMusic", playMusic);
    }
    /** Returns a copy with {@code createWorldFog} replaced. */
    default ClientboundBossEventView withCreateWorldFog(boolean createWorldFog) {
        return (ClientboundBossEventView) with("createWorldFog", createWorldFog);
    }
}
