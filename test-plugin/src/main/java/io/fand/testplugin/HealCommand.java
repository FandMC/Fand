package io.fand.testplugin;

import static io.fand.testplugin.DemoSupport.*;

import io.fand.api.command.CommandSender;
import io.fand.api.entity.Player;
import io.fand.api.plugin.PluginContext;
import java.util.List;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

@TestCommand(label = "fandheal", arguments = {"player"}, aliases = {"fheal"}, permission = "fand.testplugin.heal")
final class HealCommand implements TestCommandHandler, TestCommandTabHandler {

    private final PluginContext context;

    HealCommand(PluginContext context) {
        this.context = context;
    }

    @Override
    public void execute(CommandSender sender, String label, List<String> args) {
        if (args.size() > 1) {
            sender.sendMessage(Component.text("Usage: /fandheal [player]", NamedTextColor.RED));
            return;
        }
        Player target = args.isEmpty() ? sender instanceof Player player ? player : null : player(sender, args.get(0));
        if (target == null) {
            sender.sendMessage(Component.text("Console must provide a target player: /fandheal <player>", NamedTextColor.RED));
            return;
        }
        target.setHealth(target.maxHealth());
        target.setFoodLevel(20);
        target.setSaturation(20.0F);
        int xp = Math.max(0, context.config().getInt("defaults.heal-xp", 5));
        if (xp > 0) {
            target.giveExperience(xp);
        }
        target.sendMessage(Component.text("Healed by test-plugin.", NamedTextColor.GREEN));
        if (target != sender) {
            sender.sendMessage(Component.text("Healed " + target.name() + ".", NamedTextColor.GREEN));
        }
    }

    @Override
    public List<String> complete(CommandSender sender, String label, List<String> args) {
        return args.size() <= 1 ? matching(playerNames(), args.isEmpty() ? "" : args.getLast()) : List.of();
    }
}
