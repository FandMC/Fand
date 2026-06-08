package io.fand.api.packet.view;

import io.fand.api.packet.PacketView;
import java.util.UUID;

/** Typed view of {@link ClientboundUpdateAdvancementsPacket}. */
public interface ClientboundUpdateAdvancementsView extends PacketView {

    default boolean reset() {
        return require("reset", boolean.class);
    }
    default Object added() {
        return require("added", Object.class);
    }
    default Object removed() {
        return require("removed", Object.class);
    }
    default boolean showAdvancements() {
        return require("showAdvancements", boolean.class);
    }

    /** Returns a copy with {@code reset} replaced. */
    default ClientboundUpdateAdvancementsView withReset(boolean reset) {
        return (ClientboundUpdateAdvancementsView) with("reset", reset);
    }
    /** Returns a copy with {@code added} replaced. */
    default ClientboundUpdateAdvancementsView withAdded(Object added) {
        return (ClientboundUpdateAdvancementsView) with("added", added);
    }
    /** Returns a copy with {@code removed} replaced. */
    default ClientboundUpdateAdvancementsView withRemoved(Object removed) {
        return (ClientboundUpdateAdvancementsView) with("removed", removed);
    }
    /** Returns a copy with {@code showAdvancements} replaced. */
    default ClientboundUpdateAdvancementsView withShowAdvancements(boolean showAdvancements) {
        return (ClientboundUpdateAdvancementsView) with("showAdvancements", showAdvancements);
    }
}
