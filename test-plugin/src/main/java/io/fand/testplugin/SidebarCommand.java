package io.fand.testplugin;

import static io.fand.testplugin.DemoSupport.*;

import io.fand.api.Fand;
import io.fand.api.command.CommandCompleter;
import io.fand.api.command.CommandExecutor;
import io.fand.api.command.CommandSender;
import io.fand.api.command.CommandSpec;
import io.fand.api.entity.Player;
import java.util.ArrayList;
import java.util.List;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

@CommandSpec(label = "fandsidebar", arguments = {"player", "mode"}, aliases = {"fsidebar"}, permission = "fand.testplugin.sidebar")
final class SidebarCommand implements CommandExecutor, CommandCompleter {

    @Override
    public void execute(CommandSender sender, String label, List<String> args) {
        TargetedArgs targeted = targetedArgs(sender, args, "/fandsidebar <player> [show|clear]");
        if (targeted == null) {
            return;
        }
        String mode = targeted.args().isEmpty() ? SHOW_MODE : targeted.args().getFirst();
        if (isClearMode(mode)) {
            targeted.player().clearSidebar();
            sender.sendMessage(Component.text("Cleared sidebar for " + targeted.player().name() + ".", NamedTextColor.GREEN));
            return;
        }
        if (!isShowMode(mode)) {
            sender.sendMessage(Component.text("Usage: /fandsidebar <player> [show|clear]", NamedTextColor.RED));
            return;
        }
        targeted.player().showSidebar(demoSidebar(targeted.player()));
        if (targeted.player() != sender) {
            sender.sendMessage(Component.text("Shown sidebar for " + targeted.player().name() + ".", NamedTextColor.GREEN));
        }
    }

    @Override
    public List<String> complete(CommandSender sender, String label, List<String> args) {
        if (args.size() <= 1) {
            var values = new ArrayList<>(playerNames());
            if (sender instanceof Player) {
                values.addAll(List.of(SHOW_MODE, CLEAR_MODE));
            }
            return matching(values, args.isEmpty() ? "" : args.getLast());
        }
        if (args.size() == 2 && Fand.server().player(args.getFirst()).isPresent()) {
            return matching(List.of(SHOW_MODE, CLEAR_MODE), args.getLast());
        }
        return List.of();
    }
}
