package io.fand.server.plugin;

import com.google.gson.JsonElement;
import io.fand.api.datapack.DataPack;
import io.fand.api.datapack.DataPackFile;
import io.fand.api.datapack.DataPackRegistration;
import io.fand.api.datapack.DataPackService;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public final class PluginDataPackService implements DataPackService {

    private final DataPackService delegate;
    private final PluginResourceTracker tracker;
    private final String namespace;

    public PluginDataPackService(DataPackService delegate, PluginResourceTracker tracker, String namespace) {
        this.delegate = Objects.requireNonNull(delegate, "delegate");
        this.tracker = Objects.requireNonNull(tracker, "tracker");
        this.namespace = Objects.requireNonNull(namespace, "namespace");
    }

    @Override
    public Path rootDirectory() {
        return delegate.rootDirectory();
    }

    @Override
    public Collection<DataPack> packs() {
        return delegate.packs().stream()
                .filter(pack -> namespace.equals(pack.id()) || pack.id().startsWith(namespace + "."))
                .toList();
    }

    @Override
    public Optional<DataPack> pack(String id) {
        return delegate.pack(scopedId(id))
                .filter(pack -> namespace.equals(pack.id()) || pack.id().startsWith(namespace + "."));
    }

    @Override
    public DataPackRegistration create(String id, String description) {
        return create(new DataPack(id, description, true));
    }

    @Override
    public DataPackRegistration create(DataPack pack) {
        Objects.requireNonNull(pack, "pack");
        return tracker.track(delegate.create(new DataPack(scopedId(pack.id()), pack.description(), pack.enabled())));
    }

    @Override
    public void writeJson(String packId, String path, JsonElement json) {
        delegate.writeJson(scopedId(packId), scopedPath(path), json);
    }

    @Override
    public void writeText(String packId, String path, String content) {
        delegate.write(scopedId(packId), scopedPath(path), content.getBytes(StandardCharsets.UTF_8));
    }

    @Override
    public void write(String packId, String path, byte[] content) {
        delegate.write(scopedId(packId), scopedPath(path), content);
    }

    @Override
    public Optional<byte[]> read(String packId, String path) {
        return delegate.read(scopedId(packId), scopedPath(path));
    }

    @Override
    public Collection<DataPackFile> files(String packId) {
        return delegate.files(scopedId(packId));
    }

    @Override
    public boolean deleteFile(String packId, String path) {
        return delegate.deleteFile(scopedId(packId), scopedPath(path));
    }

    @Override
    public boolean delete(String packId) {
        return delegate.delete(scopedId(packId));
    }

    @Override
    public boolean enable(String packId) {
        return delegate.enable(scopedId(packId));
    }

    @Override
    public boolean disable(String packId) {
        return delegate.disable(scopedId(packId));
    }

    @Override
    public CompletableFuture<Boolean> reload() {
        return delegate.reload();
    }

    private String scopedId(String id) {
        Objects.requireNonNull(id, "id");
        var normalized = new DataPack(id, "", false).id();
        if (namespace.equals(normalized)) {
            return normalized;
        }
        if (normalized.startsWith(namespace + ".")) {
            return normalized;
        }
        return namespace + "." + normalized;
    }

    private String scopedPath(String path) {
        Objects.requireNonNull(path, "path");
        var normalized = path.replace('\\', '/');
        if (normalized.startsWith("data/")) {
            var prefix = "data/" + namespace + "/";
            if (!normalized.startsWith(prefix)) {
                throw new IllegalArgumentException("Plugin data pack files must stay under data/" + namespace + ": " + path);
            }
        }
        return normalized;
    }
}
