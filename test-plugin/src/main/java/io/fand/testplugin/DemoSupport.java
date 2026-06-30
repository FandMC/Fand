package io.fand.testplugin;

import com.google.gson.JsonObject;
import io.fand.api.Fand;
import io.fand.api.block.BlockType;
import io.fand.api.block.BlockTypes;
import io.fand.api.command.CommandSender;
import io.fand.api.component.DataComponentKey;
import io.fand.api.config.Configuration;
import io.fand.api.entity.EntityType;
import io.fand.api.entity.EntityTypes;
import io.fand.api.entity.Player;
import io.fand.api.inventory.Inventory;
import io.fand.api.inventory.InventoryType;
import io.fand.api.inventory.Inventories;
import io.fand.api.item.ItemKey;
import io.fand.api.item.ItemStack;
import io.fand.api.item.ItemType;
import io.fand.api.item.ItemTypes;
import io.fand.api.item.component.CustomModelData;
import io.fand.api.item.component.EnchantmentKey;
import io.fand.api.item.component.ItemConsumable;
import io.fand.api.item.component.ItemComponentKeys;
import io.fand.api.item.component.ItemFood;
import io.fand.api.item.component.ItemRarity;
import io.fand.api.item.component.ItemUseCooldown;
import io.fand.api.item.component.ItemWrittenBookContent;
import io.fand.api.performance.MetricStatistics;
import io.fand.api.performance.TickAverages;
import io.fand.api.permission.PermissionDefault;
import io.fand.api.permission.PermissionDescriptor;
import io.fand.api.plugin.PluginContext;
import io.fand.api.recipe.CookingRecipe;
import io.fand.api.recipe.CookingRecipeCategory;
import io.fand.api.recipe.CraftingRecipeCategory;
import io.fand.api.recipe.Recipe;
import io.fand.api.recipe.RecipeIngredient;
import io.fand.api.recipe.RecipeType;
import io.fand.api.recipe.ShapedRecipe;
import io.fand.api.recipe.ShapelessRecipe;
import io.fand.api.recipe.StonecuttingRecipe;
import io.fand.api.world.World;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.UUID;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.key.InvalidKeyException;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.title.Title;

final class DemoSupport {

    private DemoSupport() {
    }

    static final int DEMO_GUI_LOCKED_SLOT = 22;
    static final String DEMO_GUI_LOCKED_ITEM = "minecraft:barrier";
    static final String MUTE_NEXT_COMMAND = "!mute-next";
    static final String CLEAR_MODE = "clear";
    static final String COMMAND_ALIAS_DEMO = "fwhere";
    static final Key DEMO_COMPONENT_RECIPE = Key.key("fand-test-plugin:component_diamond");
    static final Key DEMO_NAVIGATOR_RECIPE = Key.key("fand-test-plugin:kit_navigator");
    static final Key DEMO_SNACK_RECIPE = Key.key("fand-test-plugin:demo_snack");
    static final Key DEMO_GLASS_RECIPE = Key.key("fand-test-plugin:cut_glass");
    static final Key MERCY_ENCHANTMENT = Key.key("fand-test-plugin:mercy");
    static final Key PLUNDER_ENCHANTMENT = Key.key("fand-test-plugin:plunder");

