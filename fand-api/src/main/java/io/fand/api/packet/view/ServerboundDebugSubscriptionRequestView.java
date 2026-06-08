package io.fand.api.packet.view;

import io.fand.api.packet.PacketView;
import java.util.UUID;

/** Typed view of {@link ServerboundDebugSubscriptionRequestPacket}. */
public interface ServerboundDebugSubscriptionRequestView extends PacketView {

    default Object subscriptions() {
        return require("subscriptions", Object.class);
    }

    /** Returns a copy with {@code subscriptions} replaced. */
    default ServerboundDebugSubscriptionRequestView withSubscriptions(Object subscriptions) {
        return (ServerboundDebugSubscriptionRequestView) with("subscriptions", subscriptions);
    }
}
