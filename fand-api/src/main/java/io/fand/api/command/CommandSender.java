package io.fand.api.command;

/**
 * The actor invoking a command. May be a player, the console, or a command block;
 * implementations vary by source.
 */
public interface CommandSender {

    String name();

    void sendMessage(net.kyori.adventure.text.Component message);

    boolean hasPermission(String permission);
}
