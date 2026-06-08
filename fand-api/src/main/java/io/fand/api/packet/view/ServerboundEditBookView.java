package io.fand.api.packet.view;

import io.fand.api.packet.PacketView;
import java.util.UUID;

/** Typed view of {@link ServerboundEditBookPacket}. */
public interface ServerboundEditBookView extends PacketView {

    default int slot() {
        return require("slot", int.class);
    }
    default Object pages() {
        return require("pages", Object.class);
    }
    default Object title() {
        return require("title", Object.class);
    }

    /** Returns a copy with {@code slot} replaced. */
    default ServerboundEditBookView withSlot(int slot) {
        return (ServerboundEditBookView) with("slot", slot);
    }
    /** Returns a copy with {@code pages} replaced. */
    default ServerboundEditBookView withPages(Object pages) {
        return (ServerboundEditBookView) with("pages", pages);
    }
    /** Returns a copy with {@code title} replaced. */
    default ServerboundEditBookView withTitle(Object title) {
        return (ServerboundEditBookView) with("title", title);
    }
}
