package io.fand.server.plugin;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import io.fand.api.storage.PluginStorage;
import io.fand.api.storage.ScopedStorage;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.kyori.adventure.key.Key;

public final class FandPluginStorage implements PluginStorage {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private final Path root;
    private final ConcurrentHashMap<String, ScopedStorage> stores = new ConcurrentHashMap<>();

    public FandPluginStorage(Path pluginDataDirectory) {
        this.root = Objects.requireNonNull(pluginDataDirectory, "pluginDataDirectory").resolve("storage");
    }

    @Override
    public ScopedStorage global() {
        return store("global", root.resolve("global.json"));
    }

    @Override
    public ScopedStorage player(UUID playerId) {
        Objects.requireNonNull(playerId, "playerId");
        return store("player/" + playerId, root.resolve("players").resolve(playerId + ".json"));
    }

    @Override
    public ScopedStorage world(Key world) {
        Objects.requireNonNull(world, "world");
        return store("world/" + world.asString(), root.resolve("worlds").resolve(fileName(world)).resolve("world.json"));
    }

    @Override
    public ScopedStorage chunk(Key world, int chunkX, int chunkZ) {
        Objects.requireNonNull(world, "world");
        return store(
                "chunk/" + world.asString() + "/" + chunkX + "," + chunkZ,
                root.resolve("worlds").resolve(fileName(world)).resolve("chunks").resolve(chunkX + "_" + chunkZ + ".json"));
    }

    @Override
    public ScopedStorage block(Key world, int x, int y, int z) {
        Objects.requireNonNull(world, "world");
        return store(
                "block/" + world.asString() + "/" + x + "," + y + "," + z,
                root.resolve("worlds").resolve(fileName(world)).resolve("blocks").resolve(x + "_" + y + "_" + z + ".json"));
    }

    private ScopedStorage store(String id, Path path) {
        return stores.computeIfAbsent(id, ignored -> new JsonScopedStorage(path));
    }

    private static String fileName(Key key) {
        return key.namespace() + "__" + key.value().replace('/', '_');
    }

    private static final class JsonScopedStorage implements ScopedStorage {

        private final Path path;
        private final Object lock = new Object();
        private JsonObject values;

        private JsonScopedStorage(Path path) {
            this.path = path;
        }

        @Override
        public Optional<JsonElement> get(String key) {
            Objects.requireNonNull(key, "key");
            synchronized (lock) {
                var value = load().get(key);
                return value == null ? Optional.empty() : Optional.of(value.deepCopy());
            }
        }

        @Override
        public Map<String, JsonElement> entries() {
            synchronized (lock) {
                var copy = new LinkedHashMap<String, JsonElement>();
                for (var entry : load().entrySet()) {
                    copy.put(entry.getKey(), entry.getValue().deepCopy());
                }
                return Collections.unmodifiableMap(copy);
            }
        }

        @Override
        public void set(String key, JsonElement value) {
            Objects.requireNonNull(key, "key");
            Objects.requireNonNull(value, "value");
            synchronized (lock) {
                load().add(key, value.deepCopy());
                save();
            }
        }

        @Override
        public void remove(String key) {
            Objects.requireNonNull(key, "key");
            synchronized (lock) {
                if (load().remove(key) != null) {
                    save();
                }
            }
        }

        @Override
        public void clear() {
            synchronized (lock) {
                values = new JsonObject();
                save();
            }
        }

        @Override
        public JsonObject toJson() {
            synchronized (lock) {
                return load().deepCopy();
            }
        }

        private JsonObject load() {
            if (values != null) {
                return values;
            }
            if (!Files.isRegularFile(path)) {
                values = new JsonObject();
                return values;
            }
            try {
                var parsed = JsonParser.parseString(Files.readString(path, StandardCharsets.UTF_8));
                values = parsed.isJsonObject() ? parsed.getAsJsonObject() : new JsonObject();
            } catch (IOException ex) {
                throw new UncheckedIOException("Failed to read plugin storage " + path, ex);
            } catch (JsonParseException ex) {
                throw new IllegalStateException("Invalid plugin storage JSON: " + path, ex);
            }
            return values;
        }

        private void save() {
            try {
                Files.createDirectories(path.getParent());
                Files.writeString(path, GSON.toJson(values), StandardCharsets.UTF_8);
            } catch (IOException ex) {
                throw new UncheckedIOException("Failed to write plugin storage " + path, ex);
            }
        }
    }
}
