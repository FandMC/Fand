package io.fand.server.command;

import io.fand.api.command.CommandCompleter;
import io.fand.api.command.CommandDescriptor;
import io.fand.api.command.CommandExecutor;
import io.fand.api.command.CommandSender;
import io.fand.api.command.CommandSpec;
import io.fand.api.permission.PermissionDescriptor;
import io.fand.server.plugin.PluginRuntime;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

@CommandSpec(label = "plugin", namespace = "fand", arguments = {"subcommand"})
public final class PluginCommand implements CommandExecutor, CommandCompleter {

    private static final List<String> SUBCOMMANDS = List.of(
            "list",
            "info",
            "status",
            "load",
            "unload",
            "reload",
            "enable",
            "disable",
            "depends",
            "dependents",
            "commands",
            "permissions",
            "errors",
            "help"
    );

    private final io.fand.server.FandServer server;

    public PluginCommand(io.fand.server.FandServer server) {
        this.server = server;
    }

    @Override
    public void execute(CommandSender sender, String label, List<String> args) {
        if (args.isEmpty() || "help".equalsIgnoreCase(args.getFirst())) {
            sendHelp(sender);
            return;
        }

        var runtime = server.pluginRuntime();
        var subcommand = args.getFirst().toLowerCase(Locale.ROOT);
        switch (subcommand) {
            case "list" -> {
                if (requires(sender, "fand.command.plugin.list", "fand.command.plugins")) {
                    sendPluginList(sender, runtime, args.contains("--all"));
                }
            }
            case "info" -> withPlugin(sender, args, "fand.command.plugin.info", status -> sendInfo(sender, status));
            case "status" -> withPlugin(sender, args, "fand.command.plugin.status", status -> sendStatus(sender, status));
            case "load" -> operate(sender, args, "fand.command.plugin.load", runtime::loadPlugin);
            case "unload" -> operate(sender, args, "fand.command.plugin.unload", id -> runtime.unloadPlugin(id, args.contains("--cascade")));
            case "reload" -> operate(sender, args, "fand.command.plugin.reload", id -> runtime.reloadPlugin(id, args.contains("--cascade")));
            case "enable" -> operate(sender, args, "fand.command.plugin.enable", runtime::enablePlugin);
            case "disable" -> operate(sender, args, "fand.command.plugin.disable", id -> runtime.disablePlugin(id, args.contains("--cascade")));
            case "depends" -> withPlugin(sender, args, "fand.command.plugin.depends", status -> sendStringList(sender, "Dependencies", status.dependencies()));
            case "dependents" -> withPlugin(sender, args, "fand.command.plugin.dependents", status -> sendStringList(sender, "Dependents", status.dependents()));
            case "commands" -> withPlugin(sender, args, "fand.command.plugin.commands", status -> sendCommands(sender, status.commands()));
            case "permissions" -> withPlugin(sender, args, "fand.command.plugin.permissions", status -> sendPermissions(sender, status.permissions()));
            case "errors" -> {
                if (requires(sender, "fand.command.plugin.errors")) {
                    sendErrors(sender, args);
                }
            }
            default -> {
                sender.sendMessage(Component.text("Unknown plugin subcommand: " + subcommand, NamedTextColor.RED));
                sendHelp(sender);
            }
        }
    }

    @Override
    public List<String> complete(CommandSender sender, String label, List<String> args) {
        var runtime = server.pluginRuntime();
        if (args.isEmpty()) {
            return SUBCOMMANDS;
        }
        if (args.size() == 1) {
            return filter(SUBCOMMANDS, args.getFirst());
        }
        var subcommand = args.getFirst().toLowerCase(Locale.ROOT);
        if (args.size() == 2) {
            var prefix = args.get(1);
            return switch (subcommand) {
                case "load" -> filter(runtime.loadSuggestions(), prefix);
                case "reload" -> filter(withAll(runtime.pluginIdSuggestions()), prefix);
                case "info", "status", "unload", "enable", "disable", "depends", "dependents", "commands", "permissions", "errors" ->
                        filter(runtime.pluginIdSuggestions(), prefix);
                default -> List.of();
            };
        }
        var last = args.getLast();
        if (("unload".equals(subcommand) || "reload".equals(subcommand) || "disable".equals(subcommand)) && "--cascade".startsWith(last)) {
            return List.of("--cascade");
        }
        if ("list".equals(subcommand) && "--all".startsWith(last)) {
            return List.of("--all");
        }
        return List.of();
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

    private void sendCommands(CommandSender sender, List<CommandDescriptor> commands) {
        if (commands.isEmpty()) {
            sender.sendMessage(Component.text("Commands: none", NamedTextColor.YELLOW));
            return;
        }
        sender.sendMessage(Component.text("Commands:", NamedTextColor.YELLOW));
        for (var command : commands) {
            var path = command.namespace() + ":" + command.label();
            if (!command.subcommands().isEmpty()) {
                path += " " + String.join(" ", command.subcommands());
            }
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

    private void sendErrors(CommandSender sender, List<String> args) {
        var runtime = server.pluginRuntime();
        if (args.size() >= 2 && !args.get(1).startsWith("--")) {
            var status = runtime.pluginStatus(args.get(1));
            if (status.isEmpty()) {
                sender.sendMessage(Component.text("Plugin not found: " + args.get(1), NamedTextColor.RED));
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

    private void withPlugin(
            CommandSender sender,
            List<String> args,
            String permission,
            java.util.function.Consumer<PluginRuntime.PluginStatus> consumer
    ) {
        if (!requires(sender, permission)) {
            return;
        }
        if (args.size() < 2) {
            sender.sendMessage(Component.text("Plugin id is required", NamedTextColor.RED));
            return;
        }
        var status = server.pluginRuntime().pluginStatus(args.get(1));
        if (status.isEmpty()) {
            sender.sendMessage(Component.text("Plugin not found: " + args.get(1), NamedTextColor.RED));
            return;
        }
        consumer.accept(status.get());
    }

    private void operate(
            CommandSender sender,
            List<String> args,
            String permission,
            java.util.function.Function<String, PluginRuntime.PluginOperationResult> operation
    ) {
        if (!requires(sender, permission)) {
            return;
        }
        if (args.size() < 2) {
            sender.sendMessage(Component.text("Plugin id or file is required", NamedTextColor.RED));
            return;
        }
        var result = operation.apply(args.get(1));
        sender.sendMessage(Component.text(result.message(), result.success() ? NamedTextColor.GREEN : NamedTextColor.RED));
    }

    private static boolean requires(CommandSender sender, String permission, String... alternatives) {
        if (sender.hasPermission(permission)) {
            return true;
        }
        for (var alternative : alternatives) {
            if (sender.hasPermission(alternative)) {
                return true;
            }
        }
        sender.sendMessage(Component.text("Missing permission: " + permission, NamedTextColor.RED));
        return false;
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
}
