package io.fand.testplugin;

import static io.fand.testplugin.DemoSupport.*;

import io.fand.api.event.Event;
import io.fand.api.event.EventPriority;
import io.fand.api.event.entity.DamageEventRegistry;
import io.fand.api.event.entity.EntityDamageByBlockEvent;
import io.fand.api.event.entity.EntityDamageByEntityEvent;
import io.fand.api.event.entity.EntityDamageEvent;
import io.fand.api.plugin.PluginContext;
import java.util.LinkedHashSet;
import java.util.Set;

final class DemoDetailedDamageEvents {

    private DemoDetailedDamageEvents() {
    }

    static void registerAll(PluginContext context) {
        for (var type : detailedDamageTypes()) {
            register(context, type);
        }
    }

    static Set<Class<? extends Event>> subscribedEventTypes() {
        return Set.copyOf(detailedDamageTypes());
    }

    private static Set<Class<? extends EntityDamageEvent>> detailedDamageTypes() {
        var types = new LinkedHashSet<Class<? extends EntityDamageEvent>>();
        for (var type : DamageEventRegistry.detailedEventTypes()) {
            if (EntityDamageEvent.class.isAssignableFrom(type)) {
                @SuppressWarnings("unchecked")
                var damageType = (Class<? extends EntityDamageEvent>) type;
                types.add(damageType);
            }
        }
        return types;
    }

    private static <E extends EntityDamageEvent> void register(PluginContext context, Class<E> type) {
        context.events().subscribe(type, EventPriority.OBSERVER, event -> logDetailedDamage(context, type, event));
    }

    private static void logDetailedDamage(
            PluginContext context,
            Class<? extends EntityDamageEvent> subscribedType,
            EntityDamageEvent event) {
        if (event.getClass() != subscribedType
                || !context.config().getBoolean("features.log-detailed-damage-events", false)) {
            return;
        }

        context.logger().info("Detailed damage {}: target={} cause={} amount={} source={}",
                event.getClass().getSimpleName(),
                event.entity().type().key().asString(),
                event.damageCause().asString(),
                trim(event.amount()),
                sourceSummary(event));
    }

    private static String sourceSummary(EntityDamageEvent event) {
        if (event instanceof EntityDamageByEntityEvent byEntity) {
            return "entity " + byEntity.damager().type().key().asString();
        }
        if (event instanceof EntityDamageByBlockEvent byBlock) {
            var block = byBlock.damager();
            return "block " + block.type().key().asString()
                    + " at " + block.x() + "," + block.y() + "," + block.z();
        }
        return event.attacker()
                .map(attacker -> "attacker " + attacker.type().key().asString())
                .or(() -> event.directEntity().map(direct -> "direct " + direct.type().key().asString()))
                .orElse("environment");
    }
}
