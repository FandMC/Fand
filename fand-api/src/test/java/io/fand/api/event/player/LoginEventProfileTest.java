package io.fand.api.event.player;

import static org.assertj.core.api.Assertions.assertThat;

import io.fand.api.player.PlayerProfile;
import io.fand.api.player.PlayerSkin;
import java.net.InetSocketAddress;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import org.junit.jupiter.api.Test;

final class LoginEventProfileTest {

    @Test
    void profileConstructorsPreserveAuthenticatedSkin() {
        var profile = new PlayerProfile(
                UUID.randomUUID(),
                "Player",
                new PlayerSkin("skin-value", "skin-signature"));
        var address = new InetSocketAddress("127.0.0.1", 25565);

        var asyncEvent = new AsyncPlayerPreLoginEvent(
                profile,
                address,
                AsyncPlayerPreLoginEvent.Result.ALLOWED,
                Component.empty());
        var preLoginEvent = new PlayerPreLoginEvent(
                profile,
                address,
                PlayerPreLoginEvent.Result.ALLOWED,
                Component.empty());
        var loginEvent = new PlayerLoginEvent(
                profile,
                address,
                PlayerLoginEvent.Result.ALLOWED,
                Component.empty());

        assertThat(asyncEvent.profile()).isSameAs(profile);
        assertThat(preLoginEvent.profile()).isSameAs(profile);
        assertThat(loginEvent.profile()).isSameAs(profile);
    }
}
