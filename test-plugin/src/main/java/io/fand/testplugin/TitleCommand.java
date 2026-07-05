package io.fand.testplugin;

import static io.fand.testplugin.DemoSupport.*;

import io.fand.api.Fand;
import io.fand.api.command.CommandSender;
import io.fand.api.plugin.PluginContext;
import java.time.Duration;
import java.util.List;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.title.Title;

@TestCommand(label = "fandtitle", arguments = {"player", "title"}, aliases = {"ftitle"}, permission = "fand.testplugin.title")
final class TitleCommand implements TestCommandHandler, TestCommandTabHandler {

    private final PluginContext context;

    TitleCommand(PluginContext context) {
        this.context = context;
    }

    @Override
    public void execute(CommandSender sender, String label, List<String> args) {
        TargetedArgs targeted = targetedArgs(sender, args, "/fandtitle <player> [title...]");
        if (targeted == null) {
            return;
        }
        var titleText = demoTitle(
                messageText(targeted.args(), message(context.config(), "messages.title", "Fand API")),
                message(context.config(), "messages.title", "Fand API"),
                message(context.config(), "messages.subtitle", "Title demo from test-plugin."));
        targeted.player().showTitle(Title.title(
                Component.text(titleText.title(), NamedTextColor.GOLD),
                Component.text(titleText.subtitle(), NamedTextColor.YELLOW),
                Title.Times.times(Duration.ofMillis(500), Duration.ofSeconds(3), Duration.ofMillis(750))));
        if (targeted.player() != sender) {
            sender.sendMessage(Component.text("Sent title to " + targeted.player().name() + ".", NamedTextColor.GREEN));
        }
    }

    @Override
    public List<String> complete(CommandSender sender, String label, List<String> args) {
        return args.size() <= 1 ? matching(playerNames(), args.isEmpty() ? "" : args.getLast()) : List.of();
    }
}
