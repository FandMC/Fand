package io.fand.api.player;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.fand.api.world.WorldCreateOptions;
import io.fand.api.world.WorldTemplate;
import io.fand.api.world.generation.GenerationMode;
import io.fand.api.world.generation.WorldGeneratorSettings;
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

    @Test
    void playerProfileCarriesSkinWithoutLeakingTextureValue() {
        var skin = new PlayerSkin(" texture-value ", " signature-value ");
        var profile = new PlayerProfile(UUID.randomUUID(), " Steve ", skin);

        assertThat(profile.name()).isEqualTo("Steve");
        assertThat(profile.skin()).contains(skin);
        assertThat(profile.skinOrNull()).isSameAs(skin);
        assertThat(profile.withSkin(null).skin()).isEmpty();
        assertThat(skin.value()).isEqualTo("texture-value");
        assertThat(skin.signature()).contains("signature-value");
        assertThat(skin.toString()).doesNotContain("texture-value");
    }

    @Test
    void playerSkinRejectsBlankTextureValue() {
        assertThatThrownBy(() -> PlayerSkin.unsigned(" "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("value cannot be blank");
    }

    @Test
    void worldCreateOptionsSeparateVoidAndGeneratorModes() {
        var template = WorldCreateOptions.of(WorldTemplate.NETHER);
        var generated = WorldCreateOptions.generated(chunk -> { });
        var voidWorld = WorldCreateOptions.voidWorld();

        assertThat(template.template()).isEqualTo(WorldTemplate.NETHER);
        assertThat(template.generator()).isEmpty();
        assertThat(template.generatorSettings().mode()).isEqualTo(GenerationMode.TEMPLATE);
        assertThat(template.isVoidWorld()).isFalse();
        assertThat(generated.generator()).isPresent();
        assertThat(generated.generatorSettings().mode()).isEqualTo(GenerationMode.CUSTOM);
        assertThat(generated.isVoidWorld()).isFalse();
        var vanillaGenerated = WorldCreateOptions.vanillaGenerated(chunk -> { });
        assertThat(vanillaGenerated.generator()).isPresent();
        assertThat(vanillaGenerated.generatorSettings().mode()).isEqualTo(GenerationMode.VANILLA);
        assertThat(vanillaGenerated.generatorSettings().usesVanillaNoisePipeline()).isTrue();
        assertThat(voidWorld.template()).isEqualTo(WorldTemplate.OVERWORLD);
        assertThat(voidWorld.generatorSettings().mode()).isEqualTo(GenerationMode.EMPTY);
        assertThat(voidWorld.isVoidWorld()).isTrue();
        assertThatThrownBy(() -> generated.voidWorld(true))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("mutually exclusive");
    }

    @Test
    void generatedWorldOptionsKeepExplicitSettings() {
        var settings = WorldGeneratorSettings.builder()
                .worldHeight(0, 128)
                .seaLevel(32)
                .build();

        var generated = WorldCreateOptions.generated(chunk -> { }, settings);

        assertThat(generated.generatorSettings()).isSameAs(settings);
        assertThat(generated.generatorSettings().minY()).isEqualTo(0);
        assertThat(generated.generatorSettings().height()).isEqualTo(128);
        assertThat(generated.generatorSettings().seaLevel()).isEqualTo(32);
    }
}