    static final List<String> SAMPLE_BLOCKS = List.of(
            "minecraft:stone",
            "minecraft:grass_block",
            "minecraft:oak_log",
            "minecraft:glass",
            "minecraft:diamond_block",
            "minecraft:gold_block",
            "minecraft:redstone_lamp",
            "minecraft:air"
    );
    static final List<String> SAMPLE_ITEMS = List.of(
            "minecraft:stone",
            "minecraft:oak_log",
            "minecraft:diamond",
            "minecraft:golden_apple",
            "minecraft:ender_pearl",
            "minecraft:torch",
            "minecraft:bread",
            "minecraft:compass"
    );
    static final List<String> SAMPLE_ENTITIES = List.of(
            "minecraft:zombie",
            "minecraft:skeleton",
            "minecraft:creeper",
            "minecraft:cow",
            "minecraft:pig",
            "minecraft:villager",
            "minecraft:item",
            "minecraft:arrow"
    );
    static final List<String> PARTICLE_MODES = List.of(
            "all",
            "simple",
            "dust",
            "block",
            "item",
            "trail",
            "vibration"
    );
    static final List<String> SOUND_MODES = List.of(
            "orb",
            "levelup",
            "anvil",
            "toast",
            "world"
    );
    static final List<String> WORLD_MODES = List.of(
            "list",
            "info",
            "create",
            "unload",
            "day",
            "night",
            "storm",
            "clear",
            "thunder",
            "peaceful",
            "easy",
            "normal",
            "hard",
            "border",
            "save"
    );
    static final List<String> PERMISSIONS = List.of(
            "fand.testplugin.use",
            "fand.testplugin.demo",
            "fand.testplugin.kit",
            "fand.testplugin.performance",
            "fand.testplugin.world",
            "fand.testplugin.tp",
            "fand.testplugin.spawnentity",
            "fand.testplugin.dropitem",
            "fand.testplugin.setblock",
            "fand.testplugin.give",
            "fand.testplugin.item",
            "fand.testplugin.heal",
            "fand.testplugin.mode",
            "fand.testplugin.fly",
            "fand.testplugin.gui",
            "fand.testplugin.actionbar",
            "fand.testplugin.title",
            "fand.testplugin.bossbar",
            "fand.testplugin.particle",
            "fand.testplugin.sound",
            "fand.testplugin.kick",
            "fand.testplugin.tab",
            "fand.testplugin.recipe",
            "fand.testplugin.components",
            "fand.testplugin.nms",
            "fand.testplugin.selftest"
    );
    static final DataComponentKey<String> DEMO_BLOCK_LABEL =
            DataComponentKey.string(Key.key("fand-test-plugin:block_label"));
    static final DataComponentKey<Integer> DEMO_BLOCK_USES =
            DataComponentKey.integer(Key.key("fand-test-plugin:block_uses"));
    static final DataComponentKey<String> DEMO_ENTITY_LABEL =
            DataComponentKey.string(Key.key("fand-test-plugin:entity_label"));
    static final DataComponentKey<Integer> DEMO_ENTITY_USES =
            DataComponentKey.integer(Key.key("fand-test-plugin:entity_uses"));



    static void registerPermissions(PluginContext context) {
        for (var node : PERMISSIONS) {
            context.permissions().register(new PermissionDescriptor(node, PermissionDefault.OPERATOR));
        }
    }

    static void registerDemoRecipes(PluginContext context) {
        var recipes = demoRecipes(
                ItemTypes.of(ItemKey.DIAMOND),
                ItemTypes.of(ItemKey.COMPASS),
                ItemTypes.of(ItemKey.GOLDEN_APPLE),
                ItemTypes.of(ItemKey.GLASS));
        recipes.forEach(recipe -> context.recipes().register(recipe));
        context.logger().info("Registered {} demo recipes", recipes.size());
    }

    static int give(Player target, ItemType type, int amount) {
        return give(target, type.one(), amount);
    }

    static int give(Player target, ItemStack base, int amount) {
        int remaining = amount;
        while (remaining > 0) {
            int batch = Math.min(remaining, base.maxStackSize());
            ItemStack leftover = target.inventory().add(base.withAmount(batch));
            int accepted = batch - (leftover.isEmpty() ? 0 : leftover.amount());
            remaining -= accepted;
            if (accepted < batch) {
                break;
            }
        }
        return amount - remaining;
    }

    static void put(Inventory inventory, int slot, String item, int amount) {
        inventory.set(slot, ItemTypes.of(keyString(item)).stack(amount));
    }

    static void put(Inventory inventory, int slot, ItemStack stack) {
        inventory.set(slot, stack);
    }

