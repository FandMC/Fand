package io.fand.api.command;

import net.kyori.adventure.audience.Audience;

/**
 * The actor invoking a command. May be a player, the console, or a command block;
 * implementations vary by source.
 *
 * <p>{@code CommandSender} is an Adventure {@link Audience}: in addition to
 * {@link #sendMessage(net.kyori.adventure.text.Component)}, players will also
 * receive titles, action-bar text, sounds, and boss bars sent through the
 * audience surface. Non-player senders silently ignore packets that don't
 * apply (e.g. console drops titles).
 */
public interface CommandSender extends Audience {

    String name();

    @Override
    void sendMessage(net.kyori.adventure.text.Component message);

    boolean can(String permission);

    default boolean allowed(String permission) {
        return can(permission);
    }
}
