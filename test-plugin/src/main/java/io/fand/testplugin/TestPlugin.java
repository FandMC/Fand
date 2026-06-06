package io.fand.testplugin;

import io.fand.api.Fand;
import io.fand.api.Server;
import io.fand.api.block.Block;
import io.fand.api.block.BlockType;
import io.fand.api.block.BlockTypes;
import io.fand.api.command.CommandCompleter;
import io.fand.api.command.CommandExecutor;
import io.fand.api.command.CommandSender;
import io.fand.api.command.CommandSpec;
import io.fand.api.config.Configuration;
import io.fand.api.entity.GameMode;
import io.fand.api.entity.Player;
import io.fand.api.event.Listener;
import io.fand.api.event.Subscribe;
import io.fand.api.event.block.BlockBreakEvent;
import io.fand.api.event.block.BlockPlaceEvent;
import io.fand.api.event.entity.EntityDamageEvent;
import io.fand.api.event.inventory.InventoryClickEvent;
import io.fand.api.event.inventory.InventoryCloseEvent;
import io.fand.api.event.inventory.InventoryOpenEvent;
import io.fand.api.event.player.PlayerChatEvent;
import io.fand.api.event.player.PlayerInteractEvent;
import io.fand.api.event.player.PlayerJoinEvent;
import io.fand.api.event.player.PlayerQuitEvent;
import io.fand.api.inventory.Inventory;
import io.fand.api.inventory.InventoryType;
import io.fand.api.inventory.Inventories;
import io.fand.api.item.ItemStack;
import io.fand.api.item.ItemType;
import io.fand.api.item.ItemTypes;
import io.fand.api.lifecycle.ServerStartedEvent;
import io.fand.api.permission.PermissionDefault;
import io.fand.api.permission.PermissionDescriptor;
import io.fand.api.plugin.Plugin;
import io.fand.api.plugin.PluginContext;
import io.fand.api.world.World;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.NoSuchElementException;
import net.kyori.adventure.key.InvalidKeyException;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.slf4j.Logger;

public final class TestPlugin implements Plugin {

    private static final List<String> SAMPLE_BLOCKS = List.of(
            "minecraft:stone",
            "minecraft:grass_block",
            "minecraft:oak_log",
            "minecraft:glass",
            "minecraft:diamond_block",
            "minecraft:gold_block",
            "minecraft:redstone_lamp",
            "minecraft:air"
    );
    private static final List<String> SAMPLE_ITEMS = List.of(
            "minecraft:stone",
            "minecraft:oak_log",
            "minecraft:diamond",
            "minecraft:golden_apple",
            "minecraft:ender_pearl",
            "minecraft:torch",
            "minecraft:bread",
            "minecraft:compass"
    );
    private static final List<String> PERMISSIONS = List.of(
            "fand.testplugin.use",
            "fand.testplugin.demo",
            "fand.testplugin.tp",
            "fand.testplugin.setblock",
            "fand.testplugin.give",
            "fand.testplugin.heal",
            "fand.testplugin.mode",
            "fand.testplugin.fly",
            "fand.testplugin.gui"
    );

    @Override
    public void onLoad(PluginContext context) {
        context.logger().info("test-plugin loaded; data directory={}", context.dataDirectory());
    }

