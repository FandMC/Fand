package io.fand.server.command;

import io.fand.api.command.CommandExecutor;
import io.fand.api.command.CommandSender;
import io.fand.api.command.CommandSpec;
import io.fand.api.performance.TickAverages;
import java.util.List;
import java.util.Locale;
import net.kyori.adventure.text.Component;

@CommandSpec(label = "tps", namespace = "fand", arguments = {}, permission = "fand.command.tps")
public final class TpsCommand implements CommandExecutor {

    private final io.fand.server.FandServer server;

    public TpsCommand(io.fand.server.FandServer server) {
        this.server = server;
    }

    @Override
    public void execute(CommandSender sender, String label, List<String> args) {
        var tps = server.performance().ticksPerSecond();
        sender.sendMessage(Component.text("TPS: " + format(tps)));
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
