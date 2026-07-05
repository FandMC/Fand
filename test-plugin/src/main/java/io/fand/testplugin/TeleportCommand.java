package io.fand.testplugin;

import static io.fand.testplugin.DemoSupport.*;

import io.fand.api.command.CommandSender;
import io.fand.api.entity.Player;
import io.fand.api.plugin.PluginContext;
import java.util.List;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

@TestCommand(label = "fandtp", arguments = {"x", "y", "z"}, aliases = {"ftp"}, permission = "fand.testplugin.tp")
final class TeleportCommand implements TestCommandHandler, TestCommandTabHandler {

    private final PluginContext context;

    TeleportCommand(PluginContext context) {
        this.context = context;
    }

    @Override
    public void execute(CommandSender sender, String label, List<String> args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("/fandtp must be run by a player", NamedTextColor.RED));
            return;
        }
        if (args.size() != 3) {
            sender.sendMessage(Component.text("Usage: /fandtp <x> <y> <z>", NamedTextColor.RED));
            return;
        }
        Double x = parseDouble(sender, args.get(0), "x");
        Double y = parseDouble(sender, args.get(1), "y");
        Double z = parseDouble(sender, args.get(2), "z");
        if (x == null || y == null || z == null) {
            return;
        }
        var destination = player.world().at(x, y, z, player.location().yaw(), player.location().pitch());
        player.teleport(destination).whenComplete((ok, failure) -> {
            if (failure != null) {
                context.logger().warn("Teleport failed for {}", player.name(), failure);
                player.sendMessage(Component.text("Teleport failed.", NamedTextColor.RED));
                return;
            }
            player.sendMessage(Component.text(Boolean.TRUE.equals(ok) ? "Teleported." : "Teleport rejected.",
                    Boolean.TRUE.equals(ok) ? NamedTextColor.GREEN : NamedTextColor.YELLOW));
        });
    }

    @Override
    public List<String> complete(CommandSender sender, String label, List<String> args) {
        if (!(sender instanceof Player player) || args.size() > 3) {
            return List.of();
        }
        var loc = player.location();
        var values = List.of(Integer.toString(loc.blockX()), Integer.toString(loc.blockY()), Integer.toString(loc.blockZ()));
        return args.isEmpty() ? values : matching(values, args.getLast());
    }
}
