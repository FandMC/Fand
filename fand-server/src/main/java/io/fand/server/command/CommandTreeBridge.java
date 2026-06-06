package io.fand.server.command;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.tree.CommandNode;
import com.mojang.brigadier.tree.RootCommandNode;
import io.fand.api.command.CommandRegistry;
import io.fand.api.command.CommandSender;
import io.fand.api.command.RegisteredCommand;
import java.util.LinkedHashMap;
import java.util.List;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.synchronization.SuggestionProviders;

public final class CommandTreeBridge {

    private CommandTreeBridge() {
    }

    public static void appendToRoot(CommandRegistry registry, CommandSender sender, RootCommandNode<CommandSourceStack> root) {
        var nodes = new LinkedHashMap<String, CommandNode<CommandSourceStack>>();
        for (var entry : registry.visibleCommands(sender)) {
            for (var rootLabel : rootKeys(entry.descriptor())) {
                appendEntry(registry, root, nodes, entry, rootLabel);
            }
        }
    }

    private static void appendEntry(
            CommandRegistry registry,
            RootCommandNode<CommandSourceStack> root,
            LinkedHashMap<String, CommandNode<CommandSourceStack>> nodes,
            RegisteredCommand entry,
            String rootLabel
    ) {
        var namespacedRoot = entry.descriptor().namespace() + ":" + rootLabel;
        appendPath(
                nodes.computeIfAbsent(namespacedRoot, label -> attach(root, literal(label))),
                entry.descriptor().subcommands(),
                entry.descriptor().arguments()
        );

        if (registry.lookup(rootLabel).filter(found -> found == entry).isPresent()) {
            appendPath(
                    nodes.computeIfAbsent(rootLabel, label -> attach(root, literal(label))),
                    entry.descriptor().subcommands(),
                    entry.descriptor().arguments()
            );
        }
    }

    private static void appendPath(CommandNode<CommandSourceStack> root, List<String> path, List<String> arguments) {
        var current = root;
        for (var segment : path) {
            current = attach(current, literal(segment));
        }
        attachArguments(current, arguments);
    }

    private static void attachArguments(CommandNode<CommandSourceStack> node, List<String> arguments) {
        var current = node;
        for (int index = 0; index < arguments.size(); index++) {
            var name = arguments.get(index);
            var existing = current.getChild(name);
            if (existing != null) {
                current = existing;
                continue;
            }
            var type = index == arguments.size() - 1 ? StringArgumentType.greedyString() : StringArgumentType.word();
            var child = RequiredArgumentBuilder.<CommandSourceStack, String>argument(name, type)
                    .suggests(SuggestionProviders.cast(SuggestionProviders.ASK_SERVER))
                    .executes(context -> 1)
                    .build();
            current.addChild(child);
            current = child;
        }
    }

    private static CommandNode<CommandSourceStack> attach(CommandNode<CommandSourceStack> parent, LiteralArgumentBuilder<CommandSourceStack> builder) {
        var existing = parent.getChild(builder.getLiteral());
        if (existing != null) {
            return existing;
        }
        var built = builder.executes(context -> 1).build();
        parent.addChild(built);
        return built;
    }

    private static List<String> rootKeys(io.fand.api.command.CommandDescriptor descriptor) {
        var roots = new java.util.ArrayList<String>(1 + descriptor.aliases().size());
        roots.add(descriptor.label());
        roots.addAll(descriptor.aliases());
        return roots;
    }

    private static LiteralArgumentBuilder<CommandSourceStack> literal(String name) {
        return LiteralArgumentBuilder.literal(name);
    }
}
