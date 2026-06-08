package io.fand.api.packet.view;

import io.fand.api.packet.PacketView;
import java.util.UUID;

/** Typed view of {@link ServerboundChatCommandSignedPacket}. */
public interface ServerboundChatCommandSignedView extends PacketView {

    default String command() {
        return require("command", String.class);
    }
    default Object timeStamp() {
        return require("timeStamp", Object.class);
    }
    default long salt() {
        return require("salt", long.class);
    }
    default Object argumentSignatures() {
        return require("argumentSignatures", Object.class);
    }
    default Object lastSeenMessages() {
        return require("lastSeenMessages", Object.class);
    }

    /** Returns a copy with {@code command} replaced. */
    default ServerboundChatCommandSignedView withCommand(String command) {
        return (ServerboundChatCommandSignedView) with("command", command);
    }
    /** Returns a copy with {@code timeStamp} replaced. */
    default ServerboundChatCommandSignedView withTimeStamp(Object timeStamp) {
        return (ServerboundChatCommandSignedView) with("timeStamp", timeStamp);
    }
    /** Returns a copy with {@code salt} replaced. */
    default ServerboundChatCommandSignedView withSalt(long salt) {
        return (ServerboundChatCommandSignedView) with("salt", salt);
    }
    /** Returns a copy with {@code argumentSignatures} replaced. */
    default ServerboundChatCommandSignedView withArgumentSignatures(Object argumentSignatures) {
        return (ServerboundChatCommandSignedView) with("argumentSignatures", argumentSignatures);
    }
    /** Returns a copy with {@code lastSeenMessages} replaced. */
    default ServerboundChatCommandSignedView withLastSeenMessages(Object lastSeenMessages) {
        return (ServerboundChatCommandSignedView) with("lastSeenMessages", lastSeenMessages);
    }
}
