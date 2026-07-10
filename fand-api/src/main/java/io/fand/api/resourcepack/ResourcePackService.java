package io.fand.api.resourcepack;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import io.fand.api.entity.Player;
import io.fand.api.player.ResourcePackRequest;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import net.kyori.adventure.text.Component;
import org.jspecify.annotations.Nullable;

/**
 * Creates and builds server resource packs under Fand's managed
 * {@code resourcepacks/} directory.
 *
 * <p>The service exposes the normal resource-pack file tree, so plugins can
 * publish textures, models, fonts, language files, sounds, shaders, and future
 * vanilla resource files without waiting for dedicated API surface.
 */
public interface ResourcePackService {

    Gson PRETTY_GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

    Path rootDirectory();

    Path buildDirectory();

    Collection<ResourcePack> packs();

    Optional<ResourcePack> pack(String id);

    ResourcePackRegistration create(String id, String description);

    ResourcePackRegistration create(String id, String description, int packFormat);

    ResourcePackRegistration create(ResourcePack pack);

    default void writeJson(String packId, String path, JsonElement json) {
        write(packId, path, (PRETTY_GSON.toJson(json) + System.lineSeparator()).getBytes(StandardCharsets.UTF_8));
    }

    void writeText(String packId, String path, String content);

    void write(String packId, String path, byte[] content);

    Optional<byte[]> read(String packId, String path);

    Collection<ResourcePackFile> files(String packId);

    boolean deleteFile(String packId, String path);

    boolean delete(String packId);

    ResourcePackBuild build(String packId);

    default ResourcePackRequest request(String packId, String url) {
        return build(packId).request(url);
    }

    default ResourcePackRequest request(String packId, String url, boolean required, @Nullable Component prompt) {
        return build(packId).request(url, required, prompt);
    }

    default CompletableFuture<ResourcePackBuild> buildAsync(String packId) {
        return CompletableFuture.supplyAsync(() -> build(packId));
    }

    default CompletableFuture<Void> send(Player player, String packId, String url) {
        java.util.Objects.requireNonNull(player, "player");
        return buildAsync(packId).thenAccept(build -> player.sendResourcePack(build.request(url)));
    }

    default CompletableFuture<Void> send(
            Player player,
            String packId,
            String url,
            boolean required,
            @Nullable Component prompt
    ) {
        java.util.Objects.requireNonNull(player, "player");
        return buildAsync(packId).thenAccept(build -> player.sendResourcePack(build.request(url, required, prompt)));
    }

    static ResourcePackService empty() {
        return Empty.INSTANCE;
    }

    enum Empty implements ResourcePackService {
        INSTANCE;

        @Override
        public Path rootDirectory() {
            return Path.of("resourcepacks");
        }

        @Override
        public Path buildDirectory() {
            return rootDirectory().resolve("builds");
        }

        @Override
        public Collection<ResourcePack> packs() {
            return java.util.List.of();
        }

        @Override
        public Optional<ResourcePack> pack(String id) {
            return Optional.empty();
        }

        @Override
        public ResourcePackRegistration create(String id, String description) {
            throw new UnsupportedOperationException("Resource packs are not supported");
        }

        @Override
        public ResourcePackRegistration create(String id, String description, int packFormat) {
            throw new UnsupportedOperationException("Resource packs are not supported");
        }

        @Override
        public ResourcePackRegistration create(ResourcePack pack) {
            throw new UnsupportedOperationException("Resource packs are not supported");
        }

        @Override
        public void writeText(String packId, String path, String content) {
            throw new UnsupportedOperationException("Resource packs are not supported");
        }

        @Override
        public void write(String packId, String path, byte[] content) {
            throw new UnsupportedOperationException("Resource packs are not supported");
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
            throw new UnsupportedOperationException("Resource packs are not supported");
        }
    }
}
