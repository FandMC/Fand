package io.fand.testplugin;

import static io.fand.testplugin.DemoSupport.*;

import io.fand.api.Fand;
import io.fand.api.command.CommandSender;
import java.util.List;
import java.util.Locale;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

@TestCommand(label = "fandperf", arguments = {}, aliases = {"fperf"}, permission = "fand.testplugin.performance")
final class PerformanceCommand implements TestCommandHandler {

    @Override
    public void execute(CommandSender sender, String label, List<String> args) {
        var performance = Fand.server().performance();
        sender.sendMessage(Component.text("Fand performance", NamedTextColor.GOLD));
        sender.sendMessage(Component.text("TPS: " + formatTickAverages(performance.ticksPerSecond()), NamedTextColor.GREEN));
        sender.sendMessage(Component.text("MSPT 5s: " + formatMetricStatistics(performance.fiveSeconds().millisecondsPerTick())
                + ", samples=" + performance.fiveSeconds().sampleCount(), NamedTextColor.AQUA));
        sender.sendMessage(Component.text("Utilization 5s: "
                + String.format(Locale.ROOT, "%.1f%%", performance.fiveSeconds().utilization() * 100.0), NamedTextColor.GRAY));
    }
}
