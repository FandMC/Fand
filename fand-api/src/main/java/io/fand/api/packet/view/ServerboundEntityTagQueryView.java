package io.fand.api.packet.view;

import io.fand.api.packet.PacketView;
import java.util.UUID;

/** Typed view of {@link ServerboundEntityTagQueryPacket}. */
public interface ServerboundEntityTagQueryView extends PacketView {

    default int transactionId() {
        return require("transactionId", int.class);
    }
    default int entityId() {
        return require("entityId", int.class);
    }

    /** Returns a copy with {@code transactionId} replaced. */
    default ServerboundEntityTagQueryView withTransactionId(int transactionId) {
        return (ServerboundEntityTagQueryView) with("transactionId", transactionId);
    }
    /** Returns a copy with {@code entityId} replaced. */
    default ServerboundEntityTagQueryView withEntityId(int entityId) {
        return (ServerboundEntityTagQueryView) with("entityId", entityId);
    }
}
