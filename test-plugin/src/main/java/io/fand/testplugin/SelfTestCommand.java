package io.fand.testplugin;

import static io.fand.testplugin.DemoSupport.matching;

import io.fand.api.command.CommandRegistry;
import io.fand.api.command.CommandSender;
import io.fand.api.command.CommandSpec;
import io.fand.api.event.Event;
import io.fand.api.event.EventBus;
import io.fand.api.event.block.BlockBreakEvent;
import io.fand.api.event.block.BlockBurnEvent;
import io.fand.api.event.block.BlockChangeEvent;
import io.fand.api.event.block.BlockDispenseEvent;
import io.fand.api.event.block.BlockExplodeEvent;
import io.fand.api.event.block.BlockFadeEvent;
import io.fand.api.event.block.BlockFertilizeEvent;
import io.fand.api.event.block.BlockFormEvent;
import io.fand.api.event.block.BlockGrowEvent;
import io.fand.api.event.block.BlockIgniteEvent;
import io.fand.api.event.block.BlockPhysicsEvent;
import io.fand.api.event.block.BlockPistonExtendEvent;
import io.fand.api.event.block.BlockPistonPushEvent;
import io.fand.api.event.block.BlockPistonRetractEvent;
import io.fand.api.event.block.BlockPlaceEvent;
import io.fand.api.event.block.BlockRedstoneEvent;
import io.fand.api.event.block.BlockFromToEvent;
import io.fand.api.event.block.BlockSpreadEvent;
import io.fand.api.event.block.CauldronLevelChangeEvent;
import io.fand.api.event.block.FluidFlowEvent;
import io.fand.api.event.block.LeavesDecayEvent;
import io.fand.api.event.block.PortalCreateEvent;
import io.fand.api.event.block.SignChangeEvent;
import io.fand.api.event.block.SpongeAbsorbEvent;
import io.fand.api.event.command.CommandExecuteEvent;
import io.fand.api.event.command.TabCompleteEvent;
import io.fand.api.event.entity.EntityBreedEvent;
import io.fand.api.event.entity.EntityChangeBlockEvent;
import io.fand.api.event.entity.EntityCombustEvent;
import io.fand.api.event.entity.EntityDamageByEntityEvent;
import io.fand.api.event.entity.EntityDamageEvent;
import io.fand.api.event.entity.EntityDeathEvent;
import io.fand.api.event.entity.EntityDismountEvent;
import io.fand.api.event.entity.EntityDropItemEvent;
import io.fand.api.event.entity.EntityExplodeEvent;
import io.fand.api.event.entity.EntityMountEvent;
import io.fand.api.event.entity.EntityPickupItemEvent;
import io.fand.api.event.entity.EntityPortalEvent;
import io.fand.api.event.entity.EntityPotionEffectEvent;
import io.fand.api.event.entity.EntityRegainHealthEvent;
import io.fand.api.event.entity.EntityRemoveEvent;
import io.fand.api.event.entity.EntityResurrectEvent;
import io.fand.api.event.entity.EntityShootBowEvent;
import io.fand.api.event.entity.EntitySpawnEvent;
import io.fand.api.event.entity.EntityTameEvent;
import io.fand.api.event.entity.EntityTargetEvent;
import io.fand.api.event.entity.EntityTeleportEvent;
import io.fand.api.event.entity.EntityTransformEvent;
import io.fand.api.event.entity.ExplosionPrimeEvent;
import io.fand.api.event.entity.HangingBreakEvent;
import io.fand.api.event.entity.HangingPlaceEvent;
import io.fand.api.event.entity.ItemDespawnEvent;
import io.fand.api.event.entity.ItemMergeEvent;
import io.fand.api.event.entity.ItemSpawnEvent;
import io.fand.api.event.entity.LingeringPotionSplashEvent;
import io.fand.api.event.entity.PlayerItemFrameChangeEvent;
import io.fand.api.event.entity.PotionSplashEvent;
import io.fand.api.event.entity.ProjectileHitEvent;
import io.fand.api.event.entity.ProjectileLaunchEvent;
import io.fand.api.event.inventory.BrewEvent;
import io.fand.api.event.inventory.BrewingStandFuelEvent;
import io.fand.api.event.inventory.CraftItemEvent;
import io.fand.api.event.inventory.EnchantItemEvent;
import io.fand.api.event.inventory.FurnaceBurnEvent;
import io.fand.api.event.inventory.FurnaceExtractEvent;
import io.fand.api.event.inventory.FurnaceSmeltEvent;
import io.fand.api.event.inventory.HopperMoveItemEvent;
import io.fand.api.event.inventory.HopperPickupItemEvent;
import io.fand.api.event.inventory.InventoryClickEvent;
import io.fand.api.event.inventory.InventoryCloseEvent;
import io.fand.api.event.inventory.InventoryCreativeEvent;
import io.fand.api.event.inventory.InventoryDragEvent;
import io.fand.api.event.inventory.InventoryTradeEvent;
import io.fand.api.event.inventory.InventoryMoveItemEvent;
import io.fand.api.event.inventory.InventoryOpenEvent;
import io.fand.api.event.inventory.InventoryPickupItemEvent;
import io.fand.api.event.inventory.PrepareAnvilEvent;
import io.fand.api.event.inventory.PrepareItemEnchantEvent;
import io.fand.api.event.inventory.PrepareItemCraftEvent;
import io.fand.api.event.inventory.PrepareSmithingEvent;
import io.fand.api.event.inventory.PrepareTradeEvent;
import io.fand.api.event.player.PlayerAdvancementDoneEvent;
import io.fand.api.event.player.PlayerBedEnterEvent;
import io.fand.api.event.player.PlayerBedLeaveEvent;
import io.fand.api.event.player.PlayerBucketEmptyEvent;
import io.fand.api.event.player.PlayerBucketFillEvent;
import io.fand.api.event.player.PlayerChangedWorldEvent;
import io.fand.api.event.player.PlayerChatEvent;
import io.fand.api.event.player.PlayerClientBrandEvent;
import io.fand.api.event.player.PlayerCommandPreprocessEvent;
import io.fand.api.event.player.PlayerDeathEvent;
import io.fand.api.event.player.PlayerDropItemEvent;
import io.fand.api.event.player.PlayerExperienceChangeEvent;
import io.fand.api.event.player.PlayerFoodLevelChangeEvent;
import io.fand.api.event.player.PlayerGameModeChangeEvent;
import io.fand.api.event.player.PlayerInteractEntityEvent;
import io.fand.api.event.player.PlayerInteractEvent;
import io.fand.api.event.player.PlayerFishEvent;
import io.fand.api.event.player.PlayerItemConsumeEvent;
import io.fand.api.event.player.PlayerItemDamageEvent;
import io.fand.api.event.player.PlayerItemHeldEvent;
import io.fand.api.event.player.PlayerJoinEvent;
import io.fand.api.event.player.PlayerKickEvent;
import io.fand.api.event.player.PlayerLeashEntityEvent;
import io.fand.api.event.player.PlayerLocaleChangeEvent;
import io.fand.api.event.player.PlayerLoginEvent;
import io.fand.api.event.player.PlayerMoveEvent;
import io.fand.api.event.player.PlayerPickupItemEvent;
import io.fand.api.event.player.PlayerPortalEvent;
import io.fand.api.event.player.PlayerQuitEvent;
import io.fand.api.event.player.PlayerRespawnEvent;
import io.fand.api.event.player.PlayerResourcePackStatusEvent;
import io.fand.api.event.player.PlayerShearEntityEvent;
import io.fand.api.event.player.PlayerSwapHandItemsEvent;
import io.fand.api.event.player.PlayerTeleportEvent;
import io.fand.api.event.player.PlayerToggleSneakEvent;
import io.fand.api.event.player.PlayerToggleSprintEvent;
import io.fand.api.event.player.PlayerUnleashEntityEvent;
import io.fand.api.event.permission.PermissionCheckEvent;
import io.fand.api.event.server.ServerListPingEvent;
import io.fand.api.event.vehicle.VehicleCreateEvent;
import io.fand.api.event.vehicle.VehicleDestroyEvent;
import io.fand.api.event.vehicle.VehicleEnterEvent;
import io.fand.api.event.vehicle.VehicleExitEvent;
import io.fand.api.event.vehicle.VehicleMoveEvent;
import io.fand.api.event.world.ChunkLoadEvent;
import io.fand.api.event.world.ChunkUnloadEvent;
import io.fand.api.event.world.SpawnChangeEvent;
import io.fand.api.event.world.ThunderChangeEvent;
import io.fand.api.event.world.TimeSkipEvent;
import io.fand.api.event.world.WeatherChangeEvent;
import io.fand.api.event.world.WorldLoadEvent;
import io.fand.api.event.world.WorldSaveEvent;
import io.fand.api.event.world.WorldUnloadEvent;
import io.fand.api.lifecycle.ServerStartedEvent;
import io.fand.api.plugin.PluginContext;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

