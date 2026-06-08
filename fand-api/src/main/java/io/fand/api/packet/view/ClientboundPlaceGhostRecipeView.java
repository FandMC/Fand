package io.fand.api.packet.view;

import io.fand.api.packet.PacketView;
import java.util.UUID;

/** Typed view of {@link ClientboundPlaceGhostRecipePacket}. */
public interface ClientboundPlaceGhostRecipeView extends PacketView {

    default int containerId() {
        return require("containerId", int.class);
    }
    default Object recipeDisplay() {
        return require("recipeDisplay", Object.class);
    }

    /** Returns a copy with {@code containerId} replaced. */
    default ClientboundPlaceGhostRecipeView withContainerId(int containerId) {
        return (ClientboundPlaceGhostRecipeView) with("containerId", containerId);
    }
    /** Returns a copy with {@code recipeDisplay} replaced. */
    default ClientboundPlaceGhostRecipeView withRecipeDisplay(Object recipeDisplay) {
        return (ClientboundPlaceGhostRecipeView) with("recipeDisplay", recipeDisplay);
    }
}
