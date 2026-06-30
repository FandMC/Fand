package io.fand.server.plugin;

import io.fand.api.nms.NmsAccess;
import io.fand.api.nms.NmsHook;
import io.fand.api.nms.NmsHookRegistration;
import io.fand.api.nms.NmsHookService;
import io.fand.api.nms.NmsService;
import io.fand.api.service.ServicePriority;
import io.fand.server.nms.FandNmsService;
import java.util.List;
import java.util.Objects;
import net.kyori.adventure.key.Key;

final class PluginNmsService implements NmsService {

    private final NmsService delegate;
    private final PluginResourceTracker tracker;
    private final String owner;

    PluginNmsService(NmsService delegate, PluginResourceTracker tracker, String owner) {
        this.delegate = Objects.requireNonNull(delegate, "delegate");
        this.tracker = Objects.requireNonNull(tracker, "tracker");
        this.owner = Objects.requireNonNull(owner, "owner");
    }

    @Override
    public NmsAccess access() {
        return delegate.access();
    }

    @Override
    public NmsHookService hooks() {
        return new PluginNmsHookService(delegate.hooks());
    }

    private final class PluginNmsHookService implements NmsHookService {

        private final NmsHookService hooks;

        private PluginNmsHookService(NmsHookService hooks) {
            this.hooks = Objects.requireNonNull(hooks, "hooks");
        }

        @Override
        public NmsHookRegistration register(Key hook, NmsHook handler) {
            return register(hook, handler, ServicePriority.NORMAL);
        }

        @Override
        public NmsHookRegistration register(Key hook, NmsHook handler, ServicePriority priority) {
            if (delegate instanceof FandNmsService registry) {
                return tracker.track(registry.registerHook(hook, handler, priority, owner));
            }
            return tracker.track(hooks.register(hook, handler, priority));
        }

        @Override
        public List<NmsHookRegistration> hooks(Key hook) {
            return hooks.hooks(hook);
        }
    }
}
