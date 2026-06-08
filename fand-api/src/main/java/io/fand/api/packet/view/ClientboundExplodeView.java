package io.fand.api.packet.view;

import io.fand.api.packet.PacketView;
import java.util.UUID;

/** Typed view of {@link ClientboundExplodePacket}. */
public interface ClientboundExplodeView extends PacketView {

    default Object center() {
        return require("center", Object.class);
    }
    default float radius() {
        return require("radius", float.class);
    }
    default int blockCount() {
        return require("blockCount", int.class);
    }
    default Object playerKnockback() {
        return require("playerKnockback", Object.class);
    }
    default Object explosionParticle() {
        return require("explosionParticle", Object.class);
    }
    default Object explosionSound() {
        return require("explosionSound", Object.class);
    }
    default Object blockParticles() {
        return require("blockParticles", Object.class);
    }

    /** Returns a copy with {@code center} replaced. */
    default ClientboundExplodeView withCenter(Object center) {
        return (ClientboundExplodeView) with("center", center);
    }
    /** Returns a copy with {@code radius} replaced. */
    default ClientboundExplodeView withRadius(float radius) {
        return (ClientboundExplodeView) with("radius", radius);
    }
    /** Returns a copy with {@code blockCount} replaced. */
    default ClientboundExplodeView withBlockCount(int blockCount) {
        return (ClientboundExplodeView) with("blockCount", blockCount);
    }
    /** Returns a copy with {@code playerKnockback} replaced. */
    default ClientboundExplodeView withPlayerKnockback(Object playerKnockback) {
        return (ClientboundExplodeView) with("playerKnockback", playerKnockback);
    }
    /** Returns a copy with {@code explosionParticle} replaced. */
    default ClientboundExplodeView withExplosionParticle(Object explosionParticle) {
        return (ClientboundExplodeView) with("explosionParticle", explosionParticle);
    }
    /** Returns a copy with {@code explosionSound} replaced. */
    default ClientboundExplodeView withExplosionSound(Object explosionSound) {
        return (ClientboundExplodeView) with("explosionSound", explosionSound);
    }
    /** Returns a copy with {@code blockParticles} replaced. */
    default ClientboundExplodeView withBlockParticles(Object blockParticles) {
        return (ClientboundExplodeView) with("blockParticles", blockParticles);
    }
}
