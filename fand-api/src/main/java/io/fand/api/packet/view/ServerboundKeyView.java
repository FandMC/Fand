package io.fand.api.packet.view;

import io.fand.api.packet.PacketView;
import java.util.UUID;

/** Typed view of {@link ServerboundKeyPacket}. */
public interface ServerboundKeyView extends PacketView {

    default Object keybytes() {
        return require("keybytes", Object.class);
    }
    default Object encryptedChallenge() {
        return require("encryptedChallenge", Object.class);
    }

    /** Returns a copy with {@code keybytes} replaced. */
    default ServerboundKeyView withKeybytes(Object keybytes) {
        return (ServerboundKeyView) with("keybytes", keybytes);
    }
    /** Returns a copy with {@code encryptedChallenge} replaced. */
    default ServerboundKeyView withEncryptedChallenge(Object encryptedChallenge) {
        return (ServerboundKeyView) with("encryptedChallenge", encryptedChallenge);
    }
}
