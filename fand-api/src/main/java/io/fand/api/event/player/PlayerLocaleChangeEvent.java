package io.fand.api.event.player;

import io.fand.api.entity.Player;
import io.fand.api.event.Event;
import java.util.Locale;
import java.util.Objects;

/**
 * Fired on the server thread after a player reports a different client locale.
 */
public record PlayerLocaleChangeEvent(Player player, String oldLocale, String newLocale) implements Event {

    public PlayerLocaleChangeEvent(Player player, String oldLocale, String newLocale) {
        this.player = Objects.requireNonNull(player, "player");
        this.oldLocale = normalize(oldLocale);
        this.newLocale = normalize(newLocale);
    }

    private static String normalize(String locale) {
        return Objects.requireNonNull(locale, "locale").toLowerCase(Locale.ROOT);
    }
}
