package io.fand.api.packet.view;

import io.fand.api.packet.PacketView;
import java.util.UUID;

/** Typed view of {@link ClientboundLevelParticlesPacket}. */
public interface ClientboundLevelParticlesView extends PacketView {

    default double x() {
        return require("x", double.class);
    }
    default double y() {
        return require("y", double.class);
    }
    default double z() {
        return require("z", double.class);
    }
    default float xDist() {
        return require("xDist", float.class);
    }
    default float yDist() {
        return require("yDist", float.class);
    }
    default float zDist() {
        return require("zDist", float.class);
    }
    default float maxSpeed() {
        return require("maxSpeed", float.class);
    }
    default int count() {
        return require("count", int.class);
    }
    default boolean overrideLimiter() {
        return require("overrideLimiter", boolean.class);
    }
    default boolean alwaysShow() {
        return require("alwaysShow", boolean.class);
    }
    default Object particle() {
        return require("particle", Object.class);
    }

    /** Returns a copy with {@code x} replaced. */
    default ClientboundLevelParticlesView withX(double x) {
        return (ClientboundLevelParticlesView) with("x", x);
    }
    /** Returns a copy with {@code y} replaced. */
    default ClientboundLevelParticlesView withY(double y) {
        return (ClientboundLevelParticlesView) with("y", y);
    }
    /** Returns a copy with {@code z} replaced. */
    default ClientboundLevelParticlesView withZ(double z) {
        return (ClientboundLevelParticlesView) with("z", z);
    }
    /** Returns a copy with {@code xDist} replaced. */
    default ClientboundLevelParticlesView withXDist(float xDist) {
        return (ClientboundLevelParticlesView) with("xDist", xDist);
    }
    /** Returns a copy with {@code yDist} replaced. */
    default ClientboundLevelParticlesView withYDist(float yDist) {
        return (ClientboundLevelParticlesView) with("yDist", yDist);
    }
    /** Returns a copy with {@code zDist} replaced. */
    default ClientboundLevelParticlesView withZDist(float zDist) {
        return (ClientboundLevelParticlesView) with("zDist", zDist);
    }
    /** Returns a copy with {@code maxSpeed} replaced. */
    default ClientboundLevelParticlesView withMaxSpeed(float maxSpeed) {
        return (ClientboundLevelParticlesView) with("maxSpeed", maxSpeed);
    }
    /** Returns a copy with {@code count} replaced. */
    default ClientboundLevelParticlesView withCount(int count) {
        return (ClientboundLevelParticlesView) with("count", count);
    }
    /** Returns a copy with {@code overrideLimiter} replaced. */
    default ClientboundLevelParticlesView withOverrideLimiter(boolean overrideLimiter) {
        return (ClientboundLevelParticlesView) with("overrideLimiter", overrideLimiter);
    }
    /** Returns a copy with {@code alwaysShow} replaced. */
    default ClientboundLevelParticlesView withAlwaysShow(boolean alwaysShow) {
        return (ClientboundLevelParticlesView) with("alwaysShow", alwaysShow);
    }
    /** Returns a copy with {@code particle} replaced. */
    default ClientboundLevelParticlesView withParticle(Object particle) {
        return (ClientboundLevelParticlesView) with("particle", particle);
    }
}
