package io.fand.server.plugin;

import io.fand.api.structure.StructurePlacement;
import io.fand.api.structure.StructureFormat;
import io.fand.api.structure.StructureProjection;
import io.fand.api.structure.StructureService;
import io.fand.api.structure.StructureTemplate;
import io.fand.api.structure.StructureVolume;
import io.fand.api.world.Location;
import java.nio.file.Path;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import net.kyori.adventure.key.Key;

public final class PluginStructureService implements StructureService {

    private final StructureService delegate;
    private final String namespace;

    public PluginStructureService(StructureService delegate, String namespace) {
        this.delegate = Objects.requireNonNull(delegate, "delegate");
        this.namespace = Objects.requireNonNull(namespace, "namespace");
    }

    @Override
    public Optional<StructureTemplate> template(Key key) {
        return delegate.template(scopedKey(key)).filter(this::ownedByThisPlugin);
    }

    @Override
    public CompletableFuture<Boolean> save(Key key, StructureVolume volume) {
        return delegate.save(scopedKey(key), volume);
    }

    @Override
    public CompletableFuture<Optional<StructureProjection>> exportTemplate(Key key, StructureFormat format) {
        return delegate.exportTemplate(scopedKey(key), format);
    }

    @Override
    public CompletableFuture<Boolean> importTemplate(Key key, StructureProjection projection) {
        return delegate.importTemplate(scopedKey(key), projection);
    }

    @Override
    public CompletableFuture<Optional<StructureProjection>> load(Path path, StructureFormat format) {
        return delegate.load(path, format);
    }

    @Override
    public CompletableFuture<Boolean> save(Key key, Path path, StructureFormat format) {
        return delegate.save(scopedKey(key), path, format);
    }

    @Override
    public CompletableFuture<Boolean> place(Key key, Location origin, StructurePlacement placement) {
        return delegate.place(scopedKey(key), origin, placement);
    }

    @Override
    public CompletableFuture<Optional<Location>> locate(Key structure, Location origin, int radius) {
        return delegate.locate(structure, origin, radius);
    }

    private Key scopedKey(Key key) {
        Objects.requireNonNull(key, "key");
        if (namespace.equals(key.namespace())) {
            return key;
        }
        return Key.key(namespace, key.value());
    }

    private boolean ownedByThisPlugin(StructureTemplate template) {
        return namespace.equals(template.key().namespace());
    }
}
