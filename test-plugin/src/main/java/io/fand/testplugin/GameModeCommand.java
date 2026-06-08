package io.fand.testplugin;

import static io.fand.testplugin.DemoSupport.*;

import io.fand.api.command.CommandCompleter;
import io.fand.api.command.CommandExecutor;
import io.fand.api.command.CommandSender;
import io.fand.api.command.CommandSpec;
import io.fand.api.entity.GameMode;
import io.fand.api.entity.Player;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

@CommandSpec(label = "fandmode", arguments = {"mode", "player"}, aliases = {"fgm"}, permission = "fand.testplugin.mode")
final class GameModeCommand implements CommandExecutor, CommandCompleter {

    @Override
    public void execute(CommandSender sender, String label, List<String> args) {
        if (args.isEmpty() || args.size() > 2) {
            sender.sendMessage(Component.text("Usage: /fandmode <survival|creative|adventure|spectator> [player]", NamedTextColor.RED));
            return;
        }
        GameMode mode;
        try {
            mode = GameMode.valueOf(args.get(0).trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            sender.sendMessage(Component.text("Unknown game mode: " + args.get(0), NamedTextColor.RED));
            return;
        }
        Player target = args.size() == 2 ? player(sender, args.get(1)) : sender instanceof Player player ? player : null;
        if (target == null) {
            sender.sendMessage(Component.text("Console must provide a target player: /fandmode <mode> <player>", NamedTextColor.RED));
            return;
        }
        target.setGameMode(mode);
        target.sendMessage(Component.text("Game mode set to " + mode.name().toLowerCase(Locale.ROOT) + ".", NamedTextColor.GREEN));
        if (target != sender) {
            sender.sendMessage(Component.text("Set " + target.name() + " to " + mode.name().toLowerCase(Locale.ROOT) + ".", NamedTextColor.GREEN));
        }
    }

    @Override
    public List<String> complete(CommandSender sender, String label, List<String> args) {
        if (args.size() <= 1) {
            return matching(List.of("survival", "creative", "adventure", "spectator"), args.isEmpty() ? "" : args.getLast());
        }
        if (args.size() == 2) {
            return matching(playerNames(), args.getLast());
        }
        return List.of();
    }
}
