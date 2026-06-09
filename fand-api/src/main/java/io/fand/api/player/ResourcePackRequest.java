package io.fand.api.player;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import org.jspecify.annotations.Nullable;

/**
 * Client resource-pack request sent to an online player.
 */
public final class ResourcePackRequest {

    private final UUID id;
    private final String url;
    private final String hash;
    private final boolean required;
    private final @Nullable Component prompt;

    public ResourcePackRequest(UUID id, String url, String hash, boolean required, @Nullable Component prompt) {
        this.id = Objects.requireNonNull(id, "id");
        this.url = Objects.requireNonNull(url, "url").trim();
        this.hash = Objects.requireNonNull(hash, "hash").trim();
        this.required = required;
        this.prompt = prompt;
        if (this.url.isEmpty()) {
            throw new IllegalArgumentException("url cannot be blank");
        }
        if (this.hash.length() > 40) {
            throw new IllegalArgumentException("hash length must be <= 40");
        }
    }

    public UUID id() {
        return id;
    }

    public String url() {
        return url;
    }

    public String hash() {
        return hash;
    }

    public boolean required() {
        return required;
    }

    public Optional<Component> prompt() {
        return Optional.ofNullable(prompt);
    }

    public static ResourcePackRequest of(String url, String hash) {
        return new ResourcePackRequest(UUID.randomUUID(), url, hash, false, null);
    }

    public ResourcePackRequest required(boolean required) {
        return new ResourcePackRequest(id, url, hash, required, prompt);
    }

    public ResourcePackRequest prompt(Component prompt) {
        return new ResourcePackRequest(id, url, hash, required, Objects.requireNonNull(prompt, "prompt"));
    }

    @Override
    public boolean equals(@Nullable Object other) {
        if (this == other) {
            return true;
        }
        return other instanceof ResourcePackRequest that
                && required == that.required
                && Objects.equals(id, that.id)
                && Objects.equals(url, that.url)
                && Objects.equals(hash, that.hash)
                && Objects.equals(prompt, that.prompt);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, url, hash, required, prompt);
    }

    @Override
    public String toString() {
        return "ResourcePackRequest[id=" + id + ", url=" + url + ", hash=" + hash
                + ", required=" + required + ", prompt=" + prompt + "]";
    }
}
