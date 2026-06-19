package io.fand.server.advancement;

import com.google.gson.JsonElement;
import com.mojang.serialization.JsonOps;
import io.fand.api.advancement.AdvancementRegistry;
import io.fand.api.advancement.AdvancementRegistration;
import io.fand.api.advancement.AdvancementView;
import io.fand.api.advancement.CustomAdvancement;
import io.fand.server.command.AdventureBridge;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;
import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementHolder;
import net.kyori.adventure.key.Key;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;

public final class FandAdvancementRegistry implements AdvancementRegistry {

    private final Supplier<MinecraftServer> server;
    private final Object lock = new Object();
    private final LinkedHashMap<Key, CustomEntry> customAdvancements = new LinkedHashMap<>();
    private final AtomicLong sequence = new AtomicLong();

    public FandAdvancementRegistry(Supplier<MinecraftServer> server) {
        this.server = Objects.requireNonNull(server, "server");
    }

    @Override
    public Optional<AdvancementView> advancement(Key key) {
        Objects.requireNonNull(key, "key");
        synchronized (lock) {
            var custom = customAdvancements.get(key);
            if (custom != null) {
                return Optional.of(custom.view());
            }
        }
        var current = server.get();
        if (current == null) {
            return Optional.empty();
        }
        return callOnServerThread(current, () -> Optional.ofNullable(current.getAdvancements().get(identifier(key)))
                .map(holder -> {
                    var display = holder.value().display().orElse(null);
                    var title = display == null ? null : AdventureBridge.fromVanilla(display.getTitle(), current.registryAccess());
                    var description = display == null ? null : AdventureBridge.fromVanilla(display.getDescription(), current.registryAccess());
                    return new AdvancementView(key, title, description);
                }));
    }

    @Override
    public AdvancementRegistration register(CustomAdvancement advancement) {
        Objects.requireNonNull(advancement, "advancement");
        var token = sequence.incrementAndGet();
        var view = new AdvancementView(advancement.key(), advancement.title(), advancement.description());
        synchronized (lock) {
            customAdvancements.put(advancement.key(), new CustomEntry(token, view, advancement));
        }
        applyToVanilla(advancement);
        return new Registration(this, advancement.key(), token);
    }

    public void applyLoadedAdvancements() {
        var current = server.get();
        if (current == null) {
            return;
        }
        var snapshot = customAdvancements();
        if (snapshot.isEmpty()) {
            return;
        }
        snapshot.stream()
                .map(advancement -> vanillaAdvancement(current, advancement))
                .forEach(current.getAdvancements()::fand$addCustomAdvancement);
    }

    public boolean registered(Key key) {
        synchronized (lock) {
            return customAdvancements.containsKey(key);
        }
    }

    public boolean remove(Key key) {
        Objects.requireNonNull(key, "key");
        synchronized (lock) {
            if (customAdvancements.remove(key) == null) {
                return false;
            }
        }
        removeFromVanilla(key);
        return true;
    }

    private boolean registered(Key key, long token) {
        synchronized (lock) {
            var entry = customAdvancements.get(key);
            return entry != null && entry.token() == token;
        }
    }

    private boolean remove(Key key, long token) {
        synchronized (lock) {
            var entry = customAdvancements.get(key);
            if (entry == null || entry.token() != token) {
                return false;
            }
            customAdvancements.remove(key);
            removeFromVanilla(key);
            return true;
        }
    }

    private List<CustomAdvancement> customAdvancements() {
        synchronized (lock) {
            return customAdvancements.values().stream()
                    .map(CustomEntry::advancement)
                    .toList();
        }
    }

    private void applyToVanilla(CustomAdvancement advancement) {
        var current = server.get();
        if (current == null) {
            return;
        }
        var holder = vanillaAdvancement(current, advancement);
        callOnServerThread(current, () -> {
            current.getAdvancements().fand$addCustomAdvancement(holder);
            current.getPlayerList().reloadResources();
            return null;
        });
    }

    private void removeFromVanilla(Key key) {
        var current = server.get();
        if (current == null) {
            return;
        }
        callOnServerThread(current, () -> {
            current.getAdvancements().fand$removeCustomAdvancement(identifier(key));
            current.getPlayerList().reloadResources();
            return null;
        });
    }

    private static AdvancementHolder vanillaAdvancement(MinecraftServer server, CustomAdvancement advancement) {
        var id = identifier(advancement.key());
        Advancement vanilla = Advancement.CODEC.parse(ops(server), advancement.toVanillaJson())
                .getOrThrow(error -> new IllegalArgumentException("Invalid custom advancement "
                        + advancement.key().asString()
                        + ": "
                        + error));
        advancement.display().ifPresent(apiDisplay ->
                vanilla.display().ifPresent(display -> display.setLocation(apiDisplay.x(), apiDisplay.y())));
        return new AdvancementHolder(id, vanilla);
    }

    private static com.mojang.serialization.DynamicOps<JsonElement> ops(MinecraftServer server) {
        return server.registryAccess().createSerializationContext(JsonOps.INSTANCE);
    }

    private static Identifier identifier(Key key) {
        return Identifier.fromNamespaceAndPath(key.namespace(), key.value());
    }

    private static <T> T callOnServerThread(MinecraftServer server, Supplier<T> task) {
        if (server.isSameThread()) {
            return task.get();
        }
        return server.submit(task::get).join();
    }

    private record CustomEntry(long token, AdvancementView view, CustomAdvancement advancement) {
    }

    private static final class Registration implements AdvancementRegistration {

        private final FandAdvancementRegistry owner;
        private final Key key;
        private final long token;
        private final AtomicBoolean active = new AtomicBoolean(true);

        private Registration(FandAdvancementRegistry owner, Key key, long token) {
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
        public void close() {
            if (active.compareAndSet(true, false)) {
                owner.remove(key, token);
            }
        }
    }
}
