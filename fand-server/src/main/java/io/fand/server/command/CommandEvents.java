package io.fand.server.command;

import io.fand.api.command.CommandSender;
import io.fand.api.event.command.CommandExecuteEvent;
import io.fand.api.event.command.TabCompleteEvent;
import io.fand.api.event.player.PlayerCommandPreprocessEvent;
import io.fand.api.event.server.ServerCommandEvent;
import io.fand.server.hooks.FandHooks;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Callable;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.level.ServerPlayer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class CommandEvents {

    private static final Logger LOGGER = LoggerFactory.getLogger(CommandEvents.class);
    private static final ThreadLocal<Integer> COMMAND_DEPTH = ThreadLocal.withInitial(() -> 0);
    private static final ThreadLocal<Integer> SUPPRESS_EXECUTE_EVENT = ThreadLocal.withInitial(() -> 0);

    private CommandEvents() {
    }

    public static CommandSender sender(CommandSourceStack source) {
        ServerPlayer player = source.getPlayer();
        var runtime = io.fand.server.Main.runtime();
        if (player != null) {
            var fandPlayer = FandHooks.findPlayer(player.getUUID());
            if (fandPlayer != null) {
                return fandPlayer;
            }
        }
        return new CommandSourceSender(source, runtime.permissions());
    }

    public static Optional<String> firePlayerCommandPreprocess(ServerPlayer player, String command) {
        var bus = FandHooks.events();
        if (!bus.hasListeners(PlayerCommandPreprocessEvent.class)) {
            return Optional.of(stripCommandPrefix(command));
        }
        var fandPlayer = FandHooks.findPlayer(player.getUUID());
        if (fandPlayer == null) {
            return Optional.of(stripCommandPrefix(command));
        }
        var event = new PlayerCommandPreprocessEvent(fandPlayer, command);
        try {
            bus.fire(event);
        } catch (RuntimeException failure) {
            LOGGER.warn("PlayerCommandPreprocessEvent listener failed", failure);
            return Optional.of(stripCommandPrefix(command));
        }
        return event.cancelled() ? Optional.empty() : Optional.of(event.command());
    }

    public static Optional<String> fireCommandExecute(CommandSourceStack source, String command) {
        var bus = FandHooks.events();
        if (SUPPRESS_EXECUTE_EVENT.get() > 0 || !bus.hasListeners(CommandExecuteEvent.class)) {
            return Optional.of(stripCommandPrefix(command));
        }
        var event = new CommandExecuteEvent(sender(source), command);
        try {
            bus.fire(event);
        } catch (RuntimeException failure) {
            LOGGER.warn("CommandExecuteEvent listener failed", failure);
            return Optional.of(stripCommandPrefix(command));
        }
        return event.cancelled() ? Optional.empty() : Optional.of(event.command());
    }

    public static Optional<String> fireServerCommand(CommandSourceStack source, String command) {
        var bus = FandHooks.events();
        if (!bus.hasListeners(ServerCommandEvent.class)) {
            return Optional.of(stripCommandPrefix(command));
        }
        var event = new ServerCommandEvent(sender(source), command);
        try {
            bus.fire(event);
        } catch (RuntimeException failure) {
            LOGGER.warn("ServerCommandEvent listener failed", failure);
            return Optional.of(stripCommandPrefix(command));
        }
        return event.cancelled() ? Optional.empty() : Optional.of(event.commandLine());
    }

    public static Optional<List<String>> fireTabComplete(ServerPlayer player, String buffer, List<String> completions) {
        var bus = FandHooks.events();
        if (!bus.hasListeners(TabCompleteEvent.class)) {
            return Optional.of(completions);
        }
        var fandPlayer = FandHooks.findPlayer(player.getUUID());
        if (fandPlayer == null) {
            return Optional.of(completions);
        }
        var event = new TabCompleteEvent(fandPlayer, buffer, completions);
        try {
            bus.fire(event);
        } catch (RuntimeException failure) {
            LOGGER.warn("TabCompleteEvent listener failed", failure);
            return Optional.of(completions);
        }
        return event.cancelled() ? Optional.empty() : Optional.of(List.copyOf(event.completions()));
    }

    public static void runWithoutCommandExecuteEvent(Runnable task) {
        int previous = SUPPRESS_EXECUTE_EVENT.get();
        SUPPRESS_EXECUTE_EVENT.set(previous + 1);
        try {
            task.run();
        } finally {
            restoreSuppressDepth(previous);
        }
    }

    public static void runInCommandContext(Runnable task) {
        int previous = COMMAND_DEPTH.get();
        COMMAND_DEPTH.set(previous + 1);
        try {
            task.run();
        } finally {
            restoreDepth(previous);
        }
    }

    public static <T> T callInCommandContext(Callable<T> task) throws Exception {
        int previous = COMMAND_DEPTH.get();
        COMMAND_DEPTH.set(previous + 1);
        try {
            return task.call();
        } finally {
            restoreDepth(previous);
        }
    }

    public static boolean inCommandContext() {
        return COMMAND_DEPTH.get() > 0;
    }

    private static void restoreDepth(int previous) {
        if (previous == 0) {
            COMMAND_DEPTH.remove();
        } else {
            COMMAND_DEPTH.set(previous);
        }
    }

    private static void restoreSuppressDepth(int previous) {
        if (previous == 0) {
            SUPPRESS_EXECUTE_EVENT.remove();
        } else {
            SUPPRESS_EXECUTE_EVENT.set(previous);
        }
    }

    private static String stripCommandPrefix(String command) {
        String stripped = command.stripLeading();
        return stripped.startsWith("/") ? stripped.substring(1) : stripped;
    }
}
