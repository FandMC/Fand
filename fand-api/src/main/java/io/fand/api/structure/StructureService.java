package io.fand.api.structure;

import io.fand.api.world.Location;
import java.nio.file.Path;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import net.kyori.adventure.key.Key;

public interface StructureService {

    Optional<StructureTemplate> template(Key key);

    CompletableFuture<Boolean> save(Key key, StructureVolume volume);

    CompletableFuture<Optional<StructureProjection>> exportTemplate(Key key, StructureFormat format);

    CompletableFuture<Boolean> importTemplate(Key key, StructureProjection projection);

    default CompletableFuture<Boolean> importTemplate(Key key, StructureFormat format, byte[] data) {
        return importTemplate(key, StructureProjection.of(format, data));
    }

    CompletableFuture<Optional<StructureProjection>> load(Path path, StructureFormat format);

    CompletableFuture<Boolean> save(Key key, Path path, StructureFormat format);

    CompletableFuture<Boolean> place(Key key, Location origin, StructurePlacement placement);

    default CompletableFuture<Boolean> place(StructureProjection projection, Location origin, StructurePlacement placement) {
        var key = projection.sourceKey().orElseGet(() -> Key.key("fand", "projection/" + Integer.toHexString(java.util.Arrays.hashCode(projection.data()))));
        return importTemplate(key, projection).thenCompose(imported -> imported
                ? place(key, origin, placement)
                : CompletableFuture.completedFuture(false));
    }

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