    @Override
    public void onEnable(PluginContext context) {
        registerPermissions(context);
        context.commands().register(new HelloCommand(context));
        context.commands().register(new DemoCommand(context));
        context.commands().register(new TeleportCommand(context));
        context.commands().register(new SetBlockCommand(context));
        context.commands().register(new GiveCommand(context));
        context.commands().register(new HealCommand(context));
        context.commands().register(new GameModeCommand());
        context.commands().register(new FlyCommand());
        context.commands().register(new GuiCommand(context));
        context.events().subscribe(ServerStartedEvent.class, event ->
                context.logger().info("Server started; Fand brand={} version={} minecraft={}",
                        event.server().brand(), event.server().version(), event.server().minecraftVersion()));
        context.events().subscribe(PlayerJoinEvent.class, event -> {
            context.logger().info("{} joined ({} online players)", event.player().name(), Fand.server().onlinePlayers());
            var welcome = message(context.config(), "messages.welcome", "Welcome from test-plugin, {player}!");
            if (!welcome.isBlank()) {
                event.player().sendMessage(Component.text(welcome.replace("{player}", event.player().name()), NamedTextColor.AQUA));
            }
        });
        context.events().registerListener(new PlayerEvents(context));
        context.scheduler().runAsync(() -> context.logger().info("test-plugin config file={}", context.config().file()));
        context.scheduler().runMainRepeating(
                () -> context.logger().debug("test-plugin heartbeat; online={}", Fand.server().onlinePlayers()),
                Duration.ofSeconds(30),
                Duration.ofSeconds(60));
        context.logger().info("test-plugin enabled");
    }

    @Override
    public void onDisable(PluginContext context) {
        context.logger().info("test-plugin disabled");
    }

    private static void registerPermissions(PluginContext context) {
        for (var node : PERMISSIONS) {
            context.permissions().register(new PermissionDescriptor(node, PermissionDefault.OPERATOR));
        }
    }

    @CommandSpec(label = "fandtest", arguments = {"greeting"}, aliases = {"ftest"}, permission = "fand.testplugin.use")
    static final class HelloCommand implements CommandExecutor {

        private final PluginContext context;

        HelloCommand(PluginContext context) {
            this.context = context;
        }

        @Override
        public void execute(CommandSender sender, String label, List<String> args) {
            var greeting = args.isEmpty() ? sender.name() : String.join(" ", args);
            sender.sendMessage(Component.text(message(context.config(), "messages.greeting", "Hello") + ", " + greeting + "!", NamedTextColor.GREEN));
            sender.sendMessage(Component.text("Server: " + Fand.server().brand() + " " + Fand.server().version(), NamedTextColor.GRAY));
            context.logger().info("/fandtest invoked by {} with args {}", sender.name(), args);
        }
    }

    @CommandSpec(label = "fanddemo", arguments = {}, aliases = {"fdemo"}, permission = "fand.testplugin.demo")
    static final class DemoCommand implements CommandExecutor {

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

    @CommandSpec(label = "fandtp", arguments = {"x", "y", "z"}, aliases = {"ftp"}, permission = "fand.testplugin.tp")
    static final class TeleportCommand implements CommandExecutor, CommandCompleter {

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

    @CommandSpec(
            label = "fandsetblock",
            arguments = {"block", "x", "y", "z", "world"},
            aliases = {"fsb"},
            permission = "fand.testplugin.setblock")
    static final class SetBlockCommand implements CommandExecutor, CommandCompleter {

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

    @CommandSpec(label = "fandgive", arguments = {"item", "amount", "player"}, aliases = {"fgive"}, permission = "fand.testplugin.give")
    static final class GiveCommand implements CommandExecutor, CommandCompleter {

        private final PluginContext context;

        GiveCommand(PluginContext context) {
            this.context = context;
        }

        @Override
        public void execute(CommandSender sender, String label, List<String> args) {
            if (args.isEmpty() || args.size() > 3) {
                sender.sendMessage(Component.text("Usage: /fandgive <item> [amount] [player]", NamedTextColor.RED));
                return;
            }
            ItemType type = itemType(sender, args.get(0));
            if (type == null) {
                return;
            }
            int amount = 1;
            if (args.size() >= 2) {
                Integer parsed = parseInt(sender, args.get(1), "amount");
                if (parsed == null) {
                    return;
                }
                amount = parsed;
            }
            int limit = Math.max(1, context.config().getInt("limits.max-give-amount", 2304));
            if (amount < 1 || amount > limit) {
                sender.sendMessage(Component.text("Amount must be in 1.." + limit, NamedTextColor.RED));
                return;
            }
            Player target = args.size() == 3 ? player(sender, args.get(2)) : sender instanceof Player player ? player : null;
            if (target == null) {
                sender.sendMessage(Component.text("Console must provide a target player: /fandgive <item> [amount] <player>", NamedTextColor.RED));
                return;
            }

            int given = give(target, type, amount);
            int leftover = amount - given;
            target.sendMessage(Component.text("Received " + given + " x " + itemName(type)
                    + (leftover > 0 ? " (" + leftover + " did not fit)" : ""), NamedTextColor.GREEN));
            if (target != sender) {
                sender.sendMessage(Component.text("Gave " + given + " x " + itemName(type) + " to " + target.name()
                        + (leftover > 0 ? " (" + leftover + " did not fit)" : ""), NamedTextColor.GREEN));
            }
            context.logger().info("/fandgive by {} -> {} x {} to {}", sender.name(), given, type.key(), target.name());
        }