@CommandSpec(label = "fandselftest", arguments = {"scope"}, aliases = {"fselftest"}, permission = "fand.testplugin.selftest")
final class SelfTestCommand implements io.fand.api.command.CommandExecutor, io.fand.api.command.CommandCompleter {

    private static final List<String> MODES = List.of("all", "commands", "listeners");

    private static final List<ExpectedCommand> EXPECTED_COMMANDS = List.of(
            command("fandtest", "fand.testplugin.use", "ftest"),
            command("fanddemo", "fand.testplugin.demo", "fdemo"),
            command("fandkit", "fand.testplugin.kit", "fkit"),
            command("fandperf", "fand.testplugin.performance", "fperf"),
            command("fandworld", "fand.testplugin.world", "fworld"),
            command("fandtp", "fand.testplugin.tp", "ftp"),
            command("fandsetblock", "fand.testplugin.setblock", "fsb"),
            command("fandgive", "fand.testplugin.give", "fgive"),
            command("fanditem", "fand.testplugin.item", "fitem"),
            command("fandheal", "fand.testplugin.heal", "fheal"),
            command("fandmode", "fand.testplugin.mode", "fgm"),
            command("fandfly", "fand.testplugin.fly", "ffly"),
            command("fandactionbar", "fand.testplugin.actionbar", "factionbar"),
            command("fandtitle", "fand.testplugin.title", "ftitle"),
            command("fandbossbar", "fand.testplugin.bossbar", "fbossbar"),
            command("fandparticle", "fand.testplugin.particle", "fparticle"),
            command("fandsound", "fand.testplugin.sound", "fsound"),
            command("fandkick", "fand.testplugin.kick", "fkick"),
            command("fandtab", "fand.testplugin.tab", "ftab"),
            command("fandsidebar", "fand.testplugin.sidebar", "fsidebar"),
            command("fandrecipe", "fand.testplugin.recipe", "frecipe"),
            command("fandcomponents", "fand.testplugin.components", "fcomponents"),
            command("fandselftest", "fand.testplugin.selftest", "fselftest"),
            command("fandgui", "fand.testplugin.gui", "fgui")
    );

