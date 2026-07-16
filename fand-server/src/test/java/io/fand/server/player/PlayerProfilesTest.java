package io.fand.server.player;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.common.collect.ImmutableMultimap;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import com.mojang.authlib.properties.PropertyMap;
import io.fand.api.player.PlayerProfile;
import io.fand.api.player.PlayerSkin;
import java.util.UUID;
import org.junit.jupiter.api.Test;

final class PlayerProfilesTest {

    @Test
    void convertsTexturePropertiesToApiProfile() {
        var id = UUID.randomUUID();
        var profile = new GameProfile(id, "Steve", new PropertyMap(ImmutableMultimap.of(
                "textures",
                new Property("textures", "skin-value", "skin-signature"))));

        var apiProfile = PlayerProfiles.fromVanilla(profile);

        assertThat(apiProfile.uniqueId()).isEqualTo(id);
        assertThat(apiProfile.name()).isEqualTo("Steve");
        assertThat(apiProfile.skin()).contains(new PlayerSkin("skin-value", "skin-signature"));
    }

    @Test
    void convertsApiProfileBackToGameProfileWithTextures() {
        var id = UUID.randomUUID();
        var skin = new PlayerSkin("skin-value", "skin-signature");

        var profile = PlayerProfiles.toGameProfile(new PlayerProfile(id, "Alex", skin));

        assertThat(profile.id()).isEqualTo(id);
        assertThat(profile.name()).isEqualTo("Alex");
        assertThat(profile.properties().get("textures"))
                .containsExactly(new Property("textures", "skin-value", "skin-signature"));
    }

    @Test
    void canReplaceAndClearSkinOnExistingProfile() {
        var profile = new GameProfile(UUID.randomUUID(), "Player", new PropertyMap(ImmutableMultimap.of(
                "extra",
                new Property("extra", "kept"))));

        var withSkin = PlayerProfiles.withSkin(profile, PlayerSkin.unsigned("skin-value"));
        var withoutSkin = PlayerProfiles.withoutSkin(withSkin);

        assertThat(withSkin.properties().get("textures"))
                .containsExactly(new Property("textures", "skin-value", null));
        assertThat(withSkin.properties().get("extra"))
                .containsExactly(new Property("extra", "kept"));
        assertThat(withoutSkin.id()).isEqualTo(profile.id());
        assertThat(withoutSkin.name()).isEqualTo("Player");
        assertThat(withoutSkin.properties().get("textures")).isEmpty();
        assertThat(withoutSkin.properties().get("extra"))
                .containsExactly(new Property("extra", "kept"));
    }

    @Test
    void appliesEventProfileWithoutDroppingUnrelatedProperties() {
        var original = new GameProfile(UUID.randomUUID(), "Original", new PropertyMap(ImmutableMultimap.of(
                "extra",
                new Property("extra", "kept"))));
        var replacementId = UUID.randomUUID();
        var replacement = new PlayerProfile(
                replacementId,
                "Replacement",
                new PlayerSkin("skin-value", "skin-signature"));

        var applied = PlayerProfiles.applyTo(original, replacement);

        assertThat(applied.id()).isEqualTo(replacementId);
        assertThat(applied.name()).isEqualTo("Replacement");
        assertThat(applied.properties().get("textures"))
                .containsExactly(new Property("textures", "skin-value", "skin-signature"));
        assertThat(applied.properties().get("extra"))
                .containsExactly(new Property("extra", "kept"));
    }
}
