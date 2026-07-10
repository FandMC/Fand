package io.fand.server.plugin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.fand.api.resourcepack.ResourcePack;
import io.fand.api.resourcepack.ResourcePackBuild;
import io.fand.api.resourcepack.ResourcePackFile;
import io.fand.api.resourcepack.ResourcePackRegistration;
import io.fand.api.resourcepack.ResourcePackService;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Optional;
import org.junit.jupiter.api.Test;

final class PluginResourcePackServiceTest {

    @Test
    void allowsScopedAssetsAfterNormalizingSeparators() {
        var delegate = new CapturingResourcePackService();
        var service = new PluginResourcePackService(delegate, new PluginResourceTracker(), "demo");

        service.create("textures", "Demo textures");
        service.writeText("textures", "assets\\demo\\lang\\en_us.json", "{}");

        assertThat(delegate.lastWritePath).isEqualTo("assets/demo/lang/en_us.json");
    }

    @Test
    void rejectsAssetPathsThatEscapePluginNamespaceAfterNormalization() {
        var service = new PluginResourcePackService(new CapturingResourcePackService(), new PluginResourceTracker(), "demo");

        assertThatThrownBy(() -> service.writeText("textures", "assets/demo/../other/lang/en_us.json", "{}"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("assets/demo");
    }

    @Test
    void rejectsAssetPathsOutsidePluginNamespace() {
        var service = new PluginResourcePackService(new CapturingResourcePackService(), new PluginResourceTracker(), "demo");

        assertThatThrownBy(() -> service.writeText("textures", "assets/other/lang/en_us.json", "{}"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("assets/demo");
    }

    private static final class CapturingResourcePackService implements ResourcePackService {

        private String lastWritePath;

        @Override
        public Path rootDirectory() {
            return Path.of("resourcepacks");
        }

        @Override
        public Path buildDirectory() {
            return Path.of("resourcepacks/builds");
        }

        @Override
        public Collection<ResourcePack> packs() {
            return java.util.List.of();
        }

        @Override
        public Optional<ResourcePack> pack(String id) {
            return Optional.of(new ResourcePack(id, "", 1));
        }

        @Override
        public ResourcePackRegistration create(String id, String description) {
            return registration(id);
        }

        @Override
        public ResourcePackRegistration create(String id, String description, int packFormat) {
            return registration(id);
        }

        @Override
        public ResourcePackRegistration create(ResourcePack pack) {
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
        public Collection<ResourcePackFile> files(String packId) {
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
        public ResourcePackBuild build(String packId) {
            throw new UnsupportedOperationException();
        }

        private static ResourcePackRegistration registration(String id) {
            return new ResourcePackRegistration() {
                @Override
                public String id() {
                    return id;
                }

                @Override
                public boolean active() {
                    return true;
                }

                @Override
                public void delete() {
                }
            };
        }
    }
}
