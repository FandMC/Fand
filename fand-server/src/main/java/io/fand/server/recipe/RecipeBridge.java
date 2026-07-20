package io.fand.server.recipe;

import io.fand.api.recipe.Recipe;
import io.fand.api.item.ItemStack;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Optional;
import java.util.function.Supplier;
import net.kyori.adventure.key.Key;

interface RecipeBridge {

    Collection<? extends Recipe> all(LinkedHashMap<Key, Recipe> customRecipes);

    Optional<? extends Recipe> find(Key key, Collection<Recipe> customRecipes);

    Optional<ItemStack> brew(ItemStack potion, ItemStack ingredient);

    void refreshAndApply(Collection<Recipe> customRecipes, boolean synchronizePlayers);

    void refreshIfManagerChanged(Collection<Recipe> customRecipes, boolean synchronizePlayers);

    void apply(Collection<Recipe> customRecipes, boolean synchronizePlayers);

    <T> T runOnServerThread(Supplier<T> supplier);
}
