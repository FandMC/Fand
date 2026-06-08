package io.fand.api.packet.view;

import io.fand.api.packet.PacketView;
import java.util.UUID;

/** Typed view of {@link ClientboundSoundEntityPacket}. */
public interface ClientboundSoundEntityView extends PacketView {

    default Object sound() {
        return require("sound", Object.class);
    }
    default Object source() {
        return require("source", Object.class);
    }
    default int id() {
        return require("id", int.class);
    }
    default float volume() {
        return require("volume", float.class);
    }
    default float pitch() {
        return require("pitch", float.class);
    }
    default long seed() {
        return require("seed", long.class);
    }

    /** Returns a copy with {@code sound} replaced. */
    default ClientboundSoundEntityView withSound(Object sound) {
        return (ClientboundSoundEntityView) with("sound", sound);
    }
    /** Returns a copy with {@code source} replaced. */
    default ClientboundSoundEntityView withSource(Object source) {
        return (ClientboundSoundEntityView) with("source", source);
    }
    /** Returns a copy with {@code id} replaced. */
    default ClientboundSoundEntityView withId(int id) {
        return (ClientboundSoundEntityView) with("id", id);
    }
    /** Returns a copy with {@code volume} replaced. */
    default ClientboundSoundEntityView withVolume(float volume) {
        return (ClientboundSoundEntityView) with("volume", volume);
    }
    /** Returns a copy with {@code pitch} replaced. */
    default ClientboundSoundEntityView withPitch(float pitch) {
        return (ClientboundSoundEntityView) with("pitch", pitch);
    }
    /** Returns a copy with {@code seed} replaced. */
    default ClientboundSoundEntityView withSeed(long seed) {
        return (ClientboundSoundEntityView) with("seed", seed);
    }
}
