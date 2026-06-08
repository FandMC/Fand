package io.fand.api.event.player;

import io.fand.api.entity.Player;
import io.fand.api.event.Event;
import java.util.Locale;
import java.util.Objects;

/**
 * Fired on the server thread after a player reports a different client locale.
 */
public final class PlayerLocaleChangeEvent implements Event {

    private final Player player;
    private final String oldLocale;
    private final String newLocale;

    public PlayerLocaleChangeEvent(Player player, String oldLocale, String newLocale) {
        this.player = Objects.requireNonNull(player, "player");
        this.oldLocale = normalize(oldLocale);
        this.newLocale = normalize(newLocale);
    }

    public Player player() {
        return player;
    }

    public String oldLocale() {
        return oldLocale;
    }

    public String newLocale() {
        return newLocale;
    }

    private static String normalize(String locale) {
        return Objects.requireNonNull(locale, "locale").toLowerCase(Locale.ROOT);
    }
}
