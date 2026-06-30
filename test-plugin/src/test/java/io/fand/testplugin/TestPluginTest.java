package io.fand.testplugin;

import static org.assertj.core.api.Assertions.assertThat;

import io.fand.api.command.CommandCompleter;
import io.fand.api.command.CommandDescriptor;
import io.fand.api.command.CommandExecutor;
import io.fand.api.command.CommandRegistration;
import io.fand.api.command.CommandRegistry;
import io.fand.api.command.CommandSender;
import io.fand.api.command.CommandSpec;
import io.fand.api.command.RegisteredCommand;
import io.fand.api.command.ResolvedCommand;
import io.fand.api.auth.LoginAuthenticationRequest;
import io.fand.api.auth.LoginAuthenticationResult;
import io.fand.api.event.Event;
import io.fand.api.event.EventBus;
import io.fand.api.event.EventListener;
import io.fand.api.event.EventPriority;
import io.fand.api.event.EventSubscription;
import io.fand.api.event.Subscribe;
import io.fand.api.event.player.PlayerJoinEvent;
import io.fand.api.event.world.ChunkLoadEvent;
import io.fand.api.event.world.ChunkUnloadEvent;
import io.fand.api.event.world.ThunderChangeEvent;
import io.fand.api.event.world.WeatherChangeEvent;
import io.fand.api.event.world.WorldLoadEvent;
import io.fand.api.event.world.WorldSaveEvent;
import io.fand.api.event.world.WorldUnloadEvent;
import io.fand.api.lifecycle.ServerStartedEvent;
import io.fand.api.inventory.InventoryType;
import io.fand.api.item.ItemStack;
import io.fand.api.item.ItemType;
import io.fand.api.item.component.EnchantmentKey;
import io.fand.api.item.component.ItemComponentKeys;
import io.fand.api.item.component.ItemRarity;
import io.fand.api.performance.MetricStatistics;
import io.fand.api.performance.TickAverages;
import io.fand.api.recipe.CookingRecipe;
import io.fand.api.recipe.RecipeType;
import io.fand.api.recipe.ShapedRecipe;
import io.fand.api.recipe.ShapelessRecipe;
import io.fand.api.recipe.StonecuttingRecipe;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.junit.jupiter.api.Test;

final class TestPluginTest {

    @Test
    void normalisesMinecraftKeys() {
        assertThat(DemoSupport.keyString("stone")).isEqualTo("minecraft:stone");
        assertThat(DemoSupport.keyString(" MINECRAFT:DIAMOND ")).isEqualTo("minecraft:diamond");
        assertThat(DemoSupport.keyString("custom:block")).isEqualTo("custom:block");
    }

    @Test
    void matchesValuesByCaseInsensitivePrefix() {
        assertThat(DemoSupport.matching(List.of("minecraft:stone", "minecraft:torch", "minecraft:diamond"), "MINECRAFT:T"))
                .containsExactly("minecraft:torch");
        assertThat(DemoSupport.matching(List.of("alpha", "beta"), ""))
                .containsExactly("alpha", "beta");
    }

    @Test
    void recognisesMuteNextCommand() {
        assertThat(DemoSupport.isMuteNextCommand("!mute-next")).isTrue();
        assertThat(DemoSupport.isMuteNextCommand("  !MUTE-NEXT  ")).isTrue();
        assertThat(DemoSupport.isMuteNextCommand("!where")).isFalse();
    }

    @Test
    void recognisesCommandAliasDemo() {
        assertThat(DemoSupport.isCommandAliasDemo(" fwhere ")).isTrue();
        assertThat(DemoSupport.isCommandAliasDemo("fanddemo")).isFalse();
    }

