package io.fand.server.component;

import com.google.gson.JsonElement;
import io.fand.api.component.DataComponentContainer;
import io.fand.api.component.DataComponentMap;
import java.util.Objects;
import java.util.function.Supplier;
import net.kyori.adventure.key.Key;
import net.minecraft.server.MinecraftServer;

public final class SavedDataComponentContainer implements DataComponentContainer {

    private final MinecraftServer server;
    private final Supplier<PersistentComponentData> existingData;
    private final Supplier<PersistentComponentData> writableData;
    private final String id;

    public SavedDataComponentContainer(
            MinecraftServer server,
            Supplier<PersistentComponentData> existingData,
            Supplier<PersistentComponentData> writableData,
            String id) {
        this.server = Objects.requireNonNull(server, "server");
        this.existingData = Objects.requireNonNull(existingData, "existingData");
        this.writableData = Objects.requireNonNull(writableData, "writableData");
        this.id = Objects.requireNonNull(id, "id");
    }

    @Override
    public DataComponentMap snapshot() {
        requireMainThread();
        var data = existingData.get();
        return data == null ? DataComponentMap.EMPTY : data.get(id);
    }

    @Override
    public void set(Key key, JsonElement value) {
        Objects.requireNonNull(key, "key");
        Objects.requireNonNull(value, "value");
        requireMainThread();
        writableData.get().put(id, snapshot().with(key, value));
    }

    @Override
    public void remove(Key key) {
        Objects.requireNonNull(key, "key");
        requireMainThread();
        var data = existingData.get();
        if (data != null) {
            data.put(id, snapshot().without(key));
        }
    }

    @Override
    public void clear() {
        requireMainThread();
        var data = existingData.get();
        if (data != null) {
            data.clear(id);
        }
    }

    private void requireMainThread() {
        if (!server.isSameThread()) {
            throw new IllegalStateException("Persistent components must be accessed on the server thread");
        }
    }
}
