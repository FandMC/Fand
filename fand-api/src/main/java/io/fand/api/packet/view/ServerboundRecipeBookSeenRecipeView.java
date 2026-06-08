package io.fand.api.packet.view;

import io.fand.api.packet.PacketView;
import java.util.UUID;

/** Typed view of {@link ServerboundRecipeBookSeenRecipePacket}. */
public interface ServerboundRecipeBookSeenRecipeView extends PacketView {

    default Object recipe() {
        return require("recipe", Object.class);
    }

    /** Returns a copy with {@code recipe} replaced. */
    default ServerboundRecipeBookSeenRecipeView withRecipe(Object recipe) {
        return (ServerboundRecipeBookSeenRecipeView) with("recipe", recipe);
    }
}
