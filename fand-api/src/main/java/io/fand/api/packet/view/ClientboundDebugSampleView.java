package io.fand.api.packet.view;

import io.fand.api.packet.PacketView;
import java.util.UUID;

/** Typed view of {@link ClientboundDebugSamplePacket}. */
public interface ClientboundDebugSampleView extends PacketView {

    default Object sample() {
        return require("sample", Object.class);
    }
    default Object debugSampleType() {
        return require("debugSampleType", Object.class);
    }

    /** Returns a copy with {@code sample} replaced. */
    default ClientboundDebugSampleView withSample(Object sample) {
        return (ClientboundDebugSampleView) with("sample", sample);
    }
    /** Returns a copy with {@code debugSampleType} replaced. */
    default ClientboundDebugSampleView withDebugSampleType(Object debugSampleType) {
        return (ClientboundDebugSampleView) with("debugSampleType", debugSampleType);
    }
}
