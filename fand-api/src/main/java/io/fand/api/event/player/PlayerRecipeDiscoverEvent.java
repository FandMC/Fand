package io.fand.api.event.player;

import io.fand.api.entity.Player;
import io.fand.api.event.Cancellable;
import io.fand.api.event.Event;
import io.fand.api.recipe.Recipe;
import java.util.List;
import java.util.Objects;

/**
 * Fired before one or more recipes are added to a player's recipe book.
 */
public final class PlayerRecipeDiscoverEvent implements Event, Cancellable {

    private final Player player;
    private final List<Recipe> recipes;
    private boolean cancelled;

    public PlayerRecipeDiscoverEvent(Player player, List<? extends Recipe> recipes) {
        this.player = Objects.requireNonNull(player, "player");
        this.recipes = List.copyOf(Objects.requireNonNull(recipes, "recipes"));
    }

    public Player player() {
        return player;
    }

    public List<Recipe> recipes() {
        return recipes;
    }

    @Override
    public boolean cancelled() {
        return cancelled;
    }

    @Override
    public void setCancelled(boolean cancelled) {
        this.cancelled = cancelled;
    }
}
