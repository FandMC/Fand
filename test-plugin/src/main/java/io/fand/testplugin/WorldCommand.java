package io.fand.testplugin;

import static io.fand.testplugin.DemoSupport.*;

import io.fand.api.Fand;
import io.fand.api.command.CommandCompleter;
import io.fand.api.command.CommandExecutor;
import io.fand.api.command.CommandSender;
import io.fand.api.command.CommandSpec;
import io.fand.api.entity.Player;
import io.fand.api.plugin.PluginContext;
import io.fand.api.world.Difficulty;
import io.fand.api.world.World;
import io.fand.api.world.WorldBorder;
import io.fand.api.world.WorldTemplate;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import net.kyori.adventure.key.InvalidKeyException;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

@CommandSpec(label = "fandworld", arguments = {"player", "mode"}, aliases = {"fworld"}, permission = "fand.testplugin.world")
final class WorldCommand implements CommandExecutor, CommandCompleter {

    private final PluginContext context;

    WorldCommand(PluginContext context) {
        this.context = context;
    }

    @Override
    public void execute(CommandSender sender, String label, List<String> args) {
        if (!args.isEmpty() && handleWorldRegistryCommand(sender, args)) {
            return;
        }

        Player target = null;
        List<String> modeArgs = args;
        if (!args.isEmpty()) {
            var namedTarget = Fand.server().player(args.getFirst());
            if (namedTarget.isPresent()) {
                target = namedTarget.get();
                modeArgs = args.subList(1, args.size());
            }
        }
        if (target == null && sender instanceof Player player) {
            target = player;
        }
        if (modeArgs.size() > 1) {
            sender.sendMessage(Component.text(worldUsage(), NamedTextColor.RED));
            return;
        }
        World world = target == null ? world(sender, null, null) : target.world();
        if (world == null) {
            return;
        }

        String mode = modeArgs.isEmpty() ? "info" : modeArgs.getFirst().toLowerCase(Locale.ROOT);
        switch (mode) {
            case "list" -> sendWorldList(sender);
            case "info" -> sendWorldInfo(sender, world);
            case "day" -> completeWorldChange(sender, world.setTime(1000), "Set " + world.name() + " time to day.");
            case "night" -> completeWorldChange(sender, world.setTime(13000), "Set " + world.name() + " time to night.");
            case "storm" -> completeWorldChange(sender,
                    world.setThundering(false).thenCompose(ignored -> world.setStorm(true)),
                    "Started rain in " + world.name() + ".");
            case "clear" -> completeWorldChange(sender,
                    world.setThundering(false).thenCompose(ignored -> world.setStorm(false)),
                    "Cleared weather in " + world.name() + ".");
            case "thunder" -> completeWorldChange(sender,
                    world.setThundering(true),
                    "Started thunder in " + world.name() + ".");
            case "peaceful" -> setDifficulty(sender, world, Difficulty.PEACEFUL);
            case "easy" -> setDifficulty(sender, world, Difficulty.EASY);
            case "normal" -> setDifficulty(sender, world, Difficulty.NORMAL);
            case "hard" -> setDifficulty(sender, world, Difficulty.HARD);
            case "border" -> applyBorderDemo(sender, world, target);
            case "save" -> completeWorldSave(sender, world.save(), world);
            default -> sender.sendMessage(Component.text(worldUsage(), NamedTextColor.RED));
        }
    }

    @Override
    public List<String> complete(CommandSender sender, String label, List<String> args) {
        if (!args.isEmpty() && args.getFirst().equalsIgnoreCase("create")) {
            if (args.size() == 2) {
                return matching(List.of("fand-test-plugin:demo_world"), args.getLast());
            }
            if (args.size() == 3) {
                return matching(worldTemplateNames(), args.getLast());
            }
            return List.of();
        }
        if (!args.isEmpty() && args.getFirst().equalsIgnoreCase("unload")) {
            return args.size() == 2 ? matching(dynamicWorldKeys(), args.getLast()) : List.of();
        }
        if (args.size() <= 1) {
            var values = new ArrayList<>(playerNames());
            values.addAll(WORLD_MODES);
            return matching(values, args.isEmpty() ? "" : args.getLast());
        }
        if (args.size() == 2 && Fand.server().player(args.getFirst()).isPresent()) {
            return matching(WORLD_MODES, args.getLast());
        }
        return List.of();
    }

    private boolean handleWorldRegistryCommand(CommandSender sender, List<String> args) {
        String mode = args.getFirst().toLowerCase(Locale.ROOT);
        switch (mode) {
            case "list" -> {
                sendWorldList(sender);
                return true;
            }
            case "create" -> {
                createWorld(sender, args);
                return true;
            }
            case "unload" -> {
                unloadWorld(sender, args);
                return true;
            }
            default -> {
                return false;
            }
        }
    }

    private void createWorld(CommandSender sender, List<String> args) {
        if (args.size() < 2 || args.size() > 3) {
            sender.sendMessage(Component.text("Usage: /fandworld create <world> [overworld|nether|end]", NamedTextColor.RED));
            return;
        }
        Key key;
        try {
            key = Key.key(keyString(args.get(1)));
        } catch (InvalidKeyException ex) {
            sender.sendMessage(Component.text("Invalid world key: " + args.get(1), NamedTextColor.RED));
            return;
        }
        WorldTemplate template = args.size() == 3 ? worldTemplate(sender, args.get(2)) : WorldTemplate.OVERWORLD;
        if (template == null) {
            return;
        }
        Fand.server().createWorld(key, template).whenComplete((world, failure) -> {
            if (failure != null) {
                context.logger().warn("World create failed for {}", key.asString(), failure);
                sender.sendMessage(Component.text("World create failed: " + failure.getMessage(), NamedTextColor.RED));
                return;
            }
            sender.sendMessage(Component.text(
                    "Created world " + world.name() + " from " + template.name().toLowerCase(Locale.ROOT) + ".",
                    NamedTextColor.GREEN));
        });
    }

