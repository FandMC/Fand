package io.fand.server.plugin;

import com.google.gson.JsonElement;
import io.fand.api.resourcepack.ResourcePack;
import io.fand.api.resourcepack.ResourcePackBuild;
import io.fand.api.resourcepack.ResourcePackFile;
import io.fand.api.resourcepack.ResourcePackRegistration;
import io.fand.api.resourcepack.ResourcePackService;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Objects;
import java.util.Optional;

public final class PluginResourcePackService implements ResourcePackService {

    private final ResourcePackService delegate;
    private final PluginResourceTracker tracker;
    private final String namespace;

    public PluginResourcePackService(ResourcePackService delegate, PluginResourceTracker tracker, String namespace) {
        this.delegate = Objects.requireNonNull(delegate, "delegate");
        this.tracker = Objects.requireNonNull(tracker, "tracker");
        this.namespace = Objects.requireNonNull(namespace, "namespace");
    }

    @Override
    public Path rootDirectory() {
        return delegate.rootDirectory();
    }

    @Override
    public Path buildDirectory() {
        return delegate.buildDirectory();
    }

    @Override
    public Collection<ResourcePack> packs() {
        return delegate.packs().stream()
                .filter(pack -> namespace.equals(pack.id()) || pack.id().startsWith(namespace + "."))
                .toList();
    }

    @Override
    public Optional<ResourcePack> pack(String id) {
        return delegate.pack(scopedId(id))
                .filter(pack -> namespace.equals(pack.id()) || pack.id().startsWith(namespace + "."));
    }

    @Override
    public ResourcePackRegistration create(String id, String description) {
        return tracker.track(delegate.create(scopedId(id), description));
    }

    @Override
    public ResourcePackRegistration create(String id, String description, int packFormat) {
        return tracker.track(delegate.create(scopedId(id), description, packFormat));
    }

    @Override
    public ResourcePackRegistration create(ResourcePack pack) {
        Objects.requireNonNull(pack, "pack");
        return tracker.track(delegate.create(new ResourcePack(scopedId(pack.id()), pack.description(), pack.packFormat())));
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
    public Collection<ResourcePackFile> files(String packId) {
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
    public ResourcePackBuild build(String packId) {
        return delegate.build(scopedId(packId));
    }

    @Override
    public Optional<String> hostedUrl(ResourcePackBuild build) {
        Objects.requireNonNull(build, "build");
        if (!owns(build.packId())) {
            throw new IllegalArgumentException("Resource pack is not owned by this plugin: " + build.packId());
        }
        return delegate.hostedUrl(build);
    }

    private String scopedId(String id) {
        Objects.requireNonNull(id, "id");
        var normalized = new ResourcePack(id, "", 1).id();
        if (namespace.equals(normalized)) {
            return normalized;
        }
        if (normalized.startsWith(namespace + ".")) {
            return normalized;
        }
        return namespace + "." + normalized;
    }

    private boolean owns(String id) {
        return namespace.equals(id) || id.startsWith(namespace + ".");
    }

    private String scopedPath(String path) {
        Objects.requireNonNull(path, "path");
        var normalized = ResourcePack.normalizeRelativePath(path);
        if (normalized.startsWith("assets/")) {
            var prefix = "assets/" + namespace + "/";
            if (!normalized.startsWith(prefix) && !normalized.startsWith("assets/minecraft/")) {
                throw new IllegalArgumentException("Plugin resource pack assets must stay under assets/" + namespace + " or assets/minecraft: " + path);
            }
        }
        return normalized;
    }
}