    @Test
    void offlineChineseLoginUsesClientProfileId() {
        var requestedProfileId = UUID.fromString("11111111-2222-3333-4444-555555555555");
        var result = TestPlugin.authenticateOfflineForNonAsciiName(new LoginAuthenticationRequest(
                "20019a啊",
                "server-id",
                new java.net.InetSocketAddress("127.0.0.1", 25565),
                null,
                requestedProfileId));

        assertThat(result.action()).isEqualTo(LoginAuthenticationResult.Action.ALLOW);
        assertThat(result.profileOrNull()).isNotNull();
        assertThat(result.profileOrNull().uniqueId()).isEqualTo(requestedProfileId);
        assertThat(result.profileOrNull().name()).isEqualTo("20019a啊");
    }

    @Test
    void asciiLoginFallsThroughToMojangAuthentication() {
        var result = TestPlugin.authenticateOfflineForNonAsciiName(new LoginAuthenticationRequest(
                "20019a",
                "server-id",
                new java.net.InetSocketAddress("127.0.0.1", 25565),
                null));

        assertThat(result.action()).isEqualTo(LoginAuthenticationResult.Action.PASS);
    }

    @Test
    void recognisesTabClearMode() {
        assertThat(DemoSupport.isClearMode(" clear ")).isTrue();
        assertThat(DemoSupport.isClearMode("show")).isFalse();
    }

    @Test
    void locksBarrierInDemoGui() {
        var barrier = stack("minecraft:barrier");

        assertThat(DemoSupport.isLockedDemoGuiClick(true, InventoryType.CHEST, DemoSupport.DEMO_GUI_LOCKED_SLOT, barrier))
                .isTrue();
    }

    @Test
    void ignoresBarrierOutsideDemoLockedSlot() {
        var barrier = stack("minecraft:barrier");

        assertThat(DemoSupport.isLockedDemoGuiClick(false, InventoryType.CHEST, DemoSupport.DEMO_GUI_LOCKED_SLOT, barrier))
                .isFalse();
        assertThat(DemoSupport.isLockedDemoGuiClick(true, InventoryType.CHEST, DemoSupport.DEMO_GUI_LOCKED_SLOT + 1, barrier))
                .isFalse();
        assertThat(DemoSupport.isLockedDemoGuiClick(true, InventoryType.PLAYER, DemoSupport.DEMO_GUI_LOCKED_SLOT, barrier))
                .isFalse();
        assertThat(DemoSupport.isLockedDemoGuiClick(true, InventoryType.CHEST, DemoSupport.DEMO_GUI_LOCKED_SLOT, ItemStack.EMPTY))
                .isFalse();
    }

    @Test
    void recognisesStackTypeWithImplicitNamespace() {
        assertThat(DemoSupport.isStackType(stack("minecraft:barrier"), "barrier")).isTrue();
        assertThat(DemoSupport.isStackType(stack("minecraft:stone"), "barrier")).isFalse();
        assertThat(DemoSupport.isStackType(ItemStack.EMPTY, "barrier")).isFalse();
    }

    @Test
    void clampsBossBarProgress() {
        assertThat(DemoSupport.boundedBossBarProgress(-0.5F)).isZero();
        assertThat(DemoSupport.boundedBossBarProgress(0.6F)).isEqualTo(0.6F);
        assertThat(DemoSupport.boundedBossBarProgress(1.5F)).isEqualTo(1.0F);
    }

    @Test
    void recognisesFiniteFloatText() {
        assertThat(DemoSupport.isFloat("0.5")).isTrue();
        assertThat(DemoSupport.isFloat("NaN")).isFalse();
        assertThat(DemoSupport.isFloat("hello")).isFalse();
    }

    @Test
    void exposesDetailEventConfigKeys() throws Exception {
        String config = java.nio.file.Files.readString(java.nio.file.Path.of("src/main/resources/config.yml"));

        assertThat(config).contains(
                "log-inventory-moves: false",
                "log-crafting-events: false",
                "log-block-detail-events: false",
                "log-entity-detail-events: false",
                "log-detailed-damage-events: false",
                "log-player-detail-events: false",
                "log-player-move-events: false",
                "decorate-player-death-message: true");
    }