    static Inventory demoGuiInventory(PluginContext context) {
        Inventory inventory = Inventories.create(InventoryType.CHEST, 27, Component.text("Fand API Demo", NamedTextColor.DARK_AQUA));
        put(inventory, 10, "minecraft:diamond", 3);
        put(inventory, 12, "minecraft:golden_apple", 1);
        put(inventory, 14, "minecraft:compass", 1);
        put(inventory, 16, "minecraft:oak_log", 16);
        put(inventory, DEMO_GUI_LOCKED_SLOT, DEMO_GUI_LOCKED_ITEM, 1);
        inventory.addSlotChangeListener((slot, oldStack, newStack) ->
                context.logger().info("demo gui slot {} changed: {} -> {}", slot, stackName(oldStack), stackName(newStack)));
        return inventory;
    }

    static Inventory demoKitInventory(PluginContext context, String playerName) {
        Inventory inventory = Inventories.create(InventoryType.CHEST, 27, Component.text("Fand Kit", NamedTextColor.GOLD));
        put(inventory, 10, demoComponentItem(ItemTypes.of("minecraft:diamond"), playerName));
        put(inventory, 12, demoKitNavigator(ItemTypes.of("minecraft:compass"), playerName));
        put(inventory, 14, demoKitBook(ItemTypes.of("minecraft:written_book"), playerName));
        put(inventory, 16, demoKitSnack(ItemTypes.of("minecraft:golden_apple")).withAmount(8));
        put(inventory, DEMO_GUI_LOCKED_SLOT, DEMO_GUI_LOCKED_ITEM, 1);
        inventory.addSlotChangeListener((slot, oldStack, newStack) ->
                context.logger().info("kit gui slot {} changed: {} -> {}", slot, stackName(oldStack), stackName(newStack)));
        return inventory;
    }

    static void openDemoInventory(
            PluginContext context,
            CommandSender sender,
            Player target,
            Set<UUID> demoGuiViewers,
            Inventory inventory,
            String openedMessage) {
        target.openInventory(inventory).whenComplete((opened, failure) -> {
            if (failure != null) {
                context.logger().warn("Could not open demo GUI for {}", target.name(), failure);
                sender.sendMessage(Component.text("Could not open demo GUI.", NamedTextColor.RED));
                return;
            }
            if (Boolean.TRUE.equals(opened)) {
                demoGuiViewers.add(target.uniqueId());
                target.sendMessage(Component.text(openedMessage, NamedTextColor.GREEN));
                if (target != sender) {
                    sender.sendMessage(Component.text("Opened demo GUI for " + target.name() + ".", NamedTextColor.GREEN));
                }
            } else {
                sender.sendMessage(Component.text("Inventory open was rejected or the player went offline.", NamedTextColor.YELLOW));
            }
        });
    }

    static List<Recipe> demoRecipes(ItemType diamond, ItemType compass, ItemType goldenApple, ItemType glass) {
        return List.of(
                new ShapelessRecipe(
                        DEMO_COMPONENT_RECIPE,
                        List.of(
                                RecipeIngredient.of(ItemKey.DIAMOND),
                                RecipeIngredient.of(ItemKey.REDSTONE)),
                        demoComponentItem(diamond, "recipe").withAmount(2),
                        "fand_demo",
                        CraftingRecipeCategory.EQUIPMENT,
                        true),
                new ShapedRecipe(
                        DEMO_NAVIGATOR_RECIPE,
                        List.of(" R ", "RCR", " R "),
                        Map.of(
                                'R', RecipeIngredient.of(ItemKey.REDSTONE),
                                'C', RecipeIngredient.of(ItemKey.COMPASS)),
                        demoKitNavigator(compass, "recipe"),
                        "fand_demo",
                        CraftingRecipeCategory.EQUIPMENT,
                        true),
                new CookingRecipe(
                        DEMO_SNACK_RECIPE,
                        RecipeType.SMELTING,
                        RecipeIngredient.of(ItemKey.APPLE),
                        demoKitSnack(goldenApple),
                        0.35F,
                        120,
                        "fand_demo",
                        CookingRecipeCategory.FOOD,
                        true),
                new StonecuttingRecipe(
                        DEMO_GLASS_RECIPE,
                        RecipeIngredient.of(ItemKey.GLASS),
                        demoGlassRecipeResult(glass),
                        "fand_demo",
                        true));
    }

