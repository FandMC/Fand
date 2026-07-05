package io.fand.testplugin;

import static io.fand.testplugin.DemoSupport.*;

import io.fand.api.Fand;
import io.fand.api.command.CommandSender;
import io.fand.api.plugin.PluginContext;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

@TestCommand(label = "fandbossbar", arguments = {"player", "progress", "message"}, aliases = {"fbossbar"}, permission = "fand.testplugin.bossbar")
final class BossBarCommand implements TestCommandHandler, TestCommandTabHandler {

    private final PluginContext context;

    BossBarCommand(PluginContext context) {
        this.context = context;
    }

    @Override
    public void execute(CommandSender sender, String label, List<String> args) {
        TargetedArgs targeted = targetedArgs(sender, args, "/fandbossbar <player> [progress] [message...]");
        if (targeted == null) {
            return;
        }
        float progress = 1.0F;
        List<String> messageArgs = targeted.args();
        if (!messageArgs.isEmpty() && isFloat(messageArgs.getFirst())) {
            Float parsed = parseFloat(sender, messageArgs.getFirst(), "progress");
            if (parsed == null) {
                return;
            }
            progress = boundedBossBarProgress(parsed);
            messageArgs = messageArgs.subList(1, messageArgs.size());
        }
        String text = messageText(messageArgs, message(context.config(), "messages.bossbar", "Boss bar from test-plugin."));
        BossBar bar = BossBar.bossBar(
                Component.text(text, NamedTextColor.GOLD),
                progress,
                BossBar.Color.BLUE,
                BossBar.Overlay.PROGRESS);
        targeted.player().showBossBar(bar);
        context.scheduler().runMainAfter(() -> targeted.player().hideBossBar(bar), Duration.ofSeconds(8));
        if (targeted.player() != sender) {
            sender.sendMessage(Component.text("Sent boss bar to " + targeted.player().name() + ".", NamedTextColor.GREEN));
        }
    }

    @Override
    public List<String> complete(CommandSender sender, String label, List<String> args) {
        if (args.size() <= 1) {
            var values = new ArrayList<>(playerNames());
            values.addAll(List.of("0.25", "0.5", "0.75", "1.0"));
            return matching(values, args.isEmpty() ? "" : args.getLast());
        }
        if (args.size() == 2 && Fand.server().player(args.getFirst()).isPresent()) {
            return matching(List.of("0.25", "0.5", "0.75", "1.0"), args.getLast());
        }
        return List.of();
    }
}