    private static final List<ExpectedEvent> EXPECTED_EVENTS = List.of(
            event("lifecycle", ServerStartedEvent.class),
            event("world", WorldLoadEvent.class),
            event("world", WorldUnloadEvent.class),
            event("world", WorldSaveEvent.class),
            event("world", WeatherChangeEvent.class),
            event("world", ThunderChangeEvent.class),
            event("world", ChunkLoadEvent.class),
            event("world", ChunkUnloadEvent.class),
            event("world", SpawnChangeEvent.class),
            event("world", TimeSkipEvent.class),
            event("server", PlayerLoginEvent.class),
            event("server", ServerListPingEvent.class),
            event("player", PlayerJoinEvent.class),
            event("command", PlayerQuitEvent.class),
            event("command", PlayerCommandPreprocessEvent.class),
            event("command", CommandExecuteEvent.class),
            event("command", TabCompleteEvent.class),
            event("command", PlayerChatEvent.class),
            event("player", PlayerDropItemEvent.class),
            event("player", PlayerPickupItemEvent.class),
            event("player", PlayerFishEvent.class),
            event("player", PlayerItemConsumeEvent.class),
            event("player", PlayerItemDamageEvent.class),
            event("player", PlayerFoodLevelChangeEvent.class),
            event("player", PlayerExperienceChangeEvent.class),
            event("player", PlayerMoveEvent.class),
            event("player", PlayerSwapHandItemsEvent.class),
            event("player", PlayerToggleSneakEvent.class),
            event("player", PlayerToggleSprintEvent.class),
            event("player", PlayerTeleportEvent.class),
            event("player", PlayerPortalEvent.class),
            event("player", PlayerChangedWorldEvent.class),
            event("player", PlayerRespawnEvent.class),
            event("player", PlayerDeathEvent.class),
            event("player", PlayerBedEnterEvent.class),
            event("player", PlayerBedLeaveEvent.class),
            event("player", PlayerAdvancementDoneEvent.class),
            event("player", PlayerGameModeChangeEvent.class),
            event("player", PlayerKickEvent.class),
            event("player", PlayerLocaleChangeEvent.class),
            event("player", PlayerClientBrandEvent.class),
            event("player", PlayerResourcePackStatusEvent.class),
            event("player", PlayerInteractEvent.class),
            event("player", PlayerInteractEntityEvent.class),
            event("player", PlayerShearEntityEvent.class),
            event("player", PlayerLeashEntityEvent.class),
            event("player", PlayerUnleashEntityEvent.class),
            event("player", PlayerItemHeldEvent.class),
            event("player", PlayerBucketFillEvent.class),
            event("player", PlayerBucketEmptyEvent.class),
            event("player", EntityDamageEvent.class),
            event("entity", EntityDamageByEntityEvent.class),
            event("entity", EntityDeathEvent.class),
            event("entity", EntitySpawnEvent.class),
            event("entity", ItemSpawnEvent.class),
            event("entity", ItemDespawnEvent.class),
            event("entity", ItemMergeEvent.class),
            event("entity", EntityPickupItemEvent.class),
            event("entity", EntityDropItemEvent.class),
            event("entity", EntityChangeBlockEvent.class),
            event("entity", EntityRemoveEvent.class),
            event("entity", EntityTeleportEvent.class),
            event("entity", EntityPortalEvent.class),
            event("entity", ExplosionPrimeEvent.class),
            event("entity", EntityExplodeEvent.class),
            event("entity", EntityCombustEvent.class),
            event("entity", EntityRegainHealthEvent.class),
            event("entity", ProjectileHitEvent.class),
            event("entity", ProjectileLaunchEvent.class),
            event("entity", EntityShootBowEvent.class),
            event("entity", EntityTargetEvent.class),
            event("entity", EntityPotionEffectEvent.class),
            event("entity", PotionSplashEvent.class),
            event("entity", LingeringPotionSplashEvent.class),
            event("entity", EntityMountEvent.class),
            event("entity", EntityDismountEvent.class),
            event("entity", EntityResurrectEvent.class),
            event("entity", EntityTameEvent.class),
            event("entity", EntityBreedEvent.class),
            event("entity", EntityTransformEvent.class),
            event("entity", HangingPlaceEvent.class),
            event("entity", HangingBreakEvent.class),
            event("entity", PlayerItemFrameChangeEvent.class),
            event("vehicle", VehicleCreateEvent.class),
            event("vehicle", VehicleDestroyEvent.class),
            event("vehicle", VehicleEnterEvent.class),
            event("vehicle", VehicleExitEvent.class),
            event("vehicle", VehicleMoveEvent.class),
            event("block", BlockBreakEvent.class),
            event("block", BlockPlaceEvent.class),
            event("block", BlockChangeEvent.class),
            event("block", BlockPhysicsEvent.class),
            event("block", BlockBurnEvent.class),
            event("block", BlockDispenseEvent.class),
            event("block", BlockExplodeEvent.class),
            event("block", BlockFadeEvent.class),
            event("block", BlockFertilizeEvent.class),
            event("block", BlockFormEvent.class),
            event("block", BlockFromToEvent.class),
            event("block", BlockGrowEvent.class),
            event("block", BlockIgniteEvent.class),
            event("block", BlockRedstoneEvent.class),
            event("block", BlockSpreadEvent.class),
            event("block", PortalCreateEvent.class),
            event("block", SpongeAbsorbEvent.class),
            event("block", CauldronLevelChangeEvent.class),
            event("block", LeavesDecayEvent.class),
            event("block", SignChangeEvent.class),
            event("block", BlockPistonExtendEvent.class),
            event("block", BlockPistonPushEvent.class),
            event("block", BlockPistonRetractEvent.class),
            event("block", FluidFlowEvent.class),
            event("inventory", InventoryOpenEvent.class),
            event("inventory", InventoryCloseEvent.class),
            event("inventory", InventoryClickEvent.class),
            event("inventory", InventoryCreativeEvent.class),
            event("inventory", InventoryDragEvent.class),
            event("inventory", InventoryMoveItemEvent.class),
            event("inventory", InventoryPickupItemEvent.class),
            event("inventory", HopperMoveItemEvent.class),
            event("inventory", HopperPickupItemEvent.class),
            event("inventory", PrepareItemCraftEvent.class),
            event("inventory", CraftItemEvent.class),
            event("inventory", PrepareItemEnchantEvent.class),
            event("inventory", EnchantItemEvent.class),
            event("inventory", PrepareAnvilEvent.class),
            event("inventory", PrepareSmithingEvent.class),
            event("inventory", PrepareTradeEvent.class),
            event("inventory", InventoryTradeEvent.class),
            event("inventory", FurnaceBurnEvent.class),
            event("inventory", FurnaceSmeltEvent.class),
            event("inventory", FurnaceExtractEvent.class),
            event("inventory", BrewingStandFuelEvent.class),
            event("inventory", BrewEvent.class),
            event("permission", PermissionCheckEvent.class)
    );

