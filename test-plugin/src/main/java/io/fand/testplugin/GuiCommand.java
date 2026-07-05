package io.fand.testplugin;

import static io.fand.testplugin.DemoSupport.*;

import io.fand.api.command.CommandSender;
import io.fand.api.entity.Player;
import io.fand.api.plugin.PluginContext;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

@TestCommand(label = "fandgui", arguments = {"player"}, aliases = {"fgui"}, permission = "fand.testplugin.gui")
final class GuiCommand implements TestCommandHandler, TestCommandTabHandler {

    private final PluginContext context;
    private final Set<UUID> demoGuiViewers;

    GuiCommand(PluginContext context, Set<UUID> demoGuiViewers) {
        this.context = context;
        this.demoGuiViewers = demoGuiViewers;
    }

    @Override
    public void execute(CommandSender sender, String label, List<String> args) {
        if (args.size() > 1) {
            sender.sendMessage(Component.text("Usage: /fandgui [player]", NamedTextColor.RED));
            return;
        }
        Player target = args.isEmpty() ? sender instanceof Player player ? player : null : player(sender, args.get(0));
        if (target == null) {
            sender.sendMessage(Component.text("Console must provide a target player: /fandgui <player>", NamedTextColor.RED));
            return;
        }
        openDemoInventory(
                context,
                sender,
                target,
                demoGuiViewers,
                demoGuiInventory(context),
                "Opened a server-created inventory.");
    }

    @Override
    public List<String> complete(CommandSender sender, String label, List<String> args) {
        return args.size() <= 1 ? matching(playerNames(), args.isEmpty() ? "" : args.getLast()) : List.of();
    }
}
