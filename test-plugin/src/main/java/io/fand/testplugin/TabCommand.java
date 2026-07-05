package io.fand.testplugin;

import static io.fand.testplugin.DemoSupport.*;

import io.fand.api.Fand;
import io.fand.api.command.CommandSender;
import io.fand.api.entity.Player;
import io.fand.api.plugin.PluginContext;
import io.fand.api.world.World;
import java.util.ArrayList;
import java.util.List;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

@TestCommand(label = "fandtab", arguments = {"player", "message"}, aliases = {"ftab"}, permission = "fand.testplugin.tab")
final class TabCommand implements TestCommandHandler, TestCommandTabHandler {

    private final PluginContext context;

    TabCommand(PluginContext context) {
        this.context = context;
    }

    @Override
    public void execute(CommandSender sender, String label, List<String> args) {
        TargetedArgs targeted = targetedArgs(sender, args, "/fandtab <player> [clear|message...]");
        if (targeted == null) {
            return;
        }
        if (targeted.args().size() == 1 && isClearMode(targeted.args().getFirst())) {
            targeted.player().clearTabList();
            sender.sendMessage(Component.text("Cleared tab header/footer for " + targeted.player().name() + ".", NamedTextColor.GREEN));
            return;
        }
        String text = messageText(targeted.args(), message(context.config(), "messages.tab-header", "Fand tab demo"));
        var location = targeted.player().location();
        targeted.player().sendTabList(
                Component.text(text, NamedTextColor.GOLD),
                Component.text("World " + targeted.player().world().name()
                        + " | XYZ " + location.blockX() + " " + location.blockY() + " " + location.blockZ(),
                        NamedTextColor.GRAY));
        if (targeted.player() != sender) {
            sender.sendMessage(Component.text("Sent tab header/footer to " + targeted.player().name() + ".", NamedTextColor.GREEN));
        }
    }

    @Override
    public List<String> complete(CommandSender sender, String label, List<String> args) {
        if (args.size() <= 1) {
            var values = new ArrayList<>(playerNames());
            if (sender instanceof Player) {
                values.add(CLEAR_MODE);
            }
            return matching(values, args.isEmpty() ? "" : args.getLast());
        }
        if (args.size() == 2 && Fand.server().player(args.getFirst()).isPresent()) {
            return matching(List.of(CLEAR_MODE), args.getLast());
        }
        return List.of();
    }
}
