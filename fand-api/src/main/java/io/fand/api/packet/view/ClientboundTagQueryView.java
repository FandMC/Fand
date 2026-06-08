package io.fand.api.packet.view;

import io.fand.api.packet.PacketView;
import java.util.UUID;

/** Typed view of {@link ClientboundTagQueryPacket}. */
public interface ClientboundTagQueryView extends PacketView {

    default int transactionId() {
        return require("transactionId", int.class);
    }

    /** Returns a copy with {@code transactionId} replaced. */
    default ClientboundTagQueryView withTransactionId(int transactionId) {
        return (ClientboundTagQueryView) with("transactionId", transactionId);
    }
}
