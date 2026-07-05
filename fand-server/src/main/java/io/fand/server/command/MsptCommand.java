package io.fand.server.command;

import io.fand.api.command.Command;
import io.fand.api.command.CommandContext;
import io.fand.api.command.Default;
import io.fand.api.command.Permission;
import io.fand.api.performance.TickWindowSnapshot;
import java.util.Locale;
import net.kyori.adventure.text.Component;

@Command(value = "mspt", namespace = "fand")
@Permission("fand.command.mspt")
public final class MsptCommand {

    private final io.fand.server.FandServer server;

    public MsptCommand(io.fand.server.FandServer server) {
        this.server = server;
    }

    @Default
    public void execute(CommandContext context) {
        var performance = server.performance();
        context.sender().sendMessage(Component.text(String.format(
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