    private void unloadWorld(CommandSender sender, List<String> args) {
        if (args.size() != 2) {
            sender.sendMessage(Component.text("Usage: /fandworld unload <world>", NamedTextColor.RED));
            return;
        }
        Key key;
        try {
            key = Key.key(keyString(args.get(1)));
        } catch (InvalidKeyException ex) {
            sender.sendMessage(Component.text("Invalid world key: " + args.get(1), NamedTextColor.RED));
            return;
        }
        Fand.server().unloadWorld(key).whenComplete((unloaded, failure) -> {
            if (failure != null) {
                context.logger().warn("World unload failed for {}", key.asString(), failure);
                sender.sendMessage(Component.text("World unload failed: " + failure.getMessage(), NamedTextColor.RED));
                return;
            }
            sender.sendMessage(Component.text(
                    Boolean.TRUE.equals(unloaded) ? "Unloaded world " + key.asString() + "." : "World was not loaded: " + key.asString(),
                    Boolean.TRUE.equals(unloaded) ? NamedTextColor.GREEN : NamedTextColor.YELLOW));
        });
    }

    private void setDifficulty(CommandSender sender, World world, Difficulty difficulty) {
        completeWorldChange(sender,
                world.setDifficulty(difficulty),
                "Set server difficulty to " + difficulty.name().toLowerCase(Locale.ROOT) + ".");
    }

    private void applyBorderDemo(CommandSender sender, World world, Player target) {
        WorldBorder border = world.worldBorder();
        String center;
        if (target != null) {
            var location = target.location();
            border.setCenter(location.x(), location.z());
            center = trim(location.x()) + ", " + trim(location.z());
        } else {
            center = trim(border.centerX()) + ", " + trim(border.centerZ());
        }
        border.setSize(64.0, Duration.ofSeconds(5));
        border.setWarningDistance(5);
        border.setWarningTime(10);
        sender.sendMessage(Component.text(
                "Applied border demo to " + world.name() + ": center=" + center + ", targetSize=64 over 5s.",
                NamedTextColor.GREEN));
    }

    private void sendWorldInfo(CommandSender sender, World world) {
        WorldBorder border = world.worldBorder();
        sender.sendMessage(Component.text("World " + world.name(), NamedTextColor.GOLD));
        sender.sendMessage(Component.text(
                "time=" + world.time()
                        + ", gameTime=" + world.gameTime()
                        + ", difficulty=" + world.difficulty().name().toLowerCase(Locale.ROOT)
                        + ", players=" + world.players().size(),
                NamedTextColor.GRAY));
        sender.sendMessage(Component.text(
                "weather: storm=" + world.storm() + ", thunder=" + world.thundering(),
                NamedTextColor.AQUA));
        sender.sendMessage(Component.text(
                "border: center=" + trim(border.centerX()) + "," + trim(border.centerZ())
                        + ", size=" + trim(border.size())
                        + ", target=" + trim(border.targetSize())
                        + ", warning=" + border.warningDistance() + " blocks/" + border.warningTime() + "s"
                        + ", damage=" + trim(border.damageAmount()) + " after " + trim(border.damageBuffer()) + " blocks",
                NamedTextColor.LIGHT_PURPLE));
    }

    private void sendWorldList(CommandSender sender) {
        sender.sendMessage(Component.text("Loaded worlds: " + String.join(", ", worldKeys()), NamedTextColor.GOLD));
    }

    private void completeWorldChange(CommandSender sender, CompletableFuture<Void> future, String successMessage) {
        future.whenComplete((ignored, failure) -> {
            if (failure != null) {
                context.logger().warn("World API demo command failed", failure);
                sender.sendMessage(Component.text("World change failed: " + failure.getMessage(), NamedTextColor.RED));
                return;
            }
            sender.sendMessage(Component.text(successMessage, NamedTextColor.GREEN));
        });
    }

    private void completeWorldSave(CommandSender sender, CompletableFuture<Boolean> future, World world) {
        future.whenComplete((saved, failure) -> {
            if (failure != null) {
                context.logger().warn("World save failed for {}", world.name(), failure);
                sender.sendMessage(Component.text("World save failed: " + failure.getMessage(), NamedTextColor.RED));
                return;
            }
            sender.sendMessage(Component.text(
                    Boolean.TRUE.equals(saved) ? "Saved " + world.name() + "." : "Save was skipped for " + world.name() + ".",
                    Boolean.TRUE.equals(saved) ? NamedTextColor.GREEN : NamedTextColor.YELLOW));
        });
    }

    private WorldTemplate worldTemplate(CommandSender sender, String raw) {
        try {
            return WorldTemplate.valueOf(raw.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            sender.sendMessage(Component.text("Unknown world template: " + raw, NamedTextColor.RED));
            return null;
        }
    }

    private List<String> worldTemplateNames() {
        return List.of("overworld", "nether", "end");
    }

    private List<String> dynamicWorldKeys() {
        return worldKeys().stream()
                .filter(key -> !key.equals("minecraft:overworld")
                        && !key.equals("minecraft:the_nether")
                        && !key.equals("minecraft:the_end"))
                .toList();
    }

    private String worldUsage() {
        return "Usage: /fandworld [player] [list|info|day|night|storm|clear|thunder|peaceful|easy|normal|hard|border|save] or /fandworld create <world> [overworld|nether|end] or /fandworld unload <world>";
    }
}
