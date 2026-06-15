package io.fand.server.plugin;

import io.fand.api.entity.Player;
import io.fand.api.messaging.PluginMessageChannel;
import io.fand.api.messaging.PluginMessageDirection;
import io.fand.api.messaging.PluginMessageHandler;
import io.fand.api.messaging.PluginMessageRegistration;
import io.fand.api.messaging.PluginMessaging;
import java.util.Collection;
import java.util.Objects;
import net.kyori.adventure.key.Key;

public final class PluginPluginMessaging implements PluginMessaging {

    private final PluginMessaging delegate;
    private final PluginResourceTracker tracker;

    public PluginPluginMessaging(PluginMessaging delegate, PluginResourceTracker tracker) {
        this.delegate = Objects.requireNonNull(delegate, "delegate");
        this.tracker = Objects.requireNonNull(tracker, "tracker");
    }

    @Override
    public Collection<PluginMessageChannel> channels() {
        return delegate.channels();
    }

    @Override
    public PluginMessageRegistration register(Key channel, PluginMessageDirection direction) {
        return tracker.track(delegate.register(channel, direction));
    }

    @Override
    public PluginMessageRegistration register(Key channel, PluginMessageDirection direction, PluginMessageHandler handler) {
        return tracker.track(delegate.register(channel, direction, handler));
    }

    @Override
    public void send(Player player, Key channel, byte[] payload) {
        delegate.send(player, channel, payload);
    }
}