    @Test
    void selfTestMetadataCoversCommandAndPermission() {
        assertThat(DemoSupport.PERMISSIONS).contains("fand.testplugin.selftest");
        assertThat(SelfTestCommand.expectedCommands()).extracting(SelfTestCommand.ExpectedCommand::label)
                .contains("fandselftest", "fandgui")
                .doesNotHaveDuplicates();
        assertThat(SelfTestCommand.expectedEvents()).extracting(event -> event.type().getName())
                .doesNotHaveDuplicates();
    }

    @Test
    void selfTestListenerMetadataMatchesDemoListenerMethods() {
        var subscribed = subscribedEventTypes(
                DemoBlockEvents.class,
                DemoCommandEvents.class,
                DemoEntityEvents.class,
                DemoInventoryEvents.class,
                DemoPermissionEvents.class,
                DemoPlayerEvents.class,
                DemoServerEvents.class,
                DemoWorldEvents.class);
        var inlineSubscriptions = Set.<Class<? extends Event>>of(
                ServerStartedEvent.class,
                WorldLoadEvent.class,
                WorldUnloadEvent.class,
                WorldSaveEvent.class,
                WeatherChangeEvent.class,
                ThunderChangeEvent.class,
                ChunkLoadEvent.class,
                ChunkUnloadEvent.class,
                PlayerJoinEvent.class);
        var covered = new java.util.LinkedHashSet<>(subscribed);
        covered.addAll(inlineSubscriptions);
        covered.addAll(DemoDetailedDamageEvents.subscribedEventTypes());

        assertThat(SelfTestCommand.expectedEvents())
                .allSatisfy(event -> assertThat(covered)
                        .as(event.type().getSimpleName())
                        .contains(event.type()));
    }

    @Test
    void allDemoCommandClassesCarryCommandSpec() {
        assertThat(List.of(
                HelloCommand.class,
                DemoCommand.class,
                KitCommand.class,
                PerformanceCommand.class,
                WorldCommand.class,
                TeleportCommand.class,
                SetBlockCommand.class,
                GiveCommand.class,
                ComponentItemCommand.class,
                HealCommand.class,
                GameModeCommand.class,
                FlyCommand.class,
                ActionBarCommand.class,
                TitleCommand.class,
                BossBarCommand.class,
                ParticleCommand.class,
                SoundCommand.class,
                KickCommand.class,
                TabCommand.class,
                RecipeCommand.class,
                ComponentsCommand.class,
                SelfTestCommand.class,
                GuiCommand.class
        )).allSatisfy(type -> assertThat(type.getAnnotation(CommandSpec.class))
                .as(type.getSimpleName())
                .isNotNull());
    }

    @Test
    void selfTestPassesWithExpectedRegistryAndListeners() {
        var report = SelfTestCommand.runSelfTest(
                new FakeCommandRegistry(SelfTestCommand.expectedCommands()),
                new FakeEventBus(SelfTestCommand.expectedEvents().stream()
                        .map(SelfTestCommand.ExpectedEvent::type)
                        .collect(java.util.stream.Collectors.toSet())),
                new TestSender(),
                EnumSet.allOf(SelfTestCommand.SelfTestScope.class));

        assertThat(report.success()).isTrue();
        assertThat(report.commandsChecked()).isEqualTo(SelfTestCommand.expectedCommands().size());
        assertThat(report.listenersChecked()).isEqualTo(SelfTestCommand.expectedEvents().size());
    }

    @Test
    void selfTestReportsMissingCommandAndListener() {
        var report = SelfTestCommand.runSelfTest(
                new FakeCommandRegistry(List.of()),
                new FakeEventBus(Set.of()),
                new TestSender(),
                EnumSet.allOf(SelfTestCommand.SelfTestScope.class));

        assertThat(report.success()).isFalse();
        assertThat(report.failures()).anySatisfy(failure -> assertThat(failure).contains("missing command /fandtest"));
        assertThat(report.failures()).anySatisfy(failure -> assertThat(failure).contains("missing listener"));
    }

