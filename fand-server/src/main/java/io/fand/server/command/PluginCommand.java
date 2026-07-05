package io.fand.server.command;

import io.fand.api.command.Arguments;
import io.fand.api.command.CommandContext;
import io.fand.api.command.CommandInfo;
import io.fand.api.command.CommandRegistry;
import io.fand.api.command.CommandSender;
import io.fand.api.permission.PermissionDescriptor;
import io.fand.server.plugin.PluginRuntime;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

public final class PluginCommand {

    private final io.fand.server.FandServer server;

    public PluginCommand(io.fand.server.FandServer server) {
        this.server = server;
    }

    public void register(CommandRegistry commands) {
        commands.register("plugin", command -> command
                .namespace("fand")
                .executes(context -> sendHelp(context.sender()))
                .literal("help", help -> help.executes(context -> sendHelp(context.sender())))
                .literal("list", list -> list
                        .permission("fand.command.plugin.list")
                        .executes(context -> sendPluginList(context.sender(), server.pluginRuntime(), false))
                        .argument("flag", Arguments.word().optional().suggests("--all"), flag -> flag
                                .suggests(context -> List.of("--all"))
                                .executes(context -> {
                                    if (invalidFlag(context, "flag", "--all")) {
                                        return;
                                    }
                                    sendPluginList(context.sender(), server.pluginRuntime(), true);
                                })))
                .literal("info", info -> pluginArgument(info, "fand.command.plugin.info",
                        plugin -> sendInfo(plugin.sender(), plugin.value())))
                .literal("status", status -> pluginArgument(status, "fand.command.plugin.status",
                        plugin -> sendStatus(plugin.sender(), plugin.value())))
                .literal("load", load -> load
                        .permission("fand.command.plugin.load")
                        .argument("plugin", Arguments.word(), plugin -> plugin
                                .suggests(context -> server.pluginRuntime().loadSuggestions())
                                .executes(context -> operate(context, server.pluginRuntime()::loadPlugin))))
                .literal("unload", unload -> cascadeOperation(unload, false, "fand.command.plugin.unload",
                        (id, cascade) -> server.pluginRuntime().unloadPlugin(id, cascade)))
                .literal("reload", reload -> cascadeOperation(reload, true, "fand.command.plugin.reload",
                        (id, cascade) -> server.pluginRuntime().reloadPlugin(id, cascade)))
                .literal("enable", enable -> enable
                        .permission("fand.command.plugin.enable")
                        .argument("plugin", Arguments.word(), plugin -> plugin
                                .suggests(context -> server.pluginRuntime().pluginIdSuggestions())
                                .executes(context -> operate(context, server.pluginRuntime()::enablePlugin))))
                .literal("disable", disable -> cascadeOperation(disable, false, "fand.command.plugin.disable",
                        (id, cascade) -> server.pluginRuntime().disablePlugin(id, cascade)))
                .literal("depends", depends -> pluginArgument(depends, "fand.command.plugin.depends",
                        status -> sendStringList(status.sender(), "Dependencies", status.value().dependencies())))
                .literal("dependents", dependents -> pluginArgument(dependents, "fand.command.plugin.dependents",
                        status -> sendStringList(status.sender(), "Dependents", status.value().dependents())))
                .literal("commands", commandList -> pluginArgument(commandList, "fand.command.plugin.commands",
                        status -> sendCommands(status.sender(), status.value().commands())))
                .literal("permissions", permissions -> pluginArgument(permissions, "fand.command.plugin.permissions",
                        status -> sendPermissions(status.sender(), status.value().permissions())))
                .literal("errors", errors -> errors
                        .permission("fand.command.plugin.errors")
                        .executes(context -> sendErrors(context.sender(), null))
                        .argument("plugin", Arguments.word().optional(), plugin -> plugin
                                .suggests(context -> server.pluginRuntime().pluginIdSuggestions())
                                .executes(context -> sendErrors(context.sender(), context.string("plugin"))))));
    }

    private void pluginArgument(
            io.fand.api.command.CommandBuilder builder,
            String permission,
            java.util.function.Consumer<PluginStatusContext> consumer
    ) {
        builder.permission(permission)
                .argument("plugin", Arguments.word(), plugin -> plugin
                        .suggests(context -> server.pluginRuntime().pluginIdSuggestions())
                        .executes(context -> withPlugin(context, consumer)));
    }

    private void cascadeOperation(
            io.fand.api.command.CommandBuilder builder,
            boolean suggestAll,
            String permission,
            CascadeOperation operation
    ) {
        builder.permission(permission)
                .argument("plugin", Arguments.word(), plugin -> plugin
                        .suggests(context -> suggestAll ? withAll(server.pluginRuntime().pluginIdSuggestions()) : server.pluginRuntime().pluginIdSuggestions())
                        .executes(context -> operate(context, id -> operation.apply(id, false)))
                        .argument("flag", Arguments.word().optional().suggests("--cascade"), flag -> flag
                                .suggests(context -> List.of("--cascade"))
                                .executes(context -> {
                                    if (invalidFlag(context, "flag", "--cascade")) {
                                        return;
                                    }
                                    operate(context, id -> operation.apply(id, true));
                                })));
    }

