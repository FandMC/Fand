package io.fand.api.player;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import net.kyori.adventure.text.Component;

/**
 * Client resource-pack request sent to an online player.
 */
public record ResourcePackRequest(
        UUID id,
        String url,
        String hash,
        boolean required,
        Optional<Component> prompt
) {

    public ResourcePackRequest {
        id = Objects.requireNonNull(id, "id");
        url = Objects.requireNonNull(url, "url").trim();
        hash = Objects.requireNonNull(hash, "hash").trim();
        prompt = Objects.requireNonNull(prompt, "prompt");
        if (url.isEmpty()) {
            throw new IllegalArgumentException("url cannot be blank");
        }
        if (hash.length() > 40) {
            throw new IllegalArgumentException("hash length must be <= 40");
        }
    }

    public static ResourcePackRequest of(String url, String hash) {
        return new ResourcePackRequest(UUID.randomUUID(), url, hash, false, Optional.empty());
    }

    public ResourcePackRequest required(boolean required) {
        return new ResourcePackRequest(id, url, hash, required, prompt);
    }

    public ResourcePackRequest prompt(Component prompt) {
        return new ResourcePackRequest(id, url, hash, required, Optional.of(prompt));
    }
}
