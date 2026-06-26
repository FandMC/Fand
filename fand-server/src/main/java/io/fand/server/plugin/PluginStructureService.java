package io.fand.server.plugin;

import io.fand.api.structure.CustomStructure;
import io.fand.api.structure.CustomStructureSet;
import io.fand.api.structure.StructureRegistration;
import io.fand.api.structure.StructurePlacement;
import io.fand.api.structure.StructureFormat;
import io.fand.api.structure.StructureProjection;
import io.fand.api.structure.StructureService;
import io.fand.api.structure.StructureTemplate;
import io.fand.api.structure.StructureVolume;
import io.fand.api.world.Location;
import java.util.Collection;
import java.nio.file.Path;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import net.kyori.adventure.key.Key;

public final class PluginStructureService implements StructureService {

    private final StructureService delegate;
    private final PluginResourceTracker tracker;
    private final String namespace;

    public PluginStructureService(StructureService delegate, PluginResourceTracker tracker, String namespace) {
        this.delegate = Objects.requireNonNull(delegate, "delegate");
        this.tracker = Objects.requireNonNull(tracker, "tracker");
        this.namespace = Objects.requireNonNull(namespace, "namespace");
    }

    @Override
    public Collection<CustomStructure> registeredStructures() {
        return delegate.registeredStructures().stream()
                .filter(structure -> namespace.equals(structure.key().namespace()))
                .toList();
    }

    @Override
    public Optional<CustomStructure> registeredStructure(Key key) {
        return delegate.registeredStructure(scopedKey(key))
                .filter(structure -> namespace.equals(structure.key().namespace()));
    }

    @Override
    public StructureRegistration registerStructure(CustomStructure structure) {
        Objects.requireNonNull(structure, "structure");
        return tracker.track(delegate.registerStructure(new CustomStructure(
                scopedKey(structure.key()),
                scopedTemplateKey(structure.template()),
                structure.biomes(),
                structure.step(),
                structure.terrainAdjustment(),
                structure.heightPlacement(),
                structure.includeEntities())));
    }

    @Override
    public Collection<CustomStructureSet> registeredStructureSets() {
        return delegate.registeredStructureSets().stream()
                .filter(structureSet -> namespace.equals(structureSet.key().namespace()))
                .toList();
    }

    @Override
    public Optional<CustomStructureSet> registeredStructureSet(Key key) {
        return delegate.registeredStructureSet(scopedKey(key))
                .filter(structureSet -> namespace.equals(structureSet.key().namespace()));
    }

    @Override
    public StructureRegistration registerStructureSet(CustomStructureSet structureSet) {
        Objects.requireNonNull(structureSet, "structureSet");
        return tracker.track(delegate.registerStructureSet(new CustomStructureSet(
                scopedKey(structureSet.key()),
                structureSet.structures().stream()
                        .map(entry -> new io.fand.api.structure.StructureSetEntry(scopedKey(entry.structure()), entry.weight()))
                        .toList(),
                structureSet.placement())));
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
    public CompletableFuture<Boolean> place(StructureProjection projection, Location origin, StructurePlacement placement) {
        return delegate.place(projection, origin, placement);
    }

    @Override
    public CompletableFuture<Optional<Location>> locate(Key structure, Location origin, int radius) {
        return delegate.locate(scopedKey(structure), origin, radius);
    }

    private Key scopedKey(Key key) {
        Objects.requireNonNull(key, "key");
        if (namespace.equals(key.namespace())) {
            return key;
        }
        return Key.key(namespace, key.value());
    }

    private Key scopedTemplateKey(Key key) {
        Objects.requireNonNull(key, "key");
        if ("minecraft".equals(key.namespace()) || namespace.equals(key.namespace())) {
            return key;
        }
        return Key.key(namespace, key.value());
    }

    private boolean ownedByThisPlugin(StructureTemplate template) {
        return namespace.equals(template.key().namespace());
    }
}
