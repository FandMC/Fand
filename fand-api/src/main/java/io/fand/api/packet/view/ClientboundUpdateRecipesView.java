package io.fand.api.packet.view;

import io.fand.api.packet.PacketView;
import java.util.UUID;

/** Typed view of {@link ClientboundUpdateRecipesPacket}. */
public interface ClientboundUpdateRecipesView extends PacketView {

    default Object itemSets() {
        return require("itemSets", Object.class);
    }
    default Object stonecutterRecipes() {
        return require("stonecutterRecipes", Object.class);
    }

    /** Returns a copy with {@code itemSets} replaced. */
    default ClientboundUpdateRecipesView withItemSets(Object itemSets) {
        return (ClientboundUpdateRecipesView) with("itemSets", itemSets);
    }
    /** Returns a copy with {@code stonecutterRecipes} replaced. */
    default ClientboundUpdateRecipesView withStonecutterRecipes(Object stonecutterRecipes) {
        return (ClientboundUpdateRecipesView) with("stonecutterRecipes", stonecutterRecipes);
    }
}
