package io.fand.testplugin;

import io.fand.api.command.CommandSender;
import java.util.List;

@FunctionalInterface
interface TestCommandHandler {
    void execute(CommandSender sender, String label, List<String> args);
}
