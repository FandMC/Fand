package io.fand.testplugin;

import static io.fand.testplugin.DemoSupport.*;

import io.fand.api.event.Listener;
import io.fand.api.event.Subscribe;
import io.fand.api.event.world.SpawnChangeEvent;
import io.fand.api.event.world.TimeSkipEvent;
import io.fand.api.plugin.PluginContext;
import org.slf4j.Logger;

final class DemoWorldEvents implements Listener {

    private final PluginContext context;
    private final Logger logger;

    DemoWorldEvents(PluginContext context) {
        this.context = context;
        this.logger = context.logger();
    }

    @Subscribe
    public void onSpawnChange(SpawnChangeEvent event) {
        if (context.config().getBoolean("features.log-world-events", true)) {
            logger.info("World spawn changed: {} -> {}",
                    compactLocation(event.previousSpawn()), compactLocation(event.newSpawn()));
        }
    }

    @Subscribe
    public void onTimeSkip(TimeSkipEvent event) {
        if (context.config().getBoolean("features.log-world-events", true)) {
            logger.info("World time skip: {} cause={} {} -> {}",
                    event.world().key().asString(), event.cause(), event.fromTime(), event.toTime());
        }
    }
}