        @Override
        public List<String> complete(CommandSender sender, String label, List<String> args) {
            if (args.size() <= 1) {
                return matching(SAMPLE_ITEMS, args.isEmpty() ? "" : args.getLast());
            }
            if (args.size() == 2) {
                return matching(List.of("1", "8", "16", "32", "64"), args.getLast());
            }
            if (args.size() == 3) {
                return matching(playerNames(), args.getLast());
            }
            return List.of();
        }
    }

    @CommandSpec(label = "fandheal", arguments = {"player"}, aliases = {"fheal"}, permission = "fand.testplugin.heal")
    static final class HealCommand implements CommandExecutor, CommandCompleter {

        private final PluginContext context;

        HealCommand(PluginContext context) {
            this.context = context;
        }

        @Override
        public void execute(CommandSender sender, String label, List<String> args) {
            if (args.size() > 1) {
                sender.sendMessage(Component.text("Usage: /fandheal [player]", NamedTextColor.RED));
                return;
            }
            Player target = args.isEmpty() ? sender instanceof Player player ? player : null : player(sender, args.get(0));
            if (target == null) {
                sender.sendMessage(Component.text("Console must provide a target player: /fandheal <player>", NamedTextColor.RED));
                return;
            }
            target.setHealth(target.maxHealth());
            target.setFoodLevel(20);
            target.setSaturation(20.0F);
            int xp = Math.max(0, context.config().getInt("defaults.heal-xp", 5));
            if (xp > 0) {
                target.giveExperience(xp);
            }
            target.sendMessage(Component.text("Healed by test-plugin.", NamedTextColor.GREEN));
            if (target != sender) {
                sender.sendMessage(Component.text("Healed " + target.name() + ".", NamedTextColor.GREEN));
            }
        }

        @Override
        public List<String> complete(CommandSender sender, String label, List<String> args) {
            return args.size() <= 1 ? matching(playerNames(), args.isEmpty() ? "" : args.getLast()) : List.of();
        }
    }

    @CommandSpec(label = "fandmode", arguments = {"mode", "player"}, aliases = {"fgm"}, permission = "fand.testplugin.mode")
    static final class GameModeCommand implements CommandExecutor, CommandCompleter {

        @Override
        public void execute(CommandSender sender, String label, List<String> args) {
            if (args.isEmpty() || args.size() > 2) {
                sender.sendMessage(Component.text("Usage: /fandmode <survival|creative|adventure|spectator> [player]", NamedTextColor.RED));
                return;
            }
            GameMode mode;
            try {
                mode = GameMode.valueOf(args.get(0).trim().toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException ex) {
                sender.sendMessage(Component.text("Unknown game mode: " + args.get(0), NamedTextColor.RED));
                return;
            }
            Player target = args.size() == 2 ? player(sender, args.get(1)) : sender instanceof Player player ? player : null;
            if (target == null) {
                sender.sendMessage(Component.text("Console must provide a target player: /fandmode <mode> <player>", NamedTextColor.RED));
                return;
            }
            target.setGameMode(mode);
            target.sendMessage(Component.text("Game mode set to " + mode.name().toLowerCase(Locale.ROOT) + ".", NamedTextColor.GREEN));
            if (target != sender) {
                sender.sendMessage(Component.text("Set " + target.name() + " to " + mode.name().toLowerCase(Locale.ROOT) + ".", NamedTextColor.GREEN));
            }
        }

        @Override
        public List<String> complete(CommandSender sender, String label, List<String> args) {
            if (args.size() <= 1) {
                return matching(List.of("survival", "creative", "adventure", "spectator"), args.isEmpty() ? "" : args.getLast());
            }
            if (args.size() == 2) {
                return matching(playerNames(), args.getLast());
            }
            return List.of();
        }
    }

