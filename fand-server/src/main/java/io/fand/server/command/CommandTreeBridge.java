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
import io.fand.api.command.CommandInfo;
import io.fand.api.command.CommandSender;
import java.util.List;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.commands.arguments.coordinates.Vec3Argument;
import net.minecraft.commands.synchronization.SuggestionProviders;

public final class CommandTreeBridge {

    private CommandTreeBridge() {
    }

    public static void appendToRoot(CommandManager registry, CommandSender sender, RootCommandNode<CommandSourceStack> root) {
        var nodes = new RootCommandNode<CommandSourceStack>();
        for (var entry : registry.visibleEntries(sender)) {
            for (var rootLabel : CommandManager.rootKeys(entry.info())) {
                appendEntry(registry, nodes, entry, rootLabel);
            }
        }
        // This is the outgoing client tree, never the server dispatcher. Claimed
        // roots must not retain vanilla branches that Fand execution shadows.
        root.getChildren().removeIf(child -> registry.claims(List.of(child.getName())));
        nodes.getChildren().forEach(root::addChild);
    }

    private static void appendEntry(
            CommandManager registry,
            RootCommandNode<CommandSourceStack> nodes,
            CommandManager.CommandEntry entry,
            String rootLabel
    ) {
        appendRoot(nodes, entry.info().namespace() + ":" + rootLabel, entry.route());
        if (localRootVisible(registry, entry.info(), rootLabel)) {
            appendRoot(nodes, rootLabel, entry.route());
        }
    }

    private static boolean localRootVisible(CommandManager registry, CommandInfo entry, String rootLabel) {
        return registry.lookup(rootLabel)
                .filter(found -> found.namespace().equals(entry.namespace()))
                .isPresent();
    }

    private static void appendRoot(
            RootCommandNode<CommandSourceStack> nodes,
            String label,
            List<CommandManager.PathToken> route
    ) {
        var builder = literal(label);
        if (optionalTail(route, 0)) {
            builder.executes(context -> 1);
        }
        var branch = builder.build();
        appendPath(branch, route, 0);
        nodes.addChild(branch);
    }

    private static void appendPath(
            CommandNode<CommandSourceStack> parent,
            List<CommandManager.PathToken> route,
            int index
    ) {
        if (index == route.size()) {
            return;
        }
        var token = route.get(index);
        var builder = token.literal()
                ? literal(token.name())
                : argument(token.name(), argumentType(token.metadata(), index == route.size() - 1))
                        .suggests(SuggestionProviders.cast(SuggestionProviders.ASK_SERVER));
        if (optionalTail(route, index + 1)) {
            builder.executes(context -> 1);
        }
        var child = builder.build();
        appendPath(child, route, index + 1);
        parent.addChild(child);
        if (token.optionalArgument()) {
            appendPath(parent, route, index + 1);
        }
    }

    private static boolean optionalTail(List<CommandManager.PathToken> route, int start) {
        for (int index = start; index < route.size(); index++) {
            if (!route.get(index).optionalArgument()) {
                return false;
            }
        }
        return true;
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

    private static LiteralArgumentBuilder<CommandSourceStack> literal(String name) {
        return LiteralArgumentBuilder.literal(name);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static RequiredArgumentBuilder<CommandSourceStack, Object> argument(String name, ArgumentType<?> type) {
        return RequiredArgumentBuilder.argument(name, (ArgumentType) type);
    }
}
