package io.fand.api.packet.view;

import io.fand.api.packet.PacketView;
import java.util.UUID;

/** Typed view of {@link ClientboundCodeOfConductPacket}. */
public interface ClientboundCodeOfConductView extends PacketView {

    default String codeOfConduct() {
        return require("codeOfConduct", String.class);
    }

    /** Returns a copy with {@code codeOfConduct} replaced. */
    default ClientboundCodeOfConductView withCodeOfConduct(String codeOfConduct) {
        return (ClientboundCodeOfConductView) with("codeOfConduct", codeOfConduct);
    }
}
