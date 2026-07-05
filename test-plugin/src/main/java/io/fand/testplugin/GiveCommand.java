package io.fand.testplugin;

import static io.fand.testplugin.DemoSupport.*;

import io.fand.api.command.CommandSender;
import io.fand.api.entity.Player;
import io.fand.api.item.ItemType;
import io.fand.api.plugin.PluginContext;
import java.util.List;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

@TestCommand(label = "fandgive", arguments = {"item", "amount", "player"}, aliases = {"fgive"}, permission = "fand.testplugin.give")
final class GiveCommand implements TestCommandHandler, TestCommandTabHandler {

    private final PluginContext context;

    GiveCommand(PluginContext context) {
        this.context = context;
    }

    @Override
    public void execute(CommandSender sender, String label, List<String> args) {
        if (args.isEmpty() || args.size() > 3) {
            sender.sendMessage(Component.text("Usage: /fandgive <item> [amount] [player]", NamedTextColor.RED));
            return;
        }
        ItemType type = itemType(sender, args.get(0));
        if (type == null) {
            return;
        }
        int amount = 1;
        if (args.size() >= 2) {
            Integer parsed = parseInt(sender, args.get(1), "amount");
            if (parsed == null) {
                return;
            }
            amount = parsed;
        }
        int limit = Math.max(1, context.config().getInt("limits.max-give-amount", 2304));
        if (amount < 1 || amount > limit) {
            sender.sendMessage(Component.text("Amount must be in 1.." + limit, NamedTextColor.RED));
            return;
        }
        Player target = args.size() == 3 ? player(sender, args.get(2)) : sender instanceof Player player ? player : null;
        if (target == null) {
            sender.sendMessage(Component.text("Console must provide a target player: /fandgive <item> [amount] <player>", NamedTextColor.RED));
            return;
        }

        int given = give(target, type, amount);
        int leftover = amount - given;
        target.sendMessage(Component.text("Received " + given + " x " + itemName(type)
                + (leftover > 0 ? " (" + leftover + " did not fit)" : ""), NamedTextColor.GREEN));
        if (target != sender) {
            sender.sendMessage(Component.text("Gave " + given + " x " + itemName(type) + " to " + target.name()
                    + (leftover > 0 ? " (" + leftover + " did not fit)" : ""), NamedTextColor.GREEN));
        }
        context.logger().info("/fandgive by {} -> {} x {} to {}", sender.name(), given, type.key(), target.name());
    }

    @Override
    public List<String> complete(CommandSender sender, String label, List<String> args) {
        if (args.size() <= 1) {
            return matching(SAMPLE_ITEMS, args.isEmpty() ? "" : args.getLast());
        }
        if (args.size() == 2) {
            return matching(List.of("1", "8", "16", "32", "64"), args.getLast());
        }
        if (args.size() == 3) {
            return matching(playerNames(), args.getLast());
        }
        return List.of();
    }
}
