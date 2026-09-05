package io.fand.server.plugin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import io.fand.api.command.CommandInfo;
import io.fand.api.event.EventSubscription;
import io.fand.api.lifecycle.PluginDisableEvent;
import io.fand.api.lifecycle.PluginEnableEvent;
import io.fand.api.scheduler.Task;
import io.fand.server.command.CommandManager;
import io.fand.server.event.EventDispatcher;
import io.fand.server.permission.PermissionManager;
import io.fand.server.scheduler.TaskScheduler;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

final class PluginLifecycleTest {

    @TempDir
    Path tempDir;

    private Path pluginsDir;
    private final PermissionManager permissions = new PermissionManager();
    private final CommandManager commands = new CommandManager(permissions);
    private final EventDispatcher events = new EventDispatcher();
    private final TaskScheduler scheduler = new TaskScheduler();
    private PluginRuntime runtime;

    @BeforeEach
    void startRuntime() throws IOException {
        pluginsDir = Files.createDirectories(tempDir.resolve("plugins"));
        runtime = newRuntime();
        runtime.loadPlugins();
        runtime.enablePlugins();
    }

    @AfterEach
    void closeRuntime() {
        try {
            runtime.close();
        } finally {
            scheduler.close();
        }
    }

    @Test
    void rollsBackFailedDynamicEnableAndPreservesItsErrorAcrossStatusReads() throws IOException {
        writePlugin("broken", List.of(), "", "throw new IllegalStateException(\"enable failure\");", "");

        var result = runtime.loadPlugin("broken.jar");

        assertThat(result.success()).isFalse();
        assertThat(result.message()).contains("enable failure");
        assertThat(runtime.byId("broken")).isEmpty();
        assertThat(commands.lookup("broken:owned")).isEmpty();
        assertThat(scheduler.tick()).isZero();
        assertThat(runtime.pluginStatus("broken").orElseThrow().lifecycle()).isEqualTo(PluginRuntime.PluginLifecycle.ERROR);
        assertThat(runtime.pluginStatus("broken").orElseThrow().lastError().message()).contains("enable failure");

        writePlugin("broken", List.of(), "", "", "");
        assertThat(runtime.loadPlugin("broken").success()).isTrue();
        assertThat(runtime.isEnabled("broken")).isTrue();
        assertThat(runtime.pluginStatus("broken").orElseThrow().lastError()).isNull();
    }

    @Test
    void rollsBackFailedExplicitEnable() throws IOException {
        runtime.close();
        writePlugin("broken", List.of(), "", "throw new IllegalStateException(\"enable failure\");", "");
        runtime = newRuntime();
        runtime.loadPlugins();

        assertThat(runtime.enablePlugin("broken").success()).isFalse();

        assertThat(runtime.byId("broken")).isEmpty();
        assertThat(commands.lookup("broken:owned")).isEmpty();
        assertThat(scheduler.tick()).isZero();
    }

    @Test
    void reloadsAllAffectedDependentsInOrderAndLeavesUnrelatedPluginsRunning() throws IOException {
        for (var id : List.of("base", "dependent", "leaf", "unrelated")) {
            var depends = switch (id) {
                case "dependent" -> List.of("base");
                case "leaf" -> List.of("dependent");
                default -> List.<String>of();
            };
            writePlugin(id, depends, "", "", "");
            assertThat(runtime.loadPlugin(id).success()).isTrue();
        }
        var oldBase = runtime.byId("base").orElseThrow();
        var oldDependent = runtime.byId("dependent").orElseThrow();
        var oldLeaf = runtime.byId("leaf").orElseThrow();
        var unrelated = runtime.byId("unrelated").orElseThrow();
        var transitions = new ArrayList<String>();
        events.subscribe(PluginDisableEvent.class, event -> transitions.add("disable:" + event.plugin().id()));
        events.subscribe(PluginEnableEvent.class, event -> transitions.add("enable:" + event.plugin().id()));

        assertThat(runtime.reloadPlugin("base", false).success()).isFalse();
        assertThat(transitions).isEmpty();
        assertThat(runtime.reloadPlugin("base", true).success()).isTrue();

        assertThat(transitions).containsExactly("disable:leaf", "disable:dependent", "disable:base",
                "enable:base", "enable:dependent", "enable:leaf");
        assertThat(runtime.byId("base").orElseThrow()).isNotSameAs(oldBase);
        assertThat(runtime.byId("dependent").orElseThrow()).isNotSameAs(oldDependent);
        assertThat(runtime.byId("leaf").orElseThrow()).isNotSameAs(oldLeaf);
        assertThat(runtime.byId("unrelated").orElseThrow()).isSameAs(unrelated);
    }

