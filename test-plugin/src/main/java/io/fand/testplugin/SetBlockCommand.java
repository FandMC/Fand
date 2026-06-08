package io.fand.testplugin;

import static io.fand.testplugin.DemoSupport.*;

import io.fand.api.block.Block;
import io.fand.api.block.BlockType;
import io.fand.api.command.CommandCompleter;
import io.fand.api.command.CommandExecutor;
import io.fand.api.command.CommandSender;
import io.fand.api.command.CommandSpec;
import io.fand.api.entity.Player;
import io.fand.api.plugin.PluginContext;
import io.fand.api.world.World;
import java.util.List;
import java.util.Set;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

@CommandSpec(
        label = "fandsetblock",
        arguments = {"block", "x", "y", "z", "world"},
        aliases = {"fsb"},
        permission = "fand.testplugin.setblock")
final class SetBlockCommand implements CommandExecutor, CommandCompleter {

    private final PluginContext context;

    SetBlockCommand(PluginContext context) {
        this.context = context;
    }

    @Override
    public void execute(CommandSender sender, String label, List<String> args) {
        if (args.size() != 1 && args.size() != 4 && args.size() != 5) {
            sender.sendMessage(Component.text("Usage: /fandsetblock <block> [x y z] [world]", NamedTextColor.RED));
            return;
        }
        BlockType type = blockType(sender, args.get(0));
        if (type == null) {
            return;
        }

        World world;
        int x;
        int y;
        int z;
        if (args.size() == 1) {
            if (!(sender instanceof Player player)) {
                sender.sendMessage(Component.text("Console must provide coordinates: /fandsetblock <block> <x> <y> <z> [world]", NamedTextColor.RED));
                return;
            }
            var loc = player.location();
            world = player.world();
            x = loc.blockX();
            y = loc.blockY() - 1;
            z = loc.blockZ();
        } else {
            Integer parsedX = parseInt(sender, args.get(1), "x");
            Integer parsedY = parseInt(sender, args.get(2), "y");
            Integer parsedZ = parseInt(sender, args.get(3), "z");
            if (parsedX == null || parsedY == null || parsedZ == null) {
                return;
            }
            x = parsedX;
            y = parsedY;
            z = parsedZ;
            world = world(sender, args.size() == 5 ? args.get(4) : null, sender instanceof Player player ? player : null);
            if (world == null) {
                return;
            }
        }

        Block block = world.blockAt(x, y, z);
        BlockType before = block.type();
        boolean changed = block.setType(type);
        sender.sendMessage(Component.text((changed ? "Set " : "Tried to set ")
                + blockName(before) + " -> " + blockName(type)
                + " at " + world.name() + " " + x + "," + y + "," + z, changed ? NamedTextColor.GREEN : NamedTextColor.YELLOW));
        context.logger().info("/fandsetblock by {} set {} at {} {},{},{} (changed={})",
                sender.name(), type.key(), world.key(), x, y, z, changed);
    }

    @Override
    public List<String> complete(CommandSender sender, String label, List<String> args) {
        if (args.size() <= 1) {
            return matching(SAMPLE_BLOCKS, args.isEmpty() ? "" : args.getLast());
        }
        if (args.size() == 5) {
            return matching(worldKeys(), args.getLast());
        }
        if (sender instanceof Player player && args.size() <= 4) {
            var loc = player.location();
            var coords = List.of(Integer.toString(loc.blockX()), Integer.toString(loc.blockY() - 1), Integer.toString(loc.blockZ()));
            return matching(coords, args.getLast());
        }
        return List.of();
    }
}