    private boolean invalidFlag(CommandContext context, String name, String expected) {
        var actual = context.string(name);
        if (expected.equals(actual)) {
            return false;
        }
        context.sender().sendMessage(Component.text("Unknown flag: " + actual, NamedTextColor.RED));
        return true;
    }

    static void sendPluginList(CommandSender sender, PluginRuntime runtime, boolean includeAvailable) {
        var statuses = runtime.pluginStatuses(includeAvailable);
        if (statuses.isEmpty()) {
            sender.sendMessage(Component.text("Plugins (0): none", NamedTextColor.YELLOW));
            return;
        }
        var line = Component.text("Plugins (" + statuses.size() + "): ", NamedTextColor.GRAY);
        for (int i = 0; i < statuses.size(); i++) {
            var status = statuses.get(i);
            if (i > 0) {
                line = line.append(Component.text(", ", NamedTextColor.GRAY));
            }
            line = line.append(Component.text(status.id(), color(status.lifecycle())));
        }
        sender.sendMessage(line);
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage(Component.text("/plugins, /pl", NamedTextColor.YELLOW)
                .append(Component.text(" - list plugins", NamedTextColor.GRAY)));
        sender.sendMessage(Component.text("/plugin list [--all]", NamedTextColor.YELLOW));
        sender.sendMessage(Component.text("/plugin info|status <id>", NamedTextColor.YELLOW));
        sender.sendMessage(Component.text("/plugin load <id|file.jar>", NamedTextColor.YELLOW));
        sender.sendMessage(Component.text("/plugin unload|reload|disable <id> [--cascade]", NamedTextColor.YELLOW));
        sender.sendMessage(Component.text("/plugin enable <id>", NamedTextColor.YELLOW));
        sender.sendMessage(Component.text("/plugin depends|dependents|commands|permissions <id>", NamedTextColor.YELLOW));
        sender.sendMessage(Component.text("/plugin errors [id]", NamedTextColor.YELLOW));
    }

    private void sendInfo(CommandSender sender, PluginRuntime.PluginStatus status) {
        var descriptor = status.descriptor();
        sender.sendMessage(Component.text("Plugin " + status.id(), color(status.lifecycle())));
        sender.sendMessage(Component.text("  Status: " + status.lifecycle() + ", enabled=" + status.enabled(), NamedTextColor.GRAY));
        if (descriptor != null) {
            sender.sendMessage(Component.text("  Version: " + descriptor.version(), NamedTextColor.GRAY));
            sender.sendMessage(Component.text("  API: " + descriptor.apiVersion(), NamedTextColor.GRAY));
            sendOptionalInfo(sender, "  Description: ", descriptor.description());
            sendOptionalInfo(sender, "  Website: ", descriptor.website());
            sendOptionalInfo(sender, "  License: ", descriptor.license());
            sender.sendMessage(Component.text("  Main: " + descriptor.mainClass(), NamedTextColor.GRAY));
            sender.sendMessage(Component.text("  Authors: " + emptyJoin(descriptor.authors()), NamedTextColor.GRAY));
        }
        sender.sendMessage(Component.text("  Jar: " + value(status.jarPath()), NamedTextColor.GRAY));
        sender.sendMessage(Component.text("  Data: " + status.dataDirectory(), NamedTextColor.GRAY));
        sender.sendMessage(Component.text("  Depends: " + emptyJoin(status.dependencies()), NamedTextColor.GRAY));
        sender.sendMessage(Component.text("  Required by: " + emptyJoin(status.dependents()), NamedTextColor.GRAY));
        if (status.lastError() != null) {
            sender.sendMessage(Component.text("  Last error: [" + status.lastError().phase() + "] " + status.lastError().message(), NamedTextColor.RED));
        }
    }

    private static void sendOptionalInfo(CommandSender sender, String label, String value) {
        if (!value.isBlank()) {
            sender.sendMessage(Component.text(label + value, NamedTextColor.GRAY));
        }
    }

    private void sendStatus(CommandSender sender, PluginRuntime.PluginStatus status) {
        sender.sendMessage(Component.text(status.id() + ": " + status.lifecycle(), color(status.lifecycle())));
        sender.sendMessage(Component.text("  loadedAt=" + time(status.loadedAtMillis()), NamedTextColor.GRAY));
        sender.sendMessage(Component.text("  enabledAt=" + time(status.enabledAtMillis()), NamedTextColor.GRAY));
        sender.sendMessage(Component.text("  disabledAt=" + time(status.disabledAtMillis()), NamedTextColor.GRAY));
        if (status.lastError() != null) {
            sender.sendMessage(Component.text("  errorAt=" + time(status.lastError().timestampMillis())
                    + " [" + status.lastError().phase() + "] " + status.lastError().message(), NamedTextColor.RED));
        }
    }

