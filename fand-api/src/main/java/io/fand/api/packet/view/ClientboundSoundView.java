package io.fand.api.packet.view;

import io.fand.api.packet.PacketView;
import java.util.UUID;

/** Typed view of {@link ClientboundSoundPacket}. */
public interface ClientboundSoundView extends PacketView {

    default Object sound() {
        return require("sound", Object.class);
    }
    default Object source() {
        return require("source", Object.class);
    }
    default int x() {
        return require("x", int.class);
    }
    default int y() {
        return require("y", int.class);
    }
    default int z() {
        return require("z", int.class);
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
    default ClientboundSoundView withSound(Object sound) {
        return (ClientboundSoundView) with("sound", sound);
    }
    /** Returns a copy with {@code source} replaced. */
    default ClientboundSoundView withSource(Object source) {
        return (ClientboundSoundView) with("source", source);
    }
    /** Returns a copy with {@code x} replaced. */
    default ClientboundSoundView withX(int x) {
        return (ClientboundSoundView) with("x", x);
    }
    /** Returns a copy with {@code y} replaced. */
    default ClientboundSoundView withY(int y) {
        return (ClientboundSoundView) with("y", y);
    }
    /** Returns a copy with {@code z} replaced. */
    default ClientboundSoundView withZ(int z) {
        return (ClientboundSoundView) with("z", z);
    }
    /** Returns a copy with {@code volume} replaced. */
    default ClientboundSoundView withVolume(float volume) {
        return (ClientboundSoundView) with("volume", volume);
    }
    /** Returns a copy with {@code pitch} replaced. */
    default ClientboundSoundView withPitch(float pitch) {
        return (ClientboundSoundView) with("pitch", pitch);
    }
    /** Returns a copy with {@code seed} replaced. */
    default ClientboundSoundView withSeed(long seed) {
        return (ClientboundSoundView) with("seed", seed);
    }
}