    static ItemStack demoGlassRecipeResult(ItemType type) {
        var customData = new JsonObject();
        customData.addProperty("created_by", "fand-test-plugin");
        customData.addProperty("demo_role", "fand_recipe_glass");
        return type.one()
                .withCustomName(Component.text("Fand Cut Glass", NamedTextColor.AQUA))
                .withLore(Component.text("Created by a stonecutting recipe registered through Fand.", NamedTextColor.GRAY))
                .withRarity(ItemRarity.UNCOMMON)
                .withCustomData(customData);
    }

    static List<String> demoRecipeKeySuggestions() {
        return List.of(
                DEMO_COMPONENT_RECIPE.asString(),
                DEMO_NAVIGATOR_RECIPE.asString(),
                DEMO_SNACK_RECIPE.asString(),
                DEMO_GLASS_RECIPE.asString(),
                DEMO_COMPONENT_RECIPE.value(),
                DEMO_NAVIGATOR_RECIPE.value(),
                DEMO_SNACK_RECIPE.value(),
                DEMO_GLASS_RECIPE.value());
    }

    static String recipeSummary(Recipe recipe) {
        return recipe.key().asString()
                + " "
                + recipe.type().name().toLowerCase(Locale.ROOT)
                + " -> "
                + stackName(recipe.result());
    }

    static ItemStack demoComponentItem(ItemType type, String source) {
        var customData = new JsonObject();
        customData.addProperty("created_by", "fand-test-plugin");
        customData.addProperty("source", source);
        return type.one()
                .withMaxStackSize(99)
                .withCustomName(Component.text("Fand Component Item", NamedTextColor.GOLD))
                .withLore(
                        Component.text("Modern data components are attached.", NamedTextColor.LIGHT_PURPLE),
                        Component.text("Lore, model data, glint, and custom data survive inventory round-trips.", NamedTextColor.GRAY))
                .withRarity(ItemRarity.RARE)
                .withEnchantmentGlintOverride(true)
                .withEnchantment(EnchantmentKey.UNBREAKING, 3)
                .withStoredEnchantment(EnchantmentKey.MENDING, 1)
                .withEnchantable(30)
                .withHiddenTooltipComponent(ItemComponentKeys.STORED_ENCHANTMENTS, true)
                .withCustomModelData(new CustomModelData(
                        List.of(20018.0F),
                        List.of(true),
                        List.of("fand-demo"),
                        List.of(0x33CCFF)))
                .withCustomData(customData);
    }

    static ItemStack demoKitNavigator(ItemType type, String playerName) {
        var customData = new JsonObject();
        customData.addProperty("created_by", "fand-test-plugin");
        customData.addProperty("demo_role", "fand_kit_navigator");
        customData.addProperty("owner", playerName);
        return type.one()
                .withCustomName(Component.text("Fand Kit Navigator", NamedTextColor.AQUA))
                .withLore(
                        Component.text("Right-click to reopen the kit GUI.", NamedTextColor.GRAY),
                        Component.text("Uses custom_data to connect an item to an event.", NamedTextColor.DARK_AQUA))
                .withRarity(ItemRarity.UNCOMMON)
                .withEnchantmentGlintOverride(true)
                .withUseCooldown(new ItemUseCooldown(1.5F))
                .withCustomModelData(new CustomModelData(
                        List.of(20018.1F),
                        List.of(true),
                        List.of("kit-navigator"),
                        List.of(0x00CCFF)))
                .withCustomData(customData);
    }

