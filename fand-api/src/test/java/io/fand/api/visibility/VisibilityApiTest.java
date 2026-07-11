package io.fand.api.visibility;

import static org.assertj.core.api.Assertions.assertThat;

import io.fand.api.entity.Player;
import io.fand.api.event.player.PlayerDisguiseStateChangeEvent;
import io.fand.api.event.player.PlayerVanishStateChangeEvent;
import java.lang.reflect.Proxy;
import java.util.UUID;
import org.junit.jupiter.api.Test;

final class VisibilityApiTest {

    @Test
    void providerContractsExposeStateAndViewerVisibility() {
        var viewer = player("Viewer");
        var target = player("Target");
        VanishService vanish = new VanishService() {
            @Override
            public boolean vanished(Player player) {
                return player == target;
            }

            @Override
            public boolean canSee(Player viewer, Player target) {
                return false;
            }
        };
        DisguiseService disguise = player -> player == target;

        assertThat(vanish.vanished(target)).isTrue();
        assertThat(vanish.canSee(viewer, target)).isFalse();
        assertThat(disguise.disguised(target)).isTrue();
    }

    @Test
    void stateEventsCarryOldAndNewValues() {
        var player = player("Player");

        assertThat(new PlayerVanishStateChangeEvent(player, false, true))
                .extracting(PlayerVanishStateChangeEvent::player,
                        PlayerVanishStateChangeEvent::oldVanished,
                        PlayerVanishStateChangeEvent::vanished)
                .containsExactly(player, false, true);
        assertThat(new PlayerDisguiseStateChangeEvent(player, true, false))
                .extracting(PlayerDisguiseStateChangeEvent::player,
                        PlayerDisguiseStateChangeEvent::oldDisguised,
                        PlayerDisguiseStateChangeEvent::disguised)
                .containsExactly(player, true, false);
    }

    private static Player player(String name) {
        var id = UUID.nameUUIDFromBytes(name.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        return (Player) Proxy.newProxyInstance(
                Player.class.getClassLoader(),
                new Class<?>[] {Player.class},
                (proxy, method, arguments) -> switch (method.getName()) {
                    case "uniqueId" -> id;
                    case "name", "toString" -> name;
                    case "hashCode" -> id.hashCode();
                    case "equals" -> proxy == arguments[0];
                    default -> throw new UnsupportedOperationException(method.toString());
                });
    }
}
