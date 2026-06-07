package io.fand.server.recipe;

import io.fand.api.recipe.Recipe;
import io.fand.api.recipe.RecipeRegistration;
import io.fand.api.recipe.RecipeRegistry;
import io.fand.api.recipe.RecipeType;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;
import net.kyori.adventure.key.Key;

public final class FandRecipeRegistry implements RecipeRegistry {

    private final Object lock = new Object();
    private final LinkedHashMap<Key, CustomRecipe> customRecipes = new LinkedHashMap<>();
    private final AtomicReference<RecipeBridge> bridge = new AtomicReference<>();
    private final AtomicLong sequence = new AtomicLong();
    private boolean live;

    public void bind(Object minecraftServer) {
        Objects.requireNonNull(minecraftServer, "minecraftServer");
        if (!bridge.compareAndSet(null, VanillaRecipeBridge.create(minecraftServer))) {
            throw new IllegalStateException("Recipe registry is already bound to a Minecraft server");
        }
    }

    public void applyLoadedRecipes() {
        var vanilla = requireBridge();
        synchronized (lock) {
            live = true;
        }
        runOnServerThread(() -> vanilla.refreshAndApply(customRecipes(), false));
    }

    public void tick() {
        var vanilla = bridge.get();
        if (vanilla == null || !isLive()) {
            return;
        }
        vanilla.refreshIfManagerChanged(customRecipes(), true);
    }

    @Override
    public Collection<? extends Recipe> all() {
        var vanilla = bridge.get();
        if (vanilla == null || !isLive()) {
            return customRecipes();
        }
        return runOnServerThread(() -> vanilla.all(customRecipeSnapshot()));
    }

    @Override
    public Optional<? extends Recipe> find(Key key) {
        Objects.requireNonNull(key, "key");
        synchronized (lock) {
            var custom = customRecipes.get(key);
            if (custom != null) {
                return Optional.of(custom.recipe());
            }
        }

        var vanilla = bridge.get();
        if (vanilla == null || !isLive()) {
            return Optional.empty();
        }
        return runOnServerThread(() -> vanilla.find(key, customRecipes()));
    }

    @Override
    public Collection<? extends Recipe> byType(RecipeType type) {
        Objects.requireNonNull(type, "type");
        return all().stream()
                .filter(recipe -> recipe.type() == type)
                .toList();
    }

    @Override
    public RecipeRegistration register(Recipe recipe) {
        Objects.requireNonNull(recipe, "recipe");
        long token = sequence.incrementAndGet();
        runOnServerThread(() -> {
            boolean apply;
            synchronized (lock) {
                customRecipes.put(recipe.key(), new CustomRecipe(token, recipe));
                apply = live;
            }
            if (apply) {
                applyCurrentCustomRecipes();
            }
        });
        return new RegisteredRecipe(this, recipe.key(), token);
    }

    @Override
    public boolean remove(Key key) {
        Objects.requireNonNull(key, "key");
        return runOnServerThread(() -> {
            var removed = removeCustomRecipe(key);
            if (removed) {
                applyCurrentCustomRecipes();
            }
            return removed;
        });
    }

    public boolean registered(Key key) {
        synchronized (lock) {
            return customRecipes.containsKey(key);
        }
    }

    private boolean removeCustomRecipe(Key key) {
        synchronized (lock) {
            return customRecipes.remove(key) != null;
        }
    }

    private void applyCurrentCustomRecipes() {
        var vanilla = bridge.get();
        if (vanilla == null || !isLive()) {
            return;
        }
        vanilla.apply(customRecipes(), true);
    }

    private boolean isLive() {
        synchronized (lock) {
            return live;
        }
    }

    private List<Recipe> customRecipes() {
        synchronized (lock) {
            return customRecipes.values().stream()
                    .map(CustomRecipe::recipe)
                    .toList();
        }
    }

    private LinkedHashMap<Key, Recipe> customRecipeSnapshot() {
        synchronized (lock) {
            var snapshot = new LinkedHashMap<Key, Recipe>();
            customRecipes.forEach((key, recipe) -> snapshot.put(key, recipe.recipe()));
            return snapshot;
        }
    }

    private <T> T runOnServerThread(Supplier<T> supplier) {
        var vanilla = bridge.get();
        if (vanilla == null) {
            return supplier.get();
        }
        return vanilla.runOnServerThread(supplier);
    }

    private void runOnServerThread(Runnable runnable) {
        runOnServerThread(() -> {
            runnable.run();
            return null;
        });
    }

    private RecipeBridge requireBridge() {
        var vanilla = bridge.get();
        if (vanilla == null) {
            throw new IllegalStateException("Recipe registry is not bound to a Minecraft server");
        }
        return vanilla;
    }

    private boolean registered(Key key, long token) {
        synchronized (lock) {
            var recipe = customRecipes.get(key);
            return recipe != null && recipe.token() == token;
        }
    }

    private boolean remove(Key key, long token) {
        return runOnServerThread(() -> {
            boolean apply;
            synchronized (lock) {
                var recipe = customRecipes.get(key);
                if (recipe == null || recipe.token() != token) {
                    return false;
                }
                customRecipes.remove(key);
                apply = live;
            }
            if (apply) {
                applyCurrentCustomRecipes();
            }
            return true;
        });
    }

    private record CustomRecipe(long token, Recipe recipe) {
    }

    private static final class RegisteredRecipe implements RecipeRegistration {

        private final FandRecipeRegistry owner;
        private final Key key;
        private final long token;
        private final AtomicBoolean active = new AtomicBoolean(true);

        private RegisteredRecipe(FandRecipeRegistry owner, Key key, long token) {
            this.owner = owner;
            this.key = key;
            this.token = token;
        }

        @Override
        public Key key() {
            return key;
        }

        @Override
        public boolean active() {
            return active.get() && owner.registered(key, token);
        }

        @Override
        public void unregister() {
            if (active.compareAndSet(true, false)) {
                owner.remove(key, token);
            }
        }
    }
}
