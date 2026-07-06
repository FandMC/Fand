package io.fand.testplugin;

import static io.fand.testplugin.DemoSupport.*;

import io.fand.api.Fand;
import io.fand.api.command.CommandSender;
import io.fand.api.item.ItemStack;
import io.fand.api.item.ItemType;
import io.fand.api.item.ItemTypes;
import io.fand.api.plugin.PluginContext;
import java.util.ArrayList;
import java.util.List;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

@TestCommand(label = "fanditem", arguments = {"player", "item", "amount"}, aliases = {"fitem"}, permission = "fand.testplugin.item")
final class ComponentItemCommand implements TestCommandHandler, TestCommandTabHandler {

    private final PluginContext context;

    ComponentItemCommand(PluginContext context) {
        this.context = context;
    }

    @Override
    public void execute(CommandSender sender, String label, List<String> args) {
        TargetedArgs targeted = targetedArgs(sender, args, "/fanditem <player> [item] [amount]");
        if (targeted == null) {
            return;
        }
        List<String> itemArgs = targeted.args();
        ItemType type = itemArgs.isEmpty() ? ItemTypes.of("minecraft:diamond") : itemType(sender, itemArgs.getFirst());
        if (type == null) {
            return;
        }
        int amount = 1;
        if (itemArgs.size() >= 2) {
            Integer parsed = parseInt(sender, itemArgs.get(1), "amount");
            if (parsed == null) {
                return;
            }
            amount = parsed;
        }
        int limit = Math.max(1, context.config().intValue("limits.max-give-amount", 2304));
        if (amount < 1 || amount > limit) {
            sender.sendMessage(Component.text("Amount must be in 1.." + limit, NamedTextColor.RED));
            return;
        }
        ItemStack demo = demoComponentItem(type, sender.name());
        int given = give(targeted.player(), demo, amount);
        int leftover = amount - given;
        targeted.player().sendMessage(Component.text("Received " + given + " component item"
                + (leftover > 0 ? " (" + leftover + " did not fit)" : ""), NamedTextColor.GREEN));
        if (targeted.player() != sender) {
            sender.sendMessage(Component.text("Gave " + given + " component item to " + targeted.player().name()
                    + (leftover > 0 ? " (" + leftover + " did not fit)" : ""), NamedTextColor.GREEN));
        }
    }

    @Override
    public List<String> complete(CommandSender sender, String label, List<String> args) {
        if (args.size() <= 1) {
            var values = new ArrayList<>(playerNames());
            values.addAll(SAMPLE_ITEMS);
            return matching(values, args.isEmpty() ? "" : args.getLast());
        }
        if (args.size() == 2 && Fand.server().player(args.getFirst()).isPresent()) {
            return matching(SAMPLE_ITEMS, args.getLast());
        }
        if (args.size() == 2 || args.size() == 3) {
            return matching(List.of("1", "8", "16", "32", "64", "99"), args.getLast());
        }
        return List.of();
    }
}
