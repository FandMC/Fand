package io.fand.server.plugin;

import static org.assertj.core.api.Assertions.assertThat;

import io.fand.server.command.CommandManager;
import io.fand.server.permission.PermissionManager;
import io.fand.server.recipe.FandRecipeRegistry;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

final class PluginCleanupTest {

    @TempDir
    Path tempDir;

    @Test
    void unregistersPluginListenersAndCancelsPluginTasksOnClose() throws IOException {
        var pluginsDir = tempDir.resolve("plugins");
        Files.createDirectories(pluginsDir);
        var logFile = tempDir.resolve("cleanup.log");
        var previousLog = System.getProperty("fand.plugin.test.log");
        System.setProperty("fand.plugin.test.log", logFile.toString());
        try {
            PluginRuntimeTestSupport.createPluginJar(
                    tempDir,
                    pluginsDir.resolve("cleanup.jar"),
                    PluginRuntimeTestSupport.descriptorJson("cleanup", "testplugins.cleanup.CleanupPlugin", List.of()),
                    Map.of("testplugins/cleanup/CleanupPlugin.java", cleanupPluginSource()),
                    List.of()
            );

            var dispatcher = new io.fand.server.event.EventDispatcher();
            var scheduler = new io.fand.server.scheduler.TaskScheduler();
            var runtime = new PluginRuntime(pluginsDir, pluginsDir, getClass().getClassLoader(), new CommandManager(), dispatcher, new PermissionManager(), scheduler);
            try {
                runtime.loadPlugins();
                runtime.enablePlugins();

                dispatcher.fire(new CleanupTestEvent());
                scheduler.tick();
                scheduler.tick();
            } finally {
                runtime.close();
            }

            dispatcher.fire(new CleanupTestEvent());
            scheduler.tick();
            scheduler.tick();
            scheduler.close();

            assertThat(Files.readAllLines(logFile)).containsExactly(
                    "event",
                    "task",
                    "cleanup-disable"
            );
        } finally {
            PluginRuntimeTestSupport.restoreProperty("fand.plugin.test.log", previousLog);
        }
    }

    @Test
    void unregistersPluginRecipesOnClose() throws IOException {
        var pluginsDir = tempDir.resolve("plugins");
        Files.createDirectories(pluginsDir);
        PluginRuntimeTestSupport.createPluginJar(
                tempDir,
                pluginsDir.resolve("recipes.jar"),
                PluginRuntimeTestSupport.descriptorJson("recipes", "testplugins.recipes.RecipesPlugin", List.of()),
                Map.of("testplugins/recipes/RecipesPlugin.java", recipesPluginSource()),
                List.of()
        );

        var recipes = new FandRecipeRegistry();
        var scheduler = new io.fand.server.scheduler.TaskScheduler();
        var runtime = new PluginRuntime(
                pluginsDir,
                pluginsDir,
                getClass().getClassLoader(),
                new CommandManager(),
                new io.fand.server.event.EventDispatcher(),
                new PermissionManager(),
                scheduler,
                recipes,
                PluginRuntime.Options.defaults()
        );
        try {
            runtime.loadPlugins();
            runtime.enablePlugins();

            assertThat(recipes.find(net.kyori.adventure.key.Key.key("recipes:stone_to_diamond"))).isPresent();
        } finally {
            runtime.close();
            scheduler.close();
        }

        assertThat(recipes.find(net.kyori.adventure.key.Key.key("recipes:stone_to_diamond"))).isEmpty();
    }

    private static String cleanupPluginSource() {
        return """
                package testplugins.cleanup;

                import io.fand.api.plugin.Plugin;
                import io.fand.api.plugin.PluginContext;
                import io.fand.server.plugin.CleanupTestEvent;
                import java.io.IOException;
                import java.nio.file.Files;
                import java.nio.file.Path;
                import java.nio.file.StandardOpenOption;
                import java.time.Duration;
                import net.kyori.adventure.key.Key;

                public final class CleanupPlugin implements Plugin {
                    @Override
                    public void onEnable(PluginContext context) {
                        context.events().subscribe(CleanupTestEvent.class, event -> log("event"));
                        context.scheduler().runMainRepeating(() -> log("task"), Duration.ZERO, Duration.ofDays(1));
                        context.scheduler().region().runAfter(
                                Key.key("minecraft:overworld"),
                                0,
                                0,
                                () -> log("region-task"),
                                Duration.ofSeconds(5)
                        );
                    }

                    @Override
                    public void onDisable(PluginContext context) {
                        log("cleanup-disable");
                    }

                    private static void log(String value) {
                        try {
                            Files.writeString(
                                    Path.of(System.getProperty("fand.plugin.test.log")),
                                    value + System.lineSeparator(),
                                    StandardOpenOption.CREATE,
                                    StandardOpenOption.APPEND
                            );
                        } catch (IOException ex) {
                            throw new RuntimeException(ex);
                        }
                    }
                }
                """;
    }

    private static String recipesPluginSource() {
        return """
                package testplugins.recipes;

                import io.fand.api.item.ItemStack;
                import io.fand.api.item.ItemType;
                import io.fand.api.plugin.Plugin;
                import io.fand.api.plugin.PluginContext;
                import io.fand.api.recipe.RecipeIngredient;
                import io.fand.api.recipe.ShapelessRecipe;
                import java.util.List;
                import net.kyori.adventure.key.Key;

                public final class RecipesPlugin implements Plugin {
                    @Override
                    public void onEnable(PluginContext context) {
                        context.recipes().register(new ShapelessRecipe(
                                Key.key("minecraft:stone_to_diamond"),
                                List.of(RecipeIngredient.of(Key.key("minecraft:stone"))),
                                new ItemStack(new TestItemType(Key.key("minecraft:diamond"), 64), 1)
                        ));
                    }

                    private record TestItemType(Key key, int maxStackSize) implements ItemType {
                    }
                }
                """;
    }
}
