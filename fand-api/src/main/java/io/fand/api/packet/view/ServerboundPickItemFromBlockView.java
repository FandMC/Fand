package io.fand.api.packet.view;

import io.fand.api.packet.PacketView;
import java.util.UUID;

/** Typed view of {@link ServerboundPickItemFromBlockPacket}. */
public interface ServerboundPickItemFromBlockView extends PacketView {

    default Object pos() {
        return require("pos", Object.class);
    }
    default boolean includeData() {
        return require("includeData", boolean.class);
    }

    /** Returns a copy with {@code pos} replaced. */
    default ServerboundPickItemFromBlockView withPos(Object pos) {
        return (ServerboundPickItemFromBlockView) with("pos", pos);
    }
    /** Returns a copy with {@code includeData} replaced. */
    default ServerboundPickItemFromBlockView withIncludeData(boolean includeData) {
        return (ServerboundPickItemFromBlockView) with("includeData", includeData);
    }
}
