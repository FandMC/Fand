package io.fand.api.packet.view;

import io.fand.api.packet.PacketView;
import java.util.UUID;

/** Typed view of {@link ServerboundCustomQueryAnswerPacket}. */
public interface ServerboundCustomQueryAnswerView extends PacketView {

    default int transactionId() {
        return require("transactionId", int.class);
    }
    default Object CustomQueryAnswerPayload() {
        return require("CustomQueryAnswerPayload", Object.class);
    }

    /** Returns a copy with {@code transactionId} replaced. */
    default ServerboundCustomQueryAnswerView withTransactionId(int transactionId) {
        return (ServerboundCustomQueryAnswerView) with("transactionId", transactionId);
    }
    /** Returns a copy with {@code CustomQueryAnswerPayload} replaced. */
    default ServerboundCustomQueryAnswerView withCustomQueryAnswerPayload(Object CustomQueryAnswerPayload) {
        return (ServerboundCustomQueryAnswerView) with("CustomQueryAnswerPayload", CustomQueryAnswerPayload);
    }
}