    private final PluginContext context;

    SelfTestCommand(PluginContext context) {
        this.context = context;
    }

    @Override
    public void execute(CommandSender sender, String label, List<String> args) {
        var scopes = parseScopes(args);
        if (scopes.isEmpty()) {
            sender.sendMessage(Component.text("Usage: /" + label + " [all|commands|listeners]", NamedTextColor.YELLOW));
            return;
        }

        var report = runSelfTest(context.commands(), context.events(), sender, scopes);
        var color = report.success() ? NamedTextColor.GREEN : NamedTextColor.RED;
        sender.sendMessage(Component.text(
                "Self-test " + (report.success() ? "passed" : "failed")
                        + ": commands " + report.commandsChecked()
                        + ", listeners " + report.listenersChecked()
                        + ", failures " + report.failures().size(),
                color));
        for (var failure : report.failures().stream().limit(12).toList()) {
            sender.sendMessage(Component.text("- " + failure, NamedTextColor.RED));
        }
        if (report.failures().size() > 12) {
            sender.sendMessage(Component.text("... " + (report.failures().size() - 12) + " more failures", NamedTextColor.RED));
        }
    }

    @Override
    public List<String> complete(CommandSender sender, String label, List<String> args) {
        return args.size() <= 1 ? matching(MODES, args.isEmpty() ? "" : args.getLast()) : List.of();
    }

