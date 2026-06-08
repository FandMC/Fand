package io.fand.api.packet.view;

import io.fand.api.packet.PacketView;
import java.util.UUID;

/** Typed view of {@link ClientboundRecipeBookRemovePacket}. */
public interface ClientboundRecipeBookRemoveView extends PacketView {

    default Object recipes() {
        return require("recipes", Object.class);
    }

    /** Returns a copy with {@code recipes} replaced. */
    default ClientboundRecipeBookRemoveView withRecipes(Object recipes) {
        return (ClientboundRecipeBookRemoveView) with("recipes", recipes);
    }
}