    @Test
    void joinsMessageTextWithFallback() {
        assertThat(DemoSupport.messageText(List.of("hello", "world"), "fallback")).isEqualTo("hello world");
        assertThat(DemoSupport.messageText(List.of(), "fallback")).isEqualTo("fallback");
        assertThat(DemoSupport.messageText(List.of("  "), "fallback")).isEqualTo("fallback");
    }

    @Test
    void splitsDemoTitleAndSubtitle() {
        var explicit = DemoSupport.demoTitle("Main | Sub", "Default", "Default Sub");
        var fallbackSubtitle = DemoSupport.demoTitle("Main", "Default", "Default Sub");
        var fallbackTitle = DemoSupport.demoTitle(" | Sub", "Default", "Default Sub");

        assertThat(explicit.title()).isEqualTo("Main");
        assertThat(explicit.subtitle()).isEqualTo("Sub");
        assertThat(fallbackSubtitle.title()).isEqualTo("Main");
        assertThat(fallbackSubtitle.subtitle()).isEqualTo("Default Sub");
        assertThat(fallbackTitle.title()).isEqualTo("Default");
        assertThat(fallbackTitle.subtitle()).isEqualTo("Sub");
    }

    @Test
    void buildsDemoComponentItem() {
        var item = DemoSupport.demoComponentItem(new TestItemType(Key.key("minecraft:diamond"), 64), "tester");

        assertThat(item.maxStackSize()).isEqualTo(99);
        assertThat(item.customName()).contains(net.kyori.adventure.text.Component.text("Fand Component Item", net.kyori.adventure.text.format.NamedTextColor.GOLD));
        assertThat(item.lore()).hasSize(2);
        assertThat(item.enchantmentGlintOverride()).contains(true);
        assertThat(item.enchantments().level(EnchantmentKey.UNBREAKING)).isEqualTo(3);
        assertThat(item.storedEnchantments().level(EnchantmentKey.MENDING)).isEqualTo(1);
        assertThat(item.enchantable()).contains(30);
        assertThat(item.tooltipDisplay().hides(ItemComponentKeys.STORED_ENCHANTMENTS)).isTrue();
        assertThat(item.rarity()).contains(ItemRarity.RARE);
        assertThat(item.components().has(ItemComponentKeys.CUSTOM_MODEL_DATA)).isTrue();
        assertThat(item.customData()).get().extracting(json -> json.get("source").getAsString()).isEqualTo("tester");
    }

    @Test
    void buildsKitNavigatorWithCustomDataMarker() {
        var item = DemoSupport.demoKitNavigator(new TestItemType(Key.key("minecraft:compass"), 64), "tester");

        assertThat(item.customName()).contains(net.kyori.adventure.text.Component.text("Fand Kit Navigator", net.kyori.adventure.text.format.NamedTextColor.AQUA));
        assertThat(item.lore()).hasSize(2);
        assertThat(item.rarity()).contains(ItemRarity.UNCOMMON);
        assertThat(item.enchantmentGlintOverride()).contains(true);
        assertThat(item.useCooldown()).get().extracting(cooldown -> cooldown.seconds()).isEqualTo(1.5F);
        assertThat(DemoSupport.isKitNavigator(item)).isTrue();
        assertThat(DemoSupport.isKitNavigator(stack("minecraft:compass"))).isFalse();
    }

    @Test
    void buildsKitBookAndSnackComponents() {
        var book = DemoSupport.demoKitBook(new TestItemType(Key.key("minecraft:written_book"), 64), "tester");
        var snack = DemoSupport.demoKitSnack(new TestItemType(Key.key("minecraft:golden_apple"), 64));

        assertThat(book.writtenBookContent()).isPresent();
        assertThat(book.customData()).get().extracting(json -> json.get("demo_role").getAsString()).isEqualTo("fand_kit_guide");
        assertThat(snack.food()).get().extracting(food -> food.nutrition()).isEqualTo(6);
        assertThat(snack.consumable()).isPresent();
        assertThat(snack.customData()).get().extracting(json -> json.get("demo_role").getAsString()).isEqualTo("fand_kit_snack");
    }

