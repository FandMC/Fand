package io.fand.testplugin;

import io.fand.api.command.CommandExecutor;
import io.fand.api.command.CommandSender;
import io.fand.api.command.CommandSpec;
import io.fand.api.entity.Player;
import io.fand.api.plugin.PluginContext;
import java.util.List;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

@CommandSpec(label = "fandtime", permission = "fand.testplugin.time")
final class TimeCommand implements CommandExecutor {

    private final PluginContext context;

    TimeCommand(PluginContext context) {
        this.context = context;
    }

    @Override
    public void execute(CommandSender sender, String label, List<String> args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("Must be run by a player", NamedTextColor.RED));
            return;
        }
        if (args.isEmpty()) {
            var world = player.world();
            long time = world.time() % 24000;
            sender.sendMessage(Component.text("Current time: " + time + " (cycle: " +
                    (world.dayNightCycleEnabled() ? "on" : "off") + ")", NamedTextColor.GREEN));
            return;
        }
        var world = player.world();
        var timeType = args.get(0).toLowerCase();
        switch (timeType) {
            case "day" -> {
                world.setTime(1000);
                sender.sendMessage(Component.text("Time set to day", NamedTextColor.GREEN));
            }
            case "noon" -> {
                world.setTime(6000);
                sender.sendMessage(Component.text("Time set to noon", NamedTextColor.GREEN));
            }
            case "night" -> {
                world.setTime(13000);
                sender.sendMessage(Component.text("Time set to night", NamedTextColor.GREEN));
            }
            case "midnight" -> {
                world.setTime(18000);
                sender.sendMessage(Component.text("Time set to midnight", NamedTextColor.GREEN));
            }
            default -> {
                try {
                    long time = Long.parseLong(timeType);
                    world.setTime(time);
                    sender.sendMessage(Component.text("Time set to " + time, NamedTextColor.GREEN));
                } catch (NumberFormatException ex) {
                    sender.sendMessage(Component.text("Usage: /fandtime [day|noon|night|midnight|<ticks>]", NamedTextColor.RED));
                }
            }
        }
    }
}
