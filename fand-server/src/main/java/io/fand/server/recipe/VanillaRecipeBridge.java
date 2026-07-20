package io.fand.server.recipe;

import io.fand.api.recipe.Recipe;
import io.fand.api.item.ItemStack;
import io.fand.server.item.FandItemStacks;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Optional;
import java.util.function.Supplier;
import net.kyori.adventure.key.Key;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.players.PlayerList;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import org.jspecify.annotations.Nullable;

final class VanillaRecipeBridge implements RecipeBridge {

    private final MinecraftServer server;
    private RecipeManager appliedManager;
    private LinkedHashMap<ResourceKey<net.minecraft.world.item.crafting.Recipe<?>>, RecipeHolder<?>> baseRecipes =
            new LinkedHashMap<>();

    private VanillaRecipeBridge(MinecraftServer server) {
        this.server = server;
    }

    static RecipeBridge create(Object minecraftServer) {
        if (minecraftServer instanceof MinecraftServer server) {
            return new VanillaRecipeBridge(server);
        }
        throw new IllegalArgumentException("Expected MinecraftServer, got " + minecraftServer.getClass().getName());
    }

    @Override
    public Collection<? extends Recipe> all(LinkedHashMap<Key, Recipe> customRecipes) {
        refreshIfManagerChanged(customRecipes.values(), false);
        return server.getRecipeManager().getRecipes().stream()
                .map(holder -> customRecipes.getOrDefault(
                        FandRecipes.key(holder.id().identifier()),
                        FandRecipes.fromVanilla(holder)))
                .toList();
    }

    @Override
    public Optional<? extends Recipe> find(Key key, Collection<Recipe> customRecipes) {
        var custom = customRecipes.stream()
                .filter(recipe -> recipe.key().equals(key))
                .findFirst();
        if (custom.isPresent()) {
            return custom;
        }

        refreshIfManagerChanged(customRecipes, false);
        return server.getRecipeManager()
                .byKey(FandRecipes.recipeKey(key))
                .map(FandRecipes::fromVanilla);
    }

    @Override
    public Optional<ItemStack> brew(ItemStack potion, ItemStack ingredient) {
        if (potion.empty() || ingredient.empty()) {
            return Optional.empty();
        }
        var source = FandItemStacks.toVanilla(potion.withAmount(1));
        var reagent = FandItemStacks.toVanilla(ingredient.withAmount(1));
        var brewing = server.potionBrewing();
        if (!brewing.hasMix(source, reagent)) {
            return Optional.empty();
        }
        return Optional.of(FandItemStacks.fromVanilla(brewing.mix(reagent, source)));
    }

    @Override
    public void refreshAndApply(Collection<Recipe> customRecipes, boolean synchronizePlayers) {
        var manager = server.getRecipeManager();
        baseRecipes = readRecipeMap(manager);
        appliedManager = manager;
        apply(customRecipes, synchronizePlayers);
    }

    @Override
    public void refreshIfManagerChanged(Collection<Recipe> customRecipes, boolean synchronizePlayers) {
        if (server.getRecipeManager() != appliedManager) {
            refreshAndApply(customRecipes, synchronizePlayers);
        }
    }

    @Override
    public void apply(Collection<Recipe> customRecipes, boolean synchronizePlayers) {
        var manager = server.getRecipeManager();
        if (manager != appliedManager) {
            refreshAndApply(customRecipes, synchronizePlayers);
            return;
        }

        var recipes = new LinkedHashMap<>(baseRecipes);
        for (var recipe : customRecipes) {
            var holder = FandRecipes.toVanilla(recipe, server.registryAccess());
            recipes.put(holder.id(), holder);
        }
        manager.fand$replaceRecipes(recipes.values(), server.getWorldData().enabledFeatures());

        var playerList = playerListOrNull();
        if (synchronizePlayers && playerList != null && playerList.getPlayerCount() > 0) {
            playerList.reloadResources();
        }
    }

    @Override
    public <T> T runOnServerThread(Supplier<T> supplier) {
        if (server.isSameThread()) {
            return supplier.get();
        }
        return server.submit(supplier).join();
    }

    private @Nullable PlayerList playerListOrNull() {
        return server.getPlayerList();
    }

    private static LinkedHashMap<ResourceKey<net.minecraft.world.item.crafting.Recipe<?>>, RecipeHolder<?>> readRecipeMap(
            RecipeManager manager
    ) {
        var recipes = new LinkedHashMap<ResourceKey<net.minecraft.world.item.crafting.Recipe<?>>, RecipeHolder<?>>();
        for (var holder : manager.getRecipes()) {
            recipes.put(holder.id(), holder);
        }
        return recipes;
    }
}