    @Test
    void buildsDemoRecipeModels() {
        var recipes = DemoSupport.demoRecipes(
                new TestItemType(Key.key("minecraft:diamond"), 64),
                new TestItemType(Key.key("minecraft:compass"), 64),
                new TestItemType(Key.key("minecraft:golden_apple"), 64),
                new TestItemType(Key.key("minecraft:glass"), 64));

        assertThat(recipes).extracting(recipe -> recipe.key()).containsExactly(
                DemoSupport.DEMO_COMPONENT_RECIPE,
                DemoSupport.DEMO_NAVIGATOR_RECIPE,
                DemoSupport.DEMO_SNACK_RECIPE,
                DemoSupport.DEMO_GLASS_RECIPE);
        assertThat(recipes).extracting(recipe -> recipe.type()).containsExactly(
                RecipeType.SHAPELESS,
                RecipeType.SHAPED,
                RecipeType.SMELTING,
                RecipeType.STONECUTTING);
        assertThat(recipes.get(0)).isInstanceOfSatisfying(ShapelessRecipe.class, recipe -> {
            assertThat(recipe.ingredients()).hasSize(2);
            assertThat(recipe.result().amount()).isEqualTo(2);
            assertThat(recipe.result().customName()).contains(Component.text("Fand Component Item", NamedTextColor.GOLD));
        });
        assertThat(recipes.get(1)).isInstanceOfSatisfying(ShapedRecipe.class, recipe -> {
            assertThat(recipe.pattern()).containsExactly(" R ", "RCR", " R ");
            assertThat(recipe.ingredients()).containsOnlyKeys('R', 'C');
            assertThat(recipe.result().customData()).get().extracting(json -> json.get("demo_role").getAsString())
                    .isEqualTo("fand_kit_navigator");
        });
        assertThat(recipes.get(2)).isInstanceOfSatisfying(CookingRecipe.class, recipe -> {
            assertThat(recipe.experience()).isEqualTo(0.35F);
            assertThat(recipe.cookingTimeTicks()).isEqualTo(120);
            assertThat(recipe.result().customData()).get().extracting(json -> json.get("demo_role").getAsString())
                    .isEqualTo("fand_kit_snack");
        });
        assertThat(recipes.get(3)).isInstanceOfSatisfying(StonecuttingRecipe.class, recipe -> {
            assertThat(recipe.result().customName()).contains(Component.text("Fand Cut Glass", NamedTextColor.AQUA));
            assertThat(recipe.result().customData()).get().extracting(json -> json.get("demo_role").getAsString())
                    .isEqualTo("fand_recipe_glass");
        });
    }

    @Test
    void formatsDemoRecipeSummariesAndSuggestions() {
        var recipe = DemoSupport.demoRecipes(
                new TestItemType(Key.key("minecraft:diamond"), 64),
                new TestItemType(Key.key("minecraft:compass"), 64),
                new TestItemType(Key.key("minecraft:golden_apple"), 64),
                new TestItemType(Key.key("minecraft:glass"), 64)).getFirst();

        assertThat(DemoSupport.recipeSummary(recipe))
                .isEqualTo("fand-test-plugin:component_diamond shapeless -> 2x minecraft:diamond");
        assertThat(DemoSupport.demoRecipeKeySuggestions()).contains(
                "fand-test-plugin:component_diamond",
                "component_diamond",
                "kit_navigator",
                "demo_snack",
                "cut_glass");
    }

    @Test
    void formatsPerformanceSnapshots() {
        assertThat(DemoSupport.formatTickAverages(new TickAverages(20.0, 19.5, 18.25)))
                .isEqualTo("20.00, 19.50, 18.25 (1m, 5m, 15m)");
        assertThat(DemoSupport.formatMetricStatistics(new MetricStatistics(4.0, 1.0, 12.5, 3.0)))
                .isEqualTo("avg 4.00 / min 1.00 / max 12.50 / median 3.00");
    }

