package io.fand.api.player;

import java.util.Objects;
import java.util.Optional;
import org.jspecify.annotations.Nullable;

/**
 * Mojang texture property used for player skins and profile-bearing items.
 */
public final class PlayerSkin {

    private final String value;
    private final @Nullable String signature;

    public PlayerSkin(String value, @Nullable String signature) {
        this.value = Objects.requireNonNull(value, "value").trim();
        var trimmedSignature = signature == null ? null : signature.trim();
        this.signature = trimmedSignature == null || trimmedSignature.isEmpty() ? null : trimmedSignature;
        if (this.value.isEmpty()) {
            throw new IllegalArgumentException("value cannot be blank");
        }
    }

    public static PlayerSkin unsigned(String value) {
        return new PlayerSkin(value, null);
    }

    public String value() {
        return value;
    }

    public Optional<String> signature() {
        return Optional.ofNullable(signature);
    }

    public @Nullable String signatureOrNull() {
        return signature;
    }

    @Override
    public boolean equals(@Nullable Object other) {
        if (this == other) {
            return true;
        }
        return other instanceof PlayerSkin that
                && value.equals(that.value)
                && Objects.equals(signature, that.signature);
    }

    @Override
    public int hashCode() {
        return Objects.hash(value, signature);
    }

    @Override
    public String toString() {
        return "PlayerSkin[value=<redacted>, signed=" + (signature != null) + "]";
    }
}
