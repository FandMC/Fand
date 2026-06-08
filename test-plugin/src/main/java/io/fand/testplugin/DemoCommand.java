package io.fand.testplugin;

import static io.fand.testplugin.DemoSupport.*;

import io.fand.api.Fand;
import io.fand.api.Server;
import io.fand.api.command.CommandExecutor;
import io.fand.api.command.CommandSender;
import io.fand.api.command.CommandSpec;
import io.fand.api.entity.Player;
import io.fand.api.plugin.Plugin;
import io.fand.api.plugin.PluginContext;
import java.util.List;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

@CommandSpec(label = "fanddemo", arguments = {}, aliases = {"fdemo"}, permission = "fand.testplugin.demo")
final class DemoCommand implements CommandExecutor {

    private final PluginContext context;

    DemoCommand(PluginContext context) {
        this.context = context;
    }

    @Override
    public void execute(CommandSender sender, String label, List<String> args) {
        Server server = Fand.server();
        sender.sendMessage(Component.text("Fand API demo", NamedTextColor.GOLD));
        sender.sendMessage(Component.text("Brand=" + server.brand()
                + ", version=" + server.version()
                + ", phase=" + server.phase(), NamedTextColor.GRAY));
        sender.sendMessage(Component.text("Players=" + server.onlinePlayers()
                + "/" + server.maxPlayers()
                + ", worlds=" + worldKeys(), NamedTextColor.GRAY));
        sender.sendMessage(Component.text("Plugin data=" + context.dataDirectory(), NamedTextColor.DARK_GRAY));
        if (sender instanceof Player player) {
            var loc = player.location();
            sender.sendMessage(Component.text("You: world=" + player.world().name()
                    + " xyz=" + loc.blockX() + "," + loc.blockY() + "," + loc.blockZ()
                    + " gm=" + player.gameMode()
                    + " hp=" + trim(player.health()) + "/" + trim(player.maxHealth())
                    + " food=" + player.foodLevel(), NamedTextColor.AQUA));
            sender.sendMessage(Component.text("Held=" + stackName(player.inventory().heldItem())
                    + ", hotbar=" + player.inventory().selectedSlot()
                    + ", openInventory=" + player.openInventory().map(inv -> inv.type().name()).orElse("none"), NamedTextColor.AQUA));
        }
    }
}