    static ItemStack demoKitBook(ItemType type, String playerName) {
        var customData = new JsonObject();
        customData.addProperty("created_by", "fand-test-plugin");
        customData.addProperty("demo_role", "fand_kit_guide");
        customData.addProperty("owner", playerName);
        return type.one()
                .withCustomName(Component.text("Fand API Field Guide", NamedTextColor.GOLD))
                .withLore(
                        Component.text("A written book built through typed item components.", NamedTextColor.GRAY),
                        Component.text("Shows text components embedded inside item data.", NamedTextColor.DARK_GRAY))
                .withRarity(ItemRarity.RARE)
                .withWrittenBookContent(new ItemWrittenBookContent(
                        "Fand API",
                        "fand-test-plugin",
                        List.of(
                                Component.text("Welcome, " + playerName + ".", NamedTextColor.GOLD)
                                        .append(Component.newline())
                                        .append(Component.text("This kit was built with the public Fand API.", NamedTextColor.GRAY)),
                                Component.text("Try /fandperf, /fandgui, and /fandtab.", NamedTextColor.AQUA))))
                .withCustomData(customData);
    }

    static ItemStack demoKitSnack(ItemType type) {
        var customData = new JsonObject();
        customData.addProperty("created_by", "fand-test-plugin");
        customData.addProperty("demo_role", "fand_kit_snack");
        return type.one()
                .withCustomName(Component.text("Fand Demo Snack", NamedTextColor.YELLOW))
                .withLore(Component.text("Food and consumable components can be overridden too.", NamedTextColor.GRAY))
                .withRarity(ItemRarity.UNCOMMON)
                .withFood(new ItemFood(6, 8.0F, true))
                .withConsumable(ItemConsumable.DEFAULT)
                .withCustomData(customData);
    }

    static ItemStack demoMercyWeapon() {
        var customData = new JsonObject();
        customData.addProperty("created_by", "fand-test-plugin");
        customData.addProperty("demo_role", "fand_mercy_weapon");
        return ItemTypes.of("minecraft:diamond_sword").one()
                .withCustomName(Component.text("\u4EC1\u6148\u5251", NamedTextColor.GREEN))
                .withLore(
                        Component.text("\u4EC1\u6148: \u653B\u51FB\u5B9E\u4F53\u65F6\u4F24\u5BB3\u53D8\u4E3A 0\u3002", NamedTextColor.GREEN))
                .withRarity(ItemRarity.RARE)
                .withEnchantmentGlintOverride(true)
                .withEnchantment(MERCY_ENCHANTMENT, 1)
                .withCustomData(customData);
    }

    static ItemStack demoPlunderWeapon() {
        var customData = new JsonObject();
        customData.addProperty("created_by", "fand-test-plugin");
        customData.addProperty("demo_role", "fand_plunder_weapon");
        return ItemTypes.of("minecraft:iron_sword").one()
                .withCustomName(Component.text("\u63A0\u593A\u5251", NamedTextColor.GOLD))
                .withLore(
                        Component.text("\u63A0\u593A: \u653B\u51FB\u5B9E\u4F53\u65F6\u6389\u843D\u76EE\u6807\u624B\u6301\u7269\u3002", NamedTextColor.YELLOW))
                .withRarity(ItemRarity.UNCOMMON)
                .withEnchantmentGlintOverride(true)
                .withEnchantment(PLUNDER_ENCHANTMENT, 1)
                .withCustomData(customData);
    }

    static boolean hasEnchantment(ItemStack stack, Key enchantment) {
        return !stack.isEmpty() && stack.enchantments().has(enchantment);
    }

    static boolean isKitNavigator(ItemStack stack) {
        return !stack.isEmpty()
                && stack.customData()
                        .map(data -> data.has("demo_role")
                                && data.get("demo_role").getAsString().equals("fand_kit_navigator"))
                        .orElse(false);
    }

