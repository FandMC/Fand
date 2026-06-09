package io.fand.api.player;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import org.junit.jupiter.api.Test;

final class PlayerAccessModelsTest {

    @Test
    void banEntryUsesNullableStorageWithOptionalAccessors() {
        var profile = new PlayerProfile(UUID.randomUUID(), "tester");
        var created = Instant.parse("2026-06-09T00:00:00Z");
        var permanent = new BanEntry(profile, created, "console", null, null);

        assertThat(permanent.profile()).isEqualTo(profile);
        assertThat(permanent.created()).isEqualTo(created);
        assertThat(permanent.source()).isEqualTo("console");
        assertThat(permanent.expires()).isEmpty();
        assertThat(permanent.reason()).isEmpty();
        assertThat(permanent.permanent()).isTrue();

        var expiry = Instant.parse("2026-06-10T00:00:00Z");
        var temporary = new BanEntry(profile, created, "console", expiry, "test ban");

        assertThat(temporary.expires()).contains(expiry);
        assertThat(temporary.reason()).contains("test ban");
        assertThat(temporary.permanent()).isFalse();
    }

    @Test
    void resourcePackRequestUsesNullablePromptWithOptionalAccessor() {
        var request = ResourcePackRequest.of(" https://example.com/pack.zip ", "abc123");

        assertThat(request.url()).isEqualTo("https://example.com/pack.zip");
        assertThat(request.hash()).isEqualTo("abc123");
        assertThat(request.required()).isFalse();
        assertThat(request.prompt()).isEmpty();

        var required = request.required(true).prompt(Component.text("Install pack"));

        assertThat(required.required()).isTrue();
        assertThat(required.prompt()).contains(Component.text("Install pack"));
    }

    @Test
    void resourcePackRequestRejectsInvalidValues() {
        assertThatThrownBy(() -> ResourcePackRequest.of(" ", "abc123"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("url cannot be blank");
        assertThatThrownBy(() -> ResourcePackRequest.of("https://example.com/pack.zip", "a".repeat(41)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("hash length");
    }
}
