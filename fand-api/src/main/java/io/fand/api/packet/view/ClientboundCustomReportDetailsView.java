package io.fand.api.packet.view;

import io.fand.api.packet.PacketView;
import java.util.UUID;

/** Typed view of {@link ClientboundCustomReportDetailsPacket}. */
public interface ClientboundCustomReportDetailsView extends PacketView {

    default Object details() {
        return require("details", Object.class);
    }

    /** Returns a copy with {@code details} replaced. */
    default ClientboundCustomReportDetailsView withDetails(Object details) {
        return (ClientboundCustomReportDetailsView) with("details", details);
    }
}
