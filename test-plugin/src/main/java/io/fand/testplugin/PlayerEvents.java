package io.fand.testplugin;

import static io.fand.testplugin.DemoSupport.*;

import io.fand.api.plugin.PluginContext;
import java.util.Set;
import java.util.UUID;

final class PlayerEvents {

    private PlayerEvents() {
    }

    static void registerAll(PluginContext context, Set<UUID> demoGuiViewers) {
        var events = context.events();
        events.registerListener(new DemoCommandEvents(context, demoGuiViewers));
        events.registerListener(new DemoPlayerEvents(context, demoGuiViewers));
        events.registerListener(new DemoEntityEvents(context));
        events.registerListener(new DemoBlockEvents(context));
        events.registerListener(new DemoInventoryEvents(context, demoGuiViewers));
        events.registerListener(new DemoWorldEvents(context));
        events.registerListener(new DemoServerEvents(context));
        events.registerListener(new DemoPermissionEvents(context));
    }
}
