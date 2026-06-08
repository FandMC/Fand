package io.fand.testplugin;

import static io.fand.testplugin.DemoSupport.*;

import io.fand.api.event.Listener;
import io.fand.api.event.Subscribe;
import io.fand.api.event.command.CommandExecuteEvent;
import io.fand.api.event.player.PlayerChatEvent;
import io.fand.api.event.player.PlayerCommandPreprocessEvent;
import io.fand.api.event.player.PlayerQuitEvent;
import io.fand.api.plugin.PluginContext;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.slf4j.Logger;

final class DemoCommandEvents implements Listener {

    private final PluginContext context;
    private final Logger logger;
    private final Set<UUID> demoGuiViewers;
    private final Set<UUID> mutedNextMessages = new HashSet<>();

    DemoCommandEvents(PluginContext context, Set<UUID> demoGuiViewers) {
        this.context = context;
        this.logger = context.logger();
        this.demoGuiViewers = demoGuiViewers;
    }

    @Subscribe
    public void onQuit(PlayerQuitEvent event) {
        demoGuiViewers.remove(event.player().uniqueId());
        mutedNextMessages.remove(event.player().uniqueId());
        logger.info("{} left", event.player().name());
    }

    @Subscribe
    public void onCommandPreprocess(PlayerCommandPreprocessEvent event) {
        if (event.command().equalsIgnoreCase(COMMAND_ALIAS_DEMO)) {
            event.setCommand("fanddemo");
            event.player().sendActionBar(Component.text("Rewrote /" + COMMAND_ALIAS_DEMO + " to /fanddemo", NamedTextColor.AQUA));
            return;
        }
        if (event.command().equalsIgnoreCase("stopdemo")) {
            event.setCancelled(true);
            event.player().sendMessage(Component.text("test-plugin cancelled /stopdemo before dispatch.", NamedTextColor.RED));
        }
    }

    @Subscribe
    public void onCommandExecute(CommandExecuteEvent event) {
        if (context.config().getBoolean("features.log-command-execute", false)) {
            logger.info("{} executing /{}", event.sender().name(), event.command());
        }
    }

    @Subscribe
    public void onChat(PlayerChatEvent event) {
        if (event.originalText().equalsIgnoreCase("!where")) {
            var loc = event.player().location();
            event.setCancelled(true);
            event.player().sendMessage(Component.text("You are at " + event.player().world().name()
                    + " " + loc.blockX() + "," + loc.blockY() + "," + loc.blockZ(), NamedTextColor.AQUA));
            return;
        }
        if (context.config().getBoolean("features.mute-next-demo", true)) {
            UUID playerId = event.player().uniqueId();
            if (isMuteNextCommand(event.originalText())) {
                mutedNextMessages.add(playerId);
                event.setCancelled(true);
                event.player().sendMessage(Component.text(
                        message(context.config(), "messages.mute-next-armed", "Your next chat message will be blocked by test-plugin."),
                        NamedTextColor.YELLOW));
                return;
            }
            if (mutedNextMessages.remove(playerId)) {
                event.setCancelled(true);
                event.player().sendMessage(Component.text(
                        message(context.config(), "messages.muted-chat", "Your message was blocked by the test-plugin mute demo."),
                        NamedTextColor.RED));
                return;
            }
        }
        if (context.config().getBoolean("features.chat-prefix", true)) {
            event.setMessage(Component.text("[FandDemo] ", NamedTextColor.LIGHT_PURPLE).append(event.message()));
        }
    }
}