    static void sendKitPresentation(PluginContext context, Player target) {
        var performance = Fand.server().performance();
        target.sendActionBar(Component.text(
                "TPS " + formatTickAverages(performance.ticksPerSecond()) + " | MSPT " + trim(performance.currentMillisecondsPerTick()),
                NamedTextColor.AQUA));
        target.showTitle(Title.title(
                Component.text(message(context.config(), "messages.kit-title", "Fand Kit"), NamedTextColor.GOLD),
                Component.text(message(context.config(), "messages.kit-subtitle", "Components, GUI, tab, and performance."), NamedTextColor.YELLOW),
                Title.Times.times(Duration.ofMillis(500), Duration.ofSeconds(3), Duration.ofMillis(750))));
        target.sendTabList(
                Component.text("Fand Kit Demo", NamedTextColor.GOLD),
                Component.text("TPS " + formatTickAverages(performance.ticksPerSecond()), NamedTextColor.GRAY));
        BossBar bar = BossBar.bossBar(
                Component.text("Fand kit ready", NamedTextColor.GOLD),
                boundedBossBarProgress((float) (1.0 - Math.min(1.0, performance.fiveSeconds().utilization()))),
                BossBar.Color.GREEN,
                BossBar.Overlay.PROGRESS);
        target.showBossBar(bar);
        context.scheduler().runMainAfter(() -> target.hideBossBar(bar), Duration.ofSeconds(8));
    }

    static Player player(CommandSender sender, String name) {
        return Fand.server().player(name).orElseGet(() -> {
            sender.sendMessage(Component.text("Unknown online player: " + name, NamedTextColor.RED));
            return null;
        });
    }

