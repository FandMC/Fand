package io.fand.api.packet.view;

import io.fand.api.packet.PacketView;

/**
 * Typed view of a positioned sound effect. Read-only. The sound event itself is
 * exposed via the dynamic {@code get("sound", ...)} as an opaque value. The
 * coordinate accessors undo the 1/8-block wire scaling.
 */
public interface ClientboundSoundView extends PacketView {

    /** The sound category, e.g. {@code "MASTER"}, {@code "BLOCKS"}, {@code "HOSTILE"}. */
    default String source() {
        return require("source", String.class);
    }

    default double x() {
        return require("x", Integer.class) / 8.0;
    }

    default double y() {
        return require("y", Integer.class) / 8.0;
    }

    default double z() {
        return require("z", Integer.class) / 8.0;
    }

    default float volume() {
        return require("volume", Float.class);
    }

    default float pitch() {
        return require("pitch", Float.class);
    }

    default long seed() {
        return require("seed", Long.class);
    }
}
