package io.fand.server.command;

import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.LongArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.tree.CommandNode;
import com.mojang.brigadier.tree.RootCommandNode;
import io.fand.api.command.CommandArgument;
import io.fand.api.command.CommandArgumentType;
import io.fand.api.command.CommandInfo;
import io.fand.api.command.CommandRegistry;
import io.fand.api.command.CommandSender;
import java.util.LinkedHashMap;
import java.util.List;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.commands.arguments.coordinates.Vec3Argument;
import net.minecraft.commands.synchronization.SuggestionProviders;

public final class CommandTreeBridge {

    private CommandTreeBridge() {
    }

    public static void appendToRoot(CommandRegistry registry, CommandSender sender, RootCommandNode<CommandSourceStack> root) {
        var nodes = new LinkedHashMap<String, CommandNode<CommandSourceStack>>();
        for (var entry : registry.visibleCommands(sender)) {
            for (var rootLabel : rootKeys(entry)) {
                appendEntry(registry, root, nodes, entry, rootLabel);
            }
        }
    }

    private static void appendEntry(
            CommandRegistry registry,
            RootCommandNode<CommandSourceStack> root,
            LinkedHashMap<String, CommandNode<CommandSourceStack>> nodes,
            CommandInfo entry,
            String rootLabel
    ) {
        var namespacedRoot = entry.namespace() + ":" + rootLabel;
        appendPath(
                nodes.computeIfAbsent(namespacedRoot, label -> attach(root, literal(label))),
                entry.path(),
                entry.arguments()
        );

        if (localRootVisible(registry, entry, rootLabel)) {
            appendPath(
                    nodes.computeIfAbsent(rootLabel, label -> attach(root, literal(label))),
                    entry.path(),
                    entry.arguments()
            );
        }
    }

    private static boolean localRootVisible(CommandRegistry registry, CommandInfo entry, String rootLabel) {
        return registry.lookup(rootLabel)
                .filter(found -> found.namespace().equals(entry.namespace()))
                .filter(found -> rootKeys(found).contains(rootLabel))
                .isPresent();
    }

    private static void appendPath(CommandNode<CommandSourceStack> root, List<String> path, List<CommandArgument> arguments) {
        var current = root;
        for (var segment : path) {
            current = attach(current, literal(segment));
        }
        attachArguments(current, arguments);
    }

    private static void attachArguments(CommandNode<CommandSourceStack> node, List<CommandArgument> arguments) {
        var current = node;
        for (int index = 0; index < arguments.size(); index++) {
            var argument = arguments.get(index);
            var existing = current.getChild(argument.name());
            if (existing != null) {
                current = existing;
                continue;
            }
            var child = argument(argument.name(), argumentType(argument, index == arguments.size() - 1))
                    .suggests(SuggestionProviders.cast(SuggestionProviders.ASK_SERVER))
                    .executes(context -> 1)
                    .build();
            current.addChild(child);
            current = child;
        }
    }

    private static ArgumentType<?> argumentType(CommandArgument argument, boolean last) {
        return switch (argument.type()) {
            case BOOLEAN -> BoolArgumentType.bool();
            case INTEGER -> IntegerArgumentType.integer();
            case LONG -> LongArgumentType.longArg();
            case FLOAT -> FloatArgumentType.floatArg();
            case DOUBLE -> DoubleArgumentType.doubleArg();
            case PLAYER -> EntityArgument.player();
            case PLAYERS -> EntityArgument.players();
            case ENTITY -> EntityArgument.entity();
            case ENTITIES -> EntityArgument.entities();
            case LOCATION, VECTOR -> Vec3Argument.vec3();
            case BLOCK_POSITION -> BlockPosArgument.blockPos();
            case ENUM, WORD -> StringArgumentType.word();
            case REGISTRY_KEY -> StringArgumentType.word();
            case GREEDY_STRING -> StringArgumentType.greedyString();
            case STRING -> last ? StringArgumentType.greedyString() : StringArgumentType.word();
        };
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

    private static List<String> rootKeys(CommandInfo info) {
        var roots = new java.util.ArrayList<String>(1 + info.aliases().size());
        roots.add(info.label());
        roots.addAll(info.aliases());
        return roots;
    }

    private static LiteralArgumentBuilder<CommandSourceStack> literal(String name) {
        return LiteralArgumentBuilder.literal(name);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static RequiredArgumentBuilder<CommandSourceStack, Object> argument(String name, ArgumentType<?> type) {
        return RequiredArgumentBuilder.argument(name, (ArgumentType) type);
    }
}
