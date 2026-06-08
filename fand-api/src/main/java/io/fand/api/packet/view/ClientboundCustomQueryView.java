package io.fand.api.packet.view;

import io.fand.api.packet.PacketView;
import java.util.UUID;

/** Typed view of {@link ClientboundCustomQueryPacket}. */
public interface ClientboundCustomQueryView extends PacketView {

    default int transactionId() {
        return require("transactionId", int.class);
    }
    default Object payload() {
        return require("payload", Object.class);
    }

    /** Returns a copy with {@code transactionId} replaced. */
    default ClientboundCustomQueryView withTransactionId(int transactionId) {
        return (ClientboundCustomQueryView) with("transactionId", transactionId);
    }
    /** Returns a copy with {@code payload} replaced. */
    default ClientboundCustomQueryView withPayload(Object payload) {
        return (ClientboundCustomQueryView) with("payload", payload);
    }
}