    private static ItemStack stack(String key) {
        return new ItemStack(new TestItemType(Key.key(key), 64), 1);
    }

    private static Set<Class<? extends Event>> subscribedEventTypes(Class<?>... listenerTypes) {
        var subscribed = new java.util.LinkedHashSet<Class<? extends Event>>();
        for (var listenerType : listenerTypes) {
            for (var method : listenerType.getDeclaredMethods()) {
                if (method.getAnnotation(Subscribe.class) == null) {
                    continue;
                }
                assertThat(method.getParameterCount())
                        .as(listenerType.getSimpleName() + "#" + method.getName())
                        .isEqualTo(1);
                assertThat(Event.class.isAssignableFrom(method.getParameterTypes()[0]))
                        .as(listenerType.getSimpleName() + "#" + method.getName())
                        .isTrue();
                @SuppressWarnings("unchecked")
                var eventType = (Class<? extends Event>) method.getParameterTypes()[0];
                subscribed.add(eventType);
            }
        }
        return subscribed;
    }

    private record TestItemType(Key key, int maxStackSize) implements ItemType {
    }

    private static final class FakeCommandRegistry implements CommandRegistry {

        private final Map<String, RegisteredCommand> commands = new LinkedHashMap<>();

        FakeCommandRegistry(List<SelfTestCommand.ExpectedCommand> expectedCommands) {
            for (var expected : expectedCommands) {
                var descriptor = new CommandDescriptor(
                        "fand-test-plugin",
                        expected.label(),
                        List.of(),
                        List.of("scope"),
                        expected.aliases(),
                        expected.permission());
                var command = new FakeRegisteredCommand(descriptor);
                commands.put(expected.label(), command);
                expected.aliases().forEach(alias -> commands.put(alias, command));
            }
        }

        @Override
        public CommandRegistration register(CommandDescriptor descriptor, CommandExecutor executor, CommandCompleter completer) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Optional<RegisteredCommand> lookup(String name) {
            return Optional.ofNullable(commands.get(name));
        }

        @Override
        public boolean claims(List<String> tokens) {
            return !tokens.isEmpty() && commands.containsKey(tokens.getFirst());
        }

        @Override
        public Optional<ResolvedCommand> resolve(CommandSender sender, List<String> tokens) {
            return lookup(tokens.getFirst()).map(command -> new ResolvedCommand(command, 1, tokens.getFirst()));
        }

        @Override
        public List<String> suggestions(CommandSender sender, List<String> tokens) {
            return List.of();
        }

        @Override
        public List<RegisteredCommand> visibleCommands(CommandSender sender) {
            return commands.values().stream().distinct().toList();
        }
    }

    private record FakeRegisteredCommand(CommandDescriptor descriptor) implements RegisteredCommand {

        @Override
        public CommandExecutor executor() {
            return (sender, label, args) -> {
            };
        }

        @Override
        public CommandCompleter completer() {
            return (sender, label, args) -> List.of();
        }
    }

    private record FakeEventBus(Set<Class<? extends Event>> listeners) implements EventBus {

        @Override
        public <E extends Event> EventSubscription subscribe(Class<E> type, EventPriority priority, EventListener<E> listener) {
            throw new UnsupportedOperationException();
        }

        @Override
        public <E extends Event> E fire(E event) {
            return event;
        }

        @Override
        public boolean hasListeners(Class<? extends Event> type) {
            return listeners.contains(type);
        }

        @Override
        public <E extends Event> CompletableFuture<E> fireAsync(E event, Executor executor) {
            return CompletableFuture.completedFuture(event);
        }
    }

    private static final class TestSender implements CommandSender {

        @Override
        public String name() {
            return "test";
        }

        @Override
        public void sendMessage(Component message) {
        }

        @Override
        public boolean hasPermission(String permission) {
            return true;
        }
    }
}
