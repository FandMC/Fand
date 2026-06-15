package io.fand.server.structure;

import static org.assertj.core.api.Assertions.assertThat;

import io.fand.api.structure.StructurePlacement;
import io.fand.api.structure.StructureVolume;
import io.fand.api.world.Location;
import io.fand.api.world.World;
import java.lang.reflect.Proxy;
import java.util.List;
import net.kyori.adventure.key.Key;
import org.junit.jupiter.api.Test;

final class FandStructureServiceTest {

    private static final Key TEMPLATE = Key.key("fand:test_template");
    private static final World WORLD = world(Key.key("minecraft:overworld"));
    private static final Location ORIGIN = new Location(WORLD, 0.0, 64.0, 0.0, 0.0F, 0.0F);

    @Test
    void returnsEmptyTemplateWithoutAttachedServer() {
        var service = new FandStructureService(() -> null);

        assertThat(service.template(TEMPLATE)).isEmpty();
    }

    @Test
    void asyncOperationsFailWithoutAttachedServer() {
        var service = new FandStructureService(() -> null);
        var volume = new StructureVolume(WORLD, 0, 64, 0, 1, 65, 1);

        assertThat(service.save(TEMPLATE, volume)).isCompletedExceptionally();
        assertThat(service.place(TEMPLATE, ORIGIN, StructurePlacement.defaults())).isCompletedExceptionally();
        assertThat(service.locate(Key.key("minecraft:village_plains"), ORIGIN, 8)).isCompletedExceptionally();
    }

    @Test
    void locateRejectsNegativeRadius() {
        var service = new FandStructureService(() -> null);

        assertThat(service.locate(Key.key("minecraft:village_plains"), ORIGIN, -1)).isCompletedExceptionally();
    }

    private static World world(Key key) {
        return (World) Proxy.newProxyInstance(
                World.class.getClassLoader(),
                new Class<?>[] { World.class },
                (proxy, method, args) -> switch (method.getName()) {
                    case "key" -> key;
                    case "audiences" -> List.of();
                    case "toString" -> "TestWorld[" + key.asString() + "]";
                    case "hashCode" -> System.identityHashCode(proxy);
                    case "equals" -> proxy == args[0];
                    default -> throw new UnsupportedOperationException(method.toString());
                });
    }
}
