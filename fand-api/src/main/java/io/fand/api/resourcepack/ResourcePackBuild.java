package io.fand.api.resourcepack;

import io.fand.api.player.ResourcePackRequest;
import java.nio.file.Path;
import java.util.Objects;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import org.jspecify.annotations.Nullable;

/**
 * Built zip artifact and SHA-1 metadata for a managed resource pack.
 */
public record ResourcePackBuild(
        String packId,
        Path file,
        String sha1,
        long size
) {

    public ResourcePackBuild {
        packId = ResourcePack.normalizeId(packId);
        file = Objects.requireNonNull(file, "file");
        sha1 = Objects.requireNonNull(sha1, "sha1").trim().toLowerCase(java.util.Locale.ROOT);
        if (!sha1.matches("[0-9a-f]{40}")) {
            throw new IllegalArgumentException("sha1 must be a 40-character lowercase hex string");
        }
        if (size < 0) {
            throw new IllegalArgumentException("size must be >= 0");
        }
    }

    public ResourcePackRequest request(String url) {
        return request(UUID.randomUUID(), url, false, null);
    }

    public ResourcePackRequest request(String url, boolean required, @Nullable Component prompt) {
        return request(UUID.randomUUID(), url, required, prompt);
    }

    public ResourcePackRequest request(UUID id, String url, boolean required, @Nullable Component prompt) {
        return new ResourcePackRequest(id, url, sha1, required, prompt);
    }
}
