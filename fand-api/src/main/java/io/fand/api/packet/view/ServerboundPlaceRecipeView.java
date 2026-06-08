package io.fand.api.packet.view;

import io.fand.api.packet.PacketView;
import java.util.UUID;

/** Typed view of {@link ServerboundPlaceRecipePacket}. */
public interface ServerboundPlaceRecipeView extends PacketView {

    default int containerId() {
        return require("containerId", int.class);
    }
    default Object recipe() {
        return require("recipe", Object.class);
    }
    default boolean useMaxItems() {
        return require("useMaxItems", boolean.class);
    }

    /** Returns a copy with {@code containerId} replaced. */
    default ServerboundPlaceRecipeView withContainerId(int containerId) {
        return (ServerboundPlaceRecipeView) with("containerId", containerId);
    }
    /** Returns a copy with {@code recipe} replaced. */
    default ServerboundPlaceRecipeView withRecipe(Object recipe) {
        return (ServerboundPlaceRecipeView) with("recipe", recipe);
    }
    /** Returns a copy with {@code useMaxItems} replaced. */
    default ServerboundPlaceRecipeView withUseMaxItems(boolean useMaxItems) {
        return (ServerboundPlaceRecipeView) with("useMaxItems", useMaxItems);
    }
}