    @CommandSpec(label = "fandfly", arguments = {"state", "player"}, aliases = {"ffly"}, permission = "fand.testplugin.fly")
    static final class FlyCommand implements CommandExecutor, CommandCompleter {

        @Override
        public void execute(CommandSender sender, String label, List<String> args) {
            if (args.size() > 2) {
                sender.sendMessage(Component.text("Usage: /fandfly [on|off|toggle] [player]", NamedTextColor.RED));
                return;
            }
            String mode = args.isEmpty() ? "toggle" : args.get(0).toLowerCase(Locale.ROOT);
            Player target = args.size() == 2 ? player(sender, args.get(1)) : sender instanceof Player player ? player : null;
            if (target == null) {
                sender.sendMessage(Component.text("Console must provide a target player: /fandfly <on|off|toggle> <player>", NamedTextColor.RED));
                return;
            }
            boolean enabled;
            if (mode.equals("toggle")) {
                enabled = !target.allowFlight();
            } else if (mode.equals("on") || mode.equals("true")) {
                enabled = true;
            } else if (mode.equals("off") || mode.equals("false")) {
                enabled = false;
            } else {
                sender.sendMessage(Component.text("Flight mode must be on, off, or toggle", NamedTextColor.RED));
                return;
            }
            target.setAllowFlight(enabled);
            target.setFlying(enabled);
            target.sendMessage(Component.text("Flight " + (enabled ? "enabled." : "disabled."), enabled ? NamedTextColor.GREEN : NamedTextColor.YELLOW));
            if (target != sender) {
                sender.sendMessage(Component.text("Flight " + (enabled ? "enabled" : "disabled") + " for " + target.name() + ".", NamedTextColor.GREEN));
            }
        }

        @Override
        public List<String> complete(CommandSender sender, String label, List<String> args) {
            if (args.size() <= 1) {
                return matching(List.of("toggle", "on", "off"), args.isEmpty() ? "" : args.getLast());
            }
            if (args.size() == 2) {
                return matching(playerNames(), args.getLast());
            }
            return List.of();
        }
    }

    @CommandSpec(label = "fandgui", arguments = {"player"}, aliases = {"fgui"}, permission = "fand.testplugin.gui")
    static final class GuiCommand implements CommandExecutor, CommandCompleter {

        private final PluginContext context;

        GuiCommand(PluginContext context) {
            this.context = context;
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
            Inventory inventory = Inventories.create(InventoryType.CHEST, 27, Component.text("Fand API Demo", NamedTextColor.DARK_AQUA));
            put(inventory, 10, "minecraft:diamond", 3);
            put(inventory, 12, "minecraft:golden_apple", 1);
            put(inventory, 14, "minecraft:compass", 1);
            put(inventory, 16, "minecraft:oak_log", 16);
            inventory.addSlotChangeListener((slot, oldStack, newStack) ->
                    context.logger().info("demo gui slot {} changed: {} -> {}", slot, stackName(oldStack), stackName(newStack)));
            target.openInventory(inventory).whenComplete((opened, failure) -> {
                if (failure != null) {
                    context.logger().warn("Could not open demo GUI for {}", target.name(), failure);
                    sender.sendMessage(Component.text("Could not open demo GUI.", NamedTextColor.RED));
                    return;
                }
                if (Boolean.TRUE.equals(opened)) {
                    target.sendMessage(Component.text("Opened a server-created inventory.", NamedTextColor.GREEN));
                    if (target != sender) {
                        sender.sendMessage(Component.text("Opened demo GUI for " + target.name() + ".", NamedTextColor.GREEN));
                    }
                } else {
                    sender.sendMessage(Component.text("Inventory open was rejected or the player went offline.", NamedTextColor.YELLOW));
                }
            });
        }

