package io.fand.api.packet.view;

import io.fand.api.packet.PacketView;

/**
 * Typed view of a particle spawn. Read-only. The particle type/options are
 * exposed via the dynamic {@code get("particle", ...)} as an opaque value.
 */
public interface ClientboundLevelParticlesView extends PacketView {

    default double x() {
        return require("x", Double.class);
    }

    default double y() {
        return require("y", Double.class);
    }

    default double z() {
        return require("z", Double.class);
    }

    /** Number of particles to spawn. */
    default int count() {
        return require("count", Integer.class);
    }

    /** Particle speed multiplier. */
    default float maxSpeed() {
        return require("maxSpeed", Float.class);
    }
}
