package io.fand.testplugin;

import io.fand.api.event.Listener;
import io.fand.api.event.Subscribe;
import io.fand.api.event.player.AsyncPlayerPreLoginEvent;
import io.fand.api.event.player.PlayerLoginEvent;
import io.fand.api.event.player.PlayerPreLoginEvent;
import io.fand.api.event.server.ServerListPingEvent;
import io.fand.api.plugin.PluginContext;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.slf4j.Logger;

final class DemoServerEvents implements Listener {

    private final PluginContext context;
    private final Logger logger;

    DemoServerEvents(PluginContext context) {
        this.context = context;
        this.logger = context.logger();
    }

    @Subscribe
    public void onAsyncPlayerPreLogin(AsyncPlayerPreLoginEvent event) {
        if (context.config().getBoolean("features.log-server-events", false)) {
            logger.info("Async pre-login check: {} {} result={}", event.name(), event.address(), event.result());
        }
    }

    @Subscribe
    public void onPlayerPreLogin(PlayerPreLoginEvent event) {
        if (context.config().getBoolean("features.log-server-events", false)) {
            logger.info("Pre-login check: {} {} result={}", event.name(), event.address(), event.result());
        }
    }

    @Subscribe
    public void onPlayerLogin(PlayerLoginEvent event) {
        if (context.config().getBoolean("features.log-server-events", false)) {
            logger.info("Login check: {} {} result={}", event.name(), event.address(), event.result());
        }
    }

    @Subscribe
    public void onServerListPing(ServerListPingEvent event) {
        if (context.config().getBoolean("features.decorate-server-list", true)) {
            event.setMotd(Component.text("Fand test server", NamedTextColor.AQUA)
                    .append(Component.text(" - events online", NamedTextColor.GRAY)));
        }
        if (context.config().getBoolean("features.log-server-events", false)) {
            logger.info("Server ping: online={} max={} hidden={}",
                    event.onlinePlayers(), event.maxPlayers(), event.hidePlayers());
        }
    }
}