    private void sendCommands(CommandSender sender, List<CommandInfo> commands) {
        if (commands.isEmpty()) {
            sender.sendMessage(Component.text("Commands: none", NamedTextColor.YELLOW));
            return;
        }
        sender.sendMessage(Component.text("Commands:", NamedTextColor.YELLOW));
        for (var command : commands) {
            var path = command.usage();
            var permission = command.permission() == null ? "" : " permission=" + command.permission();
            sender.sendMessage(Component.text("  " + path + permission, NamedTextColor.GRAY));
        }
    }

    private void sendPermissions(CommandSender sender, List<PermissionDescriptor> permissions) {
        if (permissions.isEmpty()) {
            sender.sendMessage(Component.text("Permissions: none", NamedTextColor.YELLOW));
            return;
        }
        sender.sendMessage(Component.text("Permissions:", NamedTextColor.YELLOW));
        for (var permission : permissions) {
            sender.sendMessage(Component.text("  " + permission.node() + " default=" + permission.defaultAccess(), NamedTextColor.GRAY));
        }
    }

    private void sendErrors(CommandSender sender, String pluginId) {
        var runtime = server.pluginRuntime();
        if (pluginId != null) {
            var status = runtime.pluginStatus(pluginId);
            if (status.isEmpty()) {
                sender.sendMessage(Component.text("Plugin not found: " + pluginId, NamedTextColor.RED));
                return;
            }
            sendError(sender, status.get());
            return;
        }
        var found = false;
        for (var status : runtime.pluginStatuses(true)) {
            if (status.lastError() != null) {
                sendError(sender, status);
                found = true;
            }
        }
        if (!found) {
            sender.sendMessage(Component.text("Plugin errors: none", NamedTextColor.GREEN));
        }
    }

    private static void sendError(CommandSender sender, PluginRuntime.PluginStatus status) {
        if (status.lastError() == null) {
            sender.sendMessage(Component.text(status.id() + ": no recent errors", NamedTextColor.GREEN));
            return;
        }
        sender.sendMessage(Component.text(status.id() + ": [" + status.lastError().phase() + "] " + status.lastError().message(), NamedTextColor.RED));
    }

    private void sendStringList(CommandSender sender, String title, List<String> values) {
        sender.sendMessage(Component.text(title + ": " + emptyJoin(values), values.isEmpty() ? NamedTextColor.YELLOW : NamedTextColor.GRAY));
    }

    private void withPlugin(CommandContext context, java.util.function.Consumer<PluginStatusContext> consumer) {
        var pluginId = context.string("plugin");
        var status = server.pluginRuntime().pluginStatus(pluginId);
        if (status.isEmpty()) {
            context.sender().sendMessage(Component.text("Plugin not found: " + pluginId, NamedTextColor.RED));
            return;
        }
        consumer.accept(new PluginStatusContext(context.sender(), status.get()));
    }

    private static void operate(CommandContext context, java.util.function.Function<String, PluginRuntime.PluginOperationResult> operation) {
        var result = operation.apply(context.string("plugin"));
        context.sender().sendMessage(Component.text(result.message(), result.success() ? NamedTextColor.GREEN : NamedTextColor.RED));
    }

    private static NamedTextColor color(PluginRuntime.PluginLifecycle lifecycle) {
        return switch (lifecycle) {
            case ENABLED -> NamedTextColor.GREEN;
            case ERROR -> NamedTextColor.RED;
            case DISABLED, AVAILABLE -> NamedTextColor.YELLOW;
        };
    }

    private static List<String> filter(List<String> values, String prefix) {
        var normalized = prefix.toLowerCase(Locale.ROOT);
        return values.stream()
                .filter(value -> value.toLowerCase(Locale.ROOT).startsWith(normalized))
                .toList();
    }

    private static List<String> withAll(List<String> ids) {
        var values = new ArrayList<String>(ids.size() + 1);
        values.add("all");
        values.addAll(new LinkedHashSet<>(ids));
        return values;
    }

    private static String emptyJoin(List<String> values) {
        return values.isEmpty() ? "none" : String.join(", ", values);
    }

    private static String value(Object value) {
        return value == null ? "unknown" : value.toString();
    }

    private static String time(long timestampMillis) {
        return timestampMillis <= 0 ? "never" : Instant.ofEpochMilli(timestampMillis).toString();
    }

    private record PluginStatusContext(CommandSender sender, PluginRuntime.PluginStatus value) {
    }

    @FunctionalInterface
    private interface CascadeOperation {
        PluginRuntime.PluginOperationResult apply(String id, boolean cascade);
    }
}
