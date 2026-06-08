package io.fand.api.packet.view;

import io.fand.api.packet.PacketView;
import java.util.UUID;

/** Typed view of {@link ServerboundInteractPacket}. */
public interface ServerboundInteractView extends PacketView {

    default int entityId() {
        return require("entityId", int.class);
    }
    default Object hand() {
        return require("hand", Object.class);
    }
    default Object location() {
        return require("location", Object.class);
    }
    default boolean usingSecondaryAction() {
        return require("usingSecondaryAction", boolean.class);
    }

    /** Returns a copy with {@code entityId} replaced. */
    default ServerboundInteractView withEntityId(int entityId) {
        return (ServerboundInteractView) with("entityId", entityId);
    }
    /** Returns a copy with {@code hand} replaced. */
    default ServerboundInteractView withHand(Object hand) {
        return (ServerboundInteractView) with("hand", hand);
    }
    /** Returns a copy with {@code location} replaced. */
    default ServerboundInteractView withLocation(Object location) {
        return (ServerboundInteractView) with("location", location);
    }
    /** Returns a copy with {@code usingSecondaryAction} replaced. */
    default ServerboundInteractView withUsingSecondaryAction(boolean usingSecondaryAction) {
        return (ServerboundInteractView) with("usingSecondaryAction", usingSecondaryAction);
    }
}
