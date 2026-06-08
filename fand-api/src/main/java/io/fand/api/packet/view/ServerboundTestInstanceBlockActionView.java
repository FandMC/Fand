package io.fand.api.packet.view;

import io.fand.api.packet.PacketView;
import java.util.UUID;

/** Typed view of {@link ServerboundTestInstanceBlockActionPacket}. */
public interface ServerboundTestInstanceBlockActionView extends PacketView {

    default Object pos() {
        return require("pos", Object.class);
    }
    default Object action() {
        return require("action", Object.class);
    }
    default Object data() {
        return require("data", Object.class);
    }

    /** Returns a copy with {@code pos} replaced. */
    default ServerboundTestInstanceBlockActionView withPos(Object pos) {
        return (ServerboundTestInstanceBlockActionView) with("pos", pos);
    }
    /** Returns a copy with {@code action} replaced. */
    default ServerboundTestInstanceBlockActionView withAction(Object action) {
        return (ServerboundTestInstanceBlockActionView) with("action", action);
    }
    /** Returns a copy with {@code data} replaced. */
    default ServerboundTestInstanceBlockActionView withData(Object data) {
        return (ServerboundTestInstanceBlockActionView) with("data", data);
    }
}