    @Test
    void reportsUnloadFailureAfterReleasingResources() throws IOException {
        writePlugin("broken", List.of(), "", "", "throw new IllegalStateException(\"disable failure\");");
        assertThat(runtime.loadPlugin("broken").success()).isTrue();

        var result = runtime.unloadPlugin("broken", false);

        assertThat(result.success()).isFalse();
        assertThat(result.message()).contains("disable failure");
        assertThat(runtime.byId("broken")).isEmpty();
        assertThat(commands.lookup("broken:owned")).isEmpty();
        assertThat(scheduler.tick()).isZero();
        assertThat(runtime.pluginStatus("broken").orElseThrow().lastError().message()).contains("disable failure");
    }

    @Test
    void reloadAllNeverLoadsDisabledPlugins() throws IOException {
        writePlugin("disabled", List.of(), "", "", "");
        assertThat(runtime.loadPlugin("disabled").success()).isTrue();
        assertThat(runtime.disablePlugin("disabled", false).success()).isTrue();
        writePlugin("disabled", List.of(), "throw new IllegalStateException(\"must not load\");", "", "");

        assertThat(runtime.reloadAllPlugins().success()).isTrue();

        assertThat(runtime.byId("disabled")).isEmpty();
        assertThat(runtime.pluginStatus("disabled").orElseThrow().lifecycle()).isEqualTo(PluginRuntime.PluginLifecycle.DISABLED);
    }

    @Test
    void rejectsDisablingUnknownPluginWithoutCreatingPhantomState() {
        assertThat(runtime.disablePlugin("missing", false).success()).isFalse();
        assertThat(runtime.pluginStatus("missing")).isEmpty();
    }

    @Test
    void reportsStorageCleanupFailureWithoutLeakingCommandsOrTasks() throws IOException {
        var dataDirectory = Files.createDirectories(pluginsDir.resolve("broken"));
        Files.writeString(dataDirectory.resolve("storage"), "blocks storage directory creation");
        writePlugin("broken", List.of(), "", "context.storage().global().setString(\"dirty\", \"value\");", "");
        assertThat(runtime.loadPlugin("broken").success()).isTrue();

        assertThat(runtime.unloadPlugin("broken", false).success()).isFalse();

        assertThat(runtime.byId("broken")).isEmpty();
        assertThat(commands.lookup("broken:owned")).isEmpty();
        assertThat(scheduler.tick()).isZero();
        assertThat(runtime.pluginStatus("broken").orElseThrow().lastError()).isNotNull();
    }

    @Test
    void startupRollbackPublishesDisabledStateForPreviouslyEnabledPlugins() throws IOException {
        runtime.close();
        writePlugin("base", List.of(), "", "", "");
        writePlugin("broken", List.of("base"), "", "throw new IllegalStateException(\"enable failure\");", "");
        runtime = newRuntime();
        runtime.reconfigure(new PluginRuntime.Options(false, false, false));
        runtime.loadPlugins();

        assertThatThrownBy(runtime::enablePlugins).isInstanceOf(PluginLoadException.class);

        assertThat(runtime.isEnabled("base")).isFalse();
        assertThat(runtime.pluginStatus("base").orElseThrow().lifecycle()).isEqualTo(PluginRuntime.PluginLifecycle.DISABLED);
        assertThat(runtime.pluginStatus("broken").orElseThrow().lifecycle()).isEqualTo(PluginRuntime.PluginLifecycle.ERROR);
        assertThat(commands.lookup("base:owned")).isEmpty();
    }

