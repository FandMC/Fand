package io.fand.api.packet.view;

import io.fand.api.packet.PacketView;
import java.util.UUID;

/** Typed view of {@link ServerboundBlockEntityTagQueryPacket}. */
public interface ServerboundBlockEntityTagQueryView extends PacketView {

    default int transactionId() {
        return require("transactionId", int.class);
    }
    default Object pos() {
        return require("pos", Object.class);
    }

    /** Returns a copy with {@code transactionId} replaced. */
    default ServerboundBlockEntityTagQueryView withTransactionId(int transactionId) {
        return (ServerboundBlockEntityTagQueryView) with("transactionId", transactionId);
    }
    /** Returns a copy with {@code pos} replaced. */
    default ServerboundBlockEntityTagQueryView withPos(Object pos) {
        return (ServerboundBlockEntityTagQueryView) with("pos", pos);
    }
}
