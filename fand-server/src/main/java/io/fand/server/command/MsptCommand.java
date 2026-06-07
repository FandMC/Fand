package io.fand.server.command;

import io.fand.api.command.CommandExecutor;
import io.fand.api.command.CommandSender;
import io.fand.api.command.CommandSpec;
import io.fand.api.performance.TickWindowSnapshot;
import java.util.List;
import java.util.Locale;
import net.kyori.adventure.text.Component;

@CommandSpec(label = "mspt", namespace = "fand", arguments = {}, permission = "fand.command.mspt")
public final class MsptCommand implements CommandExecutor {

    private final io.fand.server.FandServer server;

    public MsptCommand(io.fand.server.FandServer server) {
        this.server = server;
    }

    @Override
    public void execute(CommandSender sender, String label, List<String> args) {
        var performance = server.performance();
        sender.sendMessage(Component.text(String.format(
                Locale.ROOT,
                "MSPT (avg/min/max): %s, %s, %s (5s, 10s, 1m)",
                format(performance.fiveSeconds()),
                format(performance.tenSeconds()),
                format(performance.oneMinute())
        )));
    }

    private static String format(TickWindowSnapshot window) {
        var mspt = window.millisecondsPerTick();
        return String.format(
                Locale.ROOT,
                "%.2f/%.2f/%.2f",
                mspt.average(),
                mspt.minimum(),
                mspt.maximum());
    }
}
