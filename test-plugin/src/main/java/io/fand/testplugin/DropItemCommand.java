package io.fand.testplugin;

import static io.fand.testplugin.DemoSupport.*;

import io.fand.api.command.CommandCompleter;
import io.fand.api.command.CommandExecutor;
import io.fand.api.command.CommandSender;
import io.fand.api.command.CommandSpec;
import io.fand.api.entity.EntitySpawnOptions;
import io.fand.api.entity.Player;
import io.fand.api.item.ItemStack;
import io.fand.api.item.ItemType;
import io.fand.api.plugin.PluginContext;
import io.fand.api.world.Location;
import io.fand.api.world.World;
import java.util.List;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

@CommandSpec(
        label = "fanddropitem",
        arguments = {"item", "amount", "x", "y", "z", "world"},
        aliases = {"fdrop"},
        permission = "fand.testplugin.dropitem")
final class DropItemCommand implements CommandExecutor, CommandCompleter {

    private final PluginContext context;

    DropItemCommand(PluginContext context) {
        this.context = context;
    }

    @Override
    public void execute(CommandSender sender, String label, List<String> args) {
        int optionStart = EntitySpawnOptionParser.firstOptionIndex(args);
        var positional = args.subList(0, optionStart);
        if (positional.size() != 1 && positional.size() != 2 && positional.size() != 5 && positional.size() != 6) {
            sender.sendMessage(Component.text(
                    "Usage: /fanddropitem <item> [amount] [x y z] [world] [options...]",
                    NamedTextColor.RED));
            return;
        }
        EntitySpawnOptions options = EntitySpawnOptionParser.parse(sender, args, optionStart);
        if (options == null) {
            return;
        }
        ItemType type = itemType(sender, positional.get(0));
        if (type == null) {
            return;
        }
        int amount = amount(sender, positional);
        if (amount < 0) {
            return;
        }
        Location location = dropLocation(sender, positional);
        if (location == null) {
            return;
        }

        ItemStack item = type.stack(amount);
        location.world().dropItem(location, item, options).whenComplete((dropped, failure) -> {
            if (failure != null) {
                context.logger().warn("Item drop failed for {}", type.key().asString(), failure);
                sender.sendMessage(Component.text("Item drop failed: " + failure.getMessage(), NamedTextColor.RED));
                return;
            }
            if (dropped.isEmpty()) {
                sender.sendMessage(Component.text("No item was dropped.", NamedTextColor.YELLOW));
                return;
            }
            var entity = dropped.get();
            sender.sendMessage(Component.text("Dropped " + stackName(entity.item())
                    + " id=" + entity.entityId()
                    + " at " + compactLocation(location), NamedTextColor.GREEN));
            context.logger().info("/fanddropitem by {} dropped {} uuid={} at {} {},{},{}",
                    sender.name(), stackName(entity.item()), entity.uniqueId(), location.world().key(),
                    trim(location.x()), trim(location.y()), trim(location.z()));
        });
    }

    @Override
    public List<String> complete(CommandSender sender, String label, List<String> args) {
        if (!args.isEmpty() && args.getLast().startsWith("--")) {
            return matching(EntitySpawnOptionParser.FLAGS, args.getLast());
        }
        if (args.size() <= 1) {
            return matching(SAMPLE_ITEMS, args.isEmpty() ? "" : args.getLast());
        }
        if (args.size() == 2) {
            return matching(List.of("1", "8", "16", "32", "64"), args.getLast());
        }
        if (args.size() == 6) {
            return matching(worldKeys(), args.getLast());
        }
        if (sender instanceof Player player && args.size() <= 5) {
            var loc = player.location();
            var coords = List.of(
                    Integer.toString(loc.blockX()),
                    Integer.toString(loc.blockY()),
                    Integer.toString(loc.blockZ()));
            return matching(coords, args.getLast());
        }
        return List.of();
    }

    private int amount(CommandSender sender, List<String> args) {
        if (args.size() != 2 && args.size() != 6) {
            return 1;
        }
        Integer parsed = parseInt(sender, args.get(1), "amount");
        if (parsed == null) {
            return -1;
        }
        if (parsed < 1 || parsed > 64) {
            sender.sendMessage(Component.text("Amount must be in 1..64", NamedTextColor.RED));
            return -1;
        }
        return parsed;
    }

    private Location dropLocation(CommandSender sender, List<String> args) {
        if (args.size() == 1 || args.size() == 2) {
            if (!(sender instanceof Player player)) {
                sender.sendMessage(Component.text(
                        "Console must provide coordinates: /fanddropitem <item> <amount> <x> <y> <z> [world]",
                        NamedTextColor.RED));
                return null;
            }
            var loc = player.location();
            return player.world().at(loc.x(), loc.y(), loc.z(), loc.yaw(), loc.pitch());
        }

        int offset = args.size() == 6 ? 2 : 1;
        Double x = parseDouble(sender, args.get(offset), "x");
        Double y = parseDouble(sender, args.get(offset + 1), "y");
        Double z = parseDouble(sender, args.get(offset + 2), "z");
        if (x == null || y == null || z == null) {
            return null;
        }
        World world = world(sender, args.size() == 6 ? args.get(5) : null, sender instanceof Player player ? player : null);
        return world == null ? null : world.at(x, y, z);
    }
}