    static SelfTestReport runSelfTest(
            CommandRegistry commands,
            EventBus events,
            CommandSender sender,
            Set<SelfTestScope> scopes) {
        var failures = new ArrayList<String>();
        int commandChecks = 0;
        int listenerChecks = 0;
        if (scopes.contains(SelfTestScope.COMMANDS)) {
            for (var expected : EXPECTED_COMMANDS) {
                commandChecks++;
                checkCommand(commands, sender, expected, failures);
            }
        }
        if (scopes.contains(SelfTestScope.LISTENERS)) {
            for (var expected : EXPECTED_EVENTS) {
                listenerChecks++;
                if (!events.hasListeners(expected.type())) {
                    failures.add("missing listener for " + expected.group() + " event " + expected.type().getSimpleName());
                }
            }
        }
        return new SelfTestReport(commandChecks, listenerChecks, failures);
    }

    static List<ExpectedCommand> expectedCommands() {
        return EXPECTED_COMMANDS;
    }

    static List<ExpectedEvent> expectedEvents() {
        return EXPECTED_EVENTS;
    }

    private static void checkCommand(
            CommandRegistry commands,
            CommandSender sender,
            ExpectedCommand expected,
            List<String> failures) {
        var registered = commands.lookup(expected.label());
        if (registered.isEmpty()) {
            failures.add("missing command /" + expected.label());
            return;
        }
        var descriptor = registered.get().descriptor();
        if (!descriptor.label().equals(expected.label())) {
            failures.add("command /" + expected.label() + " resolved as /" + descriptor.label());
        }
        if (!descriptor.aliases().equals(expected.aliases())) {
            failures.add("command /" + expected.label() + " aliases " + descriptor.aliases() + " expected " + expected.aliases());
        }
        if (!expected.permission().equals(descriptor.permission())) {
            failures.add("command /" + expected.label() + " permission " + descriptor.permission() + " expected " + expected.permission());
        }
        if (!commands.claims(List.of(expected.label()))) {
            failures.add("command registry does not claim /" + expected.label());
        }
        for (var alias : expected.aliases()) {
            var aliasLookup = commands.lookup(alias);
            if (aliasLookup.isEmpty()) {
                failures.add("missing alias /" + alias + " for /" + expected.label());
            } else if (!aliasLookup.get().descriptor().label().equals(expected.label())) {
                failures.add("alias /" + alias + " resolved as /" + aliasLookup.get().descriptor().label());
            }
        }
        try {
            registered.get().completer().complete(sender, expected.label(), List.of());
        } catch (Exception exception) {
            failures.add("completer failed for /" + expected.label() + ": " + exception.getClass().getSimpleName());
        }
    }

