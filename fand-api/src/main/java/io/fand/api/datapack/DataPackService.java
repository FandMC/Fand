package io.fand.api.datapack;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * Creates and manages server data packs under Fand's managed {@code datapacks/}
 * directory.
 *
 * <p>The service exposes the standard data-pack file tree instead of a narrow
 * DSL, so plugins can publish recipes, loot tables, tags, advancements,
 * predicates, functions, structures, and future vanilla data files without API
 * churn.
 */
public interface DataPackService {

    Gson PRETTY_GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

    Path rootDirectory();

    Collection<DataPack> packs();

    Optional<DataPack> pack(String id);

    DataPackRegistration create(String id, String description);

    DataPackRegistration create(DataPack pack);

    default void writeJson(String packId, String path, JsonElement json) {
        write(packId, path, (PRETTY_GSON.toJson(json) + System.lineSeparator()).getBytes(StandardCharsets.UTF_8));
    }

    void writeText(String packId, String path, String content);

    void write(String packId, String path, byte[] content);

    Optional<byte[]> read(String packId, String path);

    Collection<DataPackFile> files(String packId);

    boolean deleteFile(String packId, String path);

    boolean delete(String packId);

    boolean enable(String packId);

    boolean disable(String packId);

    CompletableFuture<Boolean> reload();

    static DataPackService empty() {
        return Empty.INSTANCE;
    }

    enum Empty implements DataPackService {
        INSTANCE;

        @Override
        public Path rootDirectory() {
            return Path.of("datapacks");
        }

        @Override
        public Collection<DataPack> packs() {
            return java.util.List.of();
        }

        @Override
        public Optional<DataPack> pack(String id) {
            return Optional.empty();
        }

        @Override
        public DataPackRegistration create(String id, String description) {
            throw new UnsupportedOperationException("Data packs are not supported");
        }

        @Override
        public DataPackRegistration create(DataPack pack) {
            throw new UnsupportedOperationException("Data packs are not supported");
        }

        @Override
        public void writeText(String packId, String path, String content) {
            throw new UnsupportedOperationException("Data packs are not supported");
        }

        @Override
        public void write(String packId, String path, byte[] content) {
            throw new UnsupportedOperationException("Data packs are not supported");
        }

        @Override
        public Optional<byte[]> read(String packId, String path) {
            return Optional.empty();
        }

        @Override
        public Collection<DataPackFile> files(String packId) {
            return java.util.List.of();
        }

        @Override
        public boolean deleteFile(String packId, String path) {
            return false;
        }

        @Override
        public boolean delete(String packId) {
            return false;
        }

        @Override
        public boolean enable(String packId) {
            return false;
        }

        @Override
        public boolean disable(String packId) {
            return false;
        }

        @Override
        public CompletableFuture<Boolean> reload() {
            return CompletableFuture.completedFuture(false);
        }
    }
}
