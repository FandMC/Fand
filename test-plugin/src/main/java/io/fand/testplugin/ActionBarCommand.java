package io.fand.testplugin;

import static io.fand.testplugin.DemoSupport.*;

import io.fand.api.command.CommandCompleter;
import io.fand.api.command.CommandExecutor;
import io.fand.api.command.CommandSender;
import io.fand.api.command.CommandSpec;
import io.fand.api.plugin.PluginContext;
import java.util.List;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

@CommandSpec(label = "fandactionbar", arguments = {"player", "message"}, aliases = {"factionbar"}, permission = "fand.testplugin.actionbar")
final class ActionBarCommand implements CommandExecutor, CommandCompleter {

    private final PluginContext context;

    ActionBarCommand(PluginContext context) {
        this.context = context;
    }

    @Override
    public void execute(CommandSender sender, String label, List<String> args) {
        TargetedArgs targeted = targetedArgs(sender, args, "/fandactionbar <player> [message...]");
        if (targeted == null) {
            return;
        }
        String text = messageText(targeted.args(), message(context.config(), "messages.actionbar", "Action bar from test-plugin."));
        targeted.player().sendActionBar(Component.text(text, NamedTextColor.AQUA));
        if (targeted.player() != sender) {
            sender.sendMessage(Component.text("Sent action bar to " + targeted.player().name() + ".", NamedTextColor.GREEN));
        }
    }

    @Override
    public List<String> complete(CommandSender sender, String label, List<String> args) {
        return args.size() <= 1 ? matching(playerNames(), args.isEmpty() ? "" : args.getLast()) : List.of();
    }
}