    private static Set<SelfTestScope> parseScopes(List<String> args) {
        if (args.isEmpty()) {
            return EnumSet.allOf(SelfTestScope.class);
        }
        return switch (args.getFirst().toLowerCase(Locale.ROOT)) {
            case "all" -> EnumSet.allOf(SelfTestScope.class);
            case "commands" -> EnumSet.of(SelfTestScope.COMMANDS);
            case "listeners" -> EnumSet.of(SelfTestScope.LISTENERS);
            default -> Set.of();
        };
    }

    private static ExpectedCommand command(String label, String permission, String... aliases) {
        return new ExpectedCommand(label, List.of(aliases), permission);
    }

    private static ExpectedEvent event(String group, Class<? extends Event> type) {
        return new ExpectedEvent(group, type);
    }

    enum SelfTestScope {
        COMMANDS,
        LISTENERS
    }

    record ExpectedCommand(String label, List<String> aliases, String permission) {
        ExpectedCommand {
            aliases = List.copyOf(aliases);
        }
    }

    record ExpectedEvent(String group, Class<? extends Event> type) {
    }

    record SelfTestReport(int commandsChecked, int listenersChecked, List<String> failures) {
        SelfTestReport {
            failures = List.copyOf(failures);
        }

        boolean success() {
            return failures.isEmpty();
        }
    }
}