        @Override
        public List<String> complete(CommandSender sender, String label, List<String> args) {
            return args.size() <= 1 ? matching(playerNames(), args.isEmpty() ? "" : args.getLast()) : List.of();
        }
    }

    static final class PlayerEvents implements Listener {

        private final PluginContext context;
        private final Logger logger;

        PlayerEvents(PluginContext context) {
            this.context = context;
            this.logger = context.logger();
        }

        @Subscribe
        public void onQuit(PlayerQuitEvent event) {
            logger.info("{} left", event.player().name());
        }

        @Subscribe
        public void onChat(PlayerChatEvent event) {
            if (event.originalText().equalsIgnoreCase("!where")) {
                var loc = event.player().location();
                event.setCancelled(true);
                event.player().sendMessage(Component.text("You are at " + event.player().world().name()
                        + " " + loc.blockX() + "," + loc.blockY() + "," + loc.blockZ(), NamedTextColor.AQUA));
                return;
            }
            if (context.config().getBoolean("features.chat-prefix", true)) {
                event.setMessage(Component.text("[FandDemo] ", NamedTextColor.LIGHT_PURPLE).append(event.message()));
            }
        }

        @Subscribe
        public void onInteract(PlayerInteractEvent event) {
            if (!context.config().getBoolean("features.report-right-clicks", false)
                    || event.hand() != PlayerInteractEvent.Hand.MAIN_HAND) {
                return;
            }
            event.block().ifPresent(block -> event.player().sendMessage(Component.text(
                    "Right-clicked " + blockName(block.type()) + " at " + block.x() + "," + block.y() + "," + block.z(),
                    NamedTextColor.GRAY)));
        }

        @Subscribe
        public void onBlockBreak(BlockBreakEvent event) {
            if (blockName(event.blockType()).contains("diamond_ore")) {
                event.player().sendMessage(Component.text("Diamond ore break observed by the API.", NamedTextColor.AQUA));
            }
        }

        @Subscribe
        public void onBlockPlace(BlockPlaceEvent event) {
            if (context.config().getBoolean("protections.block-tnt-placement", true)
                    && blockName(event.placedType()).equals("minecraft:tnt")) {
                event.setCancelled(true);
                event.player().sendMessage(Component.text("test-plugin cancelled TNT placement.", NamedTextColor.RED));
            }
        }

        @Subscribe
        public void onDamage(EntityDamageEvent event) {
            if (context.config().getBoolean("protections.cancel-fall-damage", true)
                    && event.entity() instanceof Player player
                    && event.cause().equals("minecraft:fall")) {
                event.setCancelled(true);
                player.sendMessage(Component.text("Fall damage cancelled by test-plugin.", NamedTextColor.YELLOW));
            }
        }

        @Subscribe
        public void onInventoryOpen(InventoryOpenEvent event) {
            logger.debug("{} opened {}", event.player().name(), event.type());
        }

        @Subscribe
        public void onInventoryClose(InventoryCloseEvent event) {
            logger.debug("{} closed {}", event.player().name(), event.type());
        }

        @Subscribe
        public void onInventoryClick(InventoryClickEvent event) {
            if (context.config().getBoolean("features.log-inventory-clicks", false)) {
                logger.info("{} clicked {} slot={} current={} cursor={}",
                        event.player().name(), event.clickType(), event.slot(),
                        stackName(event.currentItem()), stackName(event.cursorItem()));
            }
        }
    }

    private static int give(Player target, ItemType type, int amount) {
        int remaining = amount;
        while (remaining > 0) {
            int batch = Math.min(remaining, type.maxStackSize());
            ItemStack leftover = target.inventory().add(type.stack(batch));
            int accepted = batch - (leftover.isEmpty() ? 0 : leftover.amount());
            remaining -= accepted;
            if (accepted < batch) {
                break;
            }
        }
        return amount - remaining;
    }