    @Test
    void shutdownWaitsForInFlightDynamicLoadBeforeReleasingResources() throws Exception {
        writePlugin("hot", List.of(), "", "", "");
        var enabling = new CountDownLatch(1);
        var releaseEnable = new CountDownLatch(1);
        var closing = new CountDownLatch(1);
        events.subscribe(PluginEnableEvent.class, event -> {
            enabling.countDown();
            try {
                if (!releaseEnable.await(10, TimeUnit.SECONDS)) {
                    throw new IllegalStateException("Timed out waiting to complete enable");
                }
            } catch (InterruptedException failure) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException(failure);
            }
        });
        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            var load = executor.submit(() -> runtime.loadPlugin("hot"));
            try {
                assertThat(enabling.await(10, TimeUnit.SECONDS)).isTrue();
                var close = executor.submit(() -> {
                    closing.countDown();
                    runtime.close();
                });
                assertThat(closing.await(10, TimeUnit.SECONDS)).isTrue();
                assertThatThrownBy(() -> close.get(150, TimeUnit.MILLISECONDS)).isInstanceOf(TimeoutException.class);
                releaseEnable.countDown();
                assertThat(load.get(10, TimeUnit.SECONDS).success()).isTrue();
                close.get(10, TimeUnit.SECONDS);
                assertThat(runtime.loaded()).isEmpty();
                assertThat(commands.lookup("hot:owned")).isEmpty();
            } finally {
                releaseEnable.countDown();
            }
        }
    }

    @Test
    void resourceCleanupContinuesAfterFailureAndReportsEveryFailure() {
        var tracker = new PluginResourceTracker();
        var subscription = mock(EventSubscription.class);
        var firstFailure = new IllegalStateException("unsubscribe failure");
        doThrow(firstFailure).when(subscription).unregister();
        tracker.track(subscription);
        tracker.track(commands.register("owned", command -> command.namespace("test").executes(context -> {})), (CommandInfo) null);
        var task = mock(Task.class);
        var secondFailure = new IllegalStateException("cancel failure");
        doThrow(secondFailure).when(task).cancel();
        tracker.track(task);

        assertThatThrownBy(tracker::close).hasCause(firstFailure).satisfies(failure ->
                assertThat(failure.getSuppressed()).contains(secondFailure));

        assertThat(commands.lookup("owned")).isEmpty();
        verify(task).cancel();
        tracker.close();
        verify(subscription).unregister();
    }

    private PluginRuntime newRuntime() {
        return new PluginRuntime(pluginsDir, pluginsDir, getClass().getClassLoader(), commands, events, permissions, scheduler);
    }

    private void writePlugin(String id, List<String> depends, String onLoad, String onEnable, String onDisable) throws IOException {
        var source = """
                package testplugins.%s;
                import io.fand.api.plugin.Plugin;
                import io.fand.api.plugin.PluginContext;
                public final class TestPlugin implements Plugin {
                    @Override public void onLoad(PluginContext context) { %s }
                    @Override public void onEnable(PluginContext context) {
                        context.commands().register("owned", command -> command.executes(sender -> {}));
                        context.scheduler().runMain(() -> {});
                        %s
                    }
                    @Override public void onDisable(PluginContext context) { %s }
                }
                """.formatted(id, onLoad, onEnable, onDisable);
        PluginRuntimeTestSupport.createPluginJar(tempDir, pluginsDir.resolve(id + ".jar"),
                PluginRuntimeTestSupport.descriptorJson(id, "testplugins." + id + ".TestPlugin", depends),
                Map.of("testplugins/" + id + "/TestPlugin.java", source), List.of());
    }
}
