package io.fand.api.structure;

import io.fand.api.world.Location;
import java.util.Collection;
import java.nio.file.Path;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import net.kyori.adventure.key.Key;

public interface StructureService {

    default Collection<CustomStructure> registeredStructures() {
        return java.util.List.of();
    }

    default Optional<CustomStructure> registeredStructure(Key key) {
        return Optional.empty();
    }

    default StructureRegistration registerStructure(CustomStructure structure) {
        throw new UnsupportedOperationException("Custom structures are not supported");
    }

    default Collection<CustomStructureSet> registeredStructureSets() {
        return java.util.List.of();
    }

    default Optional<CustomStructureSet> registeredStructureSet(Key key) {
        return Optional.empty();
    }

    default StructureRegistration registerStructureSet(CustomStructureSet structureSet) {
        throw new UnsupportedOperationException("Custom structure sets are not supported");
    }

    Optional<StructureTemplate> template(Key key);

    CompletableFuture<Boolean> save(Key key, StructureVolume volume);

    CompletableFuture<Optional<StructureProjection>> exportTemplate(Key key, StructureFormat format);

    CompletableFuture<Boolean> importTemplate(Key key, StructureProjection projection);

    CompletableFuture<Optional<StructureProjection>> load(Path path, StructureFormat format);

    CompletableFuture<Boolean> save(Key key, Path path, StructureFormat format);

    CompletableFuture<Boolean> place(Key key, Location origin, StructurePlacement placement);

    CompletableFuture<Optional<Location>> locate(Key structure, Location origin, int radius);

    static StructureService empty() {
        return new StructureService() {
            @Override
            public Optional<StructureTemplate> template(Key key) {
                return Optional.empty();
            }

            @Override
            public CompletableFuture<Boolean> save(Key key, StructureVolume volume) {
                return CompletableFuture.completedFuture(false);
            }

            @Override
            public CompletableFuture<Optional<StructureProjection>> exportTemplate(Key key, StructureFormat format) {
                return CompletableFuture.completedFuture(Optional.empty());
            }

            @Override
            public CompletableFuture<Boolean> importTemplate(Key key, StructureProjection projection) {
                return CompletableFuture.completedFuture(false);
            }

            @Override
            public CompletableFuture<Optional<StructureProjection>> load(Path path, StructureFormat format) {
                return CompletableFuture.completedFuture(Optional.empty());
            }

            @Override
            public CompletableFuture<Boolean> save(Key key, Path path, StructureFormat format) {
                return CompletableFuture.completedFuture(false);
            }

            @Override
            public CompletableFuture<Boolean> place(Key key, Location origin, StructurePlacement placement) {
                return CompletableFuture.completedFuture(false);
            }

            @Override
            public CompletableFuture<Optional<Location>> locate(Key structure, Location origin, int radius) {
                return CompletableFuture.completedFuture(Optional.empty());
            }
        };
    }
}
