package io.fand.testplugin;

import io.fand.api.command.CommandExecutor;
import io.fand.api.command.CommandSender;
import io.fand.api.command.CommandSpec;
import io.fand.api.entity.Player;
import io.fand.api.plugin.PluginContext;
import io.fand.api.world.Particles;
import java.util.List;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

@CommandSpec(label = "fandweather", permission = "fand.testplugin.weather")
final class WeatherCommand implements CommandExecutor {

    private final PluginContext context;

    WeatherCommand(PluginContext context) {
        this.context = context;
    }

    @Override
    public void execute(CommandSender sender, String label, List<String> args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("Must be run by a player", NamedTextColor.RED));
            return;
        }
        if (args.isEmpty()) {
            sender.sendMessage(Component.text("Usage: /fandweather <clear|rain|thunder>", NamedTextColor.RED));
            return;
        }
        var world = player.world();
        var weatherType = args.get(0).toLowerCase();
        switch (weatherType) {
            case "clear" -> {
                world.setStorming(false);
                world.setThundering(false);
                sender.sendMessage(Component.text("Weather set to clear", NamedTextColor.GREEN));
            }
            case "rain" -> {
                world.setStorming(true);
                world.setThundering(false);
                sender.sendMessage(Component.text("Weather set to rain", NamedTextColor.GREEN));
            }
            case "thunder" -> {
                world.setStorming(true);
                world.setThundering(true);
                sender.sendMessage(Component.text("Weather set to thunder", NamedTextColor.GREEN));
            }
            default -> sender.sendMessage(Component.text("Unknown weather: " + weatherType, NamedTextColor.RED));
        }
    }
}
