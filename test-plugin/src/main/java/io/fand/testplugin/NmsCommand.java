package io.fand.testplugin;

import static io.fand.testplugin.DemoSupport.matching;

import io.fand.api.command.CommandSender;
import java.util.List;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

@TestCommand(label = "fandnms", arguments = {"mode"}, aliases = {"fnms"}, permission = "fand.testplugin.nms")
final class NmsCommand implements TestCommandHandler, TestCommandTabHandler {

    private static final List<String> MODES = List.of("run", "status", "hooks");

    private final NmsDemo demo;

    NmsCommand(NmsDemo demo) {
        this.demo = demo;
    }

    @Override
    public void execute(CommandSender sender, String label, List<String> args) {
        var mode = args.isEmpty() ? "run" : args.getFirst().toLowerCase(java.util.Locale.ROOT);
        switch (mode) {
            case "run" -> sendReport(sender, demo.runCommandSelfTest());
            case "status" -> sendReport(sender, demo.lastReport());
            case "hooks" -> sendHooks(sender);
            default -> sender.sendMessage(Component.text("Usage: /" + label + " [run|status|hooks]", NamedTextColor.YELLOW));
        }
    }

    @Override
    public List<String> complete(CommandSender sender, String label, List<String> args) {
        return args.size() <= 1 ? matching(MODES, args.isEmpty() ? "" : args.getLast()) : List.of();
    }

    private static void sendReport(CommandSender sender, NmsDemo.NmsDemoReport report) {
        sender.sendMessage(Component.text(
                "NMS demo " + (report.success() ? "passed" : "failed") + ": " + report.summary(),
                report.success() ? NamedTextColor.GREEN : NamedTextColor.RED));
        for (var check : report.checks()) {
            sender.sendMessage(Component.text("- " + check, NamedTextColor.GRAY));
        }
        for (var failure : report.failures()) {
            sender.sendMessage(Component.text("- " + failure, NamedTextColor.RED));
        }
    }

    private void sendHooks(CommandSender sender) {
        var registrations = demo.registrations();
        sender.sendMessage(Component.text("NMS demo hooks: " + registrations.size(), NamedTextColor.GOLD));
        for (var registration : registrations) {
            sender.sendMessage(Component.text(
                    "- " + registration.hook().asString()
                            + " owner=" + registration.owner()
                            + " priority=" + registration.priority()
                            + " active=" + registration.active(),
                    NamedTextColor.GRAY));
        }
    }
}