    static World world(CommandSender sender, String raw, Player fallback) {
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

    static BlockType blockType(CommandSender sender, String raw) {
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

    static ItemType itemType(CommandSender sender, String raw) {
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

    static EntityType entityType(CommandSender sender, String raw) {
        try {
            return EntityTypes.of(keyString(raw));
        } catch (NoSuchElementException ex) {
            sender.sendMessage(Component.text("Unknown entity: " + raw, NamedTextColor.RED));
            return null;
        } catch (InvalidKeyException ex) {
            sender.sendMessage(Component.text("Invalid entity key: " + raw, NamedTextColor.RED));
            return null;
        }
    }

    static Integer parseInt(CommandSender sender, String raw, String name) {
        try {
            return Integer.parseInt(raw);
        } catch (NumberFormatException ex) {
            sender.sendMessage(Component.text(name + " must be an integer: " + raw, NamedTextColor.RED));
            return null;
        }
    }

    static Double parseDouble(CommandSender sender, String raw, String name) {
        try {
            return Double.parseDouble(raw);
        } catch (NumberFormatException ex) {
            sender.sendMessage(Component.text(name + " must be a number: " + raw, NamedTextColor.RED));
            return null;
        }
    }

    static Float parseFloat(CommandSender sender, String raw, String name) {
        try {
            float value = Float.parseFloat(raw);
            if (!Float.isFinite(value)) {
                throw new NumberFormatException(raw);
            }
            return value;
        } catch (NumberFormatException ex) {
            sender.sendMessage(Component.text(name + " must be a number: " + raw, NamedTextColor.RED));
            return null;
        }
    }

    static String message(Configuration config, String path, String fallback) {
        return config.getString(path, fallback);
    }

    static List<String> playerNames() {
        return Fand.server().players().stream().map(Player::name).toList();
    }

    static List<String> worldKeys() {
        return Fand.server().worlds().stream().map(world -> world.key().asString()).toList();
    }

    static boolean isMuteNextCommand(String text) {
        return text.trim().equalsIgnoreCase(MUTE_NEXT_COMMAND);
    }

    static boolean isClearMode(String text) {
        return text.trim().equalsIgnoreCase(CLEAR_MODE);
    }

    static boolean isCommandAliasDemo(String command) {
        return command.trim().equalsIgnoreCase(COMMAND_ALIAS_DEMO);
    }

    static boolean isLockedDemoGuiClick(boolean demoGuiViewer, InventoryType inventoryType, int slot, ItemStack currentItem) {
        return demoGuiViewer
                && inventoryType == InventoryType.CHEST
                && slot == DEMO_GUI_LOCKED_SLOT
                && isStackType(currentItem, DEMO_GUI_LOCKED_ITEM);
    }

    static boolean isStackType(ItemStack stack, String itemKey) {
        return !stack.isEmpty() && stack.type().key().asString().equals(keyString(itemKey));
    }

    static boolean isFloat(String raw) {
        try {
            return Float.isFinite(Float.parseFloat(raw));
        } catch (NumberFormatException ex) {
            return false;
        }
    }

    static float boundedBossBarProgress(float progress) {
        return Math.max(0.0F, Math.min(1.0F, progress));
    }

    static String messageText(List<String> args, String fallback) {
        var text = String.join(" ", args).trim();
        return text.isBlank() ? fallback : text;
    }

    static String formatTickAverages(TickAverages averages) {
        return String.format(
                Locale.ROOT,
                "%.2f, %.2f, %.2f (1m, 5m, 15m)",
                averages.oneMinute(),
                averages.fiveMinutes(),
                averages.fifteenMinutes());
    }

    static String formatMetricStatistics(MetricStatistics statistics) {
        return String.format(
                Locale.ROOT,
                "avg %.2f / min %.2f / max %.2f / median %.2f",
                statistics.average(),
                statistics.minimum(),
                statistics.maximum(),
                statistics.median());
    }

    static DemoTitle demoTitle(String raw, String defaultTitle, String defaultSubtitle) {
        var parts = raw.split("\\|", 2);
        var title = parts[0].trim();
        var subtitle = parts.length == 2 ? parts[1].trim() : "";
        return new DemoTitle(
                title.isBlank() ? defaultTitle : title,
                subtitle.isBlank() ? defaultSubtitle : subtitle);
    }

    static TargetedArgs targetedArgs(CommandSender sender, List<String> args, String usage) {
        if (!args.isEmpty()) {
            var explicitTarget = Fand.server().player(args.getFirst());
            if (explicitTarget.isPresent()) {
                return new TargetedArgs(explicitTarget.get(), args.subList(1, args.size()));
            }
            if (!(sender instanceof Player)) {
                player(sender, args.getFirst());
                return null;
            }
        }
        if (sender instanceof Player player) {
            return new TargetedArgs(player, args);
        }
        sender.sendMessage(Component.text("Console must provide a target player: " + usage, NamedTextColor.RED));
        return null;
    }

    static List<String> matching(List<String> values, String rawPrefix) {
        var prefix = rawPrefix.toLowerCase(Locale.ROOT);
        var matches = new ArrayList<String>();
        for (var value : values) {
            if (value.toLowerCase(Locale.ROOT).startsWith(prefix)) {
                matches.add(value);
            }
        }
        return matches;
    }

    static String keyString(String raw) {
        var value = raw.trim().toLowerCase(Locale.ROOT);
        return value.indexOf(':') >= 0 ? value : "minecraft:" + value;
    }

    static String blockName(BlockType type) {
        return type.key().asString();
    }

    static String itemName(ItemType type) {
        return type.key().asString();
    }

    static String stackName(ItemStack stack) {
        return stack.isEmpty() ? "empty" : stack.amount() + "x " + itemName(stack.type());
    }

    static String compactLocation(io.fand.api.world.Location location) {
        return location.world().name() + " "
                + location.blockX() + "," + location.blockY() + "," + location.blockZ();
    }

    static String trim(double value) {
        return value == Math.rint(value) ? Long.toString(Math.round(value)) : Double.toString(value);
    }

    record TargetedArgs(Player player, List<String> args) {
    }

    record DemoTitle(String title, String subtitle) {
    }
}
