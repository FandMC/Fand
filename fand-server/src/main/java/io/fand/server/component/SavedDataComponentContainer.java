package io.fand.server.component;

import com.google.gson.JsonElement;
import io.fand.api.component.DataComponentContainer;
import io.fand.api.component.DataComponentMap;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
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
        return callOnServerThread(() -> {
            var data = existingData.get();
            return data == null ? DataComponentMap.EMPTY : data.get(id);
        });
    }

    @Override
    public void set(Key key, JsonElement value) {
        Objects.requireNonNull(key, "key");
        Objects.requireNonNull(value, "value");
        runOnServerThread(() -> {
            var data = writableData.get();
            data.put(id, data.get(id).with(key, value));
        });
    }

    @Override
    public void remove(Key key) {
        Objects.requireNonNull(key, "key");
        runOnServerThread(() -> {
            var data = existingData.get();
            if (data != null) {
                data.put(id, data.get(id).without(key));
            }
        });
    }

    @Override
    public void clear() {
        runOnServerThread(() -> {
            var data = existingData.get();
            if (data != null) {
                data.clear(id);
            }
        });
    }

    private void runOnServerThread(Runnable task) {
        if (server.isSameThread()) {
            task.run();
            return;
        }
        var future = new CompletableFuture<Void>();
        server.executeIfPossible(() -> {
            try {
                task.run();
                future.complete(null);
            } catch (Throwable failure) {
                future.completeExceptionally(failure);
            }
        });
        future.join();
    }

    private <T> T callOnServerThread(Supplier<T> task) {
        if (server.isSameThread()) {
            return task.get();
        }
        var future = new CompletableFuture<T>();
        server.executeIfPossible(() -> {
            try {
                future.complete(task.get());
            } catch (Throwable failure) {
                future.completeExceptionally(failure);
            }
        });
        return future.join();
    }
}
