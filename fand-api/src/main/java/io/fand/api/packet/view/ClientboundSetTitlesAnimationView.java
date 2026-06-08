package io.fand.api.packet.view;

import io.fand.api.packet.PacketView;
import java.util.UUID;

/** Typed view of {@link ClientboundSetTitlesAnimationPacket}. */
public interface ClientboundSetTitlesAnimationView extends PacketView {

    default int fadeIn() {
        return require("fadeIn", int.class);
    }
    default int stay() {
        return require("stay", int.class);
    }
    default int fadeOut() {
        return require("fadeOut", int.class);
    }

    /** Returns a copy with {@code fadeIn} replaced. */
    default ClientboundSetTitlesAnimationView withFadeIn(int fadeIn) {
        return (ClientboundSetTitlesAnimationView) with("fadeIn", fadeIn);
    }
    /** Returns a copy with {@code stay} replaced. */
    default ClientboundSetTitlesAnimationView withStay(int stay) {
        return (ClientboundSetTitlesAnimationView) with("stay", stay);
    }
    /** Returns a copy with {@code fadeOut} replaced. */
    default ClientboundSetTitlesAnimationView withFadeOut(int fadeOut) {
        return (ClientboundSetTitlesAnimationView) with("fadeOut", fadeOut);
    }
}
