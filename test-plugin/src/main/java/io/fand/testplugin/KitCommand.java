package io.fand.testplugin;

import static io.fand.testplugin.DemoSupport.*;

import io.fand.api.Fand;
import io.fand.api.command.CommandCompleter;
import io.fand.api.command.CommandExecutor;
import io.fand.api.command.CommandSender;
import io.fand.api.command.CommandSpec;
import io.fand.api.entity.Player;
import io.fand.api.item.ItemKey;
import io.fand.api.item.ItemTypes;
import io.fand.api.plugin.PluginContext;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

@CommandSpec(label = "fandkit", arguments = {"player"}, aliases = {"fkit"}, permission = "fand.testplugin.kit")
final class KitCommand implements CommandExecutor, CommandCompleter {

    private final PluginContext context;
    private final Set<UUID> demoGuiViewers;

    KitCommand(PluginContext context, Set<UUID> demoGuiViewers) {
        this.context = context;
        this.demoGuiViewers = demoGuiViewers;
    }

    @Override
    public void execute(CommandSender sender, String label, List<String> args) {
        if (args.size() > 1) {
            sender.sendMessage(Component.text("Usage: /fandkit [player]", NamedTextColor.RED));
            return;
        }
        Player target = args.isEmpty() ? sender instanceof Player player ? player : null : player(sender, args.getFirst());
        if (target == null) {
            sender.sendMessage(Component.text("Console must provide a target player: /fandkit <player>", NamedTextColor.RED));
            return;
        }

        int accepted = give(target, demoComponentItem(ItemTypes.of(ItemKey.DIAMOND), sender.name()), 1);
        accepted += give(target, demoKitNavigator(ItemTypes.of(ItemKey.COMPASS), target.name()), 1);
        accepted += give(target, demoKitBook(ItemTypes.of(ItemKey.WRITTEN_BOOK), target.name()), 1);
        accepted += give(target, demoKitSnack(ItemTypes.of(ItemKey.GOLDEN_APPLE)), 8);
        accepted += give(target, demoMercyWeapon(), 1);
        accepted += give(target, demoPlunderWeapon(), 1);

        sendKitPresentation(context, target);
        openDemoInventory(
                context,
                sender,
                target,
                demoGuiViewers,
                demoKitInventory(context, target.name()),
                "Opened the kit inventory. Right-click the navigator compass to reopen it.");
        sender.sendMessage(Component.text("Prepared Fand kit for " + target.name() + " (" + accepted + " items accepted).", NamedTextColor.GREEN));
    }

    @Override
    public List<String> complete(CommandSender sender, String label, List<String> args) {
        return args.size() <= 1 ? matching(playerNames(), args.isEmpty() ? "" : args.getLast()) : List.of();
    }
}
