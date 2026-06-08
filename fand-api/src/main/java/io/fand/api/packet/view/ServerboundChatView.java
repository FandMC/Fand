package io.fand.api.packet.view;

import io.fand.api.packet.PacketView;
import java.util.UUID;

/** Typed view of {@link ServerboundChatPacket}. */
public interface ServerboundChatView extends PacketView {

    default String message() {
        return require("message", String.class);
    }
    default Object timeStamp() {
        return require("timeStamp", Object.class);
    }
    default long salt() {
        return require("salt", long.class);
    }
    default Object MessageSignature() {
        return require("MessageSignature", Object.class);
    }
    default Object lastSeenMessages() {
        return require("lastSeenMessages", Object.class);
    }

    /** Returns a copy with {@code message} replaced. */
    default ServerboundChatView withMessage(String message) {
        return (ServerboundChatView) with("message", message);
    }
    /** Returns a copy with {@code timeStamp} replaced. */
    default ServerboundChatView withTimeStamp(Object timeStamp) {
        return (ServerboundChatView) with("timeStamp", timeStamp);
    }
    /** Returns a copy with {@code salt} replaced. */
    default ServerboundChatView withSalt(long salt) {
        return (ServerboundChatView) with("salt", salt);
    }
    /** Returns a copy with {@code MessageSignature} replaced. */
    default ServerboundChatView withMessageSignature(Object MessageSignature) {
        return (ServerboundChatView) with("MessageSignature", MessageSignature);
    }
    /** Returns a copy with {@code lastSeenMessages} replaced. */
    default ServerboundChatView withLastSeenMessages(Object lastSeenMessages) {
        return (ServerboundChatView) with("lastSeenMessages", lastSeenMessages);
    }
}
