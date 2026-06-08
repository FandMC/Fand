package io.fand.api.packet.view;

import io.fand.api.packet.PacketView;
import java.util.UUID;

/** Typed view of {@link ClientboundSetScorePacket}. */
public interface ClientboundSetScoreView extends PacketView {

    default String owner() {
        return require("owner", String.class);
    }
    default String objectiveName() {
        return require("objectiveName", String.class);
    }
    default int score() {
        return require("score", int.class);
    }
    default Object display() {
        return require("display", Object.class);
    }
    default Object numberFormat() {
        return require("numberFormat", Object.class);
    }

    /** Returns a copy with {@code owner} replaced. */
    default ClientboundSetScoreView withOwner(String owner) {
        return (ClientboundSetScoreView) with("owner", owner);
    }
    /** Returns a copy with {@code objectiveName} replaced. */
    default ClientboundSetScoreView withObjectiveName(String objectiveName) {
        return (ClientboundSetScoreView) with("objectiveName", objectiveName);
    }
    /** Returns a copy with {@code score} replaced. */
    default ClientboundSetScoreView withScore(int score) {
        return (ClientboundSetScoreView) with("score", score);
    }
    /** Returns a copy with {@code display} replaced. */
    default ClientboundSetScoreView withDisplay(Object display) {
        return (ClientboundSetScoreView) with("display", display);
    }
    /** Returns a copy with {@code numberFormat} replaced. */
    default ClientboundSetScoreView withNumberFormat(Object numberFormat) {
        return (ClientboundSetScoreView) with("numberFormat", numberFormat);
    }
}
