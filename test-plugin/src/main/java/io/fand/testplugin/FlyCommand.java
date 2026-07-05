package io.fand.testplugin;

import static io.fand.testplugin.DemoSupport.*;

import io.fand.api.command.CommandSender;
import io.fand.api.entity.Player;
import java.util.List;
import java.util.Locale;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

@TestCommand(label = "fandfly", arguments = {"state", "player"}, aliases = {"ffly"}, permission = "fand.testplugin.fly")
final class FlyCommand implements TestCommandHandler, TestCommandTabHandler {

    @Override
    public void execute(CommandSender sender, String label, List<String> args) {
        if (args.size() > 2) {
            sender.sendMessage(Component.text("Usage: /fandfly [on|off|toggle] [player]", NamedTextColor.RED));
            return;
        }
        String mode = args.isEmpty() ? "toggle" : args.get(0).toLowerCase(Locale.ROOT);
        Player target = args.size() == 2 ? player(sender, args.get(1)) : sender instanceof Player player ? player : null;
        if (target == null) {
            sender.sendMessage(Component.text("Console must provide a target player: /fandfly <on|off|toggle> <player>", NamedTextColor.RED));
            return;
        }
        boolean enabled;
        if (mode.equals("toggle")) {
            enabled = !target.allowFlight();
        } else if (mode.equals("on") || mode.equals("true")) {
            enabled = true;
        } else if (mode.equals("off") || mode.equals("false")) {
            enabled = false;
        } else {
            sender.sendMessage(Component.text("Flight mode must be on, off, or toggle", NamedTextColor.RED));
            return;
        }
        target.setAllowFlight(enabled);
        target.setFlying(enabled);
        target.sendMessage(Component.text("Flight " + (enabled ? "enabled." : "disabled."), enabled ? NamedTextColor.GREEN : NamedTextColor.YELLOW));
        if (target != sender) {
            sender.sendMessage(Component.text("Flight " + (enabled ? "enabled" : "disabled") + " for " + target.name() + ".", NamedTextColor.GREEN));
        }
    }

    @Override
    public List<String> complete(CommandSender sender, String label, List<String> args) {
        if (args.size() <= 1) {
            return matching(List.of("toggle", "on", "off"), args.isEmpty() ? "" : args.getLast());
        }
        if (args.size() == 2) {
            return matching(playerNames(), args.getLast());
        }
        return List.of();
    }
}
