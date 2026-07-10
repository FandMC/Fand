package io.fand.server.plugin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.fand.api.datapack.DataPack;
import io.fand.api.datapack.DataPackFile;
import io.fand.api.datapack.DataPackRegistration;
import io.fand.api.datapack.DataPackService;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.Test;

final class PluginDataPackServiceTest {

    @Test
    void allowsScopedDataAfterNormalizingSeparators() {
        var delegate = new CapturingDataPackService();
        var service = new PluginDataPackService(delegate, new PluginResourceTracker(), "demo");

        service.create("recipes", "Demo recipes");
        service.writeText("recipes", "data\\demo\\recipe\\example.json", "{}");

        assertThat(delegate.lastWritePath).isEqualTo("data/demo/recipe/example.json");
    }

    @Test
    void rejectsDataPathsThatEscapePluginNamespaceAfterNormalization() {
        var service = new PluginDataPackService(new CapturingDataPackService(), new PluginResourceTracker(), "demo");

        assertThatThrownBy(() -> service.writeText("recipes", "data/demo/../other/recipe/example.json", "{}"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("data/demo");
    }

    private static final class CapturingDataPackService implements DataPackService {

        private String lastWritePath;

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
            return Optional.of(new DataPack(id, "", true));
        }

        @Override
        public DataPackRegistration create(String id, String description) {
            return registration(id);
        }

        @Override
        public DataPackRegistration create(DataPack pack) {
            return registration(pack.id());
        }

        @Override
        public void writeText(String packId, String path, String content) {
            write(packId, path, content.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        }

        @Override
        public void write(String packId, String path, byte[] content) {
            this.lastWritePath = path;
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
            return true;
        }

        @Override
        public boolean disable(String packId) {
            return true;
        }

        @Override
        public CompletableFuture<Boolean> reload() {
            return CompletableFuture.completedFuture(true);
        }

        private static DataPackRegistration registration(String id) {
            return new DataPackRegistration() {
                @Override
                public String id() {
                    return id;
                }

                @Override
                public boolean active() {
                    return true;
                }

                @Override
                public void enable() {
                }

                @Override
                public void disable() {
                }

                @Override
                public void delete() {
                }
            };
        }
    }
}
