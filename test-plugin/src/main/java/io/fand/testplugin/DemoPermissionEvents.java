package io.fand.testplugin;

import io.fand.api.event.Listener;
import io.fand.api.event.Subscribe;
import io.fand.api.event.permission.PermissionCheckEvent;
import io.fand.api.plugin.PluginContext;
import org.slf4j.Logger;

final class DemoPermissionEvents implements Listener {

    private final PluginContext context;
    private final Logger logger;

    DemoPermissionEvents(PluginContext context) {
        this.context = context;
        this.logger = context.logger();
    }

    @Subscribe
    public void onPermissionCheck(PermissionCheckEvent event) {
        if (context.config().getBoolean("features.log-permission-checks", false)) {
            logger.info("Permission check: node={} default={} effective={}",
                    event.node(), event.defaultResult(), event.effectiveResult());
        }
    }
}
