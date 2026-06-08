package io.fand.testplugin;

import static io.fand.testplugin.DemoSupport.*;

import io.fand.api.command.CommandCompleter;
import io.fand.api.command.CommandExecutor;
import io.fand.api.command.CommandSender;
import io.fand.api.command.CommandSpec;
import io.fand.api.entity.Player;
import io.fand.api.plugin.PluginContext;
import java.util.List;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

@CommandSpec(label = "fandkick", arguments = {"player", "reason"}, aliases = {"fkick"}, permission = "fand.testplugin.kick")
final class KickCommand implements CommandExecutor, CommandCompleter {

    private final PluginContext context;

    KickCommand(PluginContext context) {
        this.context = context;
    }

    @Override
    public void execute(CommandSender sender, String label, List<String> args) {
        if (args.isEmpty()) {
            sender.sendMessage(Component.text("Usage: /fandkick <player> [reason...]", NamedTextColor.RED));
            return;
        }
        Player target = player(sender, args.getFirst());
        if (target == null) {
            return;
        }
        String reason = messageText(args.subList(1, args.size()), message(context.config(), "messages.kick-reason", "Kicked by test-plugin."));
        target.kick(Component.text(reason, NamedTextColor.RED));
        if (target != sender) {
            sender.sendMessage(Component.text("Kicked " + target.name() + ".", NamedTextColor.GREEN));
        }
    }

    @Override
    public List<String> complete(CommandSender sender, String label, List<String> args) {
        return args.size() <= 1 ? matching(playerNames(), args.isEmpty() ? "" : args.getLast()) : List.of();
    }
}
