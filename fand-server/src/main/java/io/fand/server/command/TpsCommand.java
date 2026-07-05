package io.fand.server.command;

import io.fand.api.command.Command;
import io.fand.api.command.CommandContext;
import io.fand.api.command.Default;
import io.fand.api.command.Permission;
import io.fand.api.performance.TickAverages;
import java.util.Locale;
import net.kyori.adventure.text.Component;

@Command(value = "tps", namespace = "fand")
@Permission("fand.command.tps")
public final class TpsCommand {

    private final io.fand.server.FandServer server;

    public TpsCommand(io.fand.server.FandServer server) {
        this.server = server;
    }

    @Default
    public void execute(CommandContext context) {
        var tps = server.performance().ticksPerSecond();
        context.sender().sendMessage(Component.text("TPS: " + format(tps)));
    }

    static String format(TickAverages averages) {
        return String.format(
                Locale.ROOT,
                "%.2f, %.2f, %.2f (1m, 5m, 15m)",
                averages.oneMinute(),
                averages.fiveMinutes(),
                averages.fifteenMinutes()
        );
    }
}
