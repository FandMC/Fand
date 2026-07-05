package io.fand.testplugin;

import io.fand.api.command.CommandSender;
import java.util.List;

@FunctionalInterface
interface TestCommandTabHandler {
    List<String> complete(CommandSender sender, String label, List<String> args);
}
