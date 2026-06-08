package io.fand.testplugin;

import static io.fand.testplugin.DemoSupport.*;

import io.fand.api.Fand;
import io.fand.api.command.CommandCompleter;
import io.fand.api.command.CommandExecutor;
import io.fand.api.command.CommandSender;
import io.fand.api.command.CommandSpec;
import io.fand.api.plugin.PluginContext;
import java.util.List;
import net.kyori.adventure.key.InvalidKeyException;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

@CommandSpec(label = "fandrecipe", arguments = {"key"}, aliases = {"frecipe"}, permission = "fand.testplugin.recipe")
final class RecipeCommand implements CommandExecutor, CommandCompleter {

    private final PluginContext context;

    RecipeCommand(PluginContext context) {
        this.context = context;
    }

    @Override
    public void execute(CommandSender sender, String label, List<String> args) {
        if (args.size() > 1) {
            sender.sendMessage(Component.text("Usage: /fandrecipe [recipe]", NamedTextColor.RED));
            return;
        }
        if (args.isEmpty()) {
            var recipes = context.recipes().all();
            sender.sendMessage(Component.text("Fand demo recipes", NamedTextColor.GOLD));
            for (var recipe : recipes) {
                sender.sendMessage(Component.text(recipeSummary(recipe), NamedTextColor.GRAY));
            }
            return;
        }
        try {
            var key = Key.key(keyString(args.getFirst()));
            context.recipes().find(key).ifPresentOrElse(
                    recipe -> sender.sendMessage(Component.text(recipeSummary(recipe), NamedTextColor.GREEN)),
                    () -> sender.sendMessage(Component.text("Unknown demo recipe: " + key.asString(), NamedTextColor.RED)));
        } catch (InvalidKeyException ex) {
            sender.sendMessage(Component.text("Invalid recipe key: " + args.getFirst(), NamedTextColor.RED));
        }
    }

    @Override
    public List<String> complete(CommandSender sender, String label, List<String> args) {
        return args.size() <= 1 ? matching(demoRecipeKeySuggestions(), args.isEmpty() ? "" : args.getLast()) : List.of();
    }
}
