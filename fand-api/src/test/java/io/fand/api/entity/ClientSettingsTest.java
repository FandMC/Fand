package io.fand.api.entity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

import java.util.Set;
import org.junit.jupiter.api.Test;

class ClientSettingsTest {

    @Test
    void exposesLocaleAndViewDistance() {
        var settings = new ClientSettings("en_us", 12);

        assertThat(settings.locale()).isEqualTo("en_us");
        assertThat(settings.viewDistance()).isEqualTo(12);
        assertThat(settings.chatVisibility()).isEqualTo(ClientChatVisibility.FULL);
        assertThat(settings.chatColors()).isTrue();
        assertThat(settings.skinParts()).isEmpty();
        assertThat(settings.mainHand()).isEqualTo(ClientMainHand.RIGHT);
        assertThat(settings.textFilteringEnabled()).isFalse();
        assertThat(settings.serverListingAllowed()).isFalse();
        assertThat(settings.particleStatus()).isEqualTo(ClientParticleStatus.ALL);
    }

    @Test
    void exposesFullClientSettings() {
        var settings = new ClientSettings(
                "zh_cn",
                16,
                ClientChatVisibility.SYSTEM,
                false,
                Set.of(ClientSkinPart.CAPE, ClientSkinPart.HAT),
                ClientMainHand.LEFT,
                true,
                true,
                ClientParticleStatus.MINIMAL);

        assertThat(settings.locale()).isEqualTo("zh_cn");
        assertThat(settings.viewDistance()).isEqualTo(16);
        assertThat(settings.chatVisibility()).isEqualTo(ClientChatVisibility.SYSTEM);
        assertThat(settings.chatColors()).isFalse();
        assertThat(settings.skinParts()).containsExactlyInAnyOrder(ClientSkinPart.CAPE, ClientSkinPart.HAT);
        assertThat(settings.mainHand()).isEqualTo(ClientMainHand.LEFT);
        assertThat(settings.textFilteringEnabled()).isTrue();
        assertThat(settings.serverListingAllowed()).isTrue();
        assertThat(settings.particleStatus()).isEqualTo(ClientParticleStatus.MINIMAL);
    }

    @Test
    void rejectsNullLocale() {
        assertThatNullPointerException().isThrownBy(() -> new ClientSettings(null, 8));
    }

    @Test
    void rejectsNullFields() {
        assertThatNullPointerException().isThrownBy(() -> new ClientSettings(
                "en_us",
                8,
                null,
                true,
                Set.of(),
                ClientMainHand.RIGHT,
                false,
                false,
                ClientParticleStatus.ALL));
        assertThatNullPointerException().isThrownBy(() -> new ClientSettings(
                "en_us",
                8,
                ClientChatVisibility.FULL,
                true,
                null,
                ClientMainHand.RIGHT,
                false,
                false,
                ClientParticleStatus.ALL));
        assertThatNullPointerException().isThrownBy(() -> new ClientSettings(
                "en_us",
                8,
                ClientChatVisibility.FULL,
                true,
                Set.of(),
                null,
                false,
                false,
                ClientParticleStatus.ALL));
        assertThatNullPointerException().isThrownBy(() -> new ClientSettings(
                "en_us",
                8,
                ClientChatVisibility.FULL,
                true,
                Set.of(),
                ClientMainHand.RIGHT,
                false,
                false,
                null));
    }
}
