package io.fand.testplugin;

import static io.fand.testplugin.DemoSupport.*;

import io.fand.api.block.Block;
import io.fand.api.command.CommandSender;
import java.util.List;
import java.util.Locale;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

@TestCommand(label = "fandcomponents", arguments = {"player", "mode"}, aliases = {"fcomponents"}, permission = "fand.testplugin.components")
final class ComponentsCommand implements TestCommandHandler, TestCommandTabHandler {

    @Override
    public void execute(CommandSender sender, String label, List<String> args) {
        TargetedArgs targeted = targetedArgs(sender, args, "/fandcomponents <player> [set|show|clear]");
        if (targeted == null) {
            return;
        }
        var player = targeted.player();
        var mode = targeted.args().isEmpty() ? "show" : targeted.args().getFirst().toLowerCase(Locale.ROOT);
        var location = player.location();
        Block block = location.world().blockAt(location.blockX(), location.blockY() - 1, location.blockZ());
        switch (mode) {
            case "set" -> {
                int blockUses = block.components().value(DEMO_BLOCK_USES).orElse(0) + 1;
                int entityUses = player.components().value(DEMO_ENTITY_USES).orElse(0) + 1;
                block.components().set(DEMO_BLOCK_LABEL, "demo machine owned by " + player.name());
                block.components().set(DEMO_BLOCK_USES, blockUses);
                player.components().set(DEMO_ENTITY_LABEL, "demo tagged player");
                player.components().set(DEMO_ENTITY_USES, entityUses);
                sender.sendMessage(Component.text(
                        "Stored block/entity components for " + player.name()
                                + " at " + block.x() + "," + block.y() + "," + block.z(),
                        NamedTextColor.GREEN));
            }
            case "clear" -> {
                block.components().clear();
                player.components().remove(DEMO_ENTITY_LABEL);
                player.components().remove(DEMO_ENTITY_USES);
                sender.sendMessage(Component.text("Cleared demo components for " + player.name() + ".", NamedTextColor.YELLOW));
            }
            case "show" -> {
                var blockLabel = block.components().value(DEMO_BLOCK_LABEL).orElse("<none>");
                var blockUses = block.components().value(DEMO_BLOCK_USES).orElse(0);
                var entityLabel = player.components().value(DEMO_ENTITY_LABEL).orElse("<none>");
                var entityUses = player.components().value(DEMO_ENTITY_USES).orElse(0);
                sender.sendMessage(Component.text(
                        "Block components: " + blockLabel + " uses=" + blockUses,
                        NamedTextColor.AQUA));
                sender.sendMessage(Component.text(
                        "Entity components: " + entityLabel + " uses=" + entityUses,
                        NamedTextColor.LIGHT_PURPLE));
            }
            default -> sender.sendMessage(Component.text("Usage: /fandcomponents <player> [set|show|clear]", NamedTextColor.RED));
        }
    }

    @Override
    public List<String> complete(CommandSender sender, String label, List<String> args) {
        if (args.size() == 1) {
            return matching(playerNames(), args.getFirst());
        }
        if (args.size() == 2) {
            return matching(List.of("set", "show", "clear"), args.get(1));
        }
        return List.of();
    }
}