    private static void put(Inventory inventory, int slot, String item, int amount) {
        inventory.set(slot, ItemTypes.of(keyString(item)).stack(amount));
    }

    private static Player player(CommandSender sender, String name) {
        return Fand.server().player(name).orElseGet(() -> {
            sender.sendMessage(Component.text("Unknown online player: " + name, NamedTextColor.RED));
            return null;
        });
    }

    private static World world(CommandSender sender, String raw, Player fallback) {
        if (raw == null || raw.isBlank()) {
            if (fallback != null) {
                return fallback.world();
            }
            return Fand.server().defaultWorld().orElseGet(() -> {
                sender.sendMessage(Component.text("No default world is loaded.", NamedTextColor.RED));
                return null;
            });
        }
        try {
            var key = Key.key(keyString(raw));
            return Fand.server().world(key).orElseGet(() -> {
                sender.sendMessage(Component.text("Unknown world: " + key.asString(), NamedTextColor.RED));
                return null;
            });
        } catch (InvalidKeyException ex) {
            sender.sendMessage(Component.text("Invalid world key: " + raw, NamedTextColor.RED));
            return null;
        }
    }

    private static BlockType blockType(CommandSender sender, String raw) {
        try {
            return BlockTypes.of(keyString(raw));
        } catch (NoSuchElementException ex) {
            sender.sendMessage(Component.text("Unknown block: " + raw, NamedTextColor.RED));
            return null;
        } catch (InvalidKeyException ex) {
            sender.sendMessage(Component.text("Invalid block key: " + raw, NamedTextColor.RED));
            return null;
        }
    }

    private static ItemType itemType(CommandSender sender, String raw) {
        try {
            return ItemTypes.of(keyString(raw));
        } catch (NoSuchElementException ex) {
            sender.sendMessage(Component.text("Unknown item: " + raw, NamedTextColor.RED));
            return null;
        } catch (InvalidKeyException ex) {
            sender.sendMessage(Component.text("Invalid item key: " + raw, NamedTextColor.RED));
            return null;
        }
    }

    private static Integer parseInt(CommandSender sender, String raw, String name) {
        try {
            return Integer.parseInt(raw);
        } catch (NumberFormatException ex) {
            sender.sendMessage(Component.text(name + " must be an integer: " + raw, NamedTextColor.RED));
            return null;
        }
    }

    private static Double parseDouble(CommandSender sender, String raw, String name) {
        try {
            return Double.parseDouble(raw);
        } catch (NumberFormatException ex) {
            sender.sendMessage(Component.text(name + " must be a number: " + raw, NamedTextColor.RED));
            return null;
        }
    }

    private static String message(Configuration config, String path, String fallback) {
        return config.getString(path, fallback);
    }

    private static List<String> playerNames() {
        return Fand.server().players().stream().map(Player::name).toList();
    }

    private static List<String> worldKeys() {
        return Fand.server().worlds().stream().map(world -> world.key().asString()).toList();
    }

    private static List<String> matching(List<String> values, String rawPrefix) {
        var prefix = rawPrefix.toLowerCase(Locale.ROOT);
        var matches = new ArrayList<String>();
        for (var value : values) {
            if (value.toLowerCase(Locale.ROOT).startsWith(prefix)) {
                matches.add(value);
            }
        }
        return matches;
    }

    private static String keyString(String raw) {
        var value = raw.trim().toLowerCase(Locale.ROOT);
        return value.indexOf(':') >= 0 ? value : "minecraft:" + value;
    }

    private static String blockName(BlockType type) {
        return type.key().asString();
    }

    private static String itemName(ItemType type) {
        return type.key().asString();
    }

    private static String stackName(ItemStack stack) {
        return stack.isEmpty() ? "empty" : stack.amount() + "x " + itemName(stack.type());
    }

    private static String trim(double value) {
        return value == Math.rint(value) ? Long.toString(Math.round(value)) : Double.toString(value);
    }
}
