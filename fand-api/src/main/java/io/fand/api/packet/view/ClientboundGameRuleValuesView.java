package io.fand.api.packet.view;

import io.fand.api.packet.PacketView;
import java.util.UUID;

/** Typed view of {@link ClientboundGameRuleValuesPacket}. */
public interface ClientboundGameRuleValuesView extends PacketView {

    default Object values() {
        return require("values", Object.class);
    }

    /** Returns a copy with {@code values} replaced. */
    default ClientboundGameRuleValuesView withValues(Object values) {
        return (ClientboundGameRuleValuesView) with("values", values);
    }
}
