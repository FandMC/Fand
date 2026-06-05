package io.fand.server.plugin;

import static org.assertj.core.api.Assertions.assertThat;

import io.fand.server.command.CommandManager;
import io.fand.server.permission.PermissionManager;
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

                public final class CleanupPlugin implements Plugin {
                    @Override
                    public void onEnable(PluginContext context) {
                        context.events().subscribe(CleanupTestEvent.class, event -> log("event"));
                        context.scheduler().runMainRepeating(() -> log("task"), Duration.ZERO, Duration.ofDays(1));
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
}
